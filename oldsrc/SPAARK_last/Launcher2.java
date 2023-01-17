package SPAARK_last;

import battlecode.common.*;

import java.util.Random;

public strictfp class Launcher2 {
    private RobotController rc;
    private MapLocation me;

    private int turnCount = 0;

    private static final Random rng = new Random(2023);
    private static final Direction[] directions = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
    };

    private MapLocation[] headquarters;
    private MapLocation closestHeadquarters;

    private RobotType prioritizedRobotType = RobotType.CARRIER;
    private int amplifierRange = 200;

    private int amplifierID = -1;
    private int launcherID = -1;

    private MapLocation priortizedAmplifierLocation;

    private static int[][] launcherPositions = new int[][]{
        {-1, 2},
        {0, 2},
        {1, 2},
        {2, 1},
        {2, 0},
        {2, -1},
        {1, -2},
        {0, -2},
        {-1, -2},
        {-2, -1},
        {-2, 0},
        {-2, 1},
    };
    
    private int state = 0;
    // state
    // 0 is wander
    // 1 is travelling to amplifier
    // 2 is travelling with amplifier

    public Launcher2(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
            int hqCount = 0;
            for (int i = 1; i <= 4; i++) {
                if (GlobalArray.hasLocation(rc.readSharedArray(i)))
                    hqCount++;
            }
            headquarters = new MapLocation[hqCount];
            for (int i = 0; i < hqCount; i++) {
                headquarters[i] = GlobalArray.parseLocation(rc.readSharedArray(i + 1));
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException at Carrier constructor");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Launcher constructor");
            e.printStackTrace();
        } finally {
            Clock.yield();
        }
        run();
    }
    
    private void run() {
        while (true) {
            try {
                turnCount++;
                me = rc.getLocation();
                if (state == 0) {
                    Motion.spreadRandomly(rc, me, closestHeadquarters);
                    attemptAttack();
                    priortizedAmplifierLocation = null;
                    for (int a = 0;a < 4;a++) {
                        if (rc.readSharedArray(14 + a * 2) >> 11 != 0 && rc.readSharedArray(15 + a * 2) >> 11 != 12) {
                            MapLocation amplifierLocation = GlobalArray.parseLocation(rc.readSharedArray(14 + a * 2));
                            if (amplifierLocation.distanceSquaredTo(me) < amplifierRange) {
                                if (priortizedAmplifierLocation == null) {
                                    priortizedAmplifierLocation = amplifierLocation;
                                    amplifierID = 14 + a * 2;
                                }
                                else if (amplifierLocation.distanceSquaredTo(me) < priortizedAmplifierLocation.distanceSquaredTo(me)) {
                                    priortizedAmplifierLocation = amplifierLocation;
                                    amplifierID = 14 + a * 2;
                                }
                            }
                        }
                    }
                    if (priortizedAmplifierLocation != null) {
                        state = 1;
                        continue;
                    }
                    else {
                        closestHeadquarters = headquarters[0];
                        for (MapLocation hq : headquarters) {
                            if (hq != null) {
                                // if (hq.distanceSquaredTo(me) > rc.getType().visionRadiusSquared) {
                                //     continue;
                                // }
                                if (closestHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                                    closestHeadquarters = hq;
                                }
                            }
                        }
                        Motion.spreadRandomly(rc, me, closestHeadquarters);
                        attemptAttack();
                    }
                }
                else if (state == 1) {
                    int arrayIndex1 = rc.readSharedArray(amplifierID);
                    int arrayIndex2 = rc.readSharedArray(amplifierID + 1);
                    if (arrayIndex1 >> 11 == 0 || arrayIndex2 >> 11 == 12) {
                        state = 0;
                        continue;
                    }
                    Direction direction = me.directionTo(priortizedAmplifierLocation.translate(launcherPositions[arrayIndex2 >> 11][0], launcherPositions[arrayIndex2 >> 11][1]));
                    if (rc.canMove(direction)) {
                        rc.move(direction);
                    }
                    attemptAttack();
                    if (me.equals(priortizedAmplifierLocation.translate(launcherPositions[arrayIndex2 >> 11][0], launcherPositions[arrayIndex2 >> 11][1]))) {
                        launcherID = arrayIndex2 >> 11;
                        arrayIndex2 = arrayIndex2 & 0b0000111111111111 + (launcherID + 1) << 12;
                        rc.writeSharedArray(amplifierID + 1,GlobalArray.toggleBit(arrayIndex2,launcherID));
                        state = 2;
                        continue;
                    }
                }
                else if (state == 2) {
                    int arrayIndex1 = rc.readSharedArray(amplifierID);
                    int arrayIndex2 = rc.readSharedArray(amplifierID + 1);
                    if (arrayIndex1 >> 11 == 0 || arrayIndex2 >> 11 == 12) {
                        state = 0;
                        continue;
                    }
                    Direction direction = me.directionTo(priortizedAmplifierLocation.translate(launcherPositions[arrayIndex2 >> 11][0], launcherPositions[arrayIndex2 >> 11][1]));
                    if (rc.canMove(direction)) {
                        rc.move(direction);
                    }
                    attemptAttack();
                    rc.writeSharedArray(amplifierID + 1,GlobalArray.toggleBit(arrayIndex2,launcherID));
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException at Launcher");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Launcher");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    private void attemptAttack() throws GameActionException {
        RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared,rc.getTeam().opponent());
        if (robotInfo.length > 0) {
            RobotInfo prioritizedRobotInfo = robotInfo[0];
            MapLocation prioritizedRobotInfoLocation = robotInfo[0].getLocation();
            for (RobotInfo w : robotInfo) {
                if (prioritizedRobotInfo.getType() == prioritizedRobotType) {
                    if (w.getType() == prioritizedRobotType
                            && prioritizedRobotInfo.getLocation().distanceSquaredTo(me) > w
                                    .getLocation().distanceSquaredTo(me)) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    }
                } else {
                    if (prioritizedRobotInfo.getLocation().distanceSquaredTo(me) > w.getLocation()
                            .distanceSquaredTo(me)) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    }
                }
            }
            if (rc.canAttack(prioritizedRobotInfoLocation)) {
                rc.setIndicatorString("Attacking");
                rc.attack(prioritizedRobotInfoLocation);
            }
        }
    }
}
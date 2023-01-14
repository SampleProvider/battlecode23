package SPAARK;

import battlecode.common.*;

import java.util.Random;

public strictfp class Launcher {
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

    private RobotType prioritizedRobotType = RobotType.CARRIER;
    private int amplifierRange = 200;

    private int amplifierID = -1;
    private int launcherID = -1;

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

    public Launcher(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
        // } catch (GameActionException e) {
        //     System.out.println("GameActionException at Carrier constructor");
        //     e.printStackTrace();
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
                    if (amplifierID == 0) {
                        MapLocation priortizedAmplifierLocation = null;
                        for (int a = 0;a < 4;a++) {
                            if (rc.readSharedArray(14 + a * 2) >> 11 != 0 && rc.readSharedArray(15 + a * 2) >> 11 != 12) {
                                MapLocation amplifierLocation = GameState.parseLocation(rc.readSharedArray(14 + a * 2));
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
                            while (true) {
                                me = rc.getLocation();
                                int arrayIndex1 = rc.readSharedArray(15 + amplifierID * 2);
                                int arrayIndex2 = rc.readSharedArray(15 + amplifierID * 2);
                                if (arrayIndex1 >> 11 == 0 || arrayIndex2 >> 11 == 12) {
                                    break;
                                }
                                Direction direction = me.directionTo(priortizedAmplifierLocation.translate(launcherPositions[arrayIndex2 >> 11][0], launcherPositions[arrayIndex2 >> 11][1]));
                                if (rc.canMove(direction)) {
                                    rc.move(direction);
                                }
                                if (me.equals(priortizedAmplifierLocation.translate(launcherPositions[arrayIndex2 >> 11][0], launcherPositions[arrayIndex2 >> 11][1]))) {
                                    launcherID = arrayIndex2 >> 11;
                                    arrayIndex2 = arrayIndex2 & 0b0000111111111111 + (launcherID + 1) << 12;
                                    rc.writeSharedArray(15 + amplifierID * 2,GameState.toggleBit(arrayIndex2,launcherID));
                                    state = 1;
                                    break;
                                }
                                Clock.yield();
                            }
                        }
                    }
                    else {

                    }
                }

                int radius = rc.getType().actionRadiusSquared;
                Team opponent = rc.getTeam().opponent();
                RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
                if (enemies.length > 0) {
                    MapLocation toAttack = enemies[0].location;
                    // MapLocation toAttack = rc.getLocation().add(Direction.EAST);
                    if (rc.canAttack(toAttack)) {
                        rc.setIndicatorString("Attacking");
                        rc.attack(toAttack);
                    }
                }

                Direction direction = directions[rng.nextInt(directions.length)];
                if (rc.canMove(direction)) {
                    rc.move(direction);
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
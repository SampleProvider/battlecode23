package SPAARK;

import battlecode.common.*;

import java.util.Random;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public strictfp class Launcher {
    private RobotController rc;
    private MapLocation me;
    GlobalArray gArray = new GlobalArray();

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
    private MapLocation priortizedHeadquarters;
    private MapLocation priortizedOpponentHeadquarters;

    private RobotType prioritizedRobotType = RobotType.CARRIER;
    private int amplifierSensingRange = 100;
    private int amplifierCircleRange = 10;

    private int headquarterCircleRange = 100;
    private int headquarterCircleStuck = 0;

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

    private WellInfo[] wellInfo;

    private boolean clockwiseRotation = true;
    
    private int state = 0;
    // state
    // 0 is wander
    // 1 is travelling to amplifier
    // 2 is travelling with amplifier
    // 3 is defense

    public Launcher(RobotController rc) {
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
                    RobotInfo[] opponentRobots = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                    priortizedOpponentHeadquarters = null;
                    for (RobotInfo r : opponentRobots) {
                        if (r.getType() == RobotType.HEADQUARTERS) {
                            if (priortizedOpponentHeadquarters == null) {
                                priortizedOpponentHeadquarters = r.getLocation();
                                continue;
                            }
                            if (priortizedOpponentHeadquarters.distanceSquaredTo(me) > r.getLocation().distanceSquaredTo(me)) {
                                priortizedOpponentHeadquarters = r.getLocation();
                            }
                        }
                    }
                    if (priortizedOpponentHeadquarters != null) {
                        state = 2;
                        continue;
                    }
                    priortizedHeadquarters = headquarters[0];
                    for (MapLocation hq : headquarters) {
                        if (hq != null) {
                            if (priortizedHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                                priortizedHeadquarters = hq;
                            }
                        }
                    }
                    priortizedAmplifierLocation = null;
                    for (int a = 0;a < 4;a++) {
                        int amplifierArray = rc.readSharedArray(14 + a);
                        if (amplifierArray >> 14 != 0) {
                            MapLocation amplifierLocation = GlobalArray.parseLocation(amplifierArray);
                            if (amplifierLocation.distanceSquaredTo(me) < amplifierSensingRange) {
                                if (priortizedAmplifierLocation == null) {
                                    priortizedAmplifierLocation = amplifierLocation;
                                    amplifierID = 14 + a;
                                }
                                else if (amplifierLocation.distanceSquaredTo(me) < priortizedAmplifierLocation.distanceSquaredTo(me)) {
                                    priortizedAmplifierLocation = amplifierLocation;
                                    amplifierID = 14 + a;
                                }
                            }
                        }
                    }
                    if (priortizedAmplifierLocation != null) {
                        state = 1;
                    }
                    else {
                        priortizedHeadquarters = headquarters[0];
                        for (MapLocation hq : headquarters) {
                            if (hq != null) {
                                if (priortizedHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                                    priortizedHeadquarters = hq;
                                }
                            }
                        }
                        Motion.spreadRandomly(rc, me, priortizedHeadquarters, true);
                        // Motion.spreadCenter(rc, me);
                        attemptAttack();
                    }
                }
                if (state == 1) {
                    int amplifierArray = rc.readSharedArray(amplifierID);
                    rc.setIndicatorString(amplifierID + " " + amplifierArray);
                    if (amplifierArray >> 14 == 0) {
                        state = 0;
                        continue;
                    }
                    priortizedAmplifierLocation = GlobalArray.parseLocation(amplifierArray);
                    if (me.distanceSquaredTo(priortizedAmplifierLocation) <= amplifierCircleRange) {
                        clockwiseRotation = Motion.circleAroundTarget(rc, me, priortizedAmplifierLocation, amplifierCircleRange, clockwiseRotation);
                    }
                    else {
                        clockwiseRotation = Motion.bug(rc, priortizedAmplifierLocation, clockwiseRotation);
                    }
                    attemptAttack();
                }
                if (state == 2) {
                    if (me.distanceSquaredTo(priortizedOpponentHeadquarters) <= 2) {
                        Motion.circleAroundTarget(rc, me, priortizedOpponentHeadquarters);
                    }
                    else {
                        clockwiseRotation = Motion.bug(rc, priortizedOpponentHeadquarters, clockwiseRotation);
                    }
                    attemptAttack();
                }
                if (state == 3) {
                    priortizedHeadquarters = headquarters[0];
                    for (MapLocation hq : headquarters) {
                        if (hq != null) {
                            if (priortizedHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                                priortizedHeadquarters = hq;
                            }
                        }
                    }
                    if (me.distanceSquaredTo(priortizedHeadquarters) <= headquarterCircleRange) {
                        Motion.moveRandomly(rc);
                        // WellInfo[] wellInfo = rc.senseNearbyWells();
                        // if (wellInfo.length > 0) {
                        //     WellInfo prioritizedWellInfo = wellInfo[0];
                        //     MapLocation prioritizedWellInfoLocation = wellInfo[0].getMapLocation();
                        //     for (WellInfo w : wellInfo) {
                        //         if (prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w.getMapLocation()
                        //                 .distanceSquaredTo(me)) {
                        //             prioritizedWellInfo = w;
                        //             prioritizedWellInfoLocation = w.getMapLocation();
                        //         }
                        //     }
                        //     if (me.distanceSquaredTo(prioritizedWellInfoLocation) <= 15) {
                        //         clockwiseRotation = Motion.circleAroundTarget(rc, me, prioritizedWellInfoLocation, 9, clockwiseRotation);
                        //     }
                        //     else {
                        //         clockwiseRotation = Motion.bug(rc, prioritizedWellInfoLocation, clockwiseRotation);
                        //     }
                        // }
                        // else {
                        //     Motion.moveRandomly(rc);
                        // }
                        boolean oldClockwiseRotation = clockwiseRotation;
                        clockwiseRotation = Motion.circleAroundTarget(rc, me, priortizedHeadquarters, headquarterCircleRange, clockwiseRotation);
                        if (oldClockwiseRotation != clockwiseRotation) {
                            headquarterCircleStuck += 1;
                            if (headquarterCircleStuck == 10) {
                                state = 0;
                            }
                        }
                        else {
                            headquarterCircleStuck = 0;
                        }
                    }
                    else {
                        clockwiseRotation = Motion.bug(rc, priortizedHeadquarters, clockwiseRotation);
                    }
                    attemptAttack();
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
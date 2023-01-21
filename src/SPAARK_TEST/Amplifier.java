package SPAARK_TEST;

import battlecode.common.*;

public strictfp class Amplifier {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    private final Direction[] directions = {
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
    private MapLocation prioritizedHeadquarters;
    private RobotType prioritizedRobotType = RobotType.LAUNCHER;

    private MapLocation opponentLocation;

    private int centerRange = 2;
    private boolean arrivedAtCenter = false;
    
    private int amplifierArray;
    protected int amplifierID = 0;

    private boolean clockwiseRotation = true;
    private Direction lastDirection = Direction.CENTER;

    protected StringBuilder indicatorString = new StringBuilder();

    public Amplifier(RobotController rc) {
        try {
            this.rc = rc;
            int hqCount = 0;
            for (int i = GlobalArray.HEADQUARTERS; i < GlobalArray.HEADQUARTERS + GlobalArray.HEADQUARTERS_LENGTH; i++) {
                if (GlobalArray.hasLocation(rc.readSharedArray(i)))
                    hqCount++;
            }
            headquarters = new MapLocation[hqCount];
            for (int i = 0; i < hqCount; i++) {
                headquarters[i] = GlobalArray.parseLocation(rc.readSharedArray(i + GlobalArray.HEADQUARTERS));
            }
            round = rc.getRoundNum();
            amplifierID = 0;
            for (int a = GlobalArray.AMPLIFIERS; a < GlobalArray.AMPLIFIERS + GlobalArray.AMPLIFIERS_LENGTH; a++) {
                if (((rc.readSharedArray(a) >> 14) & 0b1) == 1) {
                    amplifierID = a;
                    rc.writeSharedArray(amplifierID, GlobalArray.setBit(GlobalArray.intifyLocation(rc.getLocation()), 15, round % 2));
                    break;
                }
            }
            if (amplifierID == 0) {
                throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Too many Amplifiers!");
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException at Amplifier constructor");
            e.printStackTrace();
            return;
        } catch (Exception e) {
            System.out.println("Exception at Amplifier constructor");
            e.printStackTrace();
        } finally {
            run();
        }
    }
    
    public void run() {
        while (true) {
            try {
                // if (amplifierID == 0) {
                //     throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Too many Amplifiers!");
                // }
                // amplifierArray = rc.readSharedArray(amplifierID);
                me = rc.getLocation();
                round = rc.getRoundNum();
                prioritizedHeadquarters = headquarters[0];
                for (MapLocation hq : headquarters) {
                    if (hq != null) {
                        if (prioritizedHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                            prioritizedHeadquarters = hq;
                        }
                    }
                }

                indicatorString = new StringBuilder();

                if (me.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2)) <= centerRange) {
                    indicatorString.append("CENT; ");
                    arrivedAtCenter = true;
                }
                RobotInfo[] robotInfo = rc.senseNearbyRobots();
                if (robotInfo.length > 0) {
                    RobotInfo prioritizedRobotInfo = null;
                    MapLocation prioritizedRobotInfoLocation = null;
                    int surroundingLaunchers = 0;
                    for (RobotInfo w : robotInfo) {
                        if (w.getTeam() == rc.getTeam()) {
                            if (w.getType() == RobotType.LAUNCHER) {
                                surroundingLaunchers += 1;
                            }
                            continue;
                        }
                        if (w.getType() == RobotType.HEADQUARTERS) {
                            continue;
                        }
                        if (w.getType() == prioritizedRobotType) {
                            if (prioritizedRobotInfo == null) {
                                prioritizedRobotInfo = w;
                                prioritizedRobotInfoLocation = w.getLocation();
                            }
                            else if (prioritizedRobotInfo.getHealth() > w.getHealth()) {
                                prioritizedRobotInfo = w;
                                prioritizedRobotInfoLocation = w.getLocation();
                            }
                        }
                        else {
                            if (prioritizedRobotInfo == null) {
                                prioritizedRobotInfo = w;
                                prioritizedRobotInfoLocation = w.getLocation();
                            }
                            else if (prioritizedRobotInfo.getHealth() > w.getHealth()) {
                                prioritizedRobotInfo = w;
                                prioritizedRobotInfoLocation = w.getLocation();
                            }
                        }
                    }
                    indicatorString.append("LAU=" + surroundingLaunchers + "; ");
                    if (prioritizedRobotInfoLocation != null) {
                        opponentLocation = prioritizedRobotInfoLocation;
                        if (surroundingLaunchers >= 15) {
                            indicatorString.append("PATH->OP-" + opponentLocation.toString() + "; ");
                            Direction[] bug2array = Motion.bug2(rc, opponentLocation, lastDirection, clockwiseRotation, indicatorString);
                            lastDirection = bug2array[0];
                            if (bug2array[1] == Direction.CENTER) {
                                clockwiseRotation = !clockwiseRotation;
                            }
                        }
                        else {
                            indicatorString.append("1 ");
                            Motion.spreadRandomly(rc, me, opponentLocation);
                        }
                        me = rc.getLocation();
                        rc.writeSharedArray(amplifierID, GlobalArray.setBit(GlobalArray.intifyLocation(me), 15, round % 2));
                        // rc.writeSharedArray(amplifierID, GlobalArray.setBit((amplifierArray & 0b1000000000000000) | GlobalArray.intifyLocation(prioritizedRobotInfoLocation), 15, round % 2));
                    }
                    else {
                        if (arrivedAtCenter && opponentLocation != null && surroundingLaunchers >= 15) {
                            indicatorString.append("PATH->OP-" + opponentLocation.toString() + "; ");
                            Direction[] bug2array = Motion.bug2(rc, opponentLocation, lastDirection, clockwiseRotation, indicatorString);
                            lastDirection = bug2array[0];
                            if (bug2array[1] == Direction.CENTER) {
                                clockwiseRotation = !clockwiseRotation;
                            }
                        }
                        else if (arrivedAtCenter) {
                            indicatorString.append("2 ");
                            Motion.moveRandomly(rc);
                        }
                        else {
                            indicatorString.append("3 ");
                            Motion.spreadCenter(rc, me);
                        }
                        me = rc.getLocation();
                        rc.writeSharedArray(amplifierID, GlobalArray.setBit(GlobalArray.intifyLocation(me), 15, round % 2));
                    }
                }
                else if (arrivedAtCenter) {
                    Motion.moveRandomly(rc);
                    me = rc.getLocation();
                    rc.writeSharedArray(amplifierID, GlobalArray.setBit(GlobalArray.intifyLocation(me), 15, round % 2));
                }
                else {
                    // if (arrivedAtCenter && opponentLocation != null) {
                    // Direction[] bug2array = Motion.bug2(rc, opponentLocation, lastDirection, clockwiseRotation, indicatorString);
                    // lastDirection = bug2array[0];
                    // if (bug2array[1] == Direction.CENTER) {
                    //     clockwiseRotation = !clockwiseRotation;
                    // }
                    // }
                    // else {
                    Motion.spreadCenter(rc, me);
                    // Motion.moveRandomly(rc);
                    // }
                    me = rc.getLocation();
                    rc.writeSharedArray(amplifierID, GlobalArray.setBit(GlobalArray.intifyLocation(me), 15, round % 2));
                }
                // Motion.moveRandomly(rc);
            } catch (GameActionException e) {
                System.out.println("GameActionException at Amplifier");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Amplifier");
                e.printStackTrace();
            } finally {
                indicatorString.append("AMPID=" + amplifierID + "; ");
                rc.setIndicatorString(indicatorString.toString());
                Clock.yield();
            }
        }
    }
}
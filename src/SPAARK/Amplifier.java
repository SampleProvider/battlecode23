package SPAARK;

import battlecode.common.*;

import java.util.Random;

public strictfp class Amplifier {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    private final Random rng = new Random(2023);

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

    protected StringBuilder indicatorString;

    public Amplifier(RobotController rc) {
        try {
            this.rc = rc;
            int hqCount = 0;
            for (int i = 1; i <= 4; i++) {
                if (GlobalArray.hasLocation(rc.readSharedArray(i)))
                    hqCount++;
            }
            headquarters = new MapLocation[hqCount];
            for (int i = 0; i < hqCount; i++) {
                headquarters[i] = GlobalArray.parseLocation(rc.readSharedArray(i + 1));
            }
            int locInt = GlobalArray.intifyLocation(rc.getLocation());
            if (!GlobalArray.hasLocation(rc.readSharedArray(14))) {
                rc.writeSharedArray(14, GlobalArray.setBit(GlobalArray.setBit(locInt, 14, 1), 15, round % 2));
                amplifierID = 14;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(15))) {
                rc.writeSharedArray(15, GlobalArray.setBit(GlobalArray.setBit(locInt, 14, 1), 15, round % 2));
                amplifierID = 15;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(16))) {
                rc.writeSharedArray(16, GlobalArray.setBit(GlobalArray.setBit(locInt, 14, 1), 15, round % 2));
                amplifierID = 16;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(17))) {
                rc.writeSharedArray(17, GlobalArray.setBit(GlobalArray.setBit(locInt, 14, 1), 15, round % 2));
                amplifierID = 17;
            } else {
                throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Too many Amplifiers!");
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException at Carrier constructor");
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
                amplifierArray = rc.readSharedArray(amplifierID);
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
                            if (rc.getMovementCooldownTurns() <= 5) {
                                indicatorString.append("PATH->OP-" + opponentLocation.toString() + "; ");
                                Motion.bug(rc, opponentLocation, indicatorString);
                            }
                        }
                        else {
                            Motion.spreadRandomly(rc, me, opponentLocation);
                        }
                        rc.writeSharedArray(amplifierID, GlobalArray.setBit((amplifierArray & 0b1100000000000000) | GlobalArray.intifyLocation(prioritizedRobotInfoLocation), 15, round % 2));
                    }
                    else {
                        if (arrivedAtCenter && opponentLocation != null && surroundingLaunchers >= 15) {
                            indicatorString.append("PATH->OP-" + opponentLocation.toString() + "; ");
                            Motion.bug(rc, opponentLocation, indicatorString);
                        }
                        else {
                            Motion.spreadCenter(rc, me);
                        }
                        me = rc.getLocation();
                        rc.writeSharedArray(amplifierID, GlobalArray.setBit((amplifierArray & 0b1100000000000000) | GlobalArray.intifyLocation(me), 15, round % 2));
                    }
                }
                else {
                    // if (arrivedAtCenter && opponentLocation != null) {
                    //     Motion.bug(rc, opponentLocation);
                    // }
                    // else {
                    Motion.spreadCenter(rc, me);
                    // }
                    me = rc.getLocation();
                    rc.writeSharedArray(amplifierID, GlobalArray.setBit((amplifierArray & 0b1100000000000000) | GlobalArray.intifyLocation(me), 15, round % 2));
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
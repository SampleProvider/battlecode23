package betterthings;

import battlecode.common.*;
import java.util.Random;

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

    private final Random rng = new Random(2023);

    private MapLocation[] headquarters;
    private MapLocation prioritizedHeadquarters;
    private RobotType prioritizedRobotType = RobotType.LAUNCHER;
    
    private StoredLocations storedLocations;

    private MapLocation opponentLocation;
    
    protected int amplifierID = 0;

    private boolean clockwiseRotation = true;
    private Direction lastDirection = Direction.CENTER;

    private MapLocation randomExploreLocation;
    private int randomExploreTime = 0;
    private final int randomExploreMinKnownLocDistSquared = 81;

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
                System.out.println("[!] Too many Amplifiers! [!]");
            }
            storedLocations = new StoredLocations(rc);
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

                storedLocations.detectWells();
                storedLocations.writeToGlobalArray();

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
                    }
                    if (opponentLocation != null && surroundingLaunchers >= 15) {
                        indicatorString.append("PATH->OP-" + opponentLocation.toString() + "; ");
                        Direction[] bug2array = Motion.bug2(rc, opponentLocation, lastDirection, clockwiseRotation, indicatorString);
                        lastDirection = bug2array[0];
                        if (bug2array[1] == Direction.CENTER) {
                            clockwiseRotation = !clockwiseRotation;
                        }
                    } else {
                        updateRandomExploreLocation();
                        if (randomExploreLocation != null) {
                            indicatorString.append("EXPL-" + randomExploreLocation.toString() + "; ");
                            Direction[] bug2array = Motion.bug2(rc, randomExploreLocation, lastDirection, clockwiseRotation, indicatorString);
                            lastDirection = bug2array[0];
                            if (bug2array[1] == Direction.CENTER) {
                                clockwiseRotation = !clockwiseRotation;
                            }
                        } else {
                            indicatorString.append("RAND");
                            Motion.moveRandomly(rc);
                        }
                    }
                }
                me = rc.getLocation();
                rc.writeSharedArray(amplifierID, GlobalArray.setBit(GlobalArray.intifyLocation(me), 15, round % 2));
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

    private void updateRandomExploreLocation() throws GameActionException {
        if (randomExploreLocation != null) return;
        MapLocation[] knownWells = GlobalArray.getKnownWellLocations(rc);
        int iteration = 0;
        search: while (randomExploreLocation == null && iteration < 16) {
            randomExploreLocation = new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight()));
            for (MapLocation well : knownWells) {
                if (well != null && well.distanceSquaredTo(randomExploreLocation) < randomExploreMinKnownLocDistSquared) {
                    randomExploreLocation = null;
                    continue search;
                }
            }
            iteration++;
        }
    }
}
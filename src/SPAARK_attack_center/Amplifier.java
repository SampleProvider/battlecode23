package SPAARK_attack_center;

import battlecode.common.*;
import java.util.Random;

public strictfp class Amplifier {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    private Random rng = new Random(2023);

    private MapLocation[] headquarters;
    private MapLocation prioritizedHeadquarters;
    private int prioritizedHeadquarterIndex;

    private StoredLocations storedLocations;

    protected int amplifierID = 0;

    private boolean clockwiseRotation = true;
    private Direction lastDirection = Direction.CENTER;

    private boolean isRandomExplorer = false;
    private MapLocation randomExploreLocation;
    private int randomExploreTime = 0;
    private final int randomExploreMinKnownWellDistSquared = 81;
    private final int randomExploreMinKnownHQDistSquared = 144;

    private int lastHealth = 0;

    // 0 - random explore
    // 1 - stand on island
    // 2 - retreat
    // 3 - follow launcher groups
    private int state = 0;

    private StringBuilder indicatorString = new StringBuilder();

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
            int amplifierArray = rc.readSharedArray(GlobalArray.AMPLIFIERCOUNT);
            amplifierID = amplifierArray >> 8;
            rc.writeSharedArray(GlobalArray.AMPLIFIERCOUNT, (amplifierArray & 0b11111111) | ((amplifierArray & 0b1111111100000000) + 0b100000000));
            // for (int a = GlobalArray.AMPLIFIERS; a < GlobalArray.AMPLIFIERS + GlobalArray.AMPLIFIERS_LENGTH; a++) {
            //     if (((rc.readSharedArray(a) >> 14) & 0b1) == 1) {
            //         amplifierID = a;
            //         rc.writeSharedArray(amplifierID, GlobalArray.setBit(GlobalArray.intifyLocation(rc.getLocation()), 15, round % 2));
            //         break;
            //     }
            // }
            // if (amplifierID == 0) {
            //     System.out.println("[!] Too many Amplifiers! [!]");
            // }
            storedLocations = new StoredLocations(rc, headquarters);
            rng = new Random(amplifierID);
            isRandomExplorer = amplifierID % 3 == 0;
            if (!isRandomExplorer) state = 1;
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
                globalArray.parseGameState(rc.readSharedArray(GlobalArray.GAMESTATE));

                storedLocations.detectWells();
                storedLocations.detectOpponentLocations();
                storedLocations.detectIslandLocations();
                storedLocations.detectSymmetry();
                storedLocations.writeToGlobalArray();
                
                indicatorString = new StringBuilder();

                if (rc.getHealth() != lastHealth && rc.getHealth() <= RobotType.AMPLIFIER.health / 2) {
                    state = 2;
                }
                lastHealth = rc.getHealth();

                RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                RobotInfo robot = Attack.senseOpponent(rc, robotInfo);
                if (robot != null && Attack.prioritizedRobot(robot.getType()) >= 3) {
                    storedLocations.storeOpponentLocation(robot.getLocation());
                    // state = 2;
                }

                runState();
                
                robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                robot = Attack.senseOpponent(rc, robotInfo);
                if (robot != null && Attack.prioritizedRobot(robot.getType()) >= 3) {
                    storedLocations.storeOpponentLocation(robot.getLocation());
                    // state = 2;
                }
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

    private void runState() throws GameActionException {
        if (state == 0) {
            MapLocation centerHeadquarters = headquarters[0];
            for (int i = 0; i < headquarters.length; i++) {
                if (headquarters[i] != null) {
                    if (centerHeadquarters.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2)) > headquarters[i].distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2))) {
                        centerHeadquarters = headquarters[i];
                    }
                }
            }
            int symmetry = (globalArray.mapSymmetry() & 0b1) + ((globalArray.mapSymmetry() >> 1) & 0b1) + ((globalArray.mapSymmetry() >> 2) & 0b1);
            indicatorString.append("SYM " + symmetry + "; ");
            if (symmetry == 2) {
                if ((globalArray.mapSymmetry() & 0b1) == 1) {
                    Direction[] bug2array = Motion.bug2(rc, new MapLocation(rc.getMapWidth() - centerHeadquarters.x, centerHeadquarters.y), lastDirection, clockwiseRotation, false, false, indicatorString);
                    lastDirection = bug2array[0];
                    if (bug2array[1] == Direction.CENTER) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                }
                else if (((globalArray.mapSymmetry() >> 1) & 0b1) == 1) {
                    Direction[] bug2array = Motion.bug2(rc, new MapLocation(centerHeadquarters.x, rc.getMapHeight() - centerHeadquarters.y), lastDirection, clockwiseRotation, false, false, indicatorString);
                    lastDirection = bug2array[0];
                    if (bug2array[1] == Direction.CENTER) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                }
                return;
            }
            else if (symmetry == 3) {
                if (rc.getID() % 2 == 0) {
                    Direction[] bug2array = Motion.bug2(rc, new MapLocation(rc.getMapWidth() - centerHeadquarters.x, centerHeadquarters.y), lastDirection, clockwiseRotation, false, false, indicatorString);
                    lastDirection = bug2array[0];
                    if (bug2array[1] == Direction.CENTER) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                }
                else {
                    Direction[] bug2array = Motion.bug2(rc, new MapLocation(centerHeadquarters.x, rc.getMapHeight() - centerHeadquarters.y), lastDirection, clockwiseRotation, false, false, indicatorString);
                    lastDirection = bug2array[0];
                    if (bug2array[1] == Direction.CENTER) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                }
                return;
            }
            updateRandomExploreLocation();
            if (randomExploreLocation != null) {
                if (rc.canSenseLocation(randomExploreLocation) && !rc.sensePassability(randomExploreLocation)) {
                    randomExploreLocation = null;
                    updateRandomExploreLocation();
                    if (randomExploreLocation == null) {
                        indicatorString.append("RAND; ");
                        Motion.moveRandomly(rc);
                        return;
                    }
                }
                indicatorString.append("EXPL-" + randomExploreLocation.toString() + "; ");
                Direction[] bug2array = Motion.bug2(rc, randomExploreLocation, lastDirection, clockwiseRotation, false, true, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
                if (GlobalArray.DEBUG_INFO >= 2) {
                    rc.setIndicatorLine(me, randomExploreLocation, 0, 175, 0);
                } else if (GlobalArray.DEBUG_INFO > 0) {
                    rc.setIndicatorDot(me, 0, 175, 0);
                    rc.setIndicatorDot(randomExploreLocation, 0, 175, 0);
                }
                randomExploreTime++;
                if (randomExploreTime > 50 || randomExploreLocation.distanceSquaredTo(me) <= 4) randomExploreLocation = null;
            } else {
                indicatorString.append("RAND; ");
                Motion.moveRandomly(rc);
            }
        } else if (state == 1) {
            int[] islands = rc.senseNearbyIslands();
            MapLocation prioritizedIslandLocation = null;
            for (int id : islands) {
                if (rc.senseTeamOccupyingIsland(id) == rc.getTeam().opponent()) {
                    MapLocation[] islandLocations = rc.senseNearbyIslandLocations(id);
                    for (MapLocation m : islandLocations) {
                        if (prioritizedIslandLocation == null) {
                            prioritizedIslandLocation = m;
                        } else if (m.distanceSquaredTo(me) < prioritizedIslandLocation.distanceSquaredTo(me)) {
                            prioritizedIslandLocation = m;
                        }
                    }
                }
            }
            if (prioritizedIslandLocation != null) {
                Direction[] bug2array = Motion.bug2(rc, prioritizedIslandLocation, lastDirection, clockwiseRotation, true, true, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
                me = rc.getLocation();
                if (GlobalArray.DEBUG_INFO >= 2) {
                    rc.setIndicatorLine(me, prioritizedIslandLocation, 75, 255, 255);
                } else if (GlobalArray.DEBUG_INFO > 0) {
                    rc.setIndicatorDot(me, 75, 255, 255);
                }
            } else if (rng.nextBoolean()) {
                // get island location from global array
                MapLocation[] islandLocations = GlobalArray.getKnownIslandLocations(rc, rc.getTeam().opponent());
                for (MapLocation m : islandLocations) {
                    if (m == null) {
                        continue;
                    }
                    if (prioritizedIslandLocation == null) {
                        prioritizedIslandLocation = m;
                    }
                    else if (m.distanceSquaredTo(me) < prioritizedIslandLocation.distanceSquaredTo(me)) {
                        prioritizedIslandLocation = m;
                    }
                }
                if (prioritizedIslandLocation != null) {
                    Direction[] bug2array = Motion.bug2(rc, prioritizedIslandLocation, lastDirection, clockwiseRotation, false, true, indicatorString);
                    lastDirection = bug2array[0];
                    if (bug2array[1] == Direction.CENTER) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                    me = rc.getLocation();
                    if (GlobalArray.DEBUG_INFO >= 2) {
                        rc.setIndicatorLine(me, prioritizedIslandLocation, 75, 255, 255);
                    } else if (GlobalArray.DEBUG_INFO > 0) {
                        rc.setIndicatorDot(me, 75, 255, 255);
                    }
                }
                else {
                    Motion.spreadRandomly(rc, me);
                }
            } else {
                state = 0;
                runState();
            }
        } else if (state == 2) {
            if (Attack.senseOpponent(rc, rc.senseNearbyRobots(RobotType.AMPLIFIER.visionRadiusSquared, rc.getTeam().opponent())) == null) {
                state = 0;
                runState();
                return;
            }
            updatePrioritizedHeadquarters();
            indicatorString.append("RET; ");
            Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, false, true, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
            if (prioritizedHeadquarters.distanceSquaredTo(me) <= RobotType.HEADQUARTERS.visionRadiusSquared) {
                state = isRandomExplorer ? 0 : 1;
            }
            me = rc.getLocation();
            if (GlobalArray.DEBUG_INFO >= 4) {
                rc.setIndicatorLine(me, prioritizedHeadquarters, 125, 255, 0);
            } else if (GlobalArray.DEBUG_INFO > 0) {
                rc.setIndicatorDot(me, 125, 255, 0);
            }
        } else if (state == 3) {
            // follow le launcher
            RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
            MapLocation prioritizedLauncherLocation = null;
            for (RobotInfo r : robots) {
                if (r.getType() == RobotType.LAUNCHER) {
                    prioritizedLauncherLocation = r.getLocation();
                }
            }
            if (prioritizedLauncherLocation != null) {
                Direction[] bug2array = Motion.bug2(rc, prioritizedLauncherLocation, lastDirection, clockwiseRotation, false, true, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
                me = rc.getLocation();
                if (GlobalArray.DEBUG_INFO >= 4) {
                    rc.setIndicatorLine(me, prioritizedLauncherLocation, 75, 255, 255);
                } else if (GlobalArray.DEBUG_INFO > 0) {
                    rc.setIndicatorDot(me, 75, 255, 255);
                }
            } else {
                Motion.spreadRandomly(rc, me, prioritizedHeadquarters);
            }
        }
    }

    private void updatePrioritizedHeadquarters() throws GameActionException {
        prioritizedHeadquarters = headquarters[0];
        for (int i = 0; i < headquarters.length; i++) {
            if (headquarters[i] != null) {
                if (prioritizedHeadquarters.distanceSquaredTo(me) > headquarters[i].distanceSquaredTo(me)) {
                    prioritizedHeadquarters = headquarters[i];
                    prioritizedHeadquarterIndex = i;
                }
            }
        }
    }

    private void updateRandomExploreLocation() throws GameActionException {
        if (randomExploreLocation != null) return;
        randomExploreTime = 0;
        MapLocation[] knownWells = GlobalArray.getKnownWellLocations(rc);
        int iteration = 0;
        search: while (randomExploreLocation == null && iteration < 16) {
            iteration++;
            randomExploreLocation = new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight()));
            for (MapLocation well : knownWells) {
                if (well != null && well.distanceSquaredTo(randomExploreLocation) < randomExploreMinKnownWellDistSquared) {
                    randomExploreLocation = null;
                    continue search;
                }
            }
            for (MapLocation hq : headquarters) {
                if (hq.distanceSquaredTo(randomExploreLocation) < randomExploreMinKnownHQDistSquared) {
                    randomExploreLocation = null;
                    continue search;
                }
            }
        }
    }
}
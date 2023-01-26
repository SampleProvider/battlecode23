package betterthings2;

import battlecode.common.*;
import java.util.Random;

public strictfp class Carrier {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    private Random rng = new Random(2023);

    private static final Direction[] DIRECTIONS = {
            Direction.SOUTHWEST,
            Direction.SOUTH,
            Direction.SOUTHEAST,
            Direction.WEST,
            Direction.EAST,
            Direction.NORTHWEST,
            Direction.NORTH,
            Direction.NORTHEAST,
    };

    private ResourceType prioritizedResourceType = ResourceType.ELIXIR;
    private int adamantiumAmount = 0;
    private int manaAmount = 0;
    private int elixirAmount = 0;
    private int resourceCollectAmount = 40;

    private RobotType prioritizedRobotType = RobotType.LAUNCHER;

    private MapLocation prioritizedWell;
    private MapLocation[] headquarters;
    private MapLocation prioritizedHeadquarters;
    private int prioritizedHeadquarterIndex;

    private StoredLocations storedLocations;

    private boolean clockwiseRotation = true;
    private Direction lastDirection = Direction.CENTER;

    private MapLocation randomExploreLocation;
    private int randomExploreTime = 0;
    private final int randomExploreMinKnownWellDistSquared = 81;
    private final int randomExploreMinKnownHQDistSquared = 144;

    private int lastHealth = 0;

    private int state = 0;
    // state
    // 0 is default (path to hq or pick up anchor, switches to 5 if nothing to do)
    // 1 is pathfinding to well
    // 2 is collecting
    // 3 is pathfinding to headquarters
    // 4 is pathfinding to island
    // 5 is retreat
    // 6 is explore

    // new carrier strat? carrier stays away from crowded headquarters to let otehr carriers out

    private StringBuilder indicatorString = new StringBuilder();

    public Carrier(RobotController rc) {
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
            lastHealth = rc.getHealth();
            storedLocations = new StoredLocations(rc, headquarters);
            rng = new Random(rc.getRoundNum());
        } catch (GameActionException e) {
            System.out.println("GameActionException at Carrier constructor");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Carrier constructor");
            e.printStackTrace();
        } finally {
            run();
        }
    }

    private void run() {
        while (true) {
            try {
                me = rc.getLocation();
                round = rc.getRoundNum();
                globalArray.parseGameState(rc.readSharedArray(GlobalArray.GAMESTATE));
                adamantiumAmount = rc.getResourceAmount(ResourceType.ADAMANTIUM);
                manaAmount = rc.getResourceAmount(ResourceType.MANA);
                elixirAmount = rc.getResourceAmount(ResourceType.ELIXIR);

                prioritizedResourceType = globalArray.prioritizedResource(prioritizedHeadquarterIndex);

                indicatorString = new StringBuilder();

                indicatorString.append(state + "; ");
                indicatorString.append("PR=" + (prioritizedResourceType == ResourceType.MANA ? "MN" : prioritizedResourceType.toString().substring(0, 2)) + "; ");

                storedLocations.updateFullWells();
                storedLocations.detectIslandLocations();
                storedLocations.writeToGlobalArray();

                // if (rc.getHealth() != lastHealth) {
                //     state = 5;
                // }
                // lastHealth = rc.getHealth();

                updatePrioritizedHeadquarters();
                RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
                RobotInfo robot = Attack.attack(rc, prioritizedHeadquarters, robotInfo, prioritizedRobotType, false, indicatorString);
                if (robot == null) {
                    robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                    robot = Attack.senseOpponent(rc, robotInfo);
                }
                if (robot != null) {
                    storedLocations.storeOpponentLocation(robot.getLocation());
                    state = 5;
                }

                runState();
                
                updatePrioritizedHeadquarters();
                robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
                robot = Attack.attack(rc, prioritizedHeadquarters, robotInfo, prioritizedRobotType, false, indicatorString);
                if (robot == null) {
                    robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                    robot = Attack.senseOpponent(rc, robotInfo);
                }
                if (robot != null) {
                    storedLocations.storeOpponentLocation(robot.getLocation());
                    state = 5;
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException at Carrier");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Carrier");
                e.printStackTrace();
            } finally {
                rc.setIndicatorString(indicatorString.toString());
                Clock.yield();
            }
        }
    }

    private void runState() throws GameActionException {
        if (state == 0) {
            MapLocation[] islands = GlobalArray.getKnownIslandLocations(rc, Team.NEUTRAL);
            if (rc.canTakeAnchor(prioritizedHeadquarters, Anchor.STANDARD) && islands.length > 0) {
                rc.takeAnchor(prioritizedHeadquarters, Anchor.STANDARD);
            }

            if (rc.getAnchor() != null) {
                state = 4;
                runState();
                return;
            }

            if (adamantiumAmount + manaAmount + elixirAmount >= resourceCollectAmount) {
                state = 3;
                runState();
                return;
            } else {
                updatePrioritizedWell();
                if (prioritizedWell != null) {
                    state = 1;
                    indicatorString.append("PATH->WELL; ");
                    Direction[] bug2array = Motion.bug2(rc, prioritizedWell, lastDirection, clockwiseRotation, false, indicatorString);
                    lastDirection = bug2array[0];
                    if (bug2array[1] == Direction.CENTER) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                    attemptCollection();
                    me = rc.getLocation();
                    if (GlobalArray.DEBUG_INFO >= 4) {
                        rc.setIndicatorLine(me, prioritizedWell, 255, 75, 75);
                    } else if (GlobalArray.DEBUG_INFO > 0) {
                        rc.setIndicatorDot(me, 255, 75, 75);
                    }
                    return;
                }
            }
            Motion.spreadRandomly(rc, me);
        } else if (state == 1) {
            updatePrioritizedWell();
            if (prioritizedWell == null) {
                state = 0;
                runState();
                return;
            }
            indicatorString.append("PATH->WELL; ");
            Direction[] bug2array = Motion.bug2(rc, prioritizedWell, lastDirection, clockwiseRotation, false, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
            attemptCollection();
            me = rc.getLocation();
            if (GlobalArray.DEBUG_INFO >= 4) {
                rc.setIndicatorLine(me, prioritizedWell, 255, 75, 75);
            } else if (GlobalArray.DEBUG_INFO > 0) {
                rc.setIndicatorDot(me, 255, 75, 75);
            }
        } else if (state == 2) {
            indicatorString.append("COLLECT; ");
            if (rc.canCollectResource(prioritizedWell, -1) && adamantiumAmount + manaAmount + elixirAmount < resourceCollectAmount) {
                rc.collectResource(prioritizedWell, -1);
                Motion.circleAroundTarget(rc, me, prioritizedWell);
                me = rc.getLocation();
                if (GlobalArray.DEBUG_INFO >= 3) {
                    rc.setIndicatorLine(me, prioritizedWell, 255, 75, 75);
                } else if (GlobalArray.DEBUG_INFO > 0) {
                    rc.setIndicatorDot(me, 255, 75, 75);
                }
            } else {
                state = 0;
                runState();
            }
        } else if (state == 3) {
            indicatorString.append("PATH->HQ; ");
            Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, false, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
            if (prioritizedHeadquarters.distanceSquaredTo(me) <= rc.getType().visionRadiusSquared) {
                attemptTransfer();
            }
            me = rc.getLocation();
            if (GlobalArray.DEBUG_INFO >= 4) {
                rc.setIndicatorLine(me, prioritizedHeadquarters, 125, 25, 255);
            } else if (GlobalArray.DEBUG_INFO > 0) {
                rc.setIndicatorDot(me, 125, 25, 255);
            }
            if (adamantiumAmount + manaAmount + elixirAmount == 0) {
                state = 0;
            }
        } else if (state == 4) {
            int[] islands = rc.senseNearbyIslands();
            MapLocation prioritizedIslandLocation = null;
            for (int id : islands) {
                if (rc.senseAnchor(id) == null) {
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
                Direction[] bug2array = Motion.bug2(rc, prioritizedIslandLocation, lastDirection, clockwiseRotation, true, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
                me = rc.getLocation();
                if (rc.canPlaceAnchor()) {
                    if (rc.senseTeamOccupyingIsland(rc.senseIsland(me)) == Team.NEUTRAL) {
                        indicatorString.append("P ANC; ");
                        rc.placeAnchor();
                        state = 0;
                    }
                }
                if (GlobalArray.DEBUG_INFO >= 2) {
                    rc.setIndicatorLine(me, prioritizedIslandLocation, 75, 125, 255);
                } else if (GlobalArray.DEBUG_INFO > 0) {
                    rc.setIndicatorDot(me, 75, 125, 255);
                }
            } else {
                // get island location from global array
                MapLocation[] islandLocations = GlobalArray.getKnownIslandLocations(rc, Team.NEUTRAL);
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
                    Direction[] bug2array = Motion.bug2(rc, prioritizedIslandLocation, lastDirection, clockwiseRotation, false, indicatorString);
                    lastDirection = bug2array[0];
                    if (bug2array[1] == Direction.CENTER) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                    me = rc.getLocation();
                    if (rc.canPlaceAnchor()) {
                        if (rc.senseTeamOccupyingIsland(rc.senseIsland(me)) == Team.NEUTRAL) {
                            indicatorString.append("P ANC; ");
                            rc.placeAnchor();
                            state = 0;
                        }
                    }
                    if (GlobalArray.DEBUG_INFO >= 2) {
                        rc.setIndicatorLine(me, prioritizedIslandLocation, 75, 125, 255);
                    } else if (GlobalArray.DEBUG_INFO > 0) {
                        rc.setIndicatorDot(me, 75, 125, 255);
                    }
                }
                else {
                    Motion.moveRandomly(rc);
                }
                return;
            }
        } else if (state == 5) {
            indicatorString.append("RET; ");
            Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, false, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
            if (prioritizedHeadquarters.distanceSquaredTo(me) <= RobotType.HEADQUARTERS.visionRadiusSquared) {
                attemptTransfer();
                state = 0;
            }
            me = rc.getLocation();
            if (GlobalArray.DEBUG_INFO >= 4) {
                rc.setIndicatorLine(me, prioritizedHeadquarters, 125, 255, 0);
            } else if (GlobalArray.DEBUG_INFO > 0) {
                rc.setIndicatorDot(me, 125, 255, 0);
            }
        } else if (state == 6) {
            updatePrioritizedWell();
            if (prioritizedWell != null) {
                state = 1;
                indicatorString.append("PATH->WELL; ");
                Direction[] bug2array = Motion.bug2(rc, prioritizedWell, lastDirection, clockwiseRotation, false, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
                attemptCollection();
                me = rc.getLocation();
                if (GlobalArray.DEBUG_INFO >= 4) {
                    rc.setIndicatorLine(me, prioritizedWell, 255, 75, 75);
                } else if (GlobalArray.DEBUG_INFO > 0) {
                    rc.setIndicatorDot(me, 255, 75, 75);
                }
                return;
            }
            updateRandomExploreLocation();
            if (randomExploreLocation != null) {
                indicatorString.append("EXPL-" + randomExploreLocation.toString() + "; ");
                Direction[] bug2array = Motion.bug2(rc, randomExploreLocation, lastDirection, clockwiseRotation, false, indicatorString);
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
                if (randomExploreTime > 50 || randomExploreLocation.distanceSquaredTo(me) <= 4) {
                    randomExploreLocation = null;
                    if (storedLocations.detectedNewLocations) {
                        state = 3;
                    }
                }
            } else {
                Motion.spreadRandomly(rc, me);
            }
        }
    }

    private void attemptCollection() throws GameActionException {
        if (rc.canCollectResource(prioritizedWell, -1) && adamantiumAmount + manaAmount + elixirAmount < resourceCollectAmount) {
            rc.collectResource(prioritizedWell, -1);
            state = 2;
        }
    }

    private void attemptTransfer() throws GameActionException {
        if (rc.canTransferResource(prioritizedHeadquarters, ResourceType.ADAMANTIUM, adamantiumAmount)) {
            rc.transferResource(prioritizedHeadquarters, ResourceType.ADAMANTIUM, adamantiumAmount);
            indicatorString.append("D AD; ");
        }
        if (rc.canTransferResource(prioritizedHeadquarters, ResourceType.MANA, manaAmount)) {
            rc.transferResource(prioritizedHeadquarters, ResourceType.MANA, manaAmount);
            indicatorString.append("D MN; ");
        }
        if (rc.canTransferResource(prioritizedHeadquarters, ResourceType.ELIXIR, elixirAmount)) {
            rc.transferResource(prioritizedHeadquarters, ResourceType.ELIXIR, elixirAmount);
            indicatorString.append("D EL; ");
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

    private void updatePrioritizedWell() throws GameActionException {
        prioritizedWell = null;
        WellInfo[] wellInfo = rc.senseNearbyWells();
        if (wellInfo.length > 0) {
            WellInfo prioritizedWellInfo = null;
            for (WellInfo w : wellInfo) {
                if (testValidWell(w.getMapLocation())) {
                    storedLocations.storeWell(w);
                    if (prioritizedWell == null) {
                        prioritizedWellInfo = w;
                        prioritizedWell = w.getMapLocation();
                    } else if (prioritizedWellInfo.getResourceType() == prioritizedResourceType) {
                        if (w.getResourceType() == prioritizedResourceType && prioritizedWell.distanceSquaredTo(me) > w.getMapLocation().distanceSquaredTo(me)) {
                            prioritizedWellInfo = w;
                            prioritizedWell = w.getMapLocation();
                        }
                    } else {
                        if (w.getResourceType() == prioritizedResourceType) {
                            prioritizedWellInfo = w;
                            prioritizedWell = w.getMapLocation();
                        } else if (prioritizedWell.distanceSquaredTo(me) > w.getMapLocation().distanceSquaredTo(me)) {
                            prioritizedWellInfo = w;
                            prioritizedWell = w.getMapLocation();
                        }
                    }
                }
            }
            if (prioritizedWell != null) {
                return;
            }
        }
        MapLocation[] wellLocations = GlobalArray.getKnownWellLocations(rc);
        for (MapLocation m : wellLocations) {
            if (m == null) {
                continue;
            }
            if (storedLocations.isFullWell(m)) {
                continue;
            }
            if (prioritizedWell == null) {
                if (testValidWell(m)) {
                    prioritizedWell = m;
                }
            }
            else if (prioritizedWell.distanceSquaredTo(me) > m
                    .distanceSquaredTo(me)) {
                if (testValidWell(m)) {
                    prioritizedWell = m;
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
            iteration++;
        }
    }

    private boolean testValidWell(MapLocation well) throws GameActionException {
        MapLocation[] opponentLocations = GlobalArray.getKnownOpponentLocations(rc);
        for (MapLocation m : opponentLocations) {
            if (m == null) {
                continue;
            }
            if (m.distanceSquaredTo(well) <= StoredLocations.MIN_EXISTING_DISTANCE_SQUARED) {
                return false;
            }
        }
        int emptySpots = 0;
        int fullSpots = 0;
        for (Direction d : DIRECTIONS) {
            MapLocation adjSpot = well.add(d);
            if (!rc.canSenseLocation(adjSpot)) {
                continue;
            }
            if (!rc.sensePassability(adjSpot) || rc.senseRobotAtLocation(adjSpot) != null) {
                fullSpots += 1;
            } else {
                emptySpots += 1;
            }
        }
        if (testFull(fullSpots, emptySpots)) {
            return true;
        }
        storedLocations.fullWell(well);
        return false;
    }
    
    private boolean testFull(int fullSpots, int emptySpots) {
        // optimize??
        // return fullSpots <= emptySpots + 1;
        if (fullSpots + emptySpots == 0) {
            return true;
        }
        return emptySpots >= 1;
    }
}
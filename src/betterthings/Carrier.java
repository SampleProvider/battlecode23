package betterthings;

import battlecode.common.*;

public strictfp class Carrier {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

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

    private int lastHealth = 0;

    private int state = 0;
    // state
    // 0 is wander
    // 1 is pathfinding to well
    // 2 is collecting
    // 3 is pathfinding to island
    // 4 is retreat

    protected StringBuilder indicatorString = new StringBuilder();

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
            storedLocations = new StoredLocations(rc);
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

                indicatorString.append(prioritizedHeadquarterIndex + "; ");
                indicatorString.append("PR=" + (prioritizedResourceType == ResourceType.MANA ? "MN" : prioritizedResourceType.toString().substring(0, 2)) + "; ");

                storedLocations.writeToGlobalArray();

                if (rc.getHealth() != lastHealth) {
                    state = 4;
                }
                lastHealth = rc.getHealth();

                RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared,rc.getTeam().opponent());
                MapLocation loc = Attack.attack(rc, me, robotInfo, prioritizedRobotType, false, indicatorString);
                if (loc == null) {
                    loc = Attack.senseOpponent(rc, me, robotInfo);
                }
                if (loc != null) {
                    storedLocations.storeOpponentLocation(loc);
                    state = 4;
                }

                runState();
                me = rc.getLocation();
                robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared,rc.getTeam().opponent());
                loc = Attack.attack(rc, me, robotInfo, prioritizedRobotType, false, indicatorString);
                if (loc == null) {
                    loc = Attack.senseOpponent(rc, me, robotInfo);
                }
                if (loc != null) {
                    storedLocations.storeOpponentLocation(loc);
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
            updatePrioritizedHeadquarters();
            if (rc.canTakeAnchor(prioritizedHeadquarters, Anchor.STANDARD)) {
                rc.takeAnchor(prioritizedHeadquarters, Anchor.STANDARD);
            }

            if (rc.getAnchor() != null) {
                state = 3;
                runState();
                return;
            }

            if (adamantiumAmount + manaAmount + elixirAmount >= resourceCollectAmount) {
                indicatorString.append("PATH->HQ; ");
                Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
                if (prioritizedHeadquarters.distanceSquaredTo(me) <= rc.getType().visionRadiusSquared) {
                    attemptTransfer();
                }
                me = rc.getLocation();
                rc.setIndicatorLine(me, prioritizedHeadquarters, 125, 25, 255);
                return;
            } else {
                updatePrioritizedWell();
                if (prioritizedWell != null) {
                    indicatorString.append("WDR-(NXT:P->W); ");
                    state = 1;
                    runState();
                    return;
                }
            }
            
            Motion.spreadRandomly(rc, me);
        }
        else if (state == 1) {
            updatePrioritizedWell();
            if (prioritizedWell == null) {
                state = 0;
                runState();
                return;
            }
            indicatorString.append("PATH->WELL; ");
            Direction[] bug2array = Motion.bug2(rc, prioritizedWell, lastDirection, clockwiseRotation, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
            attemptCollection();
            me = rc.getLocation();
            rc.setIndicatorLine(me, prioritizedWell, 255, 75, 75);
        }
        else if (state == 2) {
            indicatorString.append("COLLECT; ");
            if (rc.canCollectResource(prioritizedWell, -1)
                    && adamantiumAmount + manaAmount + elixirAmount < resourceCollectAmount) {
                rc.collectResource(prioritizedWell, -1);
                // Motion.circleAroundTarget(rc, me, prioritizedWell);
                me = rc.getLocation();
                rc.setIndicatorLine(me, prioritizedWell, 255, 75, 75);
            } else {
                state = 0;
                runState();
            }
        }
        else if (state == 3) {
            int[] islands = rc.senseNearbyIslands();
            MapLocation prioritizedIslandLocation = null;
            for (int id : islands) {
                if (rc.senseAnchor(id) == null) {
                    MapLocation[] islandLocations = rc.senseNearbyIslandLocations(id);
                    for (MapLocation m : islandLocations) {
                        if (prioritizedIslandLocation == null) {
                            prioritizedIslandLocation = m;
                        }
                        else if (m.distanceSquaredTo(me) < prioritizedIslandLocation.distanceSquaredTo(me)) {
                            prioritizedIslandLocation = m;
                        }
                    }
                }
            }
            if (prioritizedIslandLocation != null) {
                Direction[] bug2array = Motion.bug2(rc, prioritizedIslandLocation, lastDirection, clockwiseRotation, indicatorString);
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
                rc.setIndicatorDot(me, 75, 125, 255);
                rc.setIndicatorLine(me, prioritizedIslandLocation, 75, 125, 255);
            } else {
                // get island location from global array
                Motion.moveRandomly(rc);
                return;
            }
        }
        else if (state == 4) {
            updatePrioritizedHeadquarters();
            indicatorString.append("RET; ");
            Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
            if (prioritizedHeadquarters.distanceSquaredTo(me) <= RobotType.HEADQUARTERS.visionRadiusSquared) {
                attemptTransfer();
                state = 0;
            }
            me = rc.getLocation();
            rc.setIndicatorLine(me, prioritizedHeadquarters, 0, 100, 200);
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
                storedLocations.storeWell(w);
                if (prioritizedWell == null) {
                    int emptySpots = 0;
                    int fullSpots = 0;
                    for (Direction d : DIRECTIONS) {
                        MapLocation adjSpot = w.getMapLocation().add(d);
                        if (!rc.canSenseLocation(adjSpot)) {
                            continue;
                        }
                        if (!rc.sensePassability(adjSpot) || rc.senseRobotAtLocation(adjSpot) != null) {
                            fullSpots += 1;
                        }
                        else {
                            emptySpots += 1;
                        }
                    }
                    if (fullSpots <= emptySpots + 1) {
                        prioritizedWellInfo = w;
                        prioritizedWell = w.getMapLocation();
                    }
                    continue;
                }
                else if (prioritizedWellInfo.getResourceType() == prioritizedResourceType) {
                    if (w.getResourceType() == prioritizedResourceType
                            && prioritizedWell.distanceSquaredTo(me) > w
                                    .getMapLocation().distanceSquaredTo(me)) {
                        int emptySpots = 0;
                        int fullSpots = 0;
                        for (Direction d : DIRECTIONS) {
                            MapLocation adjSpot = w.getMapLocation().add(d);
                            if (!rc.canSenseLocation(adjSpot)) {
                                continue;
                            }
                            if (!rc.sensePassability(adjSpot) || rc.senseRobotAtLocation(adjSpot) != null) {
                                fullSpots += 1;
                            }
                            else {
                                emptySpots += 1;
                            }
                        }
                        if (fullSpots <= emptySpots + 1) {
                            prioritizedWellInfo = w;
                            prioritizedWell = w.getMapLocation();
                        }
                    }
                } else {
                    if (w.getResourceType() == prioritizedResourceType) {
                        int emptySpots = 0;
                        int fullSpots = 0;
                        for (Direction d : DIRECTIONS) {
                            MapLocation adjSpot = w.getMapLocation().add(d);
                            if (!rc.canSenseLocation(adjSpot)) {
                                continue;
                            }
                            if (!rc.sensePassability(adjSpot) || rc.senseRobotAtLocation(adjSpot) != null) {
                                fullSpots += 1;
                            }
                            else {
                                emptySpots += 1;
                            }
                        }
                        if (fullSpots <= emptySpots + 1) {
                            prioritizedWellInfo = w;
                            prioritizedWell = w.getMapLocation();
                        }
                    }
                    else if (prioritizedWell.distanceSquaredTo(me) > w.getMapLocation()
                            .distanceSquaredTo(me)) {
                        int emptySpots = 0;
                        int fullSpots = 0;
                        for (Direction d : DIRECTIONS) {
                            MapLocation adjSpot = w.getMapLocation().add(d);
                            if (!rc.canSenseLocation(adjSpot)) {
                                continue;
                            }
                            if (!rc.sensePassability(adjSpot) || rc.senseRobotAtLocation(adjSpot) != null) {
                                fullSpots += 1;
                            }
                            else {
                                emptySpots += 1;
                            }
                        }
                        if (fullSpots <= emptySpots + 1) {
                            prioritizedWellInfo = w;
                            prioritizedWell = w.getMapLocation();
                        }
                    }
                }
            }
        }
        else {
            MapLocation[] wellLocations = GlobalArray.getKnownWellLocations(rc);
            for (MapLocation m : wellLocations) {
                if (m == null) {
                    continue;
                }
                if (prioritizedWell == null) {
                    int emptySpots = 0;
                    int fullSpots = 0;
                    for (Direction d : DIRECTIONS) {
                        MapLocation adjSpot = m.add(d);
                        if (!rc.canSenseLocation(adjSpot)) {
                            continue;
                        }
                        if (!rc.sensePassability(adjSpot) || rc.senseRobotAtLocation(adjSpot) != null) {
                            fullSpots += 1;
                        }
                        else {
                            emptySpots += 1;
                        }
                    }
                    if (fullSpots <= emptySpots + 1) {
                        prioritizedWell = m;
                    }
                    continue;
                }
                else if (prioritizedWell.distanceSquaredTo(me) > m
                        .distanceSquaredTo(me)) {
                    int emptySpots = 0;
                    int fullSpots = 0;
                    for (Direction d : DIRECTIONS) {
                        MapLocation adjSpot = m.add(d);
                        if (!rc.canSenseLocation(adjSpot)) {
                            continue;
                        }
                        if (!rc.sensePassability(adjSpot) || rc.senseRobotAtLocation(adjSpot) != null) {
                            fullSpots += 1;
                        }
                        else {
                            emptySpots += 1;
                        }
                    }
                    if (fullSpots <= emptySpots + 1) {
                        prioritizedWell = m;
                    }
                }
            }
        }
    }
}
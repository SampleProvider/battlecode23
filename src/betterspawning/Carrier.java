package betterspawning;

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

    private WellInfo[] seenWells = new WellInfo[4];
    private int seenWellIndex = 0;

    private RobotInfo[] robotInfo;
    private MapLocation opponentLocation;

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
                adamantiumAmount = rc.getResourceAmount(ResourceType.ADAMANTIUM);
                manaAmount = rc.getResourceAmount(ResourceType.MANA);
                elixirAmount = rc.getResourceAmount(ResourceType.ELIXIR);

                globalArray.parseGameState(rc.readSharedArray(GlobalArray.GAMESTATE));
                prioritizedResourceType = globalArray.prioritizedResource(prioritizedHeadquarterIndex);
                
                indicatorString = new StringBuilder();

                indicatorString.append("PR=" + (prioritizedResourceType == ResourceType.MANA ? "MN" : prioritizedResourceType.toString().substring(0, 2)) + "; ");

                if (rc.canWriteSharedArray(0, 0)) {
                    if (round % 2 == 0) {
                        rc.writeSharedArray(GlobalArray.CARRIERCOUNT, rc.readSharedArray(GlobalArray.CARRIERCOUNT)+1);
                    }
                    for (int i = 0;i < 4;i++) {
                        if (seenWells[i] != null) {
                            if (GlobalArray.storeWell(rc, seenWells[i])) {
                                indicatorString.append("STO WELL " + seenWells[i].toString() + "; ");
                                seenWells[i] = null;
                            }
                        }
                    }
                    if (opponentLocation != null) {
                        if (GlobalArray.storeOpponentLocation(rc, opponentLocation)) {
                            indicatorString.append("STO OPP " + opponentLocation.toString() + "; ");
                            opponentLocation = null;
                        }
                    }
                }

                if (rc.getHealth() != lastHealth) {
                    state = 4;
                }
                lastHealth = rc.getHealth();

                robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared,rc.getTeam().opponent());
                MapLocation loc = Attack.attack(rc, me, robotInfo, prioritizedRobotType, false, indicatorString);
                if (loc == null) {
                    loc = Attack.senseOpponent(rc, me, robotInfo);
                }
                if (loc != null) {
                    opponentLocation = loc;
                    // state = 4;
                }

                runState();
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
                WellInfo[] wellInfo = rc.senseNearbyWells();
                if (wellInfo.length > 0) {
                    WellInfo prioritizedWellInfo = wellInfo[0];
                    MapLocation prioritizedWellInfoLocation = wellInfo[0].getMapLocation();
                    for (WellInfo w : wellInfo) {
                        if (prioritizedWellInfo.getResourceType() == prioritizedResourceType) {
                            if (w.getResourceType() == prioritizedResourceType
                                    && prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w.getMapLocation().distanceSquaredTo(me)) {
                                prioritizedWellInfo = w;
                                prioritizedWellInfoLocation = w.getMapLocation();
                            }
                        } else {
                            if (prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w.getMapLocation()
                                    .distanceSquaredTo(me)) {
                                prioritizedWellInfo = w;
                                prioritizedWellInfoLocation = w.getMapLocation();
                            }
                        }
                        if (seenWellIndex < 4) {
                            boolean newWell = true;
                            for (int i = 0;i < seenWellIndex; i++) {
                                if (seenWells[i] == null) {
                                    continue;
                                }
                                if (seenWells[i].getMapLocation().equals(w.getMapLocation())) {
                                    newWell = false;
                                }
                            }
                            if (newWell) {
                                seenWells[seenWellIndex] = w;
                                seenWellIndex += 1;
                            }
                        }
                    }
                    int emptySpots = 0;
                    int fullSpots = 0;
                    for (Direction d : DIRECTIONS) {
                        MapLocation adjSpot = prioritizedWellInfoLocation.add(d);
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
                        indicatorString.append("WANDER-(NXT:PATH->WELL); ");
                        prioritizedWell = prioritizedWellInfoLocation;
                        state = 1;
                        runState();
                        return;
                    }
                }
                else {
                    MapLocation[] wellLocations = GlobalArray.getKnownWellLocations(rc);
                    MapLocation prioritizedWellLocation = null;
                    for (MapLocation m : wellLocations) {
                        if (m == null) {
                            continue;
                        }
                        if (prioritizedWellLocation == null) {
                            prioritizedWellLocation = m;
                        }
                        else if (prioritizedWellLocation.distanceSquaredTo(me) > m.distanceSquaredTo(me)) {
                            prioritizedWellLocation = m;
                        }
                    }
                    if (prioritizedWellLocation != null) {
                        int emptySpots = 0;
                        int fullSpots = 0;
                        for (Direction d : DIRECTIONS) {
                            MapLocation adjSpot = prioritizedWellLocation.add(d);
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
                            indicatorString.append("WANDER-(NXT:PATH->WELL); ");
                            prioritizedWell = prioritizedWellLocation;
                            state = 1;
                            runState();
                            return;
                        }
                    }
                }
            }
            
            Motion.spreadRandomly(rc, me, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
        }
        else if (state == 1) {
            WellInfo[] wellInfo = rc.senseNearbyWells();
            if (wellInfo.length > 0) {
                WellInfo prioritizedWellInfo = wellInfo[0];
                MapLocation prioritizedWellInfoLocation = wellInfo[0].getMapLocation();
                for (WellInfo w : wellInfo) {
                    if (prioritizedWellInfo.getResourceType() == prioritizedResourceType) {
                        if (w.getResourceType() == prioritizedResourceType
                                && prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w
                                        .getMapLocation().distanceSquaredTo(me)) {
                            prioritizedWellInfo = w;
                            prioritizedWellInfoLocation = w.getMapLocation();
                        }
                    } else {
                        if (prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w.getMapLocation()
                                .distanceSquaredTo(me)) {
                            prioritizedWellInfo = w;
                            prioritizedWellInfoLocation = w.getMapLocation();
                        }
                    }
                    if (seenWellIndex < 4) {
                        boolean newWell = true;
                        for (int i = 0;i < seenWellIndex; i++) {
                            if (seenWells[i] == null) {
                                continue;
                            }
                            if (seenWells[i].getMapLocation().equals(w.getMapLocation())) {
                                newWell = false;
                            }
                        }
                        if (newWell) {
                            seenWells[seenWellIndex] = w;
                            seenWellIndex += 1;
                        }
                    }
                }
                int emptySpots = 0;
                int fullSpots = 0;
                for (Direction d : DIRECTIONS) {
                    MapLocation adjSpot = prioritizedWellInfoLocation.add(d);
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
                    prioritizedWell = prioritizedWellInfoLocation;
                }
            }
            else {
                MapLocation[] wellLocations = GlobalArray.getKnownWellLocations(rc);
                MapLocation prioritizedWellLocation = null;
                for (MapLocation m : wellLocations) {
                    if (m == null) {
                        continue;
                    }
                    if (prioritizedWellLocation == null) {
                        prioritizedWellLocation = m;
                    }
                    else if (prioritizedWellLocation.distanceSquaredTo(me) > m.distanceSquaredTo(me)) {
                        prioritizedWellLocation = m;
                    }
                }
                if (prioritizedWellLocation != null) {
                    int emptySpots = 0;
                    int fullSpots = 0;
                    for (Direction d : DIRECTIONS) {
                        MapLocation adjSpot = prioritizedWellLocation.add(d);
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
                        prioritizedWell = prioritizedWellLocation;
                    }
                }
            }
            int emptySpots = 0;
            int fullSpots = 0;
            for (Direction d : DIRECTIONS) {
                MapLocation adjSpot = prioritizedWell.add(d);
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
            if (fullSpots > emptySpots + 1) {
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
                Motion.circleAroundTarget(rc, me, prioritizedWell);
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
                        indicatorString.append("PLAC ANC; ");
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
            indicatorString.append("RETREAT; ");
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
            rc.setIndicatorLine(me, prioritizedHeadquarters, 255, 255, 0);
        }
        me = rc.getLocation();
        robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared,rc.getTeam().opponent());
        MapLocation loc = Attack.attack(rc, me, robotInfo, prioritizedRobotType, false, indicatorString);
        if (loc != null) {
            opponentLocation = loc;
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
            indicatorString.append("DROP AD; ");
        }
        if (rc.canTransferResource(prioritizedHeadquarters, ResourceType.MANA, manaAmount)) {
            rc.transferResource(prioritizedHeadquarters, ResourceType.MANA, manaAmount);
            indicatorString.append("DROP MN; ");
        }
        if (rc.canTransferResource(prioritizedHeadquarters, ResourceType.ELIXIR, elixirAmount)) {
            rc.transferResource(prioritizedHeadquarters, ResourceType.ELIXIR, elixirAmount);
            indicatorString.append("DROP EX; ");
        }
    }

    private void updatePrioritizedHeadquarters() throws GameActionException {
        prioritizedHeadquarters = headquarters[0];
        for (MapLocation hq : headquarters) {
            if (hq != null) {
                if (prioritizedHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                    prioritizedHeadquarters = hq;
                }
            }
        }
    }
}
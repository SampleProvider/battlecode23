package SPAARK;

import battlecode.common.*;

import java.util.Random;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public strictfp class Carrier {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();

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

    private ResourceType prioritizedResourceType = ResourceType.ELIXIR;
    private int adamantiumAmount = 0;
    private int manaAmount = 0;
    private int elixirAmount = 0;
    private int resourceCollectAmount = 40;

    private RobotType prioritizedRobotType = RobotType.LAUNCHER;

    private WellInfo[] wellInfo;
    private MapLocation prioritizedWell;
    private MapLocation[] headquarters;
    private MapLocation prioritizedHeadquarters;

    private WellInfo[] seenWells = new WellInfo[4];
    private int seenWellIndex = 0;

    private MapLocation enemyLocation;

    private boolean clockwiseRotation = true;

    private int lastHealth = 0;

    private int state = 0;
    // state
    // 0 is wander
    // 1 is pathfinding to well
    // 2 is collecting
    // 3 is pathfinding to island
    // 4 is retreat

    public Carrier(RobotController rc) {
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
            lastHealth = rc.getHealth();
        } catch (GameActionException e) {
            System.out.println("GameActionException at Carrier constructor");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Carrier constructor");
            e.printStackTrace();
        } finally {
            // Clock.yield();
        }
        run();
    }

    private void run() {
        while (true) {
            try {
                me = rc.getLocation();
                adamantiumAmount = rc.getResourceAmount(ResourceType.ADAMANTIUM);
                manaAmount = rc.getResourceAmount(ResourceType.MANA);
                elixirAmount = rc.getResourceAmount(ResourceType.ELIXIR);

                globalArray.parseGameState(rc.readSharedArray(0));
                prioritizedResourceType = globalArray.prioritizedResource();

                if (rc.canWriteSharedArray(0, 0)) {
                    for (int i = 0;i < 4;i++) {
                        if (seenWells[i] != null) {
                            if (GlobalArray.storeWell(rc, seenWells[i])) {
                                seenWells[i] = null;
                            }
                        }
                    }
                }

                if (rc.getHealth() != lastHealth) {
                    state = 4;
                }
                lastHealth = rc.getHealth();

                runState();
            } catch (GameActionException e) {
                System.out.println("GameActionException at Carrier");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Carrier");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    private void runState() throws GameActionException {
        Attack.attack(rc, me, prioritizedRobotType, false);
        if (state == 0) {
            updatePrioritizedHeadquarters();
            if (rc.canTakeAnchor(prioritizedHeadquarters, Anchor.STANDARD)) {
                rc.takeAnchor(prioritizedHeadquarters, Anchor.STANDARD);
            }

            if (rc.getAnchor() != null) {
                state = 3;
            }

            if (adamantiumAmount + manaAmount + elixirAmount >= resourceCollectAmount) {
                Motion.bug(rc, prioritizedHeadquarters);
                if (prioritizedHeadquarters.distanceSquaredTo(me) <= rc.getType().visionRadiusSquared) {
                    attemptTransfer();
                }
                rc.setIndicatorString("Pathfinding to HQ");
                return;
            } else {
                wellInfo = rc.senseNearbyWells();
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
                    }
                    int emptySpots = 0;
                    int fullSpots = 0;
                    for (Direction d : directions) {
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
                    if (fullSpots < emptySpots) {
                        rc.setIndicatorString("Wandering... (Next up: Pathfinding to Well)");
                        prioritizedWell = prioritizedWellInfoLocation;
                        if (seenWellIndex < 4) {
                            seenWells[seenWellIndex] = prioritizedWellInfo;
                            seenWellIndex += 1;
                        }
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
                        for (Direction d : directions) {
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
                        if (fullSpots < emptySpots) {
                            rc.setIndicatorString("Wandering... (Next up: Pathfinding to Well)");
                            prioritizedWell = prioritizedWellLocation;
                            state = 1;
                            runState();
                            return;
                        }
                    }
                }
            }
            
            Motion.spreadRandomly(rc, me, prioritizedHeadquarters);
        }
        else if (state == 1) {
            int emptySpots = 0;
            int fullSpots = 0;
            for (Direction d : directions) {
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
            clockwiseRotation = Motion.bug(rc, prioritizedWell, clockwiseRotation);
            attemptCollection();
            me = rc.getLocation();
            rc.setIndicatorString("Pathfinding to Well...");
            rc.setIndicatorLine(me, prioritizedWell, 255, 0, 0);
        }
        else if (state == 2) {
            rc.setIndicatorString("Collecting resources...");
            rc.setIndicatorLine(me, prioritizedWell, 255, 0, 0);
            if (rc.canCollectResource(prioritizedWell, -1)
                    && adamantiumAmount + manaAmount + elixirAmount < resourceCollectAmount) {
                rc.collectResource(prioritizedWell, -1);
                Motion.circleAroundTarget(rc, me, prioritizedWell);
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
                clockwiseRotation = Motion.bug(rc, prioritizedIslandLocation, clockwiseRotation);
                me = rc.getLocation();
                if (rc.canPlaceAnchor()) {
                    if (rc.senseTeamOccupyingIsland(rc.senseIsland(me)) == Team.NEUTRAL) {
                        rc.setIndicatorString("Placed Anchor!");
                        rc.placeAnchor();
                        state = 0;
                    }
                }
                rc.setIndicatorLine(me, prioritizedIslandLocation, 255, 0, 0);
            } else {
                // get island location from global array
                Motion.moveRandomly(rc);
                return;
            }
        }
        else if (state == 4) {
            rc.setIndicatorString("Retreating...");
            Motion.bug(rc, prioritizedHeadquarters);
            if (prioritizedHeadquarters.distanceSquaredTo(me) <= rc.getType().visionRadiusSquared) {
                attemptTransfer();
                state = 0;
            }
        }
        Attack.attack(rc, me, prioritizedRobotType, false);
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
        }
        if (rc.canTransferResource(prioritizedHeadquarters, ResourceType.MANA, manaAmount)) {
            rc.transferResource(prioritizedHeadquarters, ResourceType.MANA, manaAmount);
        }
        if (rc.canTransferResource(prioritizedHeadquarters, ResourceType.ELIXIR, elixirAmount)) {
            rc.transferResource(prioritizedHeadquarters, ResourceType.ELIXIR, elixirAmount);
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
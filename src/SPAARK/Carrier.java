package SPAARK;

import battlecode.common.*;

import java.util.Random;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public strictfp class Carrier {
    private RobotController rc;
    private MapLocation me;

    private int turnCount = 0;

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

    private ResourceType prioritizedResourceType = ResourceType.ELIXIR;
    private int adamantiumAmount = 0;
    private int manaAmount = 0;
    private int elixirAmount = 0;
    private int resourceCollectAmount = 40;

    private MapInfo[] mapInfo;
    private WellInfo[] wellInfo;
    private MapLocation closestWell;
    private MapLocation[] headquarters;
    private MapLocation closestHeadquarters;

    private int state = 0;
    // state
    // 0 is wander
    // 1 is pathfinding to well
    // 2 is collecting

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
        } catch (GameActionException e) {
            System.out.println("GameActionException at Carrier constructor");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Carrier constructor");
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
                adamantiumAmount = rc.getResourceAmount(ResourceType.ADAMANTIUM);
                manaAmount = rc.getResourceAmount(ResourceType.MANA);
                elixirAmount = rc.getResourceAmount(ResourceType.ELIXIR);
                if (state == 0) {
                    rc.setIndicatorString("Wandering...");
                    if (rc.getAnchor() != null) {
                        int[] islands = rc.senseNearbyIslands();
                        Set<MapLocation> islandLocs = new HashSet<>();
                        for (int id : islands) {
                            if (rc.senseAnchor(id) == null) {
                                MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
                                islandLocs.addAll(Arrays.asList(thisIslandLocs));
                            }
                        }
                        if (islandLocs.size() > 0) {
                            MapLocation islandLocation = islandLocs.iterator().next();
                            rc.setIndicatorString("Moving my anchor towards " + islandLocation);
                            while (!rc.getLocation().equals(islandLocation)) {
                                Direction dir = rc.getLocation().directionTo(islandLocation);
                                if (rc.canMove(dir)) {
                                    rc.move(dir);
                                }
                                Clock.yield();
                            }
                            if (rc.canPlaceAnchor()) {
                                rc.setIndicatorString("Huzzah, placed anchor!");
                                rc.placeAnchor();
                            }
                        } else {
                            Motion.moveRandomly(rc);
                            continue;
                        }
                    }
                    updateClosestHeadquarters();
                    if (rc.canTakeAnchor(closestHeadquarters, Anchor.STANDARD)) {
                        rc.takeAnchor(closestHeadquarters, Anchor.STANDARD);
                        System.out.println("Taken Anchor!");
                        continue;
                    }
                    mapInfo = rc.senseNearbyMapInfos();
                    if (adamantiumAmount + manaAmount + elixirAmount >= resourceCollectAmount) {
                        Motion.bug(rc, mapInfo, closestHeadquarters);
                        if (closestHeadquarters.distanceSquaredTo(me) <= rc.getType().visionRadiusSquared) {
                            attemptTransfer();
                        }
                        rc.setIndicatorString("Pathfinding to HQ");
                        continue;
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
                            for (Direction d : directions) {
                                MapLocation adjSpot = prioritizedWellInfoLocation.add(d);
                                if (!rc.canSenseLocation(adjSpot)) {
                                    continue;
                                }
                                if (rc.sensePassability(adjSpot) && rc.senseRobotAtLocation(adjSpot) == null) {
                                    emptySpots += 1;
                                }
                            }
                            if (emptySpots >= 2) {
                                Motion.bug(rc, mapInfo, prioritizedWellInfoLocation);
                                rc.setIndicatorString("Wandering... (Next up: Pathfinding to Well)");
                                closestWell = prioritizedWellInfoLocation;
                                state = 1;
                                continue;
                            }
                        }
                    }
                    Motion.spreadRandomly(rc, me, closestHeadquarters);
                } else if (state == 1) {
                    rc.setIndicatorString("Pathfinding to Well...");
                    mapInfo = rc.senseNearbyMapInfos();
                    Motion.bug(rc, mapInfo, closestWell);
                    me = rc.getLocation();
                    if (rc.canCollectResource(closestWell, -1)) {
                        rc.collectResource(closestWell, -1);
                        state = 2;
                    }
                } else if (state == 2) {
                    rc.setIndicatorString("Collecting resources...");
                    if (rc.canCollectResource(closestWell, -1)
                            && adamantiumAmount + manaAmount + elixirAmount < resourceCollectAmount) {
                        rc.collectResource(closestWell, -1);
                        Motion.circleAroundTarget(rc, me, closestWell);
                    } else {
                        state = 0;
                    }
                }
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

    private void attemptCollection() throws GameActionException {
        if (rc.canCollectResource(closestWell, -1) && adamantiumAmount + manaAmount + elixirAmount < resourceCollectAmount) {
            rc.collectResource(closestWell, -1);
            state = 2;
        }
    }

    private boolean attemptTransfer() throws GameActionException {
        boolean transferSuccess = false;
        if (rc.canTransferResource(closestHeadquarters, ResourceType.ADAMANTIUM, adamantiumAmount)) {
            rc.transferResource(closestHeadquarters, ResourceType.ADAMANTIUM, adamantiumAmount);
            transferSuccess = true;
        }
        if (rc.canTransferResource(closestHeadquarters, ResourceType.MANA, manaAmount)) {
            rc.transferResource(closestHeadquarters, ResourceType.MANA, manaAmount);
            transferSuccess = true;
        }
        if (rc.canTransferResource(closestHeadquarters, ResourceType.ELIXIR, elixirAmount)) {
            rc.transferResource(closestHeadquarters, ResourceType.ELIXIR, elixirAmount);
            transferSuccess = true;
        }
        return transferSuccess;
    }

    private void updateClosestHeadquarters() throws GameActionException {
        closestHeadquarters = headquarters[0];
        for (MapLocation hq : headquarters) {
            if (hq != null) {
                // if (hq.distanceSquaredTo(me) > rc.getType().visionRadiusSquared) {
                //     continue;
                // }
                if (closestHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                    closestHeadquarters = hq;
                }
            }
        }
    }
}
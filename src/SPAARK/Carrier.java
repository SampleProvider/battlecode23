package SPAARK;

import battlecode.common.*;

import java.util.Random;

public strictfp class Carrier {
    private RobotController rc;
    
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

    private ResourceType prioritizedResourceType = ResourceType.ADAMANTIUM;
    private int adamantiumAmount = 0;
    private int manaAmount = 0;
    private int elixirAmount = 0;
    private int resourceCollectAmount = 20;

    private MapInfo[] mapInfo;
    private WellInfo[] wellInfo;
    private MapLocation[] headquarters;
    private MapLocation closestHeadquarters;

    private int state = 0;
    // state
    // 0 is wander
    // 1 is pathfinding
    // 2 is collecting
    // 3 is transferring
    
    private Direction[] path = new Direction[0];
    private int pathIndex = 0;
    private int pathBlocked = 0;

    public Carrier(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
            int hqCount = 0;
            for (int i = 1; i <= 4; i++) {
                if (rc.readSharedArray(i) >> 12 == 1) hqCount++;
            }
            headquarters = new MapLocation[hqCount];
            for (int i = 0; i < hqCount; i++) {
                int data = rc.readSharedArray(i+1);
                headquarters[i] = new MapLocation(data & 0b111111, (data >> 6) & 0b111111);
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException at Carrier constructor");
            e.printStackTrace();
        }
        catch (Exception e) {
            System.out.println("Exception at Carrier constructor");
            e.printStackTrace();
        }
        finally {
            Clock.yield();
        }
        run();
    }
    private void run() {
        while (true) {
            try {
                turnCount++;
                MapLocation me = rc.getLocation();
                adamantiumAmount = rc.getResourceAmount(ResourceType.ADAMANTIUM);
                manaAmount = rc.getResourceAmount(ResourceType.MANA);
                elixirAmount = rc.getResourceAmount(ResourceType.ELIXIR);
                if (state == 0) {
                    rc.setIndicatorString("Wandering...");
                    if (rc.canCollectResource(me, -1)) {
                        rc.collectResource(me, -1);
                        state = 2;
                        continue;
                    }
                    mapInfo = rc.senseNearbyMapInfos();
                    if (adamantiumAmount + manaAmount + elixirAmount >= resourceCollectAmount) {
                        closestHeadquarters = headquarters[0];
                        for (MapLocation hq : headquarters) {
                            if (hq != null) {
                                if (hq.distanceSquaredTo(me) > rc.getType().visionRadiusSquared) {
                                    continue;
                                }
                                if (closestHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                                    closestHeadquarters = hq;
                                }
                            }
                        }
                        if (closestHeadquarters.distanceSquaredTo(me) <= rc.getType().visionRadiusSquared) {
                            if (rc.canTransferResource(closestHeadquarters, ResourceType.ADAMANTIUM, adamantiumAmount)) {
                                rc.transferResource(closestHeadquarters, ResourceType.ADAMANTIUM, adamantiumAmount);
                                state = 3;
                                continue;
                            }
                            if (rc.canTransferResource(closestHeadquarters, ResourceType.MANA, manaAmount)) {
                                rc.transferResource(closestHeadquarters, ResourceType.MANA, manaAmount);
                                state = 3;
                                continue;
                            }
                            if (rc.canTransferResource(closestHeadquarters, ResourceType.ELIXIR, elixirAmount)) {
                                rc.transferResource(closestHeadquarters, ResourceType.ELIXIR, elixirAmount);
                                state = 3;
                                continue;
                            }
                            path = BFS.run(rc, mapInfo, closestHeadquarters);
                            if (path.length > 0) {
                                System.out.println("HQ PATH: ");
                                for(Direction d : path){
                                    System.out.print(d);
                                }
                                rc.setIndicatorString("Wandering... (Next up: Pathfinding to HQ)");
                                state = 1;
                                continue;
                            }
                        }
                    }
                    else{
                        wellInfo = rc.senseNearbyWells();
                        if (wellInfo.length > 0) {
                            WellInfo prioritizedWellInfo = wellInfo[0];
                            for (WellInfo w : wellInfo) {
                                if (prioritizedWellInfo.getResourceType() == prioritizedResourceType) {
                                    if (w.getResourceType() == prioritizedResourceType && prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w.getMapLocation().distanceSquaredTo(me)) {
                                        prioritizedWellInfo = w;
                                    }
                                }
                                else {
                                    if (prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w.getMapLocation().distanceSquaredTo(me)) {
                                        prioritizedWellInfo = w;
                                    }
                                }
                            }
                            path = BFS.run(rc, mapInfo, prioritizedWellInfo.getMapLocation());
                            if (path.length > 0) {
                                System.out.println(path);
                                rc.setIndicatorString("Wandering... (Next up: Pathfinding to Well)");
                                state = 1;
                                continue;
                            }
                        }
                    }
                    while (true) {
                        Direction direction = directions[rng.nextInt(directions.length)];
                        if (rc.canMove(direction)) {
                            rc.move(direction);
                            break;
                        }
                    }
                }
                else if (state == 1) {
                    rc.setIndicatorString("Pathfinding...");
                    if (rc.canCollectResource(me, 2)) {
                        rc.collectResource(me, 2);
                        state = 2;
                        continue;
                    }
                    if (adamantiumAmount + manaAmount + elixirAmount >= resourceCollectAmount) {
                        closestHeadquarters = headquarters[0];
                        for (MapLocation hq : headquarters) {
                            if (hq != null) {
                                if (hq.distanceSquaredTo(me) > rc.getType().visionRadiusSquared) {
                                    continue;
                                }
                                if (closestHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                                    closestHeadquarters = hq;
                                }
                            }
                        }
                        if (closestHeadquarters.distanceSquaredTo(me) <= rc.getType().visionRadiusSquared) {
                            if (rc.canTransferResource(closestHeadquarters, ResourceType.ADAMANTIUM, adamantiumAmount)) {
                                rc.transferResource(closestHeadquarters, ResourceType.ADAMANTIUM, adamantiumAmount);
                                state = 3;
                                continue;
                            }
                            if (rc.canTransferResource(closestHeadquarters, ResourceType.MANA, manaAmount)) {
                                rc.transferResource(closestHeadquarters, ResourceType.MANA, manaAmount);
                                state = 3;
                                continue;
                            }
                            if (rc.canTransferResource(closestHeadquarters, ResourceType.ELIXIR, elixirAmount)) {
                                rc.transferResource(closestHeadquarters, ResourceType.ELIXIR, elixirAmount);
                                state = 3;
                                continue;
                            }
                        }
                    }
                    if (pathIndex < path.length) {
                        rc.setIndicatorString("Pathfinding... (Path: " + path[pathIndex] + ")");
                        if (rc.canMove(path[pathIndex])) {
                            rc.move(path[pathIndex]);
                            pathBlocked = 0;
                            pathIndex += 1;
                            if (pathIndex == path.length) {
                                pathIndex = 0;
                                state = 0;
                            }
                        }
                        else if (rc.getMovementCooldownTurns() < 10) {
                            pathBlocked += 1;
                            if (pathBlocked > 5) {
                                pathIndex = 0;
                                state = 0;
                                while (true) {
                                    Direction direction = directions[rng.nextInt(directions.length)];
                                    if (rc.canMove(direction)) {
                                        rc.move(direction);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    else {
                        pathIndex = 0;
                        state = 0;
                        while (true) {
                            Direction direction = directions[rng.nextInt(directions.length)];
                            if (rc.canMove(direction)) {
                                rc.move(direction);
                                break;
                            }
                        }
                    }
                }
                else if (state == 2) {
                    rc.setIndicatorString("Collecting resources...");
                    if (rc.canCollectResource(me, -1)) {
                        rc.collectResource(me, -1);
                    }
                    else {
                        state = 0;
                    }
                }
                else if (state == 3) {
                    rc.setIndicatorString("Transferring resources...");
                    state = 0;
                    if (rc.canTransferResource(closestHeadquarters, ResourceType.ADAMANTIUM, adamantiumAmount)) {
                        rc.transferResource(closestHeadquarters, ResourceType.ADAMANTIUM, adamantiumAmount);
                        state = 3;
                    }
                    if (rc.canTransferResource(closestHeadquarters, ResourceType.MANA, manaAmount)) {
                        rc.transferResource(closestHeadquarters, ResourceType.MANA, manaAmount);
                        state = 3;
                    }
                    if (rc.canTransferResource(closestHeadquarters, ResourceType.ELIXIR, elixirAmount)) {
                        rc.transferResource(closestHeadquarters, ResourceType.ELIXIR, elixirAmount);
                        state = 3;
                    }
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException at Carrier");
                e.printStackTrace();
            // } catch (Exception e) {
            //     System.out.println("Exception at Carrier");
            //     e.printStackTrace();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
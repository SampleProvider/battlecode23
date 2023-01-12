package TEMPLATE1;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Carrier {
    // counts turn count
    static int turnCount = 0;
    static final Random rng = new Random(2023);
    static final Direction[] directions = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
    };

    static ResourceType prioritizedResourceType = ResourceType.ADAMANTIUM;

    static MapInfo[] mapInfo;
    static WellInfo[] wellInfo;

    static int state = 0;
    // state
    // 0 is wander
    // 1 is pathfinding
    // 2 is collecting
    // 3 is transferring
    
    static Direction[] path = new Direction[0];
    static int pathIndex = 0;
    static int pathBlocked = 0;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        rc.setIndicatorString("Initializing");
        while (true) {
            try {
                MapLocation me = rc.getLocation();
                if (state == 0) {
                    while (true) {
                        Direction direction = directions[rng.nextInt(directions.length)];
                        if (rc.canMove(direction)) {
                            rc.move(direction);
                            break;
                        }
                    }
                    if (rc.canCollectResource(me, 2)) {
                        rc.collectResource(me, 2);
                        state = 2;
                        continue;
                    }
                    if (rc.getResourceAmount(ResourceType.ADAMANTIUM) > 0 || rc.getResourceAmount(ResourceType.MANA) > 0 || rc.getResourceAmount(ResourceType.ELIXIR) > 0) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                MapLocation headQuarterLocation = new MapLocation(me.x + dx, me.y + dy);
                                if (rc.canTransferResource(headQuarterLocation, ResourceType.ADAMANTIUM, 1)) {
                                    rc.transferResource(headQuarterLocation, ResourceType.ADAMANTIUM, 1);
                                    state = 3;
                                }
                                if (rc.canTransferResource(headQuarterLocation, ResourceType.MANA, 1)) {
                                    rc.transferResource(headQuarterLocation, ResourceType.MANA, 1);
                                    state = 3;
                                }
                                if (rc.canTransferResource(headQuarterLocation, ResourceType.ELIXIR, 1)) {
                                    rc.transferResource(headQuarterLocation, ResourceType.ELIXIR, 1);
                                    state = 3;
                                }
                            }
                        }
                        if (state == 3) {
                            rc.setIndicatorString("Transferring resources...");
                            continue;
                        }
                    }
                    mapInfo = rc.senseNearbyMapInfos();
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
                            state = 1;
                        }
                    }
                }
                else if (state == 1) {
                    if (rc.canCollectResource(me, 2)) {
                        rc.collectResource(me, 2);
                        state = 2;
                        continue;
                    }
                    if (rc.getResourceAmount(ResourceType.ADAMANTIUM) > 0 || rc.getResourceAmount(ResourceType.MANA) > 0 || rc.getResourceAmount(ResourceType.ELIXIR) > 0) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                MapLocation headQuarterLocation = new MapLocation(me.x + dx, me.y + dy);
                                if (rc.canTransferResource(headQuarterLocation, ResourceType.ADAMANTIUM, 1)) {
                                    rc.transferResource(headQuarterLocation, ResourceType.ADAMANTIUM, 1);
                                    state = 3;
                                }
                                if (rc.canTransferResource(headQuarterLocation, ResourceType.MANA, 1)) {
                                    rc.transferResource(headQuarterLocation, ResourceType.MANA, 1);
                                    state = 3;
                                }
                                if (rc.canTransferResource(headQuarterLocation, ResourceType.ELIXIR, 1)) {
                                    rc.transferResource(headQuarterLocation, ResourceType.ELIXIR, 1);
                                    state = 3;
                                }
                            }
                        }
                        if (state == 3) {
                            rc.setIndicatorString("Transferring resources...");
                            continue;
                        }
                    }
                    if (pathIndex < path.length) {
                        if (rc.canMove(path[pathIndex])) {
                            rc.move(path[pathIndex]);
                            pathBlocked = 0;
                            pathIndex += 1;
                            if (pathIndex == path.length){
                                state = 0;
                            }
                        }
                        else if (rc.getMovementCooldownTurns() < 10) {
                            pathBlocked += 1;
                            if (pathBlocked > 10) {
                                pathIndex = 0;
                                state = 0;
                            }
                        }
                    }
                    else {
                        state = 0;
                    }
                }
                else if (state == 2) {
                    if (rc.canCollectResource(me, 2)) {
                        rc.collectResource(me, 2);
                    }
                    else {
                        state = 0;
                    }
                }
                else if (state == 3) {
                    state = 0;
                    if (rc.getResourceAmount(ResourceType.ADAMANTIUM) > 0 || rc.getResourceAmount(ResourceType.MANA) > 0 || rc.getResourceAmount(ResourceType.ELIXIR) > 0) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                MapLocation headQuarterLocation = new MapLocation(me.x + dx, me.y + dy);
                                if (rc.canTransferResource(headQuarterLocation, ResourceType.ADAMANTIUM, 1)) {
                                    rc.transferResource(headQuarterLocation, ResourceType.ADAMANTIUM, 1);
                                    state = 3;
                                }
                                if (rc.canTransferResource(headQuarterLocation, ResourceType.MANA, 1)) {
                                    rc.transferResource(headQuarterLocation, ResourceType.MANA, 1);
                                    state = 3;
                                }
                                if (rc.canTransferResource(headQuarterLocation, ResourceType.ELIXIR, 1)) {
                                    rc.transferResource(headQuarterLocation, ResourceType.ELIXIR, 1);
                                    state = 3;
                                }
                            }
                        }
                        if (state == 3) {
                            rc.setIndicatorString("Transferring resources...");
                            continue;
                        }
                    }
                }
            }
            catch (GameActionException e) {
                System.out.println("GameActionException at Carrier");
                e.printStackTrace();
            }
            catch (Exception e) {
                System.out.println("Exception at Carrier");
                e.printStackTrace();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
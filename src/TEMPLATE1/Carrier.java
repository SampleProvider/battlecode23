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
    
    static Direction[] path = new Direction[0];
    static int pathIndex = 0;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        rc.setIndicatorString("Initializing");
        while (true) {
            try {
                if (rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR) > 4) {
                    MapLocation me = rc.getLocation();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            // so inefficient
                            // check if headquarters
                            MapLocation headQuarterLocation = new MapLocation(me.x + dx, me.y + dy);
                            if (rc.canTransferResource(headQuarterLocation, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM))) {
                                rc.setIndicatorString("Transferring, now have, AD:" + 
                                    rc.getResourceAmount(ResourceType.ADAMANTIUM));
                                rc.transferResource(headQuarterLocation, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM));
                            }
                            if (rc.canTransferResource(headQuarterLocation, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA))) {
                                rc.setIndicatorString("Transferring, now have, MN:" + 
                                    rc.getResourceAmount(ResourceType.MANA));
                                rc.transferResource(headQuarterLocation, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA));
                            }
                            if (rc.canTransferResource(headQuarterLocation, ResourceType.ELIXIR, rc.getResourceAmount(ResourceType.ELIXIR))) {
                                rc.setIndicatorString("Transferring, now have, EX:" + 
                                    rc.getResourceAmount(ResourceType.ELIXIR));
                                rc.transferResource(headQuarterLocation, ResourceType.ELIXIR, rc.getResourceAmount(ResourceType.ELIXIR));
                            }
                        }
                    }
                }
                if (path.length == pathIndex) {
                    MapLocation me = rc.getLocation();
                    // if (rc.canCollectResource(me, 39)) {
                    //     rc.collectResource(me, 39);
                    // }
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            MapLocation wellLocation = new MapLocation(me.x + dx, me.y + dy);
                            if (rc.canCollectResource(wellLocation, -1)) {
                                // if (rng.nextBoolean()) {
                                    rc.collectResource(wellLocation, -1);
                                    rc.setIndicatorString("Collecting, now have, AD:" + 
                                        rc.getResourceAmount(ResourceType.ADAMANTIUM) + 
                                        " MN: " + rc.getResourceAmount(ResourceType.MANA) + 
                                        " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
                                // }
                            }
                        }
                    }
                    mapInfo = rc.senseNearbyMapInfos();
                    if (rc.getResourceAmount(ResourceType.ADAMANTIUM) == 0 && rc.getResourceAmount(ResourceType.MANA) == 0 && rc.getResourceAmount(ResourceType.ELIXIR) == 0) {
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
                            // System.out.println("running BFS!");
                        }
                        else {
                            path = new Direction[10];
                            for (int i = 0;i < 10;i++) {
                                path[i] = directions[rng.nextInt(directions.length)];
                            }
                        }
                    }
                    else if (rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR) > 4) {
                        path = new Direction[10];
                        for (int i = 0;i < 10;i++) {
                            path[i] = directions[rng.nextInt(directions.length)];
                        }
                    }
                    pathIndex = 0;
                }
                try {
                    if (rc.canMove(path[pathIndex])) {
                        rc.move(path[pathIndex]);
                        pathIndex += 1;
                    }
                    else {
                        path = new Direction[2];
                        for (int i = 0;i < 2;i++) {
                            path[i] = directions[rng.nextInt(directions.length)];
                        }
                        pathIndex = 0;
                    }
                }
                catch (GameActionException e) {
                    // path = new Direction[10];
                    // for (int i = 0;i < 10;i++) {
                    //     path[i] = directions[rng.nextInt(directions.length)];
                    // }
                    // pathIndex = 0;
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
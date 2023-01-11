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
                if (path.length == pathIndex) {
                    MapLocation me = rc.getLocation();
                    if (rc.canCollectResource(me, 39)) {
                        rc.collectResource(me, 39);
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
                    else {
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
                catch (Exception e) {
                    path = new Direction[10];
                    for (int i = 0;i < 10;i++) {
                        path[i] = directions[rng.nextInt(directions.length)];
                    }
                    pathIndex = 0;
                }
                if (rc.getResourceAmount(prioritizedResourceType) == 39) {
                    MapLocation me = rc.getLocation();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            MapLocation headQuarterLocation = new MapLocation(me.x + dx, me.y + dy);
                            if (rc.canTransferResource(headQuarterLocation, null, -1) && rc.sensePassability(headQuarterLocation)) {
                                rc.transferResource(headQuarterLocation, null, -1);
                            }
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
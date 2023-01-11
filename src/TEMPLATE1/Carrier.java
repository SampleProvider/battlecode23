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
                if (state == 0) {
                    while (true) {
                        if (rc.canMove(directions[rng.nextInt(directions.length)])) {
                            rc.move(directions[rng.nextInt(directions.length)]);
                            break;
                        }
                    }
                }
                else if (state == 1) {

                }
                else if (state == 2) {

                }
                else if (state == 3) {

                }
                MapLocation me = rc.getLocation();
                if (rc.canCollectResource(me, -1)) {
                    rc.collectResource(me, -1);
                    state = 2;
                }
                if (path.length <= pathIndex) {
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
                            if (path.length == 0) {
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
                    }
                    else {
                        path = new Direction[10];
                        for (int i = 0;i < 10;i++) {
                            path[i] = directions[rng.nextInt(directions.length)];
                        }
                    }
                    pathIndex = 0;
                    state = 1;
                }
                if (rc.getResourceAmount(ResourceType.ADAMANTIUM) > 0) {
                    MapLocation me = rc.getLocation();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            MapLocation headQuarterLocation = new MapLocation(me.x + dx, me.y + dy);
                            if (rc.canTransferResource(headQuarterLocation, ResourceType.ADAMANTIUM, 1) && !rc.sensePassability(headQuarterLocation)) {
                                rc.transferResource(headQuarterLocation, ResourceType.ADAMANTIUM, 1);
                            }
                            if (rc.canTransferResource(headQuarterLocation, ResourceType.MANA, 1) && !rc.sensePassability(headQuarterLocation)) {
                                rc.transferResource(headQuarterLocation, ResourceType.MANA, 1);
                            }
                            if (rc.canTransferResource(headQuarterLocation, ResourceType.ELIXIR, 1) && !rc.sensePassability(headQuarterLocation)) {
                                rc.transferResource(headQuarterLocation, ResourceType.ELIXIR, 1);
                            }
                        }
                    }
                }
                if (path.length > 0) {
                    if (rc.canMove(path[pathIndex])) {
                        rc.move(path[pathIndex]);
                        pathIndex += 1;
                        if (pathIndex == path.length){
                            state = 0;
                        }
                        pathBlocked = 0;
                    }
                    else if (rc.getMovementCooldownTurns() < 10) {
                        pathBlocked += 1;
                        if (pathBlocked > 10) {
                            path = new Direction[2];
                            for (int i = 0;i < 2;i++) {
                                path[i] = directions[rng.nextInt(directions.length)];
                            }
                            pathIndex = 0;
                            state = 0;
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
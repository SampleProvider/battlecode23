package TEMPLATE1;

import battlecode.common.*;

import java.lang.StringBuilder;
import java.util.LinkedList;
import java.util.Queue;

public class BFS {
    static final Direction[] BFSDirections = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
    };
    @SuppressWarnings("unused")
    public static Direction[] run(RobotController rc,MapInfo[] mapInfo,MapLocation dest) throws GameActionException {
        MapLocation me = rc.getLocation();
        int visionRadius = (int) Math.sqrt(rc.getType().visionRadiusSquared);
        int visionDiameter = visionRadius * 2 + 1;
        int[][] range = new int[visionDiameter][visionDiameter];
        // MapLocation[] a = rc.getAllLocationsWithinRadiusSquared();
        Direction[][] currents = new Direction[visionDiameter][visionDiameter];
        for(MapInfo m : mapInfo){
            MapLocation mapLocation = m.getMapLocation();
            currents[mapLocation.x - me.x + visionRadius][mapLocation.y - me.y + visionRadius] = m.getCurrentDirection();
        }
        Queue<Integer> queue = new LinkedList<Integer>();
        queue.add(dest.x - me.x + visionRadius + visionDiameter * (dest.y - me.y + visionRadius));
        range[dest.x - me.x + visionRadius][dest.y - me.y + visionRadius] = 1;
        while(queue.size() > 0){
            int c = queue.poll();
            int x = c % visionDiameter;
            int y = (int) c / visionDiameter;
            for(int dy = -1;dy <= 1;dy++){
                for(int dx = -1;dx <= 1;dx++){
                    if(dx == 0 && dy == 0){
                        continue;
                    }
                    if(Math.pow(x + dx - visionRadius,2) + Math.pow(y + dy - visionRadius,2) > rc.getType().visionRadiusSquared){
                        continue;
                    }
                    try{
                        if(rc.sensePassability(new MapLocation(me.x + x + dx - visionRadius,me.y + y + dy - visionRadius)) == false){
                            continue;
                        }
                    }
                    catch(Exception e){
                        continue;
                    }
                    if(range[x + dx][y + dy] == 0){
                        range[x + dx][y + dy] = range[x][y] + 1;
                        queue.add(x + dx + visionDiameter * (y + dy));
                        if(x + dx == visionRadius && y + dy == visionRadius){
                            return getPath(rc,dest,visionRadius,visionDiameter,range,currents);
                        }
                    }
                    if(currents[x + dx][y + dy] != Direction.CENTER){
                        int cx = dx + currents[x + dx][y + dy].dx;
                        int cy = dy + currents[x + dx][y + dy].dy;
                        if(Math.pow(x + cx - visionRadius,2) + Math.pow(y + cy - visionRadius,2) > rc.getType().visionRadiusSquared){
                            continue;
                        }
                        if(range[x + cx][y + cy] == 0){
                            range[x + cx][y + cy] = range[x][y] + 1;
                            queue.add(x + cx + visionDiameter * (y + cy));
                            if(x + cx == visionRadius && y + cy == visionRadius){
                                return getPath(rc,dest,visionRadius,visionDiameter,range,currents);
                            }
                        }
                    }
                }
            }
        }
        return new Direction[0];
    }
    private static Direction[] getPath(RobotController rc,MapLocation dest,int visionRadius,int visionDiameter,int[][] range,Direction[][] currents) {
        Direction[] path = new Direction[range[visionRadius][visionRadius] - 1];
        int pathIndex = 0;
        Queue<Integer> queue = new LinkedList<Integer>();
        queue.add(visionRadius + visionDiameter * visionRadius);
        while(queue.size() > 0){
            int c = queue.poll();
            int x = c % visionDiameter;
            int y = (int) c / visionDiameter;
            for(int dy = -1;dy <= 1;dy++){
                for(int dx = -1;dx <= 1;dx++){
                    if(dx == 0 && dy == 0){
                        continue;
                    }
                    if(x + dx >= 0 && x + dx < visionDiameter && y + dy >= 0 && y + dy < visionDiameter){
                        if(range[x + dx][y + dy] == range[x][y] - 1){
                            int direction = dx + dy * 3 + 4;
                            if(direction > 4){
                                direction -= 1;
                            }
                            path[pathIndex] = BFSDirections[direction];
                            if(range[x + dx][y + dy] == 1){
                                return path;
                            }
                            pathIndex++;
                            int cx = dx + currents[x + dx][y + dy].dx;
                            int cy = dy + currents[x + dx][y + dy].dy;
                            if(x + cx >= 0 && x + cx < visionDiameter && y + cy >= 0 && y + cy < visionDiameter){
                                if(range[x + cx][y + cy] == range[x][y] - 1){
                                    queue.add(x + cx + visionDiameter * (y + cy));
                                    break;
                                }
                            }
                            queue.add(x + dx + visionDiameter * (y + dy));
                            break;
                        }
                    }
                }
                if(queue.size() > 0){
                    break;
                }
            }
        }
        return new Direction[0];
    }
}

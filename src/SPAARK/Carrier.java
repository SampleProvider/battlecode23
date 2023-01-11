package SPAARK;

import battlecode.common.*;

public strictfp class Carrier {
    // counts turn count
    static int turnCount = 0;
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

    public static void init(RobotController rc) {
        rc.setIndicatorString("Initializing");
    }
    public static void run(RobotController rc) {
        while (true) {
            try {
                // code
                turnCount++;
                throw new GameActionException(null, null);
            } catch (GameActionException e) {
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
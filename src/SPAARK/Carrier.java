package SPAARK;

import battlecode.common.*;

public strictfp class Carrier {
    private RobotController rc;
    
    private int turnCount = 0;

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

    private MapInfo[] mapInfo;
    private WellInfo[] wellInfo;
    private MapLocation[] headquarters = new MapLocation[4];
    
    private Direction[] path = new Direction[0];
    private int pathIndex = 0;

    public Carrier(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
            for (int i = 1; i <= 4; i++) {
                int data = rc.readSharedArray(i);
                if (data >> 12 == 1) {
                    headquarters[i] = new MapLocation(data & 0b111111, (data >> 6) & 0b111111);
                }
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
                // code
                turnCount++;
            // } catch (GameActionException e) {
            //     System.out.println("GameActionException at Carrier");
            //     e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Carrier");
                e.printStackTrace();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
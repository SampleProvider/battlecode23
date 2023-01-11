package SPAARK;

import battlecode.common.*;

public strictfp class HeadQuarters {
    // counts turn count
    static int turnCount = 0;

    public static void init(RobotController rc) {
        try {
            rc.setIndicatorString("Initializing");
            // setting headquarter locations
            MapLocation loc = rc.getLocation();
            if (rc.readSharedArray(1) >> 12 == 0) {
                rc.writeSharedArray(1, 0b1000000000000 | (loc.y << 6) | loc.x);
            } else if (rc.readSharedArray(2) >> 12 == 0) {
                rc.writeSharedArray(2, 0b1000000000000 | (loc.y << 6) | loc.x);
            } else if (rc.readSharedArray(3) >> 12 == 0) {
                rc.writeSharedArray(3, 0b1000000000000 | (loc.y << 6) | loc.x);
            } else if (rc.readSharedArray(4) >> 12 == 0) {
                rc.writeSharedArray(4, 0b1000000000000 | (loc.y << 6) | loc.x);
            } else {
                throw new Exception("Too many HeadQuarters!");
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException at HeadQuarters init");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at HeadQuarters init");
            e.printStackTrace();
        } finally {
            Clock.yield();
        }
    }
    public static void run(RobotController rc) {
        while (true) {
            try {
                // code
                turnCount++;
                throw new GameActionException(null, null);
            } catch (GameActionException e) {
                System.out.println("GameActionException at HeadQuarters");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at HeadQuarters");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}
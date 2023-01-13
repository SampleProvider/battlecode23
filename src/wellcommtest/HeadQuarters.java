package wellcommtest;

import battlecode.common.*;

public strictfp class HeadQuarters {
    RobotController rc;
    
    private int turnCount = 0;
    private GameState gameState = new GameState();;

    public HeadQuarters(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
            MapLocation loc = rc.getLocation();
            if (!GameState.hasLocation(rc.readSharedArray(1))) {
                rc.writeSharedArray(1, GameState.intifyLocation(loc));
            } else if (!GameState.hasLocation(rc.readSharedArray(2))) {
                rc.writeSharedArray(2, GameState.intifyLocation(loc));
            } else if (!GameState.hasLocation(rc.readSharedArray(3))) {
                rc.writeSharedArray(3, GameState.intifyLocation(loc));
            } else if (!GameState.hasLocation(rc.readSharedArray(4))) {
                rc.writeSharedArray(4, GameState.intifyLocation(loc));
            } else {
                throw new Exception("Too many HeadQuarters!");
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException at HeadQuarters constructor");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at HeadQuarters constructor");
            e.printStackTrace();
        } finally {
            Clock.yield();
        }
        run();
    }
    private void run() {
        while (true) {
            try {
                // controller logic
                turnCount++;
            // } catch (GameActionException e) {
            //     System.out.println("GameActionException at HeadQuarters");
            //     e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at HeadQuarters");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}
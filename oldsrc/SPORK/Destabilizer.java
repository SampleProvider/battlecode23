package SPORK;

import battlecode.common.*;

public strictfp class Destabilizer {
    private RobotController rc;
    private MapLocation me;
    GlobalArray gArray = new GlobalArray();

    private static int turnCount = 0;

    public Destabilizer(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
        // } catch (GameActionException e) {
        //     System.out.println("GameActionException at Destabilizer constructor");
        //     e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Destabilizer constructor");
            e.printStackTrace();
        } finally {
            Clock.yield();
        }
        run();
    }

    public void run() {
        while (true) {
            try {
                // code
                turnCount++;
            // } catch (GameActionException e) {
            //     System.out.println("GameActionException at Destabilizer");
            //     e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Destabilizer");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}
package SPAARK;

import battlecode.common.*;

public strictfp class Booster {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();

    private static int turnCount = 0;

    public Booster(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
        // } catch (GameActionException e) {
        //     System.out.println("GameActionException at Booster constructor");
        //     e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Booster constructor");
            e.printStackTrace();
        } finally {
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
            //     System.out.println("GameActionException at Booster");
            //     e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Booster");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}
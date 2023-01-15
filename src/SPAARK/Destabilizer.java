package SPAARK;

import battlecode.common.*;

public strictfp class Destabilizer {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();

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

    private void run() {
        while (true) {
            try {
                // code
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
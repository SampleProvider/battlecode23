package SPAARK_TEST;

import battlecode.common.*;

public strictfp class Destabilizer {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    protected StringBuilder indicatorString = new StringBuilder();

    public Destabilizer(RobotController rc) {
        try {
            this.rc = rc;
        // } catch (GameActionException e) {
        //     System.out.println("GameActionException at Destabilizer constructor");
        //     e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Destabilizer constructor");
            e.printStackTrace();
        } finally {
            run();
        }
    }

    private void run() {
        while (true) {
            try {
                me = rc.getLocation();
                round = rc.getRoundNum();
                indicatorString = new StringBuilder();
                // code
            // } catch (GameActionException e) {
            //     System.out.println("GameActionException at Destabilizer");
            //     e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Destabilizer");
                e.printStackTrace();
            } finally {
                rc.setIndicatorString(indicatorString.toString());
                Clock.yield();
            }
        }
    }
}
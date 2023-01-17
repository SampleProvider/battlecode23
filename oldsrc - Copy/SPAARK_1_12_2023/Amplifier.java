package SPAARK_1_12_2023;

import battlecode.common.*;

public strictfp class Amplifier {
    private RobotController rc;

    static int turnCount = 0;

    public Amplifier(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
        // } catch (GameActionException e) {
        //     System.out.println("GameActionException at Amplifier constructor");
        //     e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Amplifier constructor");
            e.printStackTrace();
        }
        finally {
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
            //     System.out.println("GameActionException at Amplifier");
            //     e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Amplifier");
                e.printStackTrace();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
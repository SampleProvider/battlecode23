package TEMPLATE;

import battlecode.common.*;

public strictfp class HeadQuarters {
    // counts turn count
    static int turnCount = 0;

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
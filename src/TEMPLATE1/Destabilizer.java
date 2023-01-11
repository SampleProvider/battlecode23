package TEMPLATE1;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Destabilizer {
    // counts turn count
    static int turnCount = 0;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        rc.setIndicatorString("Initializing");
        while (true) {
            try {
                // code
                throw new GameActionException(null, "temp");
            }
            catch (GameActionException e) {
                System.out.println("GameActionException at Destabilizer");
                e.printStackTrace();
            }
            catch (Exception e) {
                System.out.println("Exception at Destabilizer");
                e.printStackTrace();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
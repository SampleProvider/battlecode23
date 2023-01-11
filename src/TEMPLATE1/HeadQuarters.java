package TEMPLATE1;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class HeadQuarters {
    // counts turn count
    static int turnCount = 0;
    static final Random rng = new Random(2023);
    static final Direction[] directions = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
    };

    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        rc.setIndicatorString("Initializing");
        while (true) {
            try {
                // Direction dir = directions[rng.nextInt(directions.length)];
                // MapLocation newLoc = rc.getLocation().add(dir);
                if (rc.canBuildRobot(RobotType.CARRIER, rc.getLocation().add(Direction.NORTH))) {
                    rc.buildRobot(RobotType.CARRIER, rc.getLocation().add(Direction.NORTH));
                }
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
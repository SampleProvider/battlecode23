package TEMPLATE1;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Launcher {
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
                int radius = rc.getType().actionRadiusSquared;
                Team opponent = rc.getTeam().opponent();
                RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
                if (enemies.length > 0) {
                    MapLocation toAttack = enemies[0].location;
                    // MapLocation toAttack = rc.getLocation().add(Direction.EAST);
                    if (rc.canAttack(toAttack)) {
                        rc.setIndicatorString("Attacking");
                        rc.attack(toAttack);
                    }
                }
                Direction direction = directions[rng.nextInt(directions.length)];
                if (rc.canMove(direction)) {
                    rc.move(direction);
                }
            }
            catch (GameActionException e) {
                System.out.println("GameActionException at Launcher");
                e.printStackTrace();
            }
            catch (Exception e) {
                System.out.println("Exception at Launcher");
                e.printStackTrace();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
package SPAARK;

import battlecode.common.*;

import java.util.Random;

public strictfp class Launcher {
    RobotController rc;

    private int turnCount = 0;

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

    public Launcher(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
        // } catch (GameActionException e) {
        //     System.out.println("GameActionException at Launcher constructor");
        //     e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Launcher constructor");
            e.printStackTrace();
        }
        finally {
            Clock.yield();
        }
        run();
    }
    private void run() {
        while (true) {
            try {
                turnCount++;
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
            } catch (GameActionException e) {
                System.out.println("GameActionException at Launcher");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Launcher");
                e.printStackTrace();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
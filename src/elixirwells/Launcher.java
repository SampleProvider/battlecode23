package elixirwells;

import battlecode.common.*;

import java.util.Random;

public strictfp class Launcher {
    private RobotController rc;
    private MapLocation me;

    private int turnCount = 0;

    private static final Random rng = new Random(2023);
    private boolean isAttacking;
    private static int carrierCounter = 0;

    private static final Direction[] directions = {
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
            carrierCounter++;
            isAttacking = carrierCounter % 2 == 0;
        // } catch (GameActionException e) {
        //     System.out.println("GameActionException at Carrier constructor");
        //     e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Launcher constructor");
            e.printStackTrace();
        } finally {
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

                MapLocation m8 = GameState.parseLocation(rc.readSharedArray(8));
                MapLocation m9 = GameState.parseLocation(rc.readSharedArray(9));
                MapLocation m10 = GameState.parseLocation(rc.readSharedArray(10));
                MapLocation m11 = GameState.parseLocation(rc.readSharedArray(11));

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
            } finally {
                Clock.yield();
            }
        }
    }
}
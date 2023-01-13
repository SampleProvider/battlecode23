package SPAARK;

import battlecode.common.*;

import java.util.Random;

public strictfp class Launcher {
    RobotController rc;

    private int turnCount = 0;

    static final Random rng = new Random(2023);
    private boolean isAttacking;
    static int carrierCounter = 0;

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

                int locationID8 = rc.readSharedArray(8);
                int locationID9 = rc.readSharedArray(9);
                int locationID10 = rc.readSharedArray(10);
                int locationID11 = rc.readSharedArray(11);

                MapLocation m8 = new MapLocation(locationID8 & 0b111111, (locationID8 >> 6) & 0b111111);
                MapLocation m9 = new MapLocation(locationID9 & 0b111111, (locationID9 >> 6) & 0b111111);
                MapLocation m10 = new MapLocation(locationID10 & 0b111111, (locationID10 >> 6) & 0b111111);
                MapLocation m11 = new MapLocation(locationID11 & 0b111111, (locationID11 >> 6) & 0b111111);


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
package SPAARK;

import battlecode.common.*;

import java.util.Random;

public strictfp class HeadQuarters {
    RobotController rc;

    private int turnCount = 0;
    private int carriers = -100;
    private int carrierCooldown = 0;
    private boolean producedAnchor = false;

    private int[] amplifierState = new int[]{0,0,0,0};

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

    public HeadQuarters(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
            // setting headquarter locations
            int locInt = GameState.intifyLocation(rc.getLocation());
            if (!GameState.hasLocation(rc.readSharedArray(1))) {
                rc.writeSharedArray(1, locInt);
            } else if (!GameState.hasLocation(rc.readSharedArray(2))) {
                rc.writeSharedArray(2, locInt);
            } else if (!GameState.hasLocation(rc.readSharedArray(3))) {
                rc.writeSharedArray(3, locInt);
            } else if (!GameState.hasLocation(rc.readSharedArray(4))) {
                rc.writeSharedArray(4, locInt);
            } else {
                throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Too many HeadQuarters!");
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException at HeadQuarters constructor");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at HeadQuarters constructor");
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
                Direction dir = directions[rng.nextInt(directions.length)];
                MapLocation newLoc = rc.getLocation().add(dir);
                if (rc.canBuildAnchor(Anchor.STANDARD) && turnCount >= 300) {
                    rc.buildAnchor(Anchor.STANDARD);
                    System.out.println("Anchor Produced!");
                    producedAnchor = true;
                    carrierCooldown = 0;
                }
                if (carrierCooldown <= 4 || turnCount < 300) {
                    if (rc.canBuildRobot(RobotType.CARRIER, newLoc) && carriers <= 0) {
                        rc.buildRobot(RobotType.CARRIER, newLoc);
                        carriers += 10;
                        carrierCooldown += 1;
                        // if (turnCount >= 300) {
                        // carriers += 20;
                        // }
                    } else if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                        rc.buildRobot(RobotType.LAUNCHER, newLoc);
                    }
                }
                carriers -= 1;
                for (int a = 0;a < 4;a++) {
                    if (((rc.readSharedArray(14 + a * 2) >> 15) & 1) == amplifierState[a]) {
                        rc.writeSharedArray(14 + a * 2,0);
                        rc.writeSharedArray(15 + a * 2,0);
                    }
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
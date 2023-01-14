package SPAARK;

import battlecode.common.*;

import java.util.Random;

public strictfp class HeadQuarters {
    RobotController rc;

    private int turnCount = 0;
    private int carriers = -100;
    private int carrierCooldown = 0;
    private boolean producedAnchor = false;

    private boolean isPrimaryHQ = false;

    private static int[] amplifierToggleState = new int[]{0,0,0,0};
    private static boolean[] amplifierAliveState = new boolean[]{false, false, false, false};

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
            int locInt = GlobalArray.intifyLocation(rc.getLocation());
            if (!GlobalArray.hasLocation(rc.readSharedArray(1))) {
                rc.writeSharedArray(1, locInt);
                isPrimaryHQ = true;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(2))) {
                rc.writeSharedArray(2, locInt);
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(3))) {
                rc.writeSharedArray(3, locInt);
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(4))) {
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
                if (rc.canBuildAnchor(Anchor.STANDARD) && turnCount >= 300) {
                    rc.buildAnchor(Anchor.STANDARD);
                    System.out.println("Anchor Produced!");
                    producedAnchor = true;
                    carrierCooldown = 0;
                }
                // if (turnCount == 1) {
                //     rc.buildRobot(RobotType.CARRIER, newLoc);
                // }
                // if (turnCount == 2) {
                //     rc.buildRobot(RobotType.LAUNCHER, newLoc);
                // }
                Direction dir = directions[rng.nextInt(directions.length)];
                MapLocation newLoc = rc.getLocation().add(dir);
                if (rc.senseRobotAtLocation(newLoc) != null) {
                    dir = directions[rng.nextInt(directions.length)];
                    newLoc = rc.getLocation().add(dir);
                }
                if (rc.senseRobotAtLocation(newLoc) != null) {
                    dir = directions[rng.nextInt(directions.length)];
                    newLoc = rc.getLocation().add(dir);
                }
                if (isPrimaryHQ) {
                    for (int a = 0;a < 4;a++) {
                        if (((rc.readSharedArray(14 + a) >> 15) & 1) == amplifierToggleState[a] || (rc.readSharedArray(14 + a) >> 14) % 2 == 0) {
                            rc.writeSharedArray(14 + a,0);
                            amplifierAliveState[a] = false;
                        }
                        else {
                            amplifierToggleState[a] = (rc.readSharedArray(14 + a) >> 15) & 1;
                            amplifierAliveState[a] = true;
                        }
                    }
                }
                if (carrierCooldown <= 4 || turnCount < 300) {
                    boolean canProduceAmplifier = false;
                    for (boolean a : amplifierAliveState) {
                        if (a == false) {
                            canProduceAmplifier = true;
                        }
                    }
                    if (rc.canBuildRobot(RobotType.CARRIER, newLoc) && carriers <= 0) {
                        rc.buildRobot(RobotType.CARRIER, newLoc);
                        carriers += 10;
                        carrierCooldown += 1;
                        // if (turnCount >= 300) {
                        // carriers += 20;
                        // }
                    } else if (rc.canBuildRobot(RobotType.AMPLIFIER, newLoc) && rng.nextInt(20) == 1 && canProduceAmplifier) {
                        rc.buildRobot(RobotType.AMPLIFIER, newLoc);
                    } else if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                        rc.buildRobot(RobotType.LAUNCHER, newLoc);
                    }
                }
                carriers -= 1;
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
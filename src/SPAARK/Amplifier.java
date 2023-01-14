package SPAARK;

import battlecode.common.*;

import java.util.Random;

public strictfp class Amplifier {
    private RobotController rc;
    private MapLocation me;

    private int turnCount = 0;

    private final Random rng = new Random(2023);
    private final Direction[] directions = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
    };

    private RobotType prioritizedRobotType = RobotType.LAUNCHER;
    
    private int amplifierID = 0;

    private int arrayIndex1;
    private int arrayIndex2;
    private int lastArrayIndex2;

    public Amplifier(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
            int locInt = GlobalArray.intifyLocation(rc.getLocation());
            if (!GlobalArray.hasLocation(rc.readSharedArray(14))) {
                rc.writeSharedArray(14, GlobalArray.toggleBit(locInt,15));
                amplifierID = 14;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(16))) {
                rc.writeSharedArray(15, GlobalArray.toggleBit(locInt,15));
                amplifierID = 16;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(18))) {
                rc.writeSharedArray(16, GlobalArray.toggleBit(locInt,15));
                amplifierID = 18;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(20))) {
                rc.writeSharedArray(17, GlobalArray.toggleBit(locInt,15));
                amplifierID = 20;
            } else {
                throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Too many Amplifiers!");
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException at Carrier constructor");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Amplifier constructor");
            e.printStackTrace();
        } finally {
            Clock.yield();
        }
        run();
    }
    
    public void run() {
        while (true) {
            try {
                turnCount++;
                arrayIndex1 = rc.readSharedArray(amplifierID);
                lastArrayIndex2 = arrayIndex2;
                arrayIndex2 = rc.readSharedArray(amplifierID + 1);

                int aliveLaunchers = 0;
                for (int i = 0;i < 12;i++) {
                    if (((arrayIndex2 >> i) & 1) != ((lastArrayIndex2 >> i) & 1)) {
                        aliveLaunchers += 1;
                    }
                }

                arrayIndex2 = arrayIndex2 & 0b0000111111111111 + aliveLaunchers << 12;

                if (aliveLaunchers == 12) {
                    me = rc.getLocation();
                    RobotInfo[] opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                    if (opponentRobots.length > 0) {
                        RobotInfo prioritizedRobotInfo = opponentRobots[0];
                        MapLocation prioritizedRobotInfoLocation = opponentRobots[0].getLocation();
                        for (RobotInfo r : opponentRobots) {
                            if (prioritizedRobotInfo.getType() == prioritizedRobotType) {
                                if (r.getType() == prioritizedRobotType
                                        && prioritizedRobotInfo.getLocation().distanceSquaredTo(me) > r
                                                .getLocation().distanceSquaredTo(me)) {
                                    prioritizedRobotInfo = r;
                                    prioritizedRobotInfoLocation = r.getLocation();
                                }
                            } else {
                                if (prioritizedRobotInfo.getLocation().distanceSquaredTo(me) > r.getLocation()
                                        .distanceSquaredTo(me)) {
                                    prioritizedRobotInfo = r;
                                    prioritizedRobotInfoLocation = r.getLocation();
                                }
                            }
                        }
                        arrayIndex1 = arrayIndex1 & 0b1111000000000000 + GlobalArray.intifyLocation(prioritizedRobotInfoLocation);
                        arrayIndex1 = GlobalArray.setBit(arrayIndex1, 15, 1);
                    }
                    else {
                        arrayIndex1 = arrayIndex1 & 0b1111000000000000 + GlobalArray.intifyLocation(me);
                        arrayIndex1 = GlobalArray.setBit(arrayIndex1, 15, 0);
                    }
                    moveRandomly();
                }
                else if (turnCount < 10) {
                    moveRandomly();
                }
                rc.writeSharedArray(amplifierID, GlobalArray.toggleBit(arrayIndex1,15));
                rc.writeSharedArray(amplifierID + 1, arrayIndex2);
            } catch (GameActionException e) {
                System.out.println("GameActionException at Carrier constructor");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Amplifier");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    private void moveRandomly() throws GameActionException {
        while (true) {
            Direction direction = directions[rng.nextInt(directions.length)];
            // for (int x = -2;x <= 2;x++) {
            //     for (int y = -2;y <= 2;y++) {
            //         if (!rc.sensePassability(me.add(direction).translate(x,y))) {
            //             continue;
            //         }
            //     }
            // }
            if (rc.canMove(direction)) {
                rc.move(direction);
                break;
            }
        }
    }
}
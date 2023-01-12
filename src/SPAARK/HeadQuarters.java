package SPAARK;

import battlecode.common.*;

import java.util.Random;

public strictfp class HeadQuarters {
    RobotController rc;
    
    private int turnCount = 0;
    private int carriers = -100;
    private boolean producedAnchor = false;
    
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
            // bits 0-5 are x coordinate
            // bits 6-11 are y coordinate
            // bit 12 is presence marker
            MapLocation loc = rc.getLocation();
            if (rc.readSharedArray(1) >> 12 == 0) {
                rc.writeSharedArray(1, 0b1000000000000 | (loc.y << 6) | loc.x);
            } else if (rc.readSharedArray(2) >> 12 == 0) {
                rc.writeSharedArray(2, 0b1000000000000 | (loc.y << 6) | loc.x);
            } else if (rc.readSharedArray(3) >> 12 == 0) {
                rc.writeSharedArray(3, 0b1000000000000 | (loc.y << 6) | loc.x);
            } else if (rc.readSharedArray(4) >> 12 == 0) {
                rc.writeSharedArray(4, 0b1000000000000 | (loc.y << 6) | loc.x);
            } else {
                throw new Exception("Too many HeadQuarters!");
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
                }
                if (turnCount < 300 || producedAnchor == true) {
                    if (rc.canBuildRobot(RobotType.CARRIER, newLoc) && carriers <= 0) {
                        rc.buildRobot(RobotType.CARRIER, newLoc);
                        carriers += 10;
                        // if (turnCount >= 300) {
                        //     carriers += 20;
                        // }
                    }
                    else if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
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
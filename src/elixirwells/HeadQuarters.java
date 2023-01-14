package elixirwells;

import battlecode.common.*;

import java.util.Random;

public strictfp class HeadQuarters {
    RobotController rc;
    MapLocation me;
    GlobalArray globalArray = new GlobalArray();

    private int gArrIndex = 4;

    private int turnCount = 0;
    private int carrierCount = 0;
    private int launcherCount = 0;
    private int carrierCooldown = 0;
    private int launcherCooldown = 0;
    private boolean isPrimaryHQ = false;
    private boolean setTargetElixirWell = false;

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
                gArrIndex = 1;
                isPrimaryHQ = true;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(2))) {
                rc.writeSharedArray(2, locInt);
                gArrIndex = 2;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(3))) {
                rc.writeSharedArray(3, locInt);
                gArrIndex = 3;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(4))) {
                rc.writeSharedArray(4, locInt);
                gArrIndex = 4;
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
                globalArray.parseGameState(rc.readSharedArray(0));
                Direction dir = directions[rng.nextInt(directions.length)];
                MapLocation newLoc = rc.getLocation().add(dir);
                if (rc.canBuildAnchor(Anchor.STANDARD) && carrierCount >= 20) {
                    rc.buildAnchor(Anchor.STANDARD);
                    System.out.println("Anchor Produced!");
                }
                if ((carrierCount < 40 || carrierCooldown > 20) && rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                    rc.buildRobot(RobotType.CARRIER, newLoc);
                    carrierCount++;
                    carrierCooldown = 0;
                }
                if ((launcherCount < 30 || launcherCooldown > 20) && rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                    rc.buildRobot(RobotType.LAUNCHER, newLoc);
                    launcherCount++;
                    launcherCooldown = 0;
                }
                if (turnCount % 30 == 0) {
                    carrierCount--;
                    launcherCount--;
                }
                carrierCooldown++;
                launcherCooldown++;
                if (isPrimaryHQ) {
                    // set target elixir well
                    if (carrierCount > 20 && turnCount > 50 && !setTargetElixirWell) {
                        setTargetElixirWell();
                    }
                    // prioritized resource
                    // todo
                    // set game state
                    if (globalArray.changedState()) rc.writeSharedArray(0, globalArray.getGameStateNumber());
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

    private void setTargetElixirWell() throws Exception {
        try {
            setTargetElixirWell = true;
            MapLocation[] wells = GlobalArray.getKnownWellLocations(rc);
            MapLocation[] headQuarters = GlobalArray.getKnownHeadQuarterLocations(rc);
            int lowestDist = Integer.MAX_VALUE;
            int wellIndex = -1;
            int hqIndex = -1;
            for (int i = 0; i < headQuarters.length; i++) {
                for (int j = 0; j < wells.length; j++) {
                    int dist = headQuarters[i].distanceSquaredTo(wells[j]);
                    if (dist < lowestDist) {
                        lowestDist = dist;
                        hqIndex = i;
                        wellIndex = j;
                    }
                }
            }
            if (wellIndex > -1) {
                globalArray.setTargetElixirWellHQPair(wellIndex, hqIndex);
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException setting target elixir well");
            e.printStackTrace();
        }
    }
}
package elixirwells;

import battlecode.common.*;

import java.util.Random;

public strictfp class HeadQuarters {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();

    protected int hqIndex;
    private int locInt;
    private int hqCount;
    
    private int turnCount = 0;
    private int anchorCooldown = 0;
    protected boolean isPrimaryHQ = false;
    private boolean setTargetElixirWell = false;
    protected int adamantium = 0;
    protected int mana = 0;
    protected int lastAdamantium = 0;
    protected int lastMana = 0;

    private static int[] amplifierToggleState = new int[4];
    private static boolean[] amplifierAliveState = new boolean[4];

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
            locInt = GlobalArray.intifyLocation(rc.getLocation());
            if (!GlobalArray.hasLocation(rc.readSharedArray(1))) {
                rc.writeSharedArray(1, locInt);
                hqIndex = 1;
                isPrimaryHQ = true;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(2))) {
                rc.writeSharedArray(2, locInt);
                hqIndex = 2;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(3))) {
                rc.writeSharedArray(3, locInt);
                hqIndex = 3;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(4))) {
                rc.writeSharedArray(4, locInt);
                hqIndex = 4;
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
                me = rc.getLocation();
                globalArray.parseGameState(rc.readSharedArray(0));
                Direction dir = directions[rng.nextInt(directions.length)];
                // build bots and anchors based on input
                if (rc.canBuildAnchor(Anchor.STANDARD) && turnCount > 100 && anchorCooldown > 50) {
                    rc.buildAnchor(Anchor.STANDARD);
                    anchorCooldown = 0;
                    System.out.println("Anchor Produced!");
                }
                MapLocation newLoc = me.add(dir);
                anchorCooldown++;
                // store
                GlobalArray.storeHeadquarters(this);
                if (isPrimaryHQ) {
                    // set upgrade wells if resources adequate
                    if (hqCount == 0) {
                        for (int i = 1; i <= 4; i++) {
                            if (GlobalArray.hasLocation(rc.readSharedArray(i))) hqCount++;
                        }
                    }
                    boolean upgradeWells = true;
                    for (int i = 1; i <= hqCount+1; i++) {
                        if (!GlobalArray.adequateResources(rc.readSharedArray(i))) {
                            upgradeWells = false;
                        }
                    }
                    if (globalArray.upgradeWells() != upgradeWells) {
                        globalArray.setUpgradeWells(upgradeWells);
                    }
                    // set target elixir well
                    if (turnCount > 200 && !setTargetElixirWell) {
                        setTargetElixirWell();
                    }
                    // save game state
                    if (globalArray.changedState()) rc.writeSharedArray(0, globalArray.getGameStateNumber());
                }
                lastAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
                lastMana = rc.getResourceAmount(ResourceType.MANA);
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
                System.out.println("SET ELIXIR-HQ TARGET PAIR: " + wells[wellIndex].toString() + " " + headQuarters[hqIndex].toString());
                globalArray.setTargetElixirWellHQPair(wellIndex, hqIndex);
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException setting target elixir well");
            e.printStackTrace();
        }
    }
}
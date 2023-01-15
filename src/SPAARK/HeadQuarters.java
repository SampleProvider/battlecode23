package SPAARK;

import battlecode.common.*;

import java.util.Random;

public strictfp class HeadQuarters {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();

    private static final Random rng = new Random(2023);

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

    protected int hqIndex;
    private int locInt;
    private int hqCount;

    private int turnCount = 0;

    private int carriers = 0;
    private int carrierCooldown = 0;

    private int launchers = 0;
    private int launcherCooldown = 0;

    private boolean isPrimaryHQ = false;
    private boolean setTargetElixirWell = false;
    protected int adamantium = 0;
    protected int mana = 0;
    protected int lastAdamantium = 0;
    protected int lastMana = 0;

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
        for (int i = 0;i < 10;i++) {
            Clock.yield();
        }
        while (true) {
            try {
                turnCount++;
                me = rc.getLocation();
                if (rc.canBuildAnchor(Anchor.STANDARD) && turnCount >= 300) {
                    rc.buildAnchor(Anchor.STANDARD);
                    System.out.println("Anchor Produced!");
                    carrierCooldown = 0;
                }
                WellInfo[] wellInfo = rc.senseNearbyWells();
                MapLocation[] spawningLocations = rc.getAllLocationsWithinRadiusSquared(me, 9);
                MapLocation optimalSpawningLocation = null;
                int possibleSpawningLocations = 0;
                if (wellInfo.length > 0) {
                    WellInfo prioritizedWellInfo = wellInfo[0];
                    MapLocation prioritizedWellInfoLocation = wellInfo[0].getMapLocation();
                    ResourceType priortizedResourceType = ResourceType.MANA;
                    if (turnCount < 15) {
                        priortizedResourceType = ResourceType.ADAMANTIUM;
                    }
                    for (WellInfo w : wellInfo) {
                        if (prioritizedWellInfo.getResourceType() == priortizedResourceType) {
                            if (w.getResourceType() == priortizedResourceType
                                    && prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w
                                            .getMapLocation().distanceSquaredTo(me)) {
                                prioritizedWellInfo = w;
                                prioritizedWellInfoLocation = w.getMapLocation();
                            }
                        } else {
                            if (prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w.getMapLocation()
                                    .distanceSquaredTo(me)) {
                                prioritizedWellInfo = w;
                                prioritizedWellInfoLocation = w.getMapLocation();
                            }
                        }
                    }
                    for (MapLocation m : spawningLocations) {
                        if (rc.sensePassability(m) == false || rc.isLocationOccupied(m)) {
                            continue;
                        }
                        possibleSpawningLocations += 1;
                        if (optimalSpawningLocation == null) {
                            optimalSpawningLocation = m;
                        }
                        else if (optimalSpawningLocation.distanceSquaredTo(prioritizedWellInfoLocation) > m.distanceSquaredTo(prioritizedWellInfoLocation)) {
                            optimalSpawningLocation = m;
                        }
                    }
                }
                else {
                    for (MapLocation m : spawningLocations) {
                        if (rc.sensePassability(m) == false || rc.isLocationOccupied(m)) {
                            continue;
                        }
                        possibleSpawningLocations += 1;
                        if (optimalSpawningLocation == null) {
                            optimalSpawningLocation = m;
                        }
                        else if (optimalSpawningLocation.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2)) > m.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2))) {
                            optimalSpawningLocation = m;
                        }
                    }
                }
                
                if (optimalSpawningLocation != null) {
                    boolean canProduceAmplifier = false;
                    for (int a = 0;a < 4;a++) {
                        if ((rc.readSharedArray(14 + a) >> 14) % 2 == 0) {
                            canProduceAmplifier = true;
                        }
                    }
                    if (carriers < 4) {
                        if (rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocation)) {
                            rc.buildRobot(RobotType.CARRIER, optimalSpawningLocation);
                            carriers += 1;
                        }
                    }
                    else if (rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation)) {
                        rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                        launchers += 1;
                    }
                    else if (rc.canBuildRobot(RobotType.AMPLIFIER, optimalSpawningLocation) && canProduceAmplifier) {
                        rc.buildRobot(RobotType.AMPLIFIER, optimalSpawningLocation);
                    }
                    else if (rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocation) && possibleSpawningLocations >= 6) {
                        rc.buildRobot(RobotType.CARRIER, optimalSpawningLocation);
                        carriers += 1;
                    }
                }
                // store
                GlobalArray.storeHeadquarters(this);
                if (isPrimaryHQ) {
                    if (hqCount == 0) {
                        for (int i = 1; i <= 4; i++) {
                            if (GlobalArray.hasLocation(rc.readSharedArray(i))) hqCount++;
                        }
                    }
                    // set prioritized resource
                    // set upgrade wells if resources adequate
                    boolean upgradeWells = true;
                    int totalRatio = 0;
                    for (int i = 1; i <= hqCount+1; i++) {
                        int arrayHQ = rc.readSharedArray(i);
                        if (!GlobalArray.adequateResources(arrayHQ)) {
                            upgradeWells = false;
                        }
                        totalRatio += GlobalArray.resourceRatio(arrayHQ);
                    }
                    // upgrade wells
                    globalArray.setUpgradeWells(upgradeWells);
                    // prioritized resources
                    int deviation = totalRatio - (2 * hqCount);
                    if (Math.abs(deviation) <= 1) {
                        globalArray.setPrioritizedResource(ResourceType.NO_RESOURCE);
                    } else if (deviation < 0) {
                        globalArray.setPrioritizedResource(ResourceType.MANA);
                    } else {
                        globalArray.setPrioritizedResource(ResourceType.ADAMANTIUM);
                    }
                    // set target elixir well
                    if (turnCount > 200 && !setTargetElixirWell) {
                        // setTargetElixirWell();
                    }
                    // save game state
                    rc.writeSharedArray(0, globalArray.getGameStateNumber());
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
                if (headQuarters[i] != null) {
                    for (int j = 0; j < wells.length; j++) {
                        if (wells[j] != null) {
                            int dist = headQuarters[i].distanceSquaredTo(wells[j]);
                            if (dist < lowestDist) {
                                lowestDist = dist;
                                hqIndex = i;
                                wellIndex = j;
                            }
                        }
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
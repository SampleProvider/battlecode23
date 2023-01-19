package SPAARK;

import battlecode.common.*;

public strictfp class HeadQuarters {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    protected int hqIndex;
    private int locInt;
    private int hqCount;

    private int anchorCooldown = 0;

    private int carriers = 0;
    private int carrierCooldown = 0;

    private int launchers = 0;
    private int launcherCooldown = 0;

    private int possibleSpawningLocations = 0;

    private boolean isPrimaryHQ = false;
    private boolean setTargetElixirWell = false;
    protected int adamantium = 0;
    protected int mana = 0;
    protected int lastAdamantium = 0;
    protected int lastMana = 0;

    protected StringBuilder indicatorString = new StringBuilder();

    public HeadQuarters(RobotController rc) {
        try {
            this.rc = rc;
            // setting headquarter locations
            locInt = GlobalArray.intifyLocation(rc.getLocation());
            if (!GlobalArray.hasLocation(rc.readSharedArray(GlobalArray.HEADQUARTERS))) {
                rc.writeSharedArray(GlobalArray.HEADQUARTERS, locInt);
                hqIndex = GlobalArray.HEADQUARTERS;
                isPrimaryHQ = true;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(GlobalArray.HEADQUARTERS + 1))) {
                rc.writeSharedArray(GlobalArray.HEADQUARTERS + 1, locInt);
                hqIndex = GlobalArray.HEADQUARTERS + 1;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(GlobalArray.HEADQUARTERS + 2))) {
                rc.writeSharedArray(GlobalArray.HEADQUARTERS + 2, locInt);
                hqIndex = GlobalArray.HEADQUARTERS + 2;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(GlobalArray.HEADQUARTERS + 3))) {
                rc.writeSharedArray(GlobalArray.HEADQUARTERS + 3, locInt);
                hqIndex = GlobalArray.HEADQUARTERS + 3;
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
            run();
        }
    }

    private void run() {
        while (true) {
            try {
                me = rc.getLocation();
                round = rc.getRoundNum();
                globalArray.parseGameState(rc.readSharedArray(GlobalArray.GAMESTATE));
                adamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
                mana = rc.getResourceAmount(ResourceType.MANA);

                indicatorString = new StringBuilder();

                if (isPrimaryHQ) {
                    for (int a = GlobalArray.AMPLIFIERS; a < GlobalArray.AMPLIFIERS + GlobalArray.AMPLIFIERS_LENGTH; a++) {
                        int arrAmp = rc.readSharedArray(a);
                        if (GlobalArray.hasLocation(arrAmp)) {
                            if ((arrAmp >> 15) == round % 2) {
                                rc.writeSharedArray(a,0);
                                indicatorString.append("AMP " + a + " die; ");
                            }
                        }
                    }
                }

                MapLocation optimalSpawningLocationWell = optimalSpawnLocation(rc, me, true);
                MapLocation optimalSpawningLocation = optimalSpawnLocation(rc, me, false);
                // if (round > 500) {
                //     rc.resign();
                // }
                // if (round == 1) {
                //     rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                // }
                // else {
                //     Clock.yield();
                //     continue;
                // }
                if (anchorCooldown <= 0 && round >= 200 && rc.getNumAnchors(Anchor.STANDARD) == 0) {
                    if (adamantium >= 100 && mana >= 100) {
                        rc.buildAnchor(Anchor.STANDARD);
                        indicatorString.append("PROD ANC; ");
                        anchorCooldown = 100;
                    }
                    else {
                        indicatorString.append("TRY PROD ANC; ");
                        if (adamantium >= 150) {
                            if (optimalSpawningLocationWell != null && rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocationWell) && possibleSpawningLocations >= 5) {
                                rc.buildRobot(RobotType.CARRIER, optimalSpawningLocationWell);
                                carriers += 1;
                                indicatorString.append("PROD CAR; ");
                                rc.setIndicatorLine(me, optimalSpawningLocationWell, 125, 125, 125);
                            }
                        }
                        if (mana >= 160) {
                            if (optimalSpawningLocation != null && rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation) && possibleSpawningLocations >= 3) {
                                rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                                launchers++;
                                indicatorString.append("PROD LAU; ");
                                rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                            }
                        }
                    }
                }
                else {
                    int amplifierIndex = 0;
                    for (int a = GlobalArray.AMPLIFIERS; a < GlobalArray.AMPLIFIERS + GlobalArray.AMPLIFIERS_LENGTH; a++) {
                        if (!GlobalArray.hasLocation(rc.readSharedArray(a))) {
                            amplifierIndex = a;
                            break;
                        }
                    }
                    if (optimalSpawningLocation != null && rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation) && possibleSpawningLocations >= 3) {
                        rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                        launchers++;
                        indicatorString.append("PROD LAU; ");
                        rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                    }
                    else if (optimalSpawningLocation != null && rc.canBuildRobot(RobotType.AMPLIFIER, optimalSpawningLocation) && possibleSpawningLocations >= 6 && launchers > 10 && amplifierIndex != 0) {
                        rc.buildRobot(RobotType.AMPLIFIER, optimalSpawningLocation);
                        rc.writeSharedArray(amplifierIndex, GlobalArray.setBit(GlobalArray.intifyLocation(optimalSpawningLocation), 14, 1));
                        indicatorString.append("PROD AMP; ");
                        rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                    }
                    else if (optimalSpawningLocationWell != null && rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocationWell) && possibleSpawningLocations >= 5) {
                        rc.buildRobot(RobotType.CARRIER, optimalSpawningLocationWell);
                        carriers += 1;
                        indicatorString.append("PROD CAR; ");
                        rc.setIndicatorLine(me, optimalSpawningLocationWell, 125, 125, 125);
                    }
                }
                anchorCooldown -= 1;
                // store
                GlobalArray.storeHeadquarters(this);
                // prioritized resources
                double deviation = (mana - (adamantium * 1.5)) / (mana + (adamantium * 1.5));
                if (Math.abs(deviation) < 0.2) {
                    globalArray.setPrioritizedResource(ResourceType.NO_RESOURCE, hqIndex);
                    indicatorString.append("PR=NO; ");
                } else if (deviation < 0) {
                    globalArray.setPrioritizedResource(ResourceType.MANA, hqIndex);
                    indicatorString.append("PR=MN; ");
                } else {
                    globalArray.setPrioritizedResource(ResourceType.ADAMANTIUM, hqIndex);
                    indicatorString.append("PR=AD; ");
                }
                if (isPrimaryHQ) {
                    if (round == 2) {
                        for (int i = GlobalArray.HEADQUARTERS; i < GlobalArray.HEADQUARTERS + GlobalArray.HEADQUARTERS_LENGTH; i++) {
                            if (GlobalArray.hasLocation(rc.readSharedArray(i))) hqCount++;
                        }
                    }
                    // set upgrade wells if resources adequate
                    boolean upgradeWells = true;
                    for (int i = GlobalArray.HEADQUARTERS; i <= GlobalArray.HEADQUARTERS + hqCount; i++) {
                        int arrayHQ = rc.readSharedArray(i);
                        if (!GlobalArray.adequateResources(arrayHQ)) {
                            upgradeWells = false;
                        }
                    }
                    // upgrade wells
                    globalArray.setUpgradeWells(upgradeWells);
                    // set target elixir well
                    if (round > 200 && !setTargetElixirWell) {
                        // setTargetElixirWell();
                    }
                }
                // save game state
                rc.writeSharedArray(0, globalArray.getGameStateNumber());
            } catch (GameActionException e) {
                System.out.println("GameActionException at HeadQuarters");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at HeadQuarters");
                e.printStackTrace();
            } finally {
                lastAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
                lastMana = rc.getResourceAmount(ResourceType.MANA);
                rc.setIndicatorString(indicatorString.toString());
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
                indicatorString.append("EX-HQ=" + wells[wellIndex].toString() + "-" + headQuarters[hqIndex].toString() + "; ");
                globalArray.setTargetElixirWellHQPair(wellIndex, hqIndex);
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException setting target elixir well");
            e.printStackTrace();
        }
    }

    private MapLocation optimalSpawnLocation(RobotController rc, MapLocation me, boolean well) throws GameActionException {
        WellInfo[] wellInfo = rc.senseNearbyWells();
        MapLocation[] spawningLocations = rc.getAllLocationsWithinRadiusSquared(me, 9);
        MapLocation optimalSpawningLocation = null;
        possibleSpawningLocations = 0;
        if (wellInfo.length > 0 && well) {
            WellInfo prioritizedWellInfo = wellInfo[0];
            MapLocation prioritizedWellInfoLocation = wellInfo[0].getMapLocation();
            ResourceType prioritizedResourceType = ResourceType.ELIXIR;
            // if (turnCount < 15) {
            //     prioritizedResourceType = ResourceType.ADAMANTIUM;
            // }
            for (WellInfo w : wellInfo) {
                if (prioritizedWellInfo.getResourceType() == prioritizedResourceType) {
                    if (w.getResourceType() == prioritizedResourceType
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
            if (well) {
                for (MapLocation m : spawningLocations) {
                    if (rc.sensePassability(m) == false || rc.isLocationOccupied(m)) {
                        continue;
                    }
                    possibleSpawningLocations += 1;
                    if (optimalSpawningLocation == null) {
                        optimalSpawningLocation = m;
                    }
                    else if (optimalSpawningLocation.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2)) < m.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2))) {
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
        }
        return optimalSpawningLocation;
    }
}
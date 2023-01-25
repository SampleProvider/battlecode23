package SPAARK_1_23_2023;

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

    private StoredLocations storedLocations;

    private boolean isPrimaryHQ = false;
    private boolean setTargetElixirWell = false;
    protected int adamantium = 0;
    protected int mana = 0;
    protected int lastAdamantium = 0;
    protected int lastMana = 0;

    private StringBuilder indicatorString = new StringBuilder();

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
                System.out.println("[!] Too many Headquarters! [!]");
            }
            storedLocations = new StoredLocations(rc, new MapLocation[] {});
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
                                rc.writeSharedArray(a, 0);
                                indicatorString.append("AMP " + a + " die; ");
                            }
                        }
                    }
                }

                storedLocations.detectWells();
                storedLocations.detectOpponentLocations();
                storedLocations.detectIslandLocations();
                storedLocations.writeToGlobalArray();
                
                if (GlobalArray.DEBUG_INFO >= 1 && isPrimaryHQ) {
                    MapLocation[] wells = GlobalArray.getKnownWellLocations(rc);
                    for (MapLocation m : wells) {
                        if (m == null) {
                            continue;
                        }
                        rc.setIndicatorDot(m, 255, 75, 75);
                    }
                    MapLocation[] opponents = GlobalArray.getKnownOpponentLocations(rc);
                    for (MapLocation m : opponents) {
                        if (m == null) {
                            continue;
                        }
                        rc.setIndicatorDot(m, 0, 255, 0);
                    }
                    MapLocation[] islands = GlobalArray.getKnownIslandLocations(rc, Team.A);
                    for (MapLocation m : islands) {
                        if (m == null) {
                            continue;
                        }
                        rc.setIndicatorDot(m, 255, 0, 0);
                    }
                    islands = GlobalArray.getKnownIslandLocations(rc, Team.B);
                    for (MapLocation m : islands) {
                        if (m == null) {
                            continue;
                        }
                        rc.setIndicatorDot(m, 0, 0, 255);
                    }
                    islands = GlobalArray.getKnownIslandLocations(rc, Team.NEUTRAL);
                    for (MapLocation m : islands) {
                        if (m == null) {
                            continue;
                        }
                        rc.setIndicatorDot(m, 255, 255, 255);
                    }
                }

                // if (round > 200) {
                // rc.resign();
                // }
                // int amplifierIndex = 0;
                // for (int a = GlobalArray.AMPLIFIERS; a < GlobalArray.AMPLIFIERS +
                // GlobalArray.AMPLIFIERS_LENGTH; a++) {
                // if (!GlobalArray.hasLocation(rc.readSharedArray(a))) {
                // amplifierIndex = a;
                // break;
                // }
                // }
                // if (amplifierIndex != 0) {
                // rc.buildRobot(RobotType.AMPLIFIER, optimalSpawningLocation);
                // rc.writeSharedArray(amplifierIndex,
                // GlobalArray.setBit(GlobalArray.intifyLocation(optimalSpawningLocation), 14,
                // 1));
                // }
                // Clock.yield();
                // continue;
                while (rc.isActionReady()) {
                    MapLocation optimalSpawningLocationWell = optimalSpawnLocation(true);
                    MapLocation optimalSpawningLocation = optimalSpawnLocation(false);
                    boolean builtRobot = false;
                    if (anchorCooldown <= 0 && round >= 1000 && rc.getNumAnchors(Anchor.STANDARD) == 0) {
                        if (adamantium >= 100 && mana >= 100) {
                            rc.buildAnchor(Anchor.STANDARD);
                            indicatorString.append("PROD ANC; ");
                            anchorCooldown = 100;
                        } else {
                            indicatorString.append("TRY PROD ANC; ");
                            if (adamantium >= 150) {
                                if (optimalSpawningLocationWell != null && rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocationWell) && possibleSpawningLocations >= 8) {
                                    rc.buildRobot(RobotType.CARRIER, optimalSpawningLocationWell);
                                    carriers += 1;
                                    builtRobot = true;
                                    indicatorString.append("PROD CAR; ");
                                    if (GlobalArray.DEBUG_INFO >= 3) {
                                        rc.setIndicatorLine(me, optimalSpawningLocationWell, 125, 125, 125);
                                    }
                                }
                            }
                            if (mana >= 160) {
                                if (optimalSpawningLocation != null && rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation) && possibleSpawningLocations >= 3) {
                                    rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                                    launchers++;
                                    builtRobot = true;
                                    indicatorString.append("PROD LAU; ");
                                    if (GlobalArray.DEBUG_INFO >= 3) {
                                        rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                                    }
                                }
                            }
                        }
                    } else {
                        int amplifierIndex = 0;
                        for (int a = GlobalArray.AMPLIFIERS; a < GlobalArray.AMPLIFIERS
                                + GlobalArray.AMPLIFIERS_LENGTH; a++) {
                            if (!GlobalArray.hasLocation(rc.readSharedArray(a))) {
                                amplifierIndex = a;
                                break;
                            }
                        }
                        if (optimalSpawningLocation != null && rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation) && possibleSpawningLocations >= 3) {
                            rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                            launchers++;
                            builtRobot = true;
                            indicatorString.append("PROD LAU; ");
                            if (GlobalArray.DEBUG_INFO >= 3) {
                                rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                            }
                        }
                        else if (optimalSpawningLocation != null && rc.canBuildRobot(RobotType.AMPLIFIER, optimalSpawningLocation) && possibleSpawningLocations >= 6 && launchers > 10) {
                            rc.buildRobot(RobotType.AMPLIFIER, optimalSpawningLocation);
                            launchers = 0;
                            builtRobot = true;
                            // rc.writeSharedArray(amplifierIndex, GlobalArray.setBit(GlobalArray.setBit(GlobalArray.intifyLocation(optimalSpawningLocation), 14, 1), 15, round % 2));
                            indicatorString.append("PROD AMP; ");
                            if (GlobalArray.DEBUG_INFO >= 3) {
                                rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                            }
                        }
                        else if (optimalSpawningLocationWell != null && rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocationWell) && possibleSpawningLocations >= 8) {
                            rc.buildRobot(RobotType.CARRIER, optimalSpawningLocationWell);
                            carriers += 1;
                            builtRobot = true;
                            indicatorString.append("PROD CAR; ");
                            if (GlobalArray.DEBUG_INFO >= 3) {
                                rc.setIndicatorLine(me, optimalSpawningLocationWell, 125, 125, 125);
                            }
                        }
                    }
                    if (!builtRobot) {
                        break;
                    }
                }
                anchorCooldown -= 1;
                // store
                GlobalArray.storeHeadquarters(this);
                // prioritized resources
                double deviation = (mana - (adamantium * 1.8)) / (mana + (adamantium * 1.8));
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
                if (round == 2) {
                    for (int i = GlobalArray.HEADQUARTERS; i < GlobalArray.HEADQUARTERS + GlobalArray.HEADQUARTERS_LENGTH; i++) {
                        if (GlobalArray.hasLocation(rc.readSharedArray(i)))
                        hqCount++;
                    }
                    MapLocation headquarters[] = new MapLocation[hqCount];
                    for (int i = 0; i < hqCount; i++) {
                        headquarters[i] = GlobalArray.parseLocation(rc.readSharedArray(i + GlobalArray.HEADQUARTERS));
                    }
                    storedLocations.headquarters = headquarters;
                }
                if (isPrimaryHQ) {
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
                rc.writeSharedArray(GlobalArray.GAMESTATE, globalArray.getGameStateNumber());
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
            for (int i = 0; i < headQuarters.length; i++) {
                if (headQuarters[i] != null) {
                    for (int j = 0; j < wells.length; j++) {
                        if (wells[j] != null) {
                            int dist = headQuarters[i].distanceSquaredTo(wells[j]);
                            if (dist < lowestDist) {
                                lowestDist = dist;
                                wellIndex = j;
                            }
                        }
                    }
                }
            }
            if (wellIndex > -1) {
                indicatorString.append("EX=" + wells[wellIndex].toString() + "; ");
                globalArray.setTargetElixirWell(wellIndex);
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException setting target elixir well");
            e.printStackTrace();
        }
    }

    private MapLocation optimalSpawnLocation(boolean well) throws GameActionException {
        WellInfo[] wellInfo = rc.senseNearbyWells();
        MapLocation[] spawningLocations = rc.getAllLocationsWithinRadiusSquared(me, 9);
        MapLocation optimalSpawningLocation = null;
        possibleSpawningLocations = 0;
        if (wellInfo.length > 0 && well) {
            WellInfo prioritizedWellInfo = wellInfo[0];
            MapLocation prioritizedWellInfoLocation = wellInfo[0].getMapLocation();
            ResourceType prioritizedResourceType = ResourceType.ELIXIR;
            for (WellInfo w : wellInfo) {
                if (prioritizedWellInfo.getResourceType() == prioritizedResourceType) {
                    if (w.getResourceType() == prioritizedResourceType && prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w.getMapLocation().distanceSquaredTo(me)) {
                        prioritizedWellInfo = w;
                        prioritizedWellInfoLocation = w.getMapLocation();
                    }
                } else {
                    if (prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w.getMapLocation().distanceSquaredTo(me)) {
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
                } else if (optimalSpawningLocation.distanceSquaredTo(prioritizedWellInfoLocation) > m.distanceSquaredTo(prioritizedWellInfoLocation)) {
                    optimalSpawningLocation = m;
                }
            }
        } else {
            if (well) {
                for (MapLocation m : spawningLocations) {
                    if (rc.sensePassability(m) == false || rc.isLocationOccupied(m)) {
                        continue;
                    }
                    possibleSpawningLocations += 1;
                    if (optimalSpawningLocation == null) {
                        optimalSpawningLocation = m;
                    } else if (optimalSpawningLocation.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2))
                            < m.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2))) {
                        optimalSpawningLocation = m;
                    }
                }
            } else {
                for (MapLocation m : spawningLocations) {
                    if (rc.sensePassability(m) == false || rc.isLocationOccupied(m)) {
                        continue;
                    }
                    possibleSpawningLocations += 1;
                    if (optimalSpawningLocation == null) {
                        optimalSpawningLocation = m;
                    } else if (optimalSpawningLocation.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2))
                            > m.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2))) {
                        optimalSpawningLocation = m;
                    }
                }
            }
        }
        return optimalSpawningLocation;
    }
}
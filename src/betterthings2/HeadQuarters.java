package betterthings2;

import battlecode.common.*;

public strictfp class HeadQuarters {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    protected int hqIndex;
    private int locInt;
    private int hqCount = 4;

    private int anchorCooldown = 100;
    private int carrierCooldown = 0;
    private int launcherCooldown = 0;
    private int amplifierCooldown = 0;
    private int carriers = 0;
    private int launchers = 0;
    private int amplifiers = 0;
    private int nearbyCarriers = 0;
    private int nearbyLaunchers = 0;

    private int possibleSpawningLocations = 0;

    private int mapSizeFactor = 100;
    private StoredLocations storedLocations;

    private boolean isPrimaryHQ = false;
    private boolean setTargetElixirWell = false;
    protected int adamantium = 0;
    protected int mana = 0;
    protected int lastAdamantium = 0;
    protected int lastMana = 0;
    protected int deltaResources = 0;

    private StringBuilder indicatorString = new StringBuilder();

    public HeadQuarters(RobotController rc) {
        try {
            this.rc = rc;
            // setting headquarter locations
            locInt = GlobalArray.intifyLocation(rc.getLocation());
            // hqIndex = rc.getID()/2;
            // if (rc.getID() % 2 == 0)
            //     hqIndex--;
            // rc.writeSharedArray(GlobalArray.HEADQUARTERS+hqIndex, locInt);
            // if (hqIndex == 0)
            //     isPrimaryHQ = true;
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
                deltaResources = (int) ((0.7 * deltaResources) + (0.3 * (adamantium - lastAdamantium + mana - lastMana)));

                indicatorString = new StringBuilder();

                indicatorString.append(hqIndex - GlobalArray.HEADQUARTERS + "; ");

                indicatorString.append("DR=" + deltaResources + "; ");

                // detect stuff
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

                // track bots
                if (round % 2 == 1) {
                    carriers = rc.readSharedArray(GlobalArray.CARRIERCOUNT);
                    launchers = rc.readSharedArray(GlobalArray.LAUNCHERCOUNT);
                    amplifiers = rc.readSharedArray(GlobalArray.AMPLIFIERCOUNT) & 0b11111111;
                    nearbyCarriers = 0;
                    nearbyLaunchers = 0;
                    RobotInfo[] nearbyBots = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
                    for (RobotInfo r : nearbyBots) {
                        if (r.getType() == RobotType.LAUNCHER)
                            nearbyLaunchers++;
                        else if (r.getType() == RobotType.CARRIER)
                            nearbyCarriers++;
                    }
                    indicatorString.append("C-L-A-NC-NL[" + carriers + ", " + launchers + ", " + amplifiers + ", " + nearbyCarriers + ", " + nearbyLaunchers + "]; ");
                }
                if (isPrimaryHQ) {
                    if (round % 2 == 0) {
                        rc.writeSharedArray(GlobalArray.CARRIERCOUNT, 0);
                        rc.writeSharedArray(GlobalArray.LAUNCHERCOUNT, 0);
                        rc.writeSharedArray(GlobalArray.AMPLIFIERCOUNT, rc.readSharedArray(GlobalArray.AMPLIFIERCOUNT) & 0b1111111100000000);
                    }
                }

                spawnBots();

                // store
                GlobalArray.storeHeadquarters(this);

                // prioritized resources
                double deviation = ((mana + adamantium) != 0) ? (mana - (adamantium * 10.0)) / (mana + (adamantium * 10.0)) : 0;
                if (Math.abs(deviation) < 0.1) {
                    globalArray.setPrioritizedResource(ResourceType.NO_RESOURCE, hqIndex);
                    indicatorString.append("PR=NO; ");
                } else if (deviation < 0) {
                    globalArray.setPrioritizedResource(ResourceType.MANA, hqIndex);
                    indicatorString.append("PR=MN; ");
                } else {
                    globalArray.setPrioritizedResource(ResourceType.ADAMANTIUM, hqIndex);
                    indicatorString.append("PR=AD; ");
                }
                // primary headquarters stuff
                if (isPrimaryHQ) {
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
                        mapSizeFactor = (rc.getMapWidth() * rc.getMapHeight()) / 400;
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
                        setTargetElixirWell();
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
                lastAdamantium = adamantium;
                lastMana = mana;
                rc.setIndicatorString(indicatorString.toString());
                Clock.yield();
            }
        }
    }

    private void spawnBots() throws GameActionException {
        // alsso makees anchors
        int carriersProduced = 0;
        int launchersProduced = 0;
        MapLocation[] islands = GlobalArray.getKnownIslandLocations(rc, Team.NEUTRAL);
        boolean canProduceAnchor = islands.length > 0;
        if (anchorCooldown <= 0 && nearbyLaunchers > 5 && rc.getNumAnchors(Anchor.STANDARD) == 0 && canProduceAnchor) {
            if (adamantium > Anchor.STANDARD.adamantiumCost && mana > Anchor.STANDARD.manaCost) {
                rc.buildAnchor(Anchor.STANDARD);
                indicatorString.append("P ANC; ");
                anchorCooldown = 50;
            } else {
                indicatorString.append("TP ANC; ");
                while (rc.isActionReady()) {
                    MapLocation optimalSpawningLocationWell = optimalSpawnLocation(true);
                    MapLocation optimalSpawningLocation = optimalSpawnLocation(false);
                    if (mana > Anchor.STANDARD.manaCost + 60 && optimalSpawningLocation != null && rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation)
                            && (launchers < 40 * hqCount * mapSizeFactor || nearbyLaunchers < 15 || launcherCooldown <= 0) && possibleSpawningLocations >= 3) {
                        rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                        launchersProduced++;
                        if (GlobalArray.DEBUG_INFO >= 1) rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                        launcherCooldown = 5;
                    } else if (adamantium > Anchor.STANDARD.adamantiumCost + 50 && optimalSpawningLocationWell != null && rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocationWell)
                            && ((deltaResources < 0 && nearbyCarriers < 10) || carriers < 10 * hqCount || carrierCooldown <= 0) && possibleSpawningLocations >= 4) {
                        rc.buildRobot(RobotType.CARRIER, optimalSpawningLocationWell);
                        carriersProduced++;
                        if (GlobalArray.DEBUG_INFO >= 1) rc.setIndicatorLine(me, optimalSpawningLocationWell, 125, 125, 125);
                        carrierCooldown = 30;
                    } else break;
                }
            }
        } else {
            while (rc.isActionReady()) {
                MapLocation optimalSpawningLocationWell = optimalSpawnLocation(true);
                MapLocation optimalSpawningLocation = optimalSpawnLocation(false);
                if (optimalSpawningLocationWell != null && rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocationWell)
                        && ((deltaResources < 0 && nearbyCarriers < 10) || carriers < 10 * hqCount || carrierCooldown <= 0)
                        && (round > 1 || carriersProduced < 2) && possibleSpawningLocations >= 3) {
                    rc.buildRobot(RobotType.CARRIER, optimalSpawningLocationWell);
                    carriersProduced++;
                    if (GlobalArray.DEBUG_INFO >= 1) rc.setIndicatorLine(me, optimalSpawningLocationWell, 125, 125, 125);
                    carrierCooldown = 50;
                } else if (optimalSpawningLocation != null && possibleSpawningLocations >= 2) {
                    if (rc.canBuildRobot(RobotType.AMPLIFIER, optimalSpawningLocation)
                            && launchers > 10 && carriers > 0 && amplifiers < 5 && amplifierCooldown <= 0) {
                        rc.buildRobot(RobotType.AMPLIFIER, optimalSpawningLocation);
                        indicatorString.append("P AMP; ");
                        if (GlobalArray.DEBUG_INFO >= 1) rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                        amplifierCooldown = 30;
                        amplifiers = Integer.MAX_VALUE;
                    } else if (rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation)
                            && (launchers < 40 * hqCount * mapSizeFactor || nearbyLaunchers < 15 || launcherCooldown <= 0)) {
                        rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                        launchersProduced++;
                        if (GlobalArray.DEBUG_INFO >= 1) rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                        launcherCooldown = 0;
                    } else break;
                } else break;
            }
        }
        if (carriersProduced > 0)
            indicatorString.append("P CAR-x" + carriersProduced + "; ");
        if (launchersProduced > 0)
            indicatorString.append("P LAU-x" + launchersProduced + "; ");
        anchorCooldown--;
        carrierCooldown--;
        launcherCooldown--;
        amplifierCooldown--;
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
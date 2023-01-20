package betterspawning;

import battlecode.common.*;

public strictfp class HeadQuarters {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    protected int hqIndex;
    private int locInt;
    private int hqCount;

    private int anchorCooldown = 200;
    private int carrierCooldown = 0;
    private int launcherCooldown = 0;
    private int amplifierCooldown = 100;
    private int carriers = 0;
    private int launchers = 0;
    private int nearbyCarriers = 0;
    private int nearbyLaunchers = 0;

    private int possibleSpawningLocations = 0;

    private int mapSizeFactor = 100;

    private boolean isPrimaryHQ = false;
    private boolean setTargetElixirWell = false;
    protected int adamantium = 0;
    protected int mana = 0;
    protected int lastAdamantium = 0;
    protected int lastMana = 0;
    protected int deltaResources = 0;

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
                deltaResources = (int) ((0.7 * deltaResources) + (0.3 * (adamantium-lastAdamantium+mana-lastMana)));

                indicatorString = new StringBuilder();

                indicatorString.append(hqIndex - GlobalArray.HEADQUARTERS + "; ");

                indicatorString.append("DR=" + deltaResources + "; ");

                // track carriers and launchers
                if (round % 2 == 1) {
                    carriers = rc.readSharedArray(GlobalArray.CARRIERCOUNT);
                    launchers = rc.readSharedArray(GlobalArray.LAUNCHERCOUNT);
                    nearbyCarriers = 0;
                    nearbyLaunchers = 0;
                    RobotInfo[] nearbyBots = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
                    for (RobotInfo r : nearbyBots) {
                        if (r.getType() == RobotType.LAUNCHER) nearbyLaunchers++;
                        if (r.getType() == RobotType.CARRIER) nearbyCarriers++;
                    }
                    indicatorString.append("C-L-NC-NL[" + carriers + ", " + launchers + ", " + nearbyCarriers + ", " + nearbyLaunchers + "]; ");
                }
                if (isPrimaryHQ) {
                    // track amplifiers
                    for (int a = GlobalArray.AMPLIFIERS; a < GlobalArray.AMPLIFIERS + GlobalArray.AMPLIFIERS_LENGTH; a++) {
                        int arrAmp = rc.readSharedArray(a);
                        if (GlobalArray.hasLocation(arrAmp)) {
                            if ((arrAmp >> 15) == round % 2) {
                                indicatorString.append("AMP " + (a) + " d; ");
                                rc.writeSharedArray(a, 0);
                            }
                        }
                    }
                    if (round % 2 == 0) {
                        rc.writeSharedArray(GlobalArray.CARRIERCOUNT, 0);
                        rc.writeSharedArray(GlobalArray.LAUNCHERCOUNT, 0);
                    }
                }
                // spawn things
                int nextAmplifierIndex = 0;
                for (int a = GlobalArray.AMPLIFIERS; a < GlobalArray.AMPLIFIERS + GlobalArray.AMPLIFIERS_LENGTH; a++) {
                    if (!GlobalArray.hasLocation(rc.readSharedArray(a))) {
                        nextAmplifierIndex = a;
                        break;
                    }
                }
                indicatorString.append("CP-AMP=" + (nextAmplifierIndex > 0) + "; ");
                int carriersProduced = 0;
                int launchersProduced = 0;
                MapLocation optimalSpawningLocationWell = optimalSpawnLocation(rc, me, true);
                MapLocation optimalSpawningLocation = optimalSpawnLocation(rc, me, false);
                if (anchorCooldown <= 0 && rc.getNumAnchors(Anchor.STANDARD) == 0) {
                    if (adamantium > 100 && mana > 100) {
                        rc.buildAnchor(Anchor.STANDARD);
                        indicatorString.append("P ANC; ");
                        anchorCooldown = 70;
                    } else {
                        indicatorString.append("TP ANC; ");
                        for (int i = 0; i < 5; i++) {
                            if (i > 0) {
                                optimalSpawningLocationWell = optimalSpawnLocation(rc, me, true);
                                optimalSpawningLocation = optimalSpawnLocation(rc, me, false);
                            }
                            if (adamantium > 150 && optimalSpawningLocationWell != null && rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocationWell)
                                    && ((deltaResources < 6 && nearbyCarriers < 20) || carriers < 10*hqCount || carrierCooldown <= 0) && possibleSpawningLocations >= 6) {
                                rc.buildRobot(RobotType.CARRIER, optimalSpawningLocationWell);
                                carriersProduced++;
                                rc.setIndicatorLine(me, optimalSpawningLocationWell, 125, 125, 125);
                                carrierCooldown = 10;
                            } else if (mana > 160 && optimalSpawningLocation != null && rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation)
                                    && (launchers < 30*hqCount*mapSizeFactor || nearbyLaunchers < 15 || launcherCooldown <= 0) && possibleSpawningLocations >= 4) {
                                rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                                launchersProduced++;
                                rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                                launcherCooldown = 5;
                            } else break;
                        }
                    }
                } else if (possibleSpawningLocations >= 3) {
                    for (int i = 0; i < 5; i++) {
                        if (i > 0) {
                            optimalSpawningLocationWell = optimalSpawnLocation(rc, me, true);
                            optimalSpawningLocation = optimalSpawnLocation(rc, me, false);
                        }
                        if (optimalSpawningLocationWell != null && rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocationWell)
                                && ((deltaResources < 6 && nearbyCarriers < 20) || carriers < 10*hqCount || carrierCooldown <= 0) && round > 1) {
                            rc.buildRobot(RobotType.CARRIER, optimalSpawningLocationWell);
                            carriersProduced++;
                            rc.setIndicatorLine(me, optimalSpawningLocationWell, 125, 125, 125);
                            carrierCooldown = 10;
                        } else if (optimalSpawningLocation != null) {
                            if (rc.canBuildRobot(RobotType.AMPLIFIER, optimalSpawningLocation)
                                    && launchers > 10 && carriers > 0 && nextAmplifierIndex > 0 && amplifierCooldown <= 0) {
                                rc.buildRobot(RobotType.AMPLIFIER, optimalSpawningLocation);
                                indicatorString.append("P AMP; ");
                                rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                                rc.writeSharedArray(nextAmplifierIndex, GlobalArray.setBit(GlobalArray.setBit(GlobalArray.intifyLocation(optimalSpawningLocation), 15, round % 2), 14, 1));
                                amplifierCooldown = 30;
                                nextAmplifierIndex = 0;
                            } else if (rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation)
                                    && (launchers < 30*hqCount*mapSizeFactor || nearbyLaunchers < 15 || launcherCooldown <= 0)) {
                                rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                                launchersProduced++;
                                rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                                launcherCooldown = 5;
                            } else break;
                        } else break;
                    }
                }
                if (carriersProduced > 0) indicatorString.append("P CAR-x" + carriersProduced + "; ");
                if (launchersProduced > 0) indicatorString.append("P LAU-x" + launchersProduced + "; ");
                anchorCooldown--;
                carrierCooldown--;
                launcherCooldown--;
                amplifierCooldown--;
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
                // primary headquarters stuff
                if (isPrimaryHQ) {
                    if (round == 2) {
                        for (int i = GlobalArray.HEADQUARTERS; i < GlobalArray.HEADQUARTERS + GlobalArray.HEADQUARTERS_LENGTH; i++) {
                            if (GlobalArray.hasLocation(rc.readSharedArray(i))) hqCount++;
                        }
                        mapSizeFactor = (rc.getMapWidth() * rc.getMapHeight()) / 400;
                    }
                    // set prioritized resource
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
                lastAdamantium = adamantium;
                lastMana = mana;
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
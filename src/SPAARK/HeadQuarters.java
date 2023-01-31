package SPAARK;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Random;

public strictfp class HeadQuarters {
    protected RobotController rc;
    protected MapLocation me;
    private MapLocation center;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    private Random rng = new Random(2023);

    protected int hqIndex;
    private int locInt;
    private int hqCount = 4;

    private int anchorCooldown = 100;
    private int carrierCooldown = 0;
    private int amplifierCooldown = 0;
    private int carriers = 0;
    private int launchers = 0;
    private int amplifiers = 0;
    private int nearbyCarriers = 0;
    private int nearbyLaunchers = 0;
    private boolean attemptBreakout = false;
    protected boolean tooManyBots = false;
    protected boolean unsafe = false;

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
    protected ResourceType prioritizedResourceType = ResourceType.NO_RESOURCE;

    private WellInfo[] nearbyWells;
    private MapLocation[] spawningLocations;
    private WellInfo prioritizedWellInfo;

    private StringBuilder indicatorString = new StringBuilder();

    public HeadQuarters(RobotController rc) {
        try {
            this.rc = rc;
            me = rc.getLocation();
            center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
            // setting headquarter locations
            locInt = GlobalArray.intifyLocation(me);
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
            nearbyWells = rc.senseNearbyWells();
            spawningLocations = rc.getAllLocationsWithinRadiusSquared(me, RobotType.HEADQUARTERS.actionRadiusSquared);
            rng = new Random(rc.getID());
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
                storedLocations.detectSymmetry();
                storedLocations.updateMapSymmetry(globalArray.mapSymmetry());
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
                    tooManyBots = nearbyCarriers + nearbyLaunchers >= 30;
                    RobotInfo[] opponentBots = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                    int nearbyOpponents = 0;
                    for (RobotInfo r : opponentBots) {
                        if (Attack.prioritizedRobot(r.getType()) >= 3) {
                            nearbyOpponents++;
                        }
                    }
                    unsafe = nearbyOpponents >= 2;
                    indicatorString.append("C-L-A-NC-NL[" + carriers + ", " + launchers + ", " + amplifiers + ", " + nearbyCarriers + ", " + nearbyLaunchers + "]; ");
                }
                if (isPrimaryHQ) {
                    if (round % 2 == 0) {
                        rc.writeSharedArray(GlobalArray.CARRIERCOUNT, 0);
                        rc.writeSharedArray(GlobalArray.LAUNCHERCOUNT, 0);
                        rc.writeSharedArray(GlobalArray.AMPLIFIERCOUNT, rc.readSharedArray(GlobalArray.AMPLIFIERCOUNT) & 0b1111111100000000);
                    }
                }

                //unsafe
                if (unsafe) {
                    if (mana > RobotType.LAUNCHER.buildCostMana * 10) {
                        attemptBreakout = true;
                    }
                    if (mana < RobotType.LAUNCHER.buildCostMana) {
                        attemptBreakout = false;
                    }
                }

                spawnBots();

                // store
                GlobalArray.storeHeadquarters(this);

                // prioritized resources
                double deviation = ((mana + adamantium) != 0) ? (mana - (adamantium * 4.0)) / (mana + (adamantium * 4.0)) : 0;
                if (Math.abs(deviation) < 0.1) {
                    indicatorString.append("PR=NO; ");
                    prioritizedResourceType = ResourceType.NO_RESOURCE;
                } else if (deviation < 0) {
                    indicatorString.append("PR=MN; ");
                    prioritizedResourceType = ResourceType.MANA;
                } else {
                    indicatorString.append("PR=AD; ");
                    prioritizedResourceType = ResourceType.ADAMANTIUM;
                }
                globalArray.setPrioritizedResource(prioritizedResourceType, hqIndex);
                // primary headquarters stuff
                if (isPrimaryHQ) {
                    if (round == 2) {
                        hqCount = 0;
                        for (int i = GlobalArray.HEADQUARTERS; i < GlobalArray.HEADQUARTERS + GlobalArray.HEADQUARTERS_LENGTH; i++) {
                            if (GlobalArray.hasLocation(rc.readSharedArray(i))) hqCount++;
                        }
                        MapLocation headquarters[] = new MapLocation[hqCount];
                        for (int i = 0; i < hqCount; i++) {
                            headquarters[i] = GlobalArray.parseLocation(rc.readSharedArray(i + GlobalArray.HEADQUARTERS));
                        }
                        storedLocations.setHeadquarters(headquarters);
                        mapSizeFactor = (rc.getMapWidth() * rc.getMapHeight()) / 400;
                        globalArray.setMapSymmetry(7);
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
                    if (round >= 2) {
                        int manaWells = 0;
                        for (int i = 0; i < GlobalArray.MANA_WELLS_LENGTH; i++) {
                            if (GlobalArray.hasLocation(rc.readSharedArray(i + GlobalArray.MANA_WELLS))) {
                                manaWells++;
                            }
                        }
                        if (manaWells > 0) {
                            MapLocation wells[] = new MapLocation[manaWells];
                            int index = 0;
                            for (int i = 0; i < GlobalArray.MANA_WELLS_LENGTH; i++) {
                                if (GlobalArray.hasLocation(rc.readSharedArray(i + GlobalArray.MANA_WELLS))) {
                                    wells[index] = GlobalArray.parseLocation(rc.readSharedArray(i + GlobalArray.MANA_WELLS));
                                    index++;
                                }
                            }
                            MapLocationDistanceToCenterComparator distCompare = new MapLocationDistanceToCenterComparator();
                            distCompare.setCenter(center);
                            Arrays.sort(wells, distCompare);
                            rc.writeSharedArray(GlobalArray.OPPONENT_HEADQUARTERS + 1, GlobalArray.intifyLocation(wells[0]));
                        }
                    }
                    indicatorString.append(rc.readSharedArray(GlobalArray.OPPONENT_HEADQUARTERS));
                    indicatorString.append("SYM " + globalArray.mapSymmetry() + "; ");
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
        // also makees anchors
        int carriersProduced = 0;
        int launchersProduced = 0;
        MapLocation[] islands = GlobalArray.getKnownIslandLocations(rc, Team.NEUTRAL);
        boolean canProduceAnchor = islands.length > 0;
        updatePrioritizedWell();
        MapLocation optimalSpawningLocationWell = optimalSpawnLocation(true);
        MapLocation optimalSpawningLocation = optimalSpawnLocation(false);
        if (anchorCooldown <= 0 && canProduceAnchor && nearbyCarriers > 3 && rc.getNumAnchors(Anchor.STANDARD) == 0) {
            if (adamantium >= Anchor.STANDARD.adamantiumCost && mana >= Anchor.STANDARD.manaCost) {
                rc.buildAnchor(Anchor.STANDARD);
                indicatorString.append("P ANC; ");
                anchorCooldown = 50;
                if (rc.getIslandCount() >= 6 && round <= 1000) {
                    anchorCooldown = 150;
                }
            } else {
                indicatorString.append("TP ANC; ");
            }
            while (rc.isActionReady()) {
                if (optimalSpawningLocation != null && possibleSpawningLocations >= 3 && rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation)) {
                    if (round < 1000) {
                        while (optimalSpawningLocation != null && rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation)) {
                            rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                            launchersProduced++;
                            optimalSpawningLocation = updateOptimalSpawnLocation(optimalSpawningLocation, false);
                            if (GlobalArray.DEBUG_INFO >= 1) rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                        }
                    } else if (mana > Anchor.STANDARD.manaCost + RobotType.LAUNCHER.buildCostMana) {
                        //round >= 1000
                        rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                        launchersProduced++;
                        if (GlobalArray.DEBUG_INFO >= 1) rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                    }
                    else break;
                } else if (optimalSpawningLocationWell != null && adamantium > Anchor.STANDARD.adamantiumCost + RobotType.CARRIER.buildCostAdamantium && possibleSpawningLocations >= 4
                        && ((deltaResources < 0 && nearbyCarriers < 10) || carriers < 10 * hqCount || carrierCooldown <= 0)
                        && rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocationWell)) {
                    rc.buildRobot(RobotType.CARRIER, optimalSpawningLocationWell);
                    carriersProduced++;
                    if (GlobalArray.DEBUG_INFO >= 1) rc.setIndicatorLine(me, optimalSpawningLocationWell, 125, 125, 125);
                    carrierCooldown = 50;
                } else break;
                optimalSpawningLocationWell = updateOptimalSpawnLocation(optimalSpawningLocationWell, true);
                optimalSpawningLocation = updateOptimalSpawnLocation(optimalSpawningLocation, false);
            }
        } else {
            while (rc.isActionReady()) {
                if ((round > 1 || carriersProduced < 2) && possibleSpawningLocations >= 3 && !unsafe
                        && ((deltaResources < 0 && nearbyCarriers < 10) || carriers < 10 * hqCount || carrierCooldown <= 0)
                        && optimalSpawningLocationWell != null && rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocationWell)) {
                    rc.buildRobot(RobotType.CARRIER, optimalSpawningLocationWell);
                    carriersProduced++;
                    if (GlobalArray.DEBUG_INFO >= 1) rc.setIndicatorLine(me, optimalSpawningLocationWell, 125, 125, 125);
                    carrierCooldown = 50;
                } else {
                    if (rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation)
                            && (attemptBreakout || (!unsafe && (mana > RobotType.LAUNCHER.buildCostMana * 2)
                            && possibleSpawningLocations >= (unsafe ? 5 : 2)))) {
                        while (optimalSpawningLocation != null && rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation)) {
                            rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                            launchersProduced++;
                            optimalSpawningLocation = updateOptimalSpawnLocation(optimalSpawningLocation, false);
                            if (GlobalArray.DEBUG_INFO >= 1) rc.setIndicatorLine(me, optimalSpawningLocation, 125, 125, 125);
                        }
                    } else if (amplifiers < 4 && launchers > 5 && carriers > 5 && rc.canBuildRobot(RobotType.AMPLIFIER, optimalSpawningLocation)) {
                        rc.buildRobot(RobotType.AMPLIFIER, optimalSpawningLocation);
                    } else break;
                }
                optimalSpawningLocationWell = updateOptimalSpawnLocation(optimalSpawningLocationWell, true);
                optimalSpawningLocation = updateOptimalSpawnLocation(optimalSpawningLocation, false);
            }
        }
        if (carriersProduced > 0)
            indicatorString.append("P CAR-x" + carriersProduced + "; ");
        if (launchersProduced > 0)
            indicatorString.append("P LAU-x" + launchersProduced + "; ");
        anchorCooldown--;
        carrierCooldown--;
        amplifierCooldown--;
    }

    private void setTargetElixirWell() throws GameActionException {
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

    private void updatePrioritizedWell() throws GameActionException {
        if (nearbyWells.length > 0) {
            prioritizedWellInfo = nearbyWells[0];
            for (WellInfo w : nearbyWells) {
                if (prioritizedWellInfo.getResourceType() == prioritizedResourceType) {
                    if (w.getResourceType() == prioritizedResourceType && prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w.getMapLocation().distanceSquaredTo(me)) {
                        prioritizedWellInfo = w;
                    }
                } else {
                    if (prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w.getMapLocation().distanceSquaredTo(me)) {
                        prioritizedWellInfo = w;
                    }
                }
            }
        }
    }

    private MapLocation optimalSpawnLocation(boolean well) throws GameActionException {
        MapLocation optimalSpawningLocation = null;
        possibleSpawningLocations = 0;
        if (prioritizedWellInfo != null && well) {
            int optimalSpawningLocationDistance = Integer.MAX_VALUE;
            for (MapLocation m : spawningLocations) {
                if (!rc.sensePassability(m) || rc.isLocationOccupied(m)) {
                    continue;
                }
                possibleSpawningLocations++;
                if (optimalSpawningLocation == null) {
                    optimalSpawningLocation = m;
                } else {
                    int distance = m.distanceSquaredTo(prioritizedWellInfo.getMapLocation());
                    if (distance < optimalSpawningLocationDistance) {
                        optimalSpawningLocation = m;
                        optimalSpawningLocationDistance = distance;
                    }
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
                } else if (optimalSpawningLocation.distanceSquaredTo(center) > m.distanceSquaredTo(center)) {
                    optimalSpawningLocation = m;
                }
            }
        }
        return optimalSpawningLocation;
    }

    private MapLocation updateOptimalSpawnLocation(MapLocation currentLocation, boolean well) throws GameActionException {
        if (currentLocation == null) return null;
        if (prioritizedWellInfo == null) updatePrioritizedWell();
        MapLocation optimalSpawningLocation = null;
        int optimalSpawningLocationDistance = Integer.MAX_VALUE;
        for (Direction d : Motion.DIRECTIONS) {
            MapLocation newLocation = currentLocation.add(d);
            if (!rc.canSenseLocation(newLocation) || !rc.canActLocation(newLocation) || !rc.sensePassability(newLocation) || rc.isLocationOccupied(newLocation)) {
                continue;
            }
            if (prioritizedWellInfo != null) {
                int distance = newLocation.distanceSquaredTo(prioritizedWellInfo.getMapLocation());
                if (optimalSpawningLocation == null || distance < optimalSpawningLocationDistance) {
                    optimalSpawningLocation = newLocation;
                    optimalSpawningLocationDistance = distance;
                }
            } else {
                optimalSpawningLocation = newLocation;
                break;
            }
        }
        if (optimalSpawningLocation == null) {
            optimalSpawningLocation = optimalSpawnLocation(well);
        }
        return optimalSpawningLocation;
    }
}
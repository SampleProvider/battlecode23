package launcherCoagulation;

import battlecode.common.*;
import java.util.Random;

public strictfp class Carrier {
    protected RobotController rc;
    protected MapLocation me;
    private MapLocation center;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    private Random rng = new Random(2023);

    private ResourceType prioritizedResourceType = ResourceType.MANA;
    private int adamantiumAmount = 0;
    private int manaAmount = 0;
    private int elixirAmount = 0;
    private int resourceCollectAmount = 39;

    private MapLocation prioritizedWell;
    private ResourceType prioritizedWellType;
    private MapLocation[] headquarters;
    private MapLocation prioritizedHeadquarters;
    private int prioritizedHeadquarterIndex;

    private MapLocation prioritizedIslandLocation;

    private StoredLocations storedLocations;

    private boolean clockwiseRotation = true;
    private Direction lastDirection = Direction.CENTER;

    private MapLocation randomExploreLocation;
    private int randomExploreTime = 0;
    private final int randomExploreMinKnownWellDistSquared = 81;
    private final int randomExploreMinKnownHQDistSquared = 400;
    private boolean returningToStorePOI = false;

    private int lastHealth = 0;

    private int state = 0;
    // state
    // 0 is default (path to hq or pick up anchor, switches to 5 if nothing to do)
    // 1 is pathfinding to well
    // 2 is collecting
    // 3 is pathfinding to headquarters
    // 4 is pathfinding to island
    // 5 is retreat
    // 6 is explore
    // 7 is attack (lol)

    // carriers with non-prioritized resources (adamantium) will go and attack if headquarters si crowded

    private StringBuilder indicatorString = new StringBuilder();

    public Carrier(RobotController rc) {
        try {
            this.rc = rc;
            center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
            int hqCount = 0;
            for (int i = GlobalArray.HEADQUARTERS; i < GlobalArray.HEADQUARTERS + GlobalArray.HEADQUARTERS_LENGTH; i++) {
                if (GlobalArray.hasLocation(rc.readSharedArray(i)))
                    hqCount++;
            }
            headquarters = new MapLocation[hqCount];
            for (int i = 0; i < hqCount; i++) {
                headquarters[i] = GlobalArray.parseLocation(rc.readSharedArray(i + GlobalArray.HEADQUARTERS));
            }
            lastHealth = rc.getHealth();
            storedLocations = new StoredLocations(rc, headquarters);
            rng = new Random(rc.getID());
        } catch (GameActionException e) {
            System.out.println("GameActionException at Carrier constructor");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Carrier constructor");
            e.printStackTrace();
        } finally {
            run();
        }
    }

    private void run() {
        while (true) {
            try {
                if (FooBar.foobar && rng.nextInt(1000) == 0) FooBar.foo(rc);
                if (FooBar.foobar && rng.nextInt(1000) == 0) FooBar.bar(rc);
                me = rc.getLocation();
                round = rc.getRoundNum();
                globalArray.parseGameState(rc.readSharedArray(GlobalArray.GAMESTATE));
                adamantiumAmount = rc.getResourceAmount(ResourceType.ADAMANTIUM);
                manaAmount = rc.getResourceAmount(ResourceType.MANA);
                elixirAmount = rc.getResourceAmount(ResourceType.ELIXIR);

                prioritizedResourceType = globalArray.prioritizedResource(prioritizedHeadquarterIndex);

                indicatorString = new StringBuilder();

                indicatorString.append(state + "; ");
                indicatorString.append("PR=" + (prioritizedResourceType == ResourceType.MANA ? "MN" : prioritizedResourceType.toString().substring(0, 2)) + "; ");

                globalArray.incrementCount(rc);
                storedLocations.updateFullWells();
                storedLocations.detectIslandLocations();
                storedLocations.detectSymmetry(globalArray.mapSymmetry());
                if (storedLocations.writeToGlobalArray()) returningToStorePOI = false;

                updatePrioritizedHeadquarters();
                if (state != 2) {
                    updatePrioritizedWell();
                }
                MapLocation[] islands = GlobalArray.getKnownIslandLocations(rc, Team.NEUTRAL);
                boolean existsNeutralIsland = false;
                for (MapLocation m : islands) {
                    if (m != null) {
                        existsNeutralIsland = true;
                    }
                }
                if (rc.getAnchor() != null) {
                    if (!existsNeutralIsland || returningToStorePOI) {
                        indicatorString.append("TRY RET ANCHOR; ");
                        if (me.distanceSquaredTo(prioritizedHeadquarters) <= 2) {
                            if (rc.canReturnAnchor(prioritizedHeadquarters)) {
                                rc.returnAnchor(prioritizedHeadquarters);
                                state = 0;
                            }
                        }
                        else {
                            state = 3;
                            returningToStorePOI = true;
                        }
                    }
                    else {
                        state = 4;
                    }
                }
                else {
                    if (rc.canTakeAnchor(prioritizedHeadquarters, Anchor.STANDARD) && existsNeutralIsland) {
                        rc.takeAnchor(prioritizedHeadquarters, Anchor.STANDARD);
                        state = 4;
                    }
                    else {
                        if (state == 4) {
                            state = 0;
                        }
                    }
                }

                if (rc.getHealth() != lastHealth && state != 4) {
                    state = 5;
                    storedLocations.storeOpponentLocation(me);
                }
                lastHealth = rc.getHealth();

                if (rc.getAnchor() == null) {
                    RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
                    RobotInfo robot = Attack.attack(rc, prioritizedHeadquarters, robotInfo, false, indicatorString);
                    if (robot == null) {
                        robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                        robot = Attack.senseOpponent(rc, robotInfo);
                    }
                    if (robot != null && Attack.prioritizedRobot(robot.getType()) >= 3) {
                        // storedLocations.storeOpponentLocation(robot.getLocation());
                        // if (state != 4) {
                        //     state = 5;
                        // }
                    }
                }
                else {
                    RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                    RobotInfo robot = Attack.senseOpponent(rc, robotInfo);
                    if (robot != null && Attack.prioritizedRobot(robot.getType()) >= 3) {
                        // storedLocations.storeOpponentLocation(robot.getLocation());
                    }
                }

                runState();
                
                updatePrioritizedHeadquarters();
                if (rc.getAnchor() == null) {
                    RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
                    RobotInfo robot = Attack.attack(rc, prioritizedHeadquarters, robotInfo, false, indicatorString);
                    if (robot == null) {
                        robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                        robot = Attack.senseOpponent(rc, robotInfo);
                    }
                    if (robot != null && Attack.prioritizedRobot(robot.getType()) >= 3) {
                        // storedLocations.storeOpponentLocation(robot.getLocation());
                        // if (state != 4) {
                        //     state = 5;
                        // }
                    }
                }
                else {
                    RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                    RobotInfo robot = Attack.senseOpponent(rc, robotInfo);
                    if (robot != null && Attack.prioritizedRobot(robot.getType()) >= 3) {
                        // storedLocations.storeOpponentLocation(robot.getLocation());
                    }
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException at Carrier");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Carrier");
                e.printStackTrace();
            } finally {
                rc.setIndicatorString(indicatorString.toString());
                Clock.yield();
            }
        }
    }

    private void runState() throws GameActionException {
        if (state == 0) {
            MapLocation[] wellLocations = GlobalArray.getKnownWellLocations(rc);
            int storedAdamantiumWells = 0;
            int storedManaWells = 0;
            for (int i = 0;i < wellLocations.length;i++) {
                if (wellLocations[i] == null) {
                    continue;
                }
                if (i < GlobalArray.MANA_WELLS_LENGTH) {
                    storedManaWells += 1;
                }
                else {
                    storedAdamantiumWells += 1;
                }
            }
            // if (storedManaWells < 1) {
            //     state = 6;
            //     runState();
            //     return;
            // }
            if (adamantiumAmount + manaAmount + elixirAmount >= resourceCollectAmount) {
                state = 3;
                runState();
                return;
            } else {
                if (prioritizedWell != null) {
                    state = 1;
                    runState();
                    return;
                }
            }
            state = 6;
            runState();
            return;
        } else if (state == 1) {
            if (prioritizedWell == null) {
                state = 0;
                runState();
                return;
            }
            indicatorString.append("PATH->WELL; ");
            Direction[] bug2array = Motion.bug2(rc, prioritizedWell, lastDirection, clockwiseRotation, false, false, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
            if (rc.canCollectResource(prioritizedWell, -1) && adamantiumAmount + manaAmount + elixirAmount < resourceCollectAmount) {
                rc.collectResource(prioritizedWell, -1);
                state = 2;
            }
            else if (adamantiumAmount + manaAmount + elixirAmount >= resourceCollectAmount) {
                state = 3;
            }
            me = rc.getLocation();
            if (GlobalArray.DEBUG_INFO >= 4) {
                rc.setIndicatorLine(me, prioritizedWell, 255, 75, 75);
            } else if (GlobalArray.DEBUG_INFO > 0) {
                rc.setIndicatorDot(me, 255, 75, 75);
            }
        } else if (state == 2) {
            if (prioritizedWell == null) {
                state = 0;
                runState();
                return;
            }
            indicatorString.append("COLLECT; ");
            if (rc.canCollectResource(prioritizedWell, -1) && adamantiumAmount + manaAmount + elixirAmount < resourceCollectAmount) {
                rc.collectResource(prioritizedWell, -1);
                // Motion.circleAroundTarget(rc, me, prioritizedWell);
                me = rc.getLocation();
                if (GlobalArray.DEBUG_INFO >= 3) {
                    rc.setIndicatorLine(me, prioritizedWell, 255, 75, 75);
                } else if (GlobalArray.DEBUG_INFO > 0) {
                    rc.setIndicatorDot(me, 255, 75, 75);
                }
            } else {
                state = 0;
                runState();
            }
        } else if (state == 3) {
            if (GlobalArray.hasTooManyBots(rc.readSharedArray(prioritizedHeadquarterIndex))) {
                // if too many robots then linger aroudn edges to let bots out
                if (manaAmount == 0) {
                    // stop going and do attacky stuff
                }
            }
            indicatorString.append("PATH->HQ; ");
            Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, false, true, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
            if (prioritizedHeadquarters.distanceSquaredTo(me) <= rc.getType().visionRadiusSquared) {
                attemptTransfer();
            }
            me = rc.getLocation();
            if (GlobalArray.DEBUG_INFO >= 4) {
                rc.setIndicatorLine(me, prioritizedHeadquarters, 125, 25, 255);
            } else if (GlobalArray.DEBUG_INFO > 0) {
                rc.setIndicatorDot(me, 125, 25, 255);
            }
            if (adamantiumAmount + manaAmount + elixirAmount == 0 && !returningToStorePOI && rc.getAnchor() == null) {
                state = 0;
            }
        } else if (state == 4) {
            updatePrioritizedIsland();
            if (prioritizedIslandLocation != null) {
                Direction[] bug2array = Motion.bug2(rc, prioritizedIslandLocation, lastDirection, clockwiseRotation, false, true, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
                me = rc.getLocation();
                if (rc.canPlaceAnchor()) {
                    if (rc.senseTeamOccupyingIsland(rc.senseIsland(me)) == Team.NEUTRAL) {
                        indicatorString.append("P ANC; ");
                        rc.placeAnchor();
                        state = 0;
                    }
                }
                if (GlobalArray.DEBUG_INFO >= 2) {
                    rc.setIndicatorLine(me, prioritizedIslandLocation, 75, 125, 255);
                } else if (GlobalArray.DEBUG_INFO > 0) {
                    rc.setIndicatorDot(me, 75, 125, 255);
                }
            } else {
                state = 3;
                runState();
                return;
            }
        } else if (state == 5) {
            updatePrioritizedHeadquarters();
            indicatorString.append("RET; ");
            Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, false, true, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
            if (prioritizedHeadquarters.distanceSquaredTo(me) <= RobotType.HEADQUARTERS.visionRadiusSquared) {
                attemptTransfer();
                state = 0;
            }
            me = rc.getLocation();
            if (GlobalArray.DEBUG_INFO >= 4) {
                rc.setIndicatorLine(me, prioritizedHeadquarters, 125, 255, 0);
            } else if (GlobalArray.DEBUG_INFO > 0) {
                rc.setIndicatorDot(me, 125, 255, 0);
            }
        } else if (state == 6) {
            MapLocation[] wellLocations = GlobalArray.getKnownWellLocations(rc);
            int storedAdamantiumWells = 0;
            int storedManaWells = 0;
            for (int i = 0;i < wellLocations.length;i++) {
                if (wellLocations[i] == null) {
                    continue;
                }
                if (i < GlobalArray.MANA_WELLS_LENGTH) {
                    storedManaWells += 1;
                }
                else {
                    storedAdamantiumWells += 1;
                }
            }
            updatePrioritizedWell();
            if (prioritizedWell != null) {
                if (storedManaWells >= 0 || prioritizedWellType == ResourceType.MANA) {
                    state = 1;
                    runState();
                    return;
                }
            }
            if (round > 100) {
                MapLocation centerHeadquarters = headquarters[0];
                for (int i = 0; i < headquarters.length; i++) {
                    if (headquarters[i] != null) {
                        if (centerHeadquarters.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2)) > headquarters[i].distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2))) {
                            centerHeadquarters = headquarters[i];
                        }
                    }
                }
                int symmetry = (globalArray.mapSymmetry() & 0b1) + ((globalArray.mapSymmetry() >> 1) & 0b1) + ((globalArray.mapSymmetry() >> 2) & 0b1);
                if (symmetry == 2) {
                    if ((globalArray.mapSymmetry() & 0b1) == 1) {
                        Direction[] bug2array = Motion.bug2(rc, new MapLocation(rc.getMapWidth() - 1 - centerHeadquarters.x, centerHeadquarters.y), lastDirection, clockwiseRotation, false, false, indicatorString);
                        lastDirection = bug2array[0];
                        if (bug2array[1] == Direction.CENTER) {
                            clockwiseRotation = !clockwiseRotation;
                        }
                    }
                    else if (((globalArray.mapSymmetry() >> 1) & 0b1) == 1) {
                        Direction[] bug2array = Motion.bug2(rc, new MapLocation(centerHeadquarters.x, rc.getMapHeight() - 1 - centerHeadquarters.y), lastDirection, clockwiseRotation, false, false, indicatorString);
                        lastDirection = bug2array[0];
                        if (bug2array[1] == Direction.CENTER) {
                            clockwiseRotation = !clockwiseRotation;
                        }
                    }
                    if (storedLocations.foundNewLocations()) {
                        state = 3;
                        returningToStorePOI = true;
                    }
                    return;
                }
                else if (symmetry == 3) {
                    if (rc.getID() % 2 == 0) {
                        Direction[] bug2array = Motion.bug2(rc, new MapLocation(rc.getMapWidth() - 1 - centerHeadquarters.x, centerHeadquarters.y), lastDirection, clockwiseRotation, false, false, indicatorString);
                        lastDirection = bug2array[0];
                        if (bug2array[1] == Direction.CENTER) {
                            clockwiseRotation = !clockwiseRotation;
                        }
                    }
                    else {
                        Direction[] bug2array = Motion.bug2(rc, new MapLocation(centerHeadquarters.x, rc.getMapHeight() - 1 - centerHeadquarters.y), lastDirection, clockwiseRotation, false, false, indicatorString);
                        lastDirection = bug2array[0];
                        if (bug2array[1] == Direction.CENTER) {
                            clockwiseRotation = !clockwiseRotation;
                        }
                    }
                    if (storedLocations.foundNewLocations()) {
                        state = 3;
                        returningToStorePOI = true;
                    }
                    return;
                }
            }
            
            updateRandomExploreLocation();
            if (randomExploreLocation != null) {
                indicatorString.append("EXPL-" + randomExploreLocation.toString() + "; ");
                Direction[] bug2array = Motion.bug2(rc, randomExploreLocation, lastDirection, clockwiseRotation, false, false, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
                if (GlobalArray.DEBUG_INFO >= 2) {
                    rc.setIndicatorLine(me, randomExploreLocation, 0, 175, 0);
                } else if (GlobalArray.DEBUG_INFO > 0) {
                    rc.setIndicatorDot(me, 0, 175, 0);
                    rc.setIndicatorDot(randomExploreLocation, 0, 175, 0);
                }
                randomExploreTime++;
                if (randomExploreTime > 50 || randomExploreLocation.distanceSquaredTo(me) <= 4) {
                    randomExploreLocation = null;
                    if (storedLocations.foundNewLocations()) {
                        state = 3;
                        returningToStorePOI = true;
                    }
                }
            } else {
                Motion.spreadRandomly(rc, me);
            }
        }
    }

    private void attemptTransfer() throws GameActionException {
        if (rc.canTransferResource(prioritizedHeadquarters, ResourceType.ADAMANTIUM, adamantiumAmount)) {
            rc.transferResource(prioritizedHeadquarters, ResourceType.ADAMANTIUM, adamantiumAmount);
            indicatorString.append("D AD; ");
        }
        if (rc.canTransferResource(prioritizedHeadquarters, ResourceType.MANA, manaAmount)) {
            rc.transferResource(prioritizedHeadquarters, ResourceType.MANA, manaAmount);
            indicatorString.append("D MN; ");
        }
        if (rc.canTransferResource(prioritizedHeadquarters, ResourceType.ELIXIR, elixirAmount)) {
            rc.transferResource(prioritizedHeadquarters, ResourceType.ELIXIR, elixirAmount);
            indicatorString.append("D EL; ");
        }
    }

    private void updatePrioritizedHeadquarters() throws GameActionException {
        prioritizedHeadquarters = headquarters[0];
        for (int i = 0; i < headquarters.length; i++) {
            if (headquarters[i] != null) {
                if (prioritizedHeadquarters.distanceSquaredTo(me) > headquarters[i].distanceSquaredTo(me)) {
                    prioritizedHeadquarters = headquarters[i];
                    prioritizedHeadquarterIndex = i;
                }
            }
        }
    }

    private void updatePrioritizedWell() throws GameActionException {
        prioritizedWell = null;
        prioritizedWellType = null;
        WellInfo[] wellInfo = rc.senseNearbyWells();
        if (wellInfo.length > 0) {
            WellInfo prioritizedWellInfo = null;
            for (WellInfo w : wellInfo) {
                if (testValidWell(w.getMapLocation())) {
                    storedLocations.storeWell(w);
                    if (prioritizedWell == null) {
                        prioritizedWellInfo = w;
                        prioritizedWell = w.getMapLocation();
                    } else if (prioritizedWellInfo.getResourceType() == prioritizedResourceType) {
                        if (w.getResourceType() == prioritizedResourceType && prioritizedWell.distanceSquaredTo(me) > w.getMapLocation().distanceSquaredTo(me)) {
                            prioritizedWellInfo = w;
                            prioritizedWell = w.getMapLocation();
                        }
                    } else {
                        if (w.getResourceType() == prioritizedResourceType) {
                            prioritizedWellInfo = w;
                            prioritizedWell = w.getMapLocation();
                        } else if (prioritizedWell.distanceSquaredTo(me) > w.getMapLocation().distanceSquaredTo(me)) {
                            prioritizedWellInfo = w;
                            prioritizedWell = w.getMapLocation();
                        }
                    }
                }
            }
            // if (prioritizedWell != null && prioritizedWellType == prioritizedResourceType) {
            if (prioritizedWell != null) {
                prioritizedWellType = prioritizedWellInfo.getResourceType();
                return;
            }
        }
        MapLocation[] wellLocations = GlobalArray.getKnownWellLocations(rc);
        for (int i = 0;i < wellLocations.length;i++) {
            MapLocation m = wellLocations[i];
            if (m == null) {
                continue;
            }
            if (storedLocations.isFullWell(m)) {
                continue;
            }
            if (testValidWell(m)) {
                ResourceType resourceType = null;
                if (i < GlobalArray.MANA_WELLS_LENGTH) {
                    resourceType = ResourceType.MANA;
                }
                else {
                    resourceType = ResourceType.ADAMANTIUM;
                }
                if (prioritizedWell == null) {
                    prioritizedWell = m;
                    prioritizedWellType = resourceType;
                } else if (prioritizedWellType == prioritizedResourceType) {
                    if (resourceType == prioritizedResourceType && prioritizedWell.distanceSquaredTo(me) > m.distanceSquaredTo(me)) {
                        prioritizedWell = m;
                        prioritizedWellType = resourceType;
                    }
                } else {
                    if (resourceType == prioritizedResourceType) {
                        prioritizedWell = m;
                        prioritizedWellType = resourceType;
                    } else if (prioritizedWell.distanceSquaredTo(me) > m.distanceSquaredTo(me)) {
                        prioritizedWell = m;
                        prioritizedWellType = resourceType;
                    }
                }
            }
        }
    }

    private void updatePrioritizedIsland() throws GameActionException {
        int[] islands = rc.senseNearbyIslands();
        prioritizedIslandLocation = null;
        for (int id : islands) {
            if (rc.senseTeamOccupyingIsland(id) != rc.getTeam()) {
                MapLocation[] islandLocations = rc.senseNearbyIslandLocations(id);
                for (MapLocation m : islandLocations) {
                    if (prioritizedIslandLocation == null) {
                        prioritizedIslandLocation = m;
                    } else if (m.distanceSquaredTo(me) < prioritizedIslandLocation.distanceSquaredTo(me)) {
                        prioritizedIslandLocation = m;
                    }
                }
            }
        }
        if (prioritizedIslandLocation == null) {
            // get island location from global array
            MapLocation[] islandLocations = GlobalArray.getKnownIslandLocations(rc, Team.NEUTRAL);
            for (MapLocation m : islandLocations) {
                if (m == null) {
                    continue;
                }
                if (rc.canSenseLocation(m)) {
                    if (rc.senseTeamOccupyingIsland(rc.senseIsland(m)) == rc.getTeam()) {
                        continue;
                    }
                }
                if (prioritizedIslandLocation == null) {
                    prioritizedIslandLocation = m;
                }
                else if (m.distanceSquaredTo(me) < prioritizedIslandLocation.distanceSquaredTo(me)) {
                    prioritizedIslandLocation = m;
                }
            }
        }
    }

    private boolean testValidWell(MapLocation well) throws GameActionException {
        // MapLocation[] opponentLocations = GlobalArray.getKnownOpponentLocations(rc);
        // for (MapLocation m : opponentLocations) {
        //     if (m == null) {
        //         continue;
        //     }
        //     if (m.distanceSquaredTo(well) <= StoredLocations.MIN_EXISTING_DISTANCE_SQUARED) {
        //         return false;
        //     }
        // }
        int emptySpots = 0;
        int fullSpots = 0;
        for (Direction d : Motion.DIRECTIONS) {
            MapLocation adjSpot = well.add(d);
            if (!rc.canSenseLocation(adjSpot)) {
                continue;
            }
            if (!rc.sensePassability(adjSpot) || rc.senseRobotAtLocation(adjSpot) != null) {
                fullSpots += 1;
            } else {
                emptySpots += 1;
            }
        }
        if (testFull(fullSpots, emptySpots)) {
            return true;
        }
        storedLocations.fullWell(well);
        return false;
    }
    
    private boolean testFull(int fullSpots, int emptySpots) {
        // optimize??
        // return fullSpots <= emptySpots + 1;
        if (fullSpots + emptySpots == 0) {
            return true;
        }
        return emptySpots >= 1;
    }

    private void updateRandomExploreLocation() throws GameActionException {
        if (randomExploreLocation != null) return;
        randomExploreTime = 0;
        MapLocation[] knownWells = GlobalArray.getKnownWellLocations(rc);
        int iteration = 0;
        search: while (randomExploreLocation == null && iteration < 16) {
            randomExploreLocation = new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight()));
            for (MapLocation well : knownWells) {
                if (well != null && well.distanceSquaredTo(randomExploreLocation) < randomExploreMinKnownWellDistSquared) {
                    randomExploreLocation = null;
                    continue search;
                }
            }
            for (MapLocation hq : headquarters) {
                if (hq.distanceSquaredTo(randomExploreLocation) < randomExploreMinKnownHQDistSquared) {
                    randomExploreLocation = null;
                    continue search;
                }
            }
            iteration++;
        }
    }
}
package betterthings2;

import battlecode.common.*;
import java.util.Random;

public strictfp class Launcher {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    private final Random rng = new Random(2023);

    private MapLocation[] headquarters;
    private MapLocation prioritizedHeadquarters;
    private MapLocation prioritizedOpponentHeadquarters;

    private StoredLocations storedLocations;

    private RobotInfo opponent;

    private RobotType prioritizedRobotType = RobotType.LAUNCHER;
    private int amplifierSensingRange = 25;
    private int amplifierCircleRange = 7;

    private int launcherCircleRange = 2;

    private int headquarterCircleRange = 16;

    private int defenseRange = 144;
    private int edgeRange = 4;

    private MapLocation lastLauncherLocation;

    protected int amplifierID = -1;

    private MapLocation prioritizedAmplifierLocation;

    private boolean clockwiseRotation = true;
    private Direction lastDirection = Direction.CENTER;

    private int lastHealth = 0;

    private StringBuilder indicatorString = new StringBuilder();

    public Launcher(RobotController rc) {
        try {
            this.rc = rc;
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
        } catch (GameActionException e) {
            System.out.println("GameActionException at Launcher constructor");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Launcher constructor");
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

                indicatorString = new StringBuilder();

                storedLocations.detectWells();
                storedLocations.detectIslandLocations();
                storedLocations.writeToGlobalArray();

                runState();

                lastHealth = rc.getHealth();
            } catch (GameActionException e) {
                System.out.println("GameActionException at Launcher");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Launcher");
                e.printStackTrace();
            } finally {
                rc.setIndicatorString(indicatorString.toString());
                Clock.yield();
            }
        }
    }

    private void runState() throws GameActionException {

        prioritizedHeadquarters = headquarters[0];
        for (MapLocation hq : headquarters) {
            if (hq != null) {
                if (prioritizedHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                    prioritizedHeadquarters = hq;
                }
            }
        }

        RobotInfo[] friendlyRobotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam());
        RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        RobotInfo robot = Attack.attack(rc, me, robotInfo, prioritizedRobotType, true, indicatorString);
        if (robot != null && robot.getType() == RobotType.LAUNCHER) {
            Direction[] bug2array = Motion.bug2retreat(rc, robot.getLocation(), lastDirection, clockwiseRotation, false, friendlyRobotInfo, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
            opponent = robot;
            return;
        }
        robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());

        updatePrioritizedOpponentHeadquarters(robotInfo);

        if (prioritizedOpponentHeadquarters != null && me.distanceSquaredTo(prioritizedOpponentHeadquarters) <= RobotType.HEADQUARTERS.actionRadiusSquared) {
            Direction[] bug2array = Motion.bug2retreat(rc, prioritizedOpponentHeadquarters, lastDirection, clockwiseRotation, false, friendlyRobotInfo, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
            return;
        }

        if (robot == null) {
            robot = Attack.senseOpponent(rc, me, robotInfo);
        }

        if (rc.getHealth() != lastHealth) {
            if (robot != null && robot.getType() == RobotType.LAUNCHER) {
                Direction[] bug2array = Motion.bug2retreat(rc, robot.getLocation(), lastDirection, clockwiseRotation, true, friendlyRobotInfo, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
                opponent = robot;
                return;
            }
            else {
                Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, true, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
                return;
            }
        }

        if (robot != null) {
            opponent = robot;
        }
        
        if (round % 2 == 0) {
            MapLocation[] opponentLocations = GlobalArray.getKnownOpponentLocations(rc);
            MapLocation prioritizedOpponentLocation = null;
            int prioritizedOpponentLocationIndex = -1;
            int index = -1;
            for (MapLocation m : opponentLocations) {
                index++;
                if (m == null) {
                    continue;
                }
                if (prioritizedOpponentLocation == null) {
                    prioritizedOpponentLocation = m;
                    prioritizedOpponentLocationIndex = index;
                    continue;
                }
                if (prioritizedOpponentLocation.distanceSquaredTo(me) > m.distanceSquaredTo(me)) {
                    prioritizedOpponentLocation = m;
                    prioritizedOpponentLocationIndex = index;
                }
            }
            if (prioritizedOpponentLocation != null && prioritizedOpponentLocation.distanceSquaredTo(me) <= defenseRange) {
                // if carrier/amplifier/hq writes opponent location, try to go there
                if (GlobalArray.DEBUG_INFO >= 3) {
                    rc.setIndicatorLine(me, prioritizedOpponentLocation, 75, 255, 75);
                }
                else if (GlobalArray.DEBUG_INFO >= 2) {
                    rc.setIndicatorDot(me, 75, 255, 75);
                }
                if (storedLocations.removedOpponents[prioritizedOpponentLocationIndex] == true) {
                    Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, true, indicatorString);
                    lastDirection = bug2array[0];
                    if (bug2array[1] == Direction.CENTER) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                } else {
                    Direction[] bug2array = Motion.bug2(rc, prioritizedOpponentLocation, lastDirection, clockwiseRotation, true, indicatorString);
                    lastDirection = bug2array[0];
                    if (bug2array[1] == Direction.CENTER) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                    boolean allDead = true;
                    for (RobotInfo r : robotInfo) {
                        if (r.getType() != RobotType.HEADQUARTERS) {
                            allDead = false;
                        }
                    }
                    if (prioritizedOpponentLocation.distanceSquaredTo(me) <= 2 && allDead) {
                        storedLocations.removeOpponentLocation(prioritizedOpponentLocationIndex);
                    }
                }
                return;
            }
            int surroundingLaunchers = 0;
            if (friendlyRobotInfo.length > 0) {
                RobotInfo lowestIdFriendlyRobotInfo = null;
                RobotInfo highestIdFriendlyRobotInfo = null;
                for (RobotInfo w : friendlyRobotInfo) {
                    if (w.getType() != RobotType.LAUNCHER) {
                        continue;
                    }
                    surroundingLaunchers += 1;
                    if (lowestIdFriendlyRobotInfo == null) {
                        lowestIdFriendlyRobotInfo = w;
                        highestIdFriendlyRobotInfo = w;
                        continue;
                    }
                    if (lowestIdFriendlyRobotInfo.ID > w.ID) {
                        lowestIdFriendlyRobotInfo = w;
                    }
                    if (highestIdFriendlyRobotInfo.ID < w.ID) {
                        highestIdFriendlyRobotInfo = w;
                    }
                }
                if (surroundingLaunchers >= 5) {
                    // try to swarm
                    if (lowestIdFriendlyRobotInfo.ID > rc.getID()) {
                        if (opponent != null) {
                            Direction[] bug2array = Motion.bug2(rc, opponent.getLocation(), lastDirection, clockwiseRotation, true, indicatorString);
                            lastDirection = bug2array[0];
                            if (bug2array[1] == Direction.CENTER) {
                                clockwiseRotation = !clockwiseRotation;
                            }
                            me = rc.getLocation();
                            if (GlobalArray.DEBUG_INFO >= 3) {
                                rc.setIndicatorLine(me, opponent.getLocation(), 255, 125, 25);
                            }
                            else if (GlobalArray.DEBUG_INFO >= 2) {
                                rc.setIndicatorDot(me, 255, 125, 25);
                            }
                            if (me.distanceSquaredTo(opponent.getLocation()) <= 5) {
                                opponent = null;
                            }
                        } else {
                            if (surroundingLaunchers >= 3) {
                                if (prioritizedHeadquarters.distanceSquaredTo(me) <= 100) {
                                    Direction[] bug2array = Motion.bug2(rc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2), lastDirection, clockwiseRotation, true, indicatorString);
                                    lastDirection = bug2array[0];
                                    if (bug2array[1] == Direction.CENTER) {
                                        clockwiseRotation = !clockwiseRotation;
                                    }
                                } else {
                                    int[] islands = rc.senseNearbyIslands();
                                    MapLocation prioritizedIslandLocation = null;
                                    for (int id : islands) {
                                        if (rc.senseTeamOccupyingIsland(id) == rc.getTeam().opponent()) {
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
                                    if (prioritizedIslandLocation != null) {
                                        Direction[] bug2array = Motion.bug2(rc, prioritizedIslandLocation, lastDirection, clockwiseRotation, true, indicatorString);
                                        lastDirection = bug2array[0];
                                        if (bug2array[1] == Direction.CENTER) {
                                            clockwiseRotation = !clockwiseRotation;
                                        }
                                        me = rc.getLocation();
                                        if (GlobalArray.DEBUG_INFO >= 3) {
                                            rc.setIndicatorLine(me, prioritizedIslandLocation, 75, 255, 255);
                                        }
                                        if (GlobalArray.DEBUG_INFO >= 2) {
                                            rc.setIndicatorDot(me, 75, 255, 255);
                                        }
                                    } else {
                                        // get island location from global array
                                        MapLocation[] islandLocations = GlobalArray.getKnownIslandLocations(rc, rc.getTeam().opponent());
                                        for (MapLocation m : islandLocations) {
                                            if (m == null) {
                                                continue;
                                            }
                                            if (prioritizedIslandLocation == null) {
                                                prioritizedIslandLocation = m;
                                            }
                                            else if (m.distanceSquaredTo(me) < prioritizedIslandLocation.distanceSquaredTo(me)) {
                                                prioritizedIslandLocation = m;
                                            }
                                        }
                                        if (prioritizedIslandLocation != null) {
                                            Direction[] bug2array = Motion.bug2(rc, prioritizedIslandLocation, lastDirection, clockwiseRotation, false, indicatorString);
                                            lastDirection = bug2array[0];
                                            if (bug2array[1] == Direction.CENTER) {
                                                clockwiseRotation = !clockwiseRotation;
                                            }
                                            me = rc.getLocation();
                                            if (GlobalArray.DEBUG_INFO >= 3) {
                                                rc.setIndicatorLine(me, prioritizedIslandLocation, 75, 255, 255);
                                            }
                                            if (GlobalArray.DEBUG_INFO >= 2) {
                                                rc.setIndicatorDot(me, 75, 255, 255);
                                            }
                                        }
                                        else {
                                            Motion.moveRandomly(rc);
                                        }
                                    }
                                }
                            }
                            else {
                                headquarterCircleRange = 16 + surroundingLaunchers / 3;
                                if (me.distanceSquaredTo(prioritizedHeadquarters) <= headquarterCircleRange * 1.25) {
                                    if (me.x <= edgeRange || me.x >= rc.getMapWidth() - edgeRange || me.y <= edgeRange || me.y >= rc.getMapHeight() - edgeRange) {
                                        if (rc.isMovementReady()) {
                                            clockwiseRotation = !clockwiseRotation;
                                        }
                                    }
                                    clockwiseRotation = Motion.circleAroundTarget(rc, prioritizedHeadquarters, headquarterCircleRange, clockwiseRotation, true);
                                } else {
                                    Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, true, indicatorString);
                                    lastDirection = bug2array[0];
                                    if (bug2array[1] == Direction.CENTER) {
                                        clockwiseRotation = !clockwiseRotation;
                                    }
                                }
                            }
                        }
                        lastLauncherLocation = null;
                    } else {
                        if (opponent != null) {
                            if (GlobalArray.DEBUG_INFO >= 3) {
                                rc.setIndicatorLine(me, opponent.getLocation(), 255, 125, 25);
                            }
                            else if (GlobalArray.DEBUG_INFO >= 2) {
                                rc.setIndicatorDot(me, 255, 125, 25);
                            }
                            Direction[] bug2array = Motion.bug2retreat(rc, opponent.getLocation(), lastDirection, clockwiseRotation, true, friendlyRobotInfo, indicatorString);
                            lastDirection = bug2array[0];
                            if (bug2array[1] == Direction.CENTER) {
                                clockwiseRotation = !clockwiseRotation;
                            }
                            me = rc.getLocation();
                            // if (surroundingLaunchers <= 4) {
                            //     if (me.distanceSquaredTo(opponent) <= 8) {
                            //         opponent = null;
                            //     }
                            // }
                            if (me.distanceSquaredTo(opponent.getLocation()) > RobotType.LAUNCHER.visionRadiusSquared) {
                                opponent = null;
                            }
                            lastLauncherLocation = null;
                        } else {
                            if (prioritizedHeadquarters.distanceSquaredTo(me) <= 100) {
                                Direction[] bug2array = Motion.bug2(rc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2), lastDirection, clockwiseRotation, true, indicatorString);
                                lastDirection = bug2array[0];
                                if (bug2array[1] == Direction.CENTER) {
                                    clockwiseRotation = !clockwiseRotation;
                                }
                            }
                            else if (lastLauncherLocation != null && rc.canMove(lastLauncherLocation.directionTo(lowestIdFriendlyRobotInfo.getLocation()))) {
                                rc.move(lastLauncherLocation.directionTo(lowestIdFriendlyRobotInfo.getLocation()));
                                lastLauncherLocation = lowestIdFriendlyRobotInfo.getLocation();
                            }
                            else {
                                if (me.distanceSquaredTo(lowestIdFriendlyRobotInfo.getLocation()) <= launcherCircleRange * 1.5) {
                                    clockwiseRotation = Motion.circleAroundTarget(rc, lowestIdFriendlyRobotInfo.getLocation(), launcherCircleRange, clockwiseRotation, true);
                                    lastLauncherLocation = lowestIdFriendlyRobotInfo.getLocation();
                                }
                                else {
                                    Direction[] bug2array = Motion.bug2(rc, lowestIdFriendlyRobotInfo.getLocation(), lastDirection, clockwiseRotation, true, indicatorString);
                                    lastDirection = bug2array[0];
                                    if (bug2array[1] == Direction.CENTER) {
                                        clockwiseRotation = !clockwiseRotation;
                                    }
                                }
                            }
                        }
                        me = rc.getLocation();
                        if (GlobalArray.DEBUG_INFO >= 3) {
                            rc.setIndicatorLine(me, lowestIdFriendlyRobotInfo.getLocation(), 255, 255, 75);
                        }
                        else if (GlobalArray.DEBUG_INFO >= 2) {
                            rc.setIndicatorDot(me, 255, 255, 75);
                        }
                    }
                    return;
                }
                else if (surroundingLaunchers >= 1) {
                    if (lowestIdFriendlyRobotInfo.ID > rc.getID()) {
                        headquarterCircleRange = 16 + surroundingLaunchers / 3;
                        if (prioritizedHeadquarters.distanceSquaredTo(me) <= 100) {
                            Direction[] bug2array = Motion.bug2(rc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2), lastDirection, clockwiseRotation, true, indicatorString);
                            lastDirection = bug2array[0];
                            if (bug2array[1] == Direction.CENTER) {
                                clockwiseRotation = !clockwiseRotation;
                            }
                        }
                        else {
                            if (me.x <= edgeRange || me.x >= rc.getMapWidth() - edgeRange || me.y <= edgeRange || me.y >= rc.getMapHeight() - edgeRange) {
                                if (rc.isMovementReady()) {
                                    clockwiseRotation = !clockwiseRotation;
                                }
                            }
                            clockwiseRotation = Motion.circleAroundTarget(rc, prioritizedHeadquarters, 100, clockwiseRotation, true);
                        }
                    }
                    else {
                        if (lastLauncherLocation != null && rc.canMove(lastLauncherLocation.directionTo(lowestIdFriendlyRobotInfo.getLocation()))) {
                            rc.move(lastLauncherLocation.directionTo(lowestIdFriendlyRobotInfo.getLocation()));
                            lastLauncherLocation = lowestIdFriendlyRobotInfo.getLocation();
                        }
                        else {
                            if (me.distanceSquaredTo(lowestIdFriendlyRobotInfo.getLocation()) <= launcherCircleRange * 1.5) {
                                clockwiseRotation = Motion.circleAroundTarget(rc, lowestIdFriendlyRobotInfo.getLocation(), launcherCircleRange, clockwiseRotation, true);
                                lastLauncherLocation = lowestIdFriendlyRobotInfo.getLocation();
                            }
                            else {
                                Direction[] bug2array = Motion.bug2(rc, lowestIdFriendlyRobotInfo.getLocation(), lastDirection, clockwiseRotation, true, indicatorString);
                                lastDirection = bug2array[0];
                                if (bug2array[1] == Direction.CENTER) {
                                    clockwiseRotation = !clockwiseRotation;
                                }
                            }
                        }
                    }
                    return;
                }
            }
            // defend hq
            indicatorString.append("DEF; ");
            headquarterCircleRange = 16 + surroundingLaunchers / 3;
            lastLauncherLocation = null;
            // if (opponent != null) {
            //     if (GlobalArray.DEBUG_INFO >= 3) {
            //         rc.setIndicatorLine(me, opponent, 255, 125, 25);
            //     }
            //     Direction[] bug2array = Motion.bug2(rc, opponent, lastDirection, clockwiseRotation, indicatorString);
            //     lastDirection = bug2array[0];
            //     if (bug2array[1] == Direction.CENTER) {
            //         clockwiseRotation = !clockwiseRotation;
            //     }
            //     me = rc.getLocation();
            //     if (me.distanceSquaredTo(opponent) <= 2) {
            //         opponent = null;
            //     }
            // } else 
            if (me.distanceSquaredTo(prioritizedHeadquarters) <= headquarterCircleRange * 1.25) {
                if (me.x <= edgeRange || me.x >= rc.getMapWidth() - edgeRange || me.y <= edgeRange || me.y >= rc.getMapHeight() - edgeRange) {
                    if (rc.isMovementReady()) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                }
                clockwiseRotation = Motion.circleAroundTarget(rc, prioritizedHeadquarters, headquarterCircleRange, clockwiseRotation, true);
            } else {
                Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, false, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
            }
            me = rc.getLocation();
            if (GlobalArray.DEBUG_INFO >= 3) {
                rc.setIndicatorDot(me, 75, 255, 75);
            }
            else if (GlobalArray.DEBUG_INFO >= 2) {
                rc.setIndicatorDot(me, 75, 255, 75);
            }
        }
    }

    private boolean detectAmplifier() throws GameActionException {
        // prioritizedAmplifierLocation = null;
        // for (int a = 0; a < GlobalArray.AMPLIFIERS_LENGTH; a++) {
        //     int amplifierArray = rc.readSharedArray(GlobalArray.AMPLIFIERS + a);
        //     if (amplifierArray != 0) {
        //         MapLocation amplifierLocation = GlobalArray.parseLocation(amplifierArray);
        //         if (amplifierLocation.distanceSquaredTo(me) < amplifierSensingRange) {
        //             if (prioritizedAmplifierLocation == null) {
        //                 prioritizedAmplifierLocation = amplifierLocation;
        //                 amplifierID = GlobalArray.AMPLIFIERS + a;
        //             } else if (amplifierLocation.distanceSquaredTo(me) < prioritizedAmplifierLocation.distanceSquaredTo(me)) {
        //                 prioritizedAmplifierLocation = amplifierLocation;
        //                 amplifierID = GlobalArray.AMPLIFIERS + a;
        //             }
        //         }
        //     }
        // }
        // if (prioritizedAmplifierLocation != null) {
        //     return true;
        // }
        return false;
    }

    private void updatePrioritizedOpponentHeadquarters(RobotInfo[] robotInfo) throws GameActionException {
        prioritizedOpponentHeadquarters = null;
        for (RobotInfo r : robotInfo) {
            if (r.getType() == RobotType.HEADQUARTERS) {
                if (prioritizedOpponentHeadquarters == null) {
                    prioritizedOpponentHeadquarters = r.getLocation();
                    continue;
                }
                if (prioritizedOpponentHeadquarters.distanceSquaredTo(me) > r.getLocation().distanceSquaredTo(me)) {
                    prioritizedOpponentHeadquarters = r.getLocation();
                }
            }
        }
    }
}
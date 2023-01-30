package launcherexplore;

import battlecode.common.*;
import java.util.Random;

public strictfp class Launcher {
    protected RobotController rc;
    protected MapLocation me;
    private MapLocation center;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    private final Random rng = new Random(2023);

    private MapLocation[] headquarters;
    private MapLocation prioritizedHeadquarters;
    private MapLocation prioritizedOpponentHeadquarters;

    private StoredLocations storedLocations;

    private RobotInfo opponent;

    private int launcherCircleRange = 5;

    private int headquarterCircleRange = 16;

    private int defenseRange = 25;

    private MapLocation lastLauncherLocation;

    protected int amplifierID = -1;

    private boolean clockwiseRotation = true;
    private Direction lastDirection = Direction.CENTER;

    private boolean arrivedAtCenter = false;
    private int turnsMovingToCenter = 0;

    private int lastHealth = 0;

    private StringBuilder indicatorString = new StringBuilder();

    private MapLocation symmetryDetectionHeadquarters;
    private int originalSuspectedSymmetry = 1;
    private int suspectedSymmetry = 1;
    private int symmetry = 0;
    // 1: horz
    // 2: vert
    // 3: rot

    public Launcher(RobotController rc) {
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

            indicatorString = new StringBuilder();

            if (rng.nextBoolean()) {
                suspectedSymmetry = 2;
            }
            originalSuspectedSymmetry = suspectedSymmetry;
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
                globalArray.parseGameState(rc.readSharedArray(GlobalArray.GAMESTATE));

                indicatorString = new StringBuilder();

                globalArray.incrementCount(rc);
                storedLocations.detectWells();
                storedLocations.detectIslandLocations();
                storedLocations.detectSymmetry(globalArray.mapSymmetry());
                storedLocations.writeToGlobalArray();

                runState();

                updatePrioritizedHeadquarters();

                RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
                RobotInfo robot = Attack.attack(rc, prioritizedHeadquarters, robotInfo, true, indicatorString);
                robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
        
                if (robot == null) {
                    robot = Attack.senseOpponent(rc, robotInfo);
                }

                if (robot != null) {
                    opponent = robot;
                }
        
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
        updatePrioritizedHeadquarters();
        symmetryDetectionHeadquarters = headquarters[0];
        double centerX = (rc.getMapWidth() - 1.0) / 2.0;
        double centerY = (rc.getMapHeight() - 1.0) / 2.0;
        for (int i = 0; i < headquarters.length; i++) {
            if (headquarters[i] != null) {
                if (Math.sqrt((centerX - headquarters[i].x) * (centerX - headquarters[i].x) + (centerY - headquarters[i].y) * (centerY - headquarters[i].y)) < Math.sqrt((centerX - symmetryDetectionHeadquarters.x) * (centerX - symmetryDetectionHeadquarters.x) + (centerY - symmetryDetectionHeadquarters.y) * (centerY - symmetryDetectionHeadquarters.y))) {
                    symmetryDetectionHeadquarters = headquarters[i];
                }
            }
        }
        MapLocation target;
        if (symmetry == 0) {
            if (suspectedSymmetry == 1) {
                target = new MapLocation(rc.getMapWidth() - symmetryDetectionHeadquarters.x - 1, symmetryDetectionHeadquarters.y);
            } else if (suspectedSymmetry == 2) {
                target = new MapLocation(symmetryDetectionHeadquarters.x, rc.getMapHeight() - symmetryDetectionHeadquarters.y - 1);
            } else {
                //suspectedSymmetry = 3
                target = new MapLocation(rc.getMapWidth() - symmetryDetectionHeadquarters.x - 1, rc.getMapHeight() - symmetryDetectionHeadquarters.y - 1);
            }
        } else {
            if (symmetry == 1) {
                target = new MapLocation(rc.getMapWidth() - symmetryDetectionHeadquarters.x - 1, symmetryDetectionHeadquarters.y);
            } else if (symmetry == 2) {
                target = new MapLocation(symmetryDetectionHeadquarters.x, rc.getMapHeight() - symmetryDetectionHeadquarters.y - 1);
            } else {
                //symmetry = 3
                target = new MapLocation(rc.getMapWidth() - symmetryDetectionHeadquarters.x - 1, rc.getMapHeight() - symmetryDetectionHeadquarters.y - 1);
            }
        }
        if (rc.canSenseLocation(target)) {
            RobotInfo hq = rc.senseRobotAtLocation(target);
            if (hq != null && hq.type == RobotType.HEADQUARTERS && hq.team == rc.getTeam().opponent()) {
                symmetry = suspectedSymmetry;
            } else {
                if (suspectedSymmetry == 1) {
                    suspectedSymmetry = 3;
                } else if (suspectedSymmetry == 3) {
                    if (originalSuspectedSymmetry == 1) {
                        suspectedSymmetry = 2;
                    } else {
                        suspectedSymmetry = 1;
                    }
                } else if (suspectedSymmetry == 2) {
                    suspectedSymmetry = 3;
                }
            }
        }
        Direction[] bug2array = Motion.bug2(rc, target, lastDirection, clockwiseRotation, false, true, indicatorString);
        lastDirection = bug2array[0];
        if (bug2array[1] == Direction.CENTER) {
            clockwiseRotation = !clockwiseRotation;
        }
        me = rc.getLocation();
        rc.setIndicatorLine(me, target, 0, 0, 0);
        // updatePrioritizedHeadquarters();
        // RobotInfo[] friendlyRobotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam());
        // RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        // RobotInfo robot = Attack.attack(rc, prioritizedHeadquarters, robotInfo, true, indicatorString);
        // if (robot != null && Attack.prioritizedRobot(robot.getType()) >= 3) {
        //     indicatorString.append("RET; ");
        //     if (rc.isMovementReady()) {
        //         Direction[] bug2array = Motion.bug2retreat(rc, robotInfo, friendlyRobotInfo, prioritizedHeadquarters, lastDirection, clockwiseRotation, false, true, indicatorString);
        //         lastDirection = bug2array[0];
        //         if (bug2array[1] == Direction.CENTER) {
        //             clockwiseRotation = !clockwiseRotation;
        //         }
        //     }
        //     opponent = robot;
        //     return;
        // }
        // robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());

        // updatePrioritizedOpponentHeadquarters(robotInfo);

        // if (prioritizedOpponentHeadquarters != null && me.distanceSquaredTo(prioritizedOpponentHeadquarters) <= RobotType.HEADQUARTERS.actionRadiusSquared) {
        //     // uh oh, too close to opponent hq, aaa take damage
        //     Direction[] bug2array = Motion.bug2retreat(rc, robotInfo, friendlyRobotInfo, prioritizedHeadquarters, lastDirection, clockwiseRotation, false, true, indicatorString);
        //     lastDirection = bug2array[0];
        //     if (bug2array[1] == Direction.CENTER) {
        //         clockwiseRotation = !clockwiseRotation;
        //     }
        //     return;
        // }

        // if (robot == null) {
        //     robot = Attack.senseOpponent(rc, robotInfo);
        // }

        // // if (rc.getHealth() != lastHealth) {
        // //     // aa i got hit
        // //     if (robot != null && Attack.prioritizedRobot(robot.getType()) >= 3) {
        // //         indicatorString.append("RET; HIT; ");
        // //         Direction[] bug2array = Motion.bug2retreat(rc, robotInfo, friendlyRobotInfo, prioritizedHeadquarters, lastDirection, clockwiseRotation, false, true, indicatorString);
        // //         lastDirection = bug2array[0];
        // //         if (bug2array[1] == Direction.CENTER) {
        // //             clockwiseRotation = !clockwiseRotation;
        // //         }
        // //         opponent = robot;
        // //         return;
        // //     }
        // //     else {
        // //         indicatorString.append("RET; HIT; ");
        // //         Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, true, true, indicatorString);
        // //         lastDirection = bug2array[0];
        // //         if (bug2array[1] == Direction.CENTER) {
        // //             clockwiseRotation = !clockwiseRotation;
        // //         }
        // //         return;
        // //     }
        // // }

        // if (robot != null) {
        //     opponent = robot;
        // }
        
        // if (rc.isMovementReady()) {
        //     // lets move!!

        //     if (storedLocations.notSymmetry != 0) {
        //         indicatorString.append("TRY STO; ");
        //         Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, true, true, indicatorString);
        //         lastDirection = bug2array[0];
        //         if (bug2array[1] == Direction.CENTER) {
        //             clockwiseRotation = !clockwiseRotation;
        //         }
        //         return;
        //     }
            
        //     if (rc.getHealth() <= RobotType.LAUNCHER.health * 3 / 4) {
        //         indicatorString.append("LOW HP; ");
        //         int[] islands = rc.senseNearbyIslands();
        //         MapLocation prioritizedIslandLocation = null;
        //         for (int id : islands) {
        //             if (rc.senseTeamOccupyingIsland(id) == rc.getTeam()) {
        //                 MapLocation[] islandLocations = rc.senseNearbyIslandLocations(id);
        //                 for (MapLocation m : islandLocations) {
        //                     if (prioritizedIslandLocation == null) {
        //                         prioritizedIslandLocation = m;
        //                     } else if (m.distanceSquaredTo(me) < prioritizedIslandLocation.distanceSquaredTo(me)) {
        //                         prioritizedIslandLocation = m;
        //                     }
        //                 }
        //             }
        //         }
        //         if (prioritizedIslandLocation != null) {
        //             Direction[] bug2array = Motion.bug2(rc, prioritizedIslandLocation, lastDirection, clockwiseRotation, true, true, indicatorString);
        //             lastDirection = bug2array[0];
        //             if (bug2array[1] == Direction.CENTER) {
        //                 clockwiseRotation = !clockwiseRotation;
        //             }
        //             me = rc.getLocation();
        //             if (GlobalArray.DEBUG_INFO >= 3) {
        //                 rc.setIndicatorLine(me, prioritizedIslandLocation, 75, 255, 255);
        //             }
        //             if (GlobalArray.DEBUG_INFO >= 2) {
        //                 rc.setIndicatorDot(me, 75, 255, 255);
        //             }
        //             return;
        //         } else {
        //             // get island location from global array
        //             MapLocation[] islandLocations = GlobalArray.getKnownIslandLocations(rc, rc.getTeam());
        //             for (MapLocation m : islandLocations) {
        //                 if (m == null) {
        //                     continue;
        //                 }
        //                 if (prioritizedIslandLocation == null) {
        //                     prioritizedIslandLocation = m;
        //                 }
        //                 else if (m.distanceSquaredTo(me) < prioritizedIslandLocation.distanceSquaredTo(me)) {
        //                     prioritizedIslandLocation = m;
        //                 }
        //             }
        //             if (prioritizedIslandLocation != null) {
        //                 Direction[] bug2array = Motion.bug2(rc, prioritizedIslandLocation, lastDirection, clockwiseRotation, false, true, indicatorString);
        //                 lastDirection = bug2array[0];
        //                 if (bug2array[1] == Direction.CENTER) {
        //                     clockwiseRotation = !clockwiseRotation;
        //                 }
        //                 me = rc.getLocation();
        //                 if (GlobalArray.DEBUG_INFO >= 3) {
        //                     rc.setIndicatorLine(me, prioritizedIslandLocation, 75, 255, 255);
        //                 }
        //                 if (GlobalArray.DEBUG_INFO >= 2) {
        //                     rc.setIndicatorDot(me, 75, 255, 255);
        //                 }
        //                 return;
        //             }
        //         }
        //     }
        //     if (arrivedAtCenter == false) {
        //         Direction[] bug2array = Motion.bug2(rc, center, lastDirection, clockwiseRotation, false, true, indicatorString);
        //         lastDirection = bug2array[0];
        //         if (bug2array[1] == Direction.CENTER) {
        //             clockwiseRotation = !clockwiseRotation;
        //         }
        //         turnsMovingToCenter++;
        //         me = rc.getLocation();
        //         if (me.distanceSquaredTo(center) <= 5) {
        //             arrivedAtCenter = true;
        //         }
        //         if (turnsMovingToCenter >= 200) {
        //             arrivedAtCenter = true;
        //         }
        //         return;
        //     }

        //     int surroundingLaunchers = 0;
        //     if (friendlyRobotInfo.length > 0) {
        //         // get lowest id and highest id
        //         RobotInfo lowestIdFriendlyRobotInfo = null;
        //         RobotInfo highestIdFriendlyRobotInfo = null;
        //         for (RobotInfo w : friendlyRobotInfo) {
        //             if (w.getType() != RobotType.LAUNCHER) {
        //                 continue;
        //             }
        //             surroundingLaunchers += 1;
        //             if (lowestIdFriendlyRobotInfo == null) {
        //                 lowestIdFriendlyRobotInfo = w;
        //                 highestIdFriendlyRobotInfo = w;
        //                 continue;
        //             }
        //             if (lowestIdFriendlyRobotInfo.ID > w.ID) {
        //                 lowestIdFriendlyRobotInfo = w;
        //             }
        //             if (highestIdFriendlyRobotInfo.ID < w.ID) {
        //                 highestIdFriendlyRobotInfo = w;
        //             }
        //         }
        //         if (surroundingLaunchers >= 2) {
        //             // try to swarm
        //             if (lowestIdFriendlyRobotInfo.ID > rc.getID()) {
        //                 // i'm the leader!

        //                 if (arrivedAtCenter == false) {
        //                     Direction[] bug2array = Motion.bug2(rc, center, lastDirection, clockwiseRotation, false, true, indicatorString);
        //                     lastDirection = bug2array[0];
        //                     if (bug2array[1] == Direction.CENTER) {
        //                         clockwiseRotation = !clockwiseRotation;
        //                     }
        //                     turnsMovingToCenter++;
        //                     me = rc.getLocation();
        //                     if (me.distanceSquaredTo(center) <= 5) {
        //                         arrivedAtCenter = true;
        //                     }
        //                     if (turnsMovingToCenter >= 200) {
        //                         arrivedAtCenter = true;
        //                     }
        //                 }
        //                 else if (GlobalArray.hasLocation(rc.readSharedArray(GlobalArray.OPPONENT_HEADQUARTERS))) {
        //                     indicatorString.append("ATK OPP HQ; ");
        //                     Direction[] bug2array = Motion.bug2(rc, GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.OPPONENT_HEADQUARTERS)), lastDirection, clockwiseRotation, false, false, indicatorString);
        //                     lastDirection = bug2array[0];
        //                     if (bug2array[1] == Direction.CENTER) {
        //                         clockwiseRotation = !clockwiseRotation;
        //                     }
        //                     return;
        //                 }
        //                 else if (bugToStoredOpponentLocation(defenseRange)) {

        //                 }
        //                 else if (opponent != null) {
        //                     // opponent, lets move away
        //                     Direction[] bug2array = Motion.bug2retreat(rc, robotInfo, friendlyRobotInfo, prioritizedHeadquarters, lastDirection, clockwiseRotation, true, true, indicatorString);
        //                     lastDirection = bug2array[0];
        //                     if (bug2array[1] == Direction.CENTER) {
        //                         clockwiseRotation = !clockwiseRotation;
        //                     }
        //                     me = rc.getLocation();
        //                     if (GlobalArray.DEBUG_INFO >= 3) {
        //                         rc.setIndicatorLine(me, opponent.getLocation(), 255, 125, 25);
        //                     }
        //                     else if (GlobalArray.DEBUG_INFO >= 2) {
        //                         rc.setIndicatorDot(me, 255, 125, 25);
        //                     }
        //                     if (me.distanceSquaredTo(opponent.getLocation()) > RobotType.LAUNCHER.visionRadiusSquared) {
        //                         // i'm far enough, lets stop
        //                         opponent = null;
        //                     }
        //                 } else {
        //                     // stand on opponent island
        //                     int[] islands = rc.senseNearbyIslands();
        //                     MapLocation prioritizedIslandLocation = null;
        //                     for (int id : islands) {
        //                         if (rc.senseTeamOccupyingIsland(id) == rc.getTeam().opponent()) {
        //                             MapLocation[] islandLocations = rc.senseNearbyIslandLocations(id);
        //                             for (MapLocation m : islandLocations) {
        //                                 if (prioritizedIslandLocation == null) {
        //                                     prioritizedIslandLocation = m;
        //                                 } else if (m.distanceSquaredTo(me) < prioritizedIslandLocation.distanceSquaredTo(me)) {
        //                                     prioritizedIslandLocation = m;
        //                                 }
        //                             }
        //                         }
        //                     }
        //                     if (prioritizedIslandLocation != null) {
        //                         Direction[] bug2array = Motion.bug2(rc, prioritizedIslandLocation, lastDirection, clockwiseRotation, true, true, indicatorString);
        //                         lastDirection = bug2array[0];
        //                         if (bug2array[1] == Direction.CENTER) {
        //                             clockwiseRotation = !clockwiseRotation;
        //                         }
        //                         me = rc.getLocation();
        //                         if (GlobalArray.DEBUG_INFO >= 3) {
        //                             rc.setIndicatorLine(me, prioritizedIslandLocation, 75, 255, 255);
        //                         }
        //                         if (GlobalArray.DEBUG_INFO >= 2) {
        //                             rc.setIndicatorDot(me, 75, 255, 255);
        //                         }
        //                     } else {
        //                         // get island location from global array
        //                         MapLocation[] islandLocations = GlobalArray.getKnownIslandLocations(rc, rc.getTeam().opponent());
        //                         for (MapLocation m : islandLocations) {
        //                             if (m == null) {
        //                                 continue;
        //                             }
        //                             if (prioritizedIslandLocation == null) {
        //                                 prioritizedIslandLocation = m;
        //                             }
        //                             else if (m.distanceSquaredTo(me) < prioritizedIslandLocation.distanceSquaredTo(me)) {
        //                                 prioritizedIslandLocation = m;
        //                             }
        //                         }
        //                         if (prioritizedIslandLocation != null) {
        //                             Direction[] bug2array = Motion.bug2(rc, prioritizedIslandLocation, lastDirection, clockwiseRotation, false, true, indicatorString);
        //                             lastDirection = bug2array[0];
        //                             if (bug2array[1] == Direction.CENTER) {
        //                                 clockwiseRotation = !clockwiseRotation;
        //                             }
        //                             me = rc.getLocation();
        //                             if (GlobalArray.DEBUG_INFO >= 3) {
        //                                 rc.setIndicatorLine(me, prioritizedIslandLocation, 75, 255, 255);
        //                             }
        //                             if (GlobalArray.DEBUG_INFO >= 2) {
        //                                 rc.setIndicatorDot(me, 75, 255, 255);
        //                             }
        //                         }
        //                         else {
        //                             Direction[] bug2array = Motion.bug2(rc, center, lastDirection, clockwiseRotation, false, true, indicatorString);
        //                             lastDirection = bug2array[0];
        //                             if (bug2array[1] == Direction.CENTER) {
        //                                 clockwiseRotation = !clockwiseRotation;
        //                             }
        //                         }
        //                     }
        //                 }
        //                 lastLauncherLocation = null;
        //             } else {
        //                 // i'm not the leader
        //                 indicatorString.append("FOL SWARM; ");
        //                 if (opponent != null) {
        //                     // opponent detected, to reshape launcher swarm, head away from opponent
        //                     if (GlobalArray.DEBUG_INFO >= 3) {
        //                         rc.setIndicatorLine(me, opponent.getLocation(), 255, 125, 25);
        //                     }
        //                     else if (GlobalArray.DEBUG_INFO >= 2) {
        //                         rc.setIndicatorDot(me, 255, 125, 25);
        //                     }
        //                     Direction[] bug2array = Motion.bug2retreat(rc, robotInfo, friendlyRobotInfo, prioritizedHeadquarters, lastDirection, clockwiseRotation, true, true, indicatorString);
        //                     lastDirection = bug2array[0];
        //                     if (bug2array[1] == Direction.CENTER) {
        //                         clockwiseRotation = !clockwiseRotation;
        //                     }
        //                     me = rc.getLocation();
        //                     if (me.distanceSquaredTo(opponent.getLocation()) > RobotType.LAUNCHER.visionRadiusSquared) {
        //                         // i'm far enough, lets stop
        //                         opponent = null;
        //                     }
        //                     lastLauncherLocation = null;
        //                 } else {
        //                     if (arrivedAtCenter == false) {
        //                         Direction[] bug2array = Motion.bug2(rc, center, lastDirection, clockwiseRotation, false, true, indicatorString);
        //                         lastDirection = bug2array[0];
        //                         if (bug2array[1] == Direction.CENTER) {
        //                             clockwiseRotation = !clockwiseRotation;
        //                         }
        //                         me = rc.getLocation();
        //                         if (me.distanceSquaredTo(center) <= 5) {
        //                             arrivedAtCenter = true;
        //                         }
        //                     }
        //                     else if (GlobalArray.hasLocation(rc.readSharedArray(GlobalArray.OPPONENT_HEADQUARTERS))) {
        //                         indicatorString.append("ATK OPP HQ; ");
        //                         Direction[] bug2array = Motion.bug2(rc, GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.OPPONENT_HEADQUARTERS)), lastDirection, clockwiseRotation, false, false, indicatorString);
        //                         lastDirection = bug2array[0];
        //                         if (bug2array[1] == Direction.CENTER) {
        //                             clockwiseRotation = !clockwiseRotation;
        //                         }
        //                         return;
        //                     }
        //                     else if (me.distanceSquaredTo(lowestIdFriendlyRobotInfo.getLocation()) <= launcherCircleRange * 1.5) {
        //                         if (lastLauncherLocation != null && rc.canMove(lastLauncherLocation.directionTo(lowestIdFriendlyRobotInfo.getLocation()))) {
        //                             // follow leader last move
        //                             rc.move(lastLauncherLocation.directionTo(lowestIdFriendlyRobotInfo.getLocation()));
        //                             lastLauncherLocation = lowestIdFriendlyRobotInfo.getLocation();
        //                         }
        //                         else {
        //                             // circle around leader
        //                             clockwiseRotation = Motion.circleAroundTarget(rc, lowestIdFriendlyRobotInfo.getLocation(), launcherCircleRange, clockwiseRotation, true, true);
        //                             lastLauncherLocation = lowestIdFriendlyRobotInfo.getLocation();
        //                         }
        //                     }
        //                     else {
        //                         // i can see leader but not close enough, try to bug to leader
        //                         Direction[] bug2array = Motion.bug2(rc, lowestIdFriendlyRobotInfo.getLocation(), lastDirection, clockwiseRotation, true, true, indicatorString);
        //                         lastDirection = bug2array[0];
        //                         if (bug2array[1] == Direction.CENTER) {
        //                             clockwiseRotation = !clockwiseRotation;
        //                         }
        //                         lastLauncherLocation = null;
        //                     }
        //                 }
        //                 me = rc.getLocation();
        //                 if (GlobalArray.DEBUG_INFO >= 3) {
        //                     rc.setIndicatorLine(me, lowestIdFriendlyRobotInfo.getLocation(), 255, 255, 75);
        //                 }
        //                 else if (GlobalArray.DEBUG_INFO >= 2) {
        //                     rc.setIndicatorDot(me, 255, 255, 75);
        //                 }
        //             }
        //             return;
        //         }
                
        //     }
        //     if (arrivedAtCenter == false) {
        //         Direction[] bug2array = Motion.bug2(rc, center, lastDirection, clockwiseRotation, false, true, indicatorString);
        //         lastDirection = bug2array[0];
        //         if (bug2array[1] == Direction.CENTER) {
        //             clockwiseRotation = !clockwiseRotation;
        //         }
        //         me = rc.getLocation();
        //         if (me.distanceSquaredTo(center) <= 5) {
        //             arrivedAtCenter = true;
        //         }
        //     }
        //     // defend hq
        //     indicatorString.append("DEF; ");
        //     headquarterCircleRange = 4 + surroundingLaunchers / 3;
        //     lastLauncherLocation = null;
        //     if (me.distanceSquaredTo(prioritizedHeadquarters) <= headquarterCircleRange * 1.5) {
        //         // if (me.x <= edgeRange || me.x >= rc.getMapWidth() - edgeRange || me.y <= edgeRange || me.y >= rc.getMapHeight() - edgeRange) {
        //         //     if (rc.isMovementReady()) {
        //         //         clockwiseRotation = !clockwiseRotation;
        //         //     }
        //         // }
        //         clockwiseRotation = Motion.circleAroundTarget(rc, prioritizedHeadquarters, headquarterCircleRange, clockwiseRotation, true, true);
        //     } else {
        //         Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, false, true, indicatorString);
        //         lastDirection = bug2array[0];
        //         if (bug2array[1] == Direction.CENTER) {
        //             clockwiseRotation = !clockwiseRotation;
        //         }
        //     }
        //     me = rc.getLocation();
        //     if (GlobalArray.DEBUG_INFO >= 3) {
        //         rc.setIndicatorDot(me, 75, 255, 75);
        //     }
        //     else if (GlobalArray.DEBUG_INFO >= 2) {
        //         rc.setIndicatorDot(me, 75, 255, 75);
        //     }
        // }
    }

    private void updatePrioritizedHeadquarters() throws GameActionException {
        prioritizedHeadquarters = headquarters[0];
        for (int i = 0; i < headquarters.length; i++) {
            if (headquarters[i] != null) {
                if (prioritizedHeadquarters.distanceSquaredTo(me) > headquarters[i].distanceSquaredTo(me)) {
                    prioritizedHeadquarters = headquarters[i];
                }
            }
        }
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

    private boolean bugToStoredOpponentLocation(int defenseRange) throws GameActionException {
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
            indicatorString.append("PROT; ");
            if (GlobalArray.DEBUG_INFO >= 3) {
                rc.setIndicatorLine(me, prioritizedOpponentLocation, 75, 255, 75);
            }
            else if (GlobalArray.DEBUG_INFO >= 2) {
                rc.setIndicatorDot(me, 75, 255, 75);
            }
            if (storedLocations.removedOpponents[prioritizedOpponentLocationIndex] == true) {
                Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, true, true, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
            } else {
                Direction[] bug2array = Motion.bug2(rc, prioritizedOpponentLocation, lastDirection, clockwiseRotation, true, true, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
                RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                boolean allDead = true;
                for (RobotInfo r : robotInfo) {
                    if (r.getType() != RobotType.HEADQUARTERS) {
                        allDead = false;
                    }
                }
                if (prioritizedOpponentLocation.distanceSquaredTo(me) <= 5 && allDead) {
                    storedLocations.removeOpponentLocation(prioritizedOpponentLocationIndex);
                }
            }
            return true;
        }
        return false;
    }
}
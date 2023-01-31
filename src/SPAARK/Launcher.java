package SPAARK;

import battlecode.common.*;
import java.util.Random;

public strictfp class Launcher {
    protected RobotController rc;
    protected MapLocation me;
    private MapLocation center;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    private Random rng = new Random(2023);

    private MapLocation[] headquarters;
    private MapLocation prioritizedHeadquarters;
    private MapLocation prioritizedOpponentHeadquarters;

    private StoredLocations storedLocations;

    private RobotInfo opponent;

    protected int amplifierID = -1;

    private boolean clockwiseRotation = true;
    private Direction lastDirection = Direction.CENTER;

    private int lastHealth = 0;

    private StringBuilder indicatorString = new StringBuilder();

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
            Motion.headquarters = headquarters;
            lastHealth = rc.getHealth();
            storedLocations = new StoredLocations(rc, headquarters);
            rng = new Random(rc.getID());
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
                if (FooBar.foobar && rng.nextInt(1000) == 0) FooBar.foo(rc);
                if (FooBar.foobar && rng.nextInt(1000) == 0) FooBar.bar(rc);
                //FooBar.foobar is set to false, code will never run
                me = rc.getLocation();
                round = rc.getRoundNum();
                globalArray.parseGameState(rc.readSharedArray(GlobalArray.GAMESTATE));

                indicatorString = new StringBuilder();

                globalArray.incrementCount(rc);
                storedLocations.detectWells();
                storedLocations.detectIslandLocations();
                storedLocations.detectSymmetry();
                storedLocations.updateMapSymmetry(globalArray.mapSymmetry());
                storedLocations.writeToGlobalArray();

                Motion.symmetry = storedLocations.getMapSymmetry();

                runState();

                MapLocation target = storedLocations.getTarget();
                RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
                RobotInfo robot = Attack.attack(rc, target, robotInfo, true, indicatorString);
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
        MapLocation target = storedLocations.getTarget();
        RobotInfo[] friendlyRobotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
        RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        RobotInfo robot = Attack.attack(rc, target, robotInfo, true, indicatorString);
        if (robot != null && Attack.prioritizedRobot(robot.getType()) >= 3) {
            indicatorString.append("RET; ");
            if (rc.isMovementReady()) {
                Direction[] bug2array = Motion.bug2retreat(rc, robotInfo, friendlyRobotInfo, target, lastDirection, clockwiseRotation, false, true, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
            }
            opponent = robot;
            return;
        }
        robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());

        if (robot == null) {
            robot = Attack.senseOpponent(rc, robotInfo);
        }

        if (round % 3 == 0 && friendlyRobotInfo.length > 0 && rc.isMovementReady()) {
            int surroundingLaunchers = 0;
            MapLocation centerOfMass = me;
            for (RobotInfo w : friendlyRobotInfo) {
                if (w.getType() != RobotType.LAUNCHER) {
                    continue;
                }
                surroundingLaunchers += 1;
                centerOfMass.translate(w.getLocation().x - me.x, w.getLocation().y - me.y);
            }
            Direction[] bug2array = Motion.bug2(rc, centerOfMass, lastDirection, clockwiseRotation, false, false, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
        }
        else {
            Direction[] bug2array = Motion.bug2(rc, target, lastDirection, clockwiseRotation, false, false, indicatorString);
            lastDirection = bug2array[0];
            if (bug2array[1] == Direction.CENTER) {
                clockwiseRotation = !clockwiseRotation;
            }
            me = rc.getLocation();
    
            if (rc.canSenseLocation(target) && robot == null) {
                storedLocations.arrivedAtWell = true;
            }
        }
    
        if (robot != null) {
            opponent = robot;
        }
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
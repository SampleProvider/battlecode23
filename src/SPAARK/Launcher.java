package SPAARK;

import battlecode.common.*;

public strictfp class Launcher {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    private static final Direction[] DIRECTIONS = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
    };

    private MapLocation[] headquarters;
    private MapLocation prioritizedHeadquarters;
    private MapLocation prioritizedOpponentHeadquarters;

    private WellInfo[] seenWells = new WellInfo[4];
    private int seenWellIndex = 0;

    private RobotInfo[] robotInfo;
    private MapLocation opponentLocation;

    private RobotType prioritizedRobotType = RobotType.LAUNCHER;
    private int amplifierSensingRange = 50;
    private int amplifierCircleRange = 10;

    private int headquarterCircleRange = 16;
    private int headquarterCircleStuck = 0;
    
    private int defenseRange = 64;
    private int edgeRange = 4;
    private boolean[] invalidOpponentLocations = new boolean[GlobalArray.OPPONENTS_LENGTH];

    private int centerRange = 2;
    private boolean arrivedAtCenter = false;

    protected int amplifierID = -1;
    protected int launcherID = -1;

    private MapLocation prioritizedRobotInfoLocation;
    private MapLocation pathfindRobotInfoLocation;

    private MapLocation prioritizedAmplifierLocation;

    private boolean clockwiseRotation = true;
    private Direction lastDirection = Direction.CENTER;
    
    private int state = 0;
    // state
    // 0 is wander
    // 1 is travelling to amplifier
    // 2 is travelling with amplifier
    // 3 is defense
    // 4 is pathfinding to opponent
    // 5 is bad

    protected StringBuilder indicatorString = new StringBuilder();

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
            state = 3;
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
                
                robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared,rc.getTeam().opponent());
                prioritizedRobotInfoLocation = Attack.attack(rc, me, robotInfo, prioritizedRobotType, true, indicatorString);
                if (prioritizedRobotInfoLocation != null && state == 3) {
                    opponentLocation = prioritizedRobotInfoLocation;
                }

                if (rc.canWriteSharedArray(0, 0)) {
                    for (int i = 0;i < 4;i++) {
                        if (seenWells[i] != null) {
                            if (GlobalArray.storeWell(rc, seenWells[i])) {
                                indicatorString.append("STO WELL " + seenWells[i].toString() + "; ");
                                seenWells[i] = null;
                            }
                        }
                    }
                    // if (opponentLocation != null) {
                    //     if (GlobalArray.storeOpponentLocation(rc, opponentLocation)) {
                    //         opponentLocation = null;
                    //         indicatorString.append("STO OPP " + opponentLocation.toString() + "; ");
                    //     }
                    // }
                }
                WellInfo[] wellInfo = rc.senseNearbyWells();
                if (wellInfo.length > 0) {
                    for (WellInfo w : wellInfo) {
                        if (seenWellIndex < 4) {
                            boolean newWell = true;
                            for (int i = 0;i < seenWellIndex; i++) {
                                if (seenWells[i] == null) {
                                    continue;
                                }
                                if (seenWells[i].getMapLocation().equals(w.getMapLocation())) {
                                    newWell = false;
                                }
                            }
                            if (newWell) {
                                seenWells[seenWellIndex] = w;
                                seenWellIndex += 1;
                            }
                        }
                    }
                }
                
                if (state == 0) {
                    updatePrioritizedOpponentHeadquarters();
                    if (prioritizedOpponentHeadquarters != null) {
                        boolean hasSpace = false;
                        for (Direction d : DIRECTIONS) {
                            if (rc.canSenseLocation(prioritizedOpponentHeadquarters.add(d))) {
                                if (rc.senseRobotAtLocation(prioritizedOpponentHeadquarters.add(d)) == null && rc.sensePassability(prioritizedOpponentHeadquarters.add(d))) {
                                    hasSpace = true;
                                }
                            }
                        }
                        if (hasSpace) {
                            state = 2;
                            continue;
                        }
                    }
                    if (!detectAmplifier()) {
                        prioritizedHeadquarters = headquarters[0];
                        for (MapLocation hq : headquarters) {
                            if (hq != null) {
                                if (prioritizedHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                                    prioritizedHeadquarters = hq;
                                }
                            }
                        }
                        // Motion.spreadRandomly(rc, me, prioritizedHeadquarters, true);
                        // indicatorString.append("SWARM-CAR; ";
                        // if (rng.nextBoolean()) {
                        //     Motion.swarm(rc, me, RobotType.CARRIER);
                        // }
                        // else {
                        //     Motion.spreadCenter(rc, me);
                        // }
                        if (prioritizedRobotInfoLocation != null) {
                            indicatorString.append("PATH->PINFO-" + prioritizedRobotInfoLocation.toString() + "; ");
                            Direction[] bug2array = Motion.bug2(rc, prioritizedRobotInfoLocation, lastDirection, clockwiseRotation, indicatorString);
                            lastDirection = bug2array[0];
                            if (bug2array[1] == Direction.CENTER) {
                                clockwiseRotation = !clockwiseRotation;
                            }
                            rc.setIndicatorLine(me, prioritizedRobotInfoLocation, 255, 25, 125);
                        }
                        else if (arrivedAtCenter) {
                            Motion.swarm(rc, me, RobotType.LAUNCHER);
                            indicatorString.append("SWARM-LAU; ");
                        }
                        else if (me.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2)) >= centerRange) {
                            indicatorString.append("PATH->CEN; ");
                            Direction[] bug2array = Motion.bug2(rc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2), lastDirection, clockwiseRotation, indicatorString);
                            lastDirection = bug2array[0];
                            if (bug2array[1] == Direction.CENTER) {
                                clockwiseRotation = !clockwiseRotation;
                            }
                        }
                        else {
                            arrivedAtCenter = true;
                            // clockwiseRotation = Motion.circleAroundTarget(rc, me, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2), centerRange, clockwiseRotation);
                        }
                        // Motion.spreadCenter(rc, me);
                    }
                }
                if (state == 1) {
                    int amplifierArray = rc.readSharedArray(amplifierID);
                    rc.setIndicatorString(amplifierID + " " + amplifierArray);
                    if (amplifierArray >> 14 == 0) {
                        state = 0;
                        continue;
                    }
                    updatePrioritizedOpponentHeadquarters();
                    if (prioritizedOpponentHeadquarters != null) {
                        boolean hasSpace = false;
                        for (Direction d : DIRECTIONS) {
                            if (rc.canSenseLocation(prioritizedOpponentHeadquarters.add(d))) {
                                if (rc.senseRobotAtLocation(prioritizedOpponentHeadquarters.add(d)) == null && rc.sensePassability(prioritizedOpponentHeadquarters.add(d))) {
                                    hasSpace = true;
                                }
                            }
                        }
                        if (hasSpace) {
                            state = 2;
                            continue;
                        }
                    }
                    prioritizedAmplifierLocation = GlobalArray.parseLocation(amplifierArray);
                    rc.setIndicatorLine(me, prioritizedAmplifierLocation, 255, 175, 75);
                    if (me.distanceSquaredTo(prioritizedAmplifierLocation) <= amplifierCircleRange) {
                        indicatorString.append("CIRC-AMP " + prioritizedAmplifierLocation.toString() + "; ");
                        clockwiseRotation = Motion.circleAroundTarget(rc, me, prioritizedAmplifierLocation, amplifierCircleRange, clockwiseRotation);
                    }
                    else {
                        indicatorString.append("PATH->AMP " + prioritizedAmplifierLocation.toString() + "; ");
                        Direction[] bug2array = Motion.bug2(rc, prioritizedAmplifierLocation, lastDirection, clockwiseRotation, indicatorString);
                        lastDirection = bug2array[0];
                        if (bug2array[1] == Direction.CENTER) {
                            clockwiseRotation = !clockwiseRotation;
                        }
                    }
                }
                if (state == 2) {
                    indicatorString.append("BLK HQ; ");
                    if (me.distanceSquaredTo(prioritizedOpponentHeadquarters) <= 2) {
                        Motion.circleAroundTarget(rc, me, prioritizedOpponentHeadquarters);
                    }
                    else {
                        boolean hasSpace = false;
                        for (Direction d : DIRECTIONS) {
                            if (rc.canSenseLocation(prioritizedOpponentHeadquarters.add(d))) {
                                if (rc.senseRobotAtLocation(prioritizedOpponentHeadquarters.add(d)) == null && rc.sensePassability(prioritizedOpponentHeadquarters.add(d))) {
                                    hasSpace = true;
                                }
                            }
                        }
                        if (hasSpace) {
                            Direction[] bug2array = Motion.bug2(rc, prioritizedOpponentHeadquarters, lastDirection, clockwiseRotation, indicatorString);
                            lastDirection = bug2array[0];
                            if (bug2array[1] == Direction.CENTER) {
                                clockwiseRotation = !clockwiseRotation;
                            }
                        }
                        else {
                            state = 0;
                        }
                    }
                }
                if (state == 3) {
                    if (!detectAmplifier()) {
                        indicatorString.append("DEF; ");
                        prioritizedHeadquarters = headquarters[0];
                        for (MapLocation hq : headquarters) {
                            if (hq != null) {
                                if (prioritizedHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                                    prioritizedHeadquarters = hq;
                                }
                            }
                        }
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
                            rc.setIndicatorLine(me, prioritizedOpponentLocation, 75, 255, 75);
                            if (invalidOpponentLocations[prioritizedOpponentLocationIndex] == true) {
                                Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, indicatorString);
                                lastDirection = bug2array[0];
                                if (bug2array[1] == Direction.CENTER) {
                                    clockwiseRotation = !clockwiseRotation;
                                }
                                if (rc.canWriteSharedArray(0, 0)) {
                                    rc.writeSharedArray(22 + prioritizedOpponentLocationIndex, 0);
                                    invalidOpponentLocations[prioritizedOpponentLocationIndex] = false;
                                }
                            }
                            else {
                                Direction[] bug2array = Motion.bug2(rc, prioritizedOpponentLocation, lastDirection, clockwiseRotation, indicatorString);
                                lastDirection = bug2array[0];
                                if (bug2array[1] == Direction.CENTER) {
                                    clockwiseRotation = !clockwiseRotation;
                                }
                                RobotInfo[] robotInfo = rc.senseNearbyRobots(RobotType.LAUNCHER.visionRadiusSquared,rc.getTeam().opponent());
                                boolean allDead = true;
                                for (RobotInfo r : robotInfo) {
                                    if (r.getType() != RobotType.HEADQUARTERS) {
                                        allDead = false;
                                    }
                                }
                                if (prioritizedOpponentLocation.distanceSquaredTo(me) <= 2 && allDead) {
                                    invalidOpponentLocations[prioritizedOpponentLocationIndex] = true;
                                }
                            }
                        }
                        else {
                            int surroundingLaunchers = 0;
                            RobotInfo[] friendlyRobotInfo = rc.senseNearbyRobots(RobotType.LAUNCHER.visionRadiusSquared, rc.getTeam());
                            for (RobotInfo w : friendlyRobotInfo) {
                                if (w.getType() == RobotType.LAUNCHER) {
                                    surroundingLaunchers += 1;
                                }
                            }
                            headquarterCircleRange = 16 + surroundingLaunchers / 3;
                            if (opponentLocation != null) {
                                rc.setIndicatorLine(me, opponentLocation, 255, 125, 25);
                                Direction[] bug2array = Motion.bug2(rc, opponentLocation, lastDirection, clockwiseRotation, indicatorString);
                                lastDirection = bug2array[0];
                                if (bug2array[1] == Direction.CENTER) {
                                    clockwiseRotation = !clockwiseRotation;
                                }
                                me = rc.getLocation();
                                if (me.distanceSquaredTo(opponentLocation) <= 2) {
                                    opponentLocation = null;
                                }
                            }
                            else if (me.distanceSquaredTo(prioritizedHeadquarters) <= headquarterCircleRange * 1.25) {
                                if (me.x <= edgeRange || me.x >= rc.getMapWidth() - edgeRange || me.y <= edgeRange || me.y >= rc.getMapHeight() - edgeRange) {
                                    if (rc.isMovementReady()) {
                                        clockwiseRotation = !clockwiseRotation;
                                    }
                                }
                                clockwiseRotation = Motion.circleAroundTarget(rc, me, prioritizedHeadquarters, headquarterCircleRange, clockwiseRotation);
                                if (me.equals(rc.getLocation())) {
                                    headquarterCircleStuck += 1;
                                    if (headquarterCircleStuck == 10) {
                                        state = 0;
                                    }
                                }
                                else {
                                    headquarterCircleStuck = 0;
                                }
                            }
                            else {
                                Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, indicatorString);
                                lastDirection = bug2array[0];
                                if (bug2array[1] == Direction.CENTER) {
                                    clockwiseRotation = !clockwiseRotation;
                                }
                            }
                        }
                        me = rc.getLocation();
                        rc.setIndicatorDot(me, 75, 255, 75);
                    }
                }
                if (state == 5) {
                    if (!detectAmplifier()) {
                        Motion.spreadEdges(rc, me);
                    }
                }
                me = rc.getLocation();
                robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared,rc.getTeam().opponent());
                prioritizedRobotInfoLocation = Attack.attack(rc, me, robotInfo, prioritizedRobotType, true, indicatorString);
                if (prioritizedRobotInfoLocation != null && state == 3) {
                    opponentLocation = prioritizedRobotInfoLocation;
                }
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

    private boolean detectAmplifier() throws GameActionException {
        prioritizedAmplifierLocation = null;
        for (int a = 0; a < GlobalArray.AMPLIFIERS_LENGTH; a++) {
            int amplifierArray = rc.readSharedArray(14 + a);
            if (amplifierArray >> 14 != 0) {
                MapLocation amplifierLocation = GlobalArray.parseLocation(amplifierArray);
                if (amplifierLocation.distanceSquaredTo(me) < amplifierSensingRange) {
                    if (prioritizedAmplifierLocation == null) {
                        prioritizedAmplifierLocation = amplifierLocation;
                        amplifierID = GlobalArray.AMPLIFIERS + a;
                    }
                    else if (amplifierLocation.distanceSquaredTo(me) < prioritizedAmplifierLocation.distanceSquaredTo(me)) {
                        prioritizedAmplifierLocation = amplifierLocation;
                        amplifierID = GlobalArray.AMPLIFIERS + a;
                    }
                }
            }
        }
        if (prioritizedAmplifierLocation != null) {
            state = 1;
            return true;
        }
        return false;
    }
    
    private void updatePrioritizedOpponentHeadquarters() throws GameActionException {
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
package launcherexplore;

import battlecode.common.*;

import java.util.Random;

public class Motion {
    private static final Random rng = new Random(2023);
    protected static final Direction[] DIRECTIONS = {
            Direction.SOUTHWEST,
            Direction.SOUTH,
            Direction.SOUTHEAST,
            Direction.WEST,
            Direction.EAST,
            Direction.NORTHWEST,
            Direction.NORTH,
            Direction.NORTHEAST,
    };
    private static final String[] DIRABBREV = {
            "C",
            "W",
            "NW",
            "N",
            "NE",
            "E",
            "SE",
            "S",
            "SW",
    };

    private static RobotInfo[] robotInfo;

    protected static void moveRandomly(RobotController rc) throws GameActionException {
        while (rc.isMovementReady()) {
            Direction direction = DIRECTIONS[rng.nextInt(DIRECTIONS.length)];
            if (rc.canMove(direction)) {
                rc.move(direction);
            }
            boolean stuck = true;
            for (Direction d : Direction.allDirections()) {
                if (rc.canMove(d)) {
                    stuck = false;
                }
            }
            if (stuck) {
                break;
            }
        }
    }

    protected static void spreadRandomly(RobotController rc, MapLocation me) throws GameActionException {
        RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
        if (robotInfo.length > 0) {
            RobotInfo prioritizedRobotInfo = null;
            for (RobotInfo w : robotInfo) {
                if (w.getType() != rc.getType()) {
                    continue;
                }
                if (me.distanceSquaredTo(w.getLocation()) > 9) {
                    continue;
                }
                if (prioritizedRobotInfo == null) {
                    prioritizedRobotInfo = w;
                    continue;
                }
                if (me.distanceSquaredTo(prioritizedRobotInfo.getLocation()) > me.distanceSquaredTo(w.getLocation())) {
                    prioritizedRobotInfo = w;
                }
            }
            Direction direction = null;
            if (prioritizedRobotInfo != null) {
                direction = me.directionTo(prioritizedRobotInfo.getLocation()).opposite();
                while (rc.isMovementReady()) {
                    if (rc.canMove(direction)) {
                        rc.move(direction);
                        continue;
                    }
                    if (rc.canMove(direction.rotateLeft())) {
                        rc.move(direction.rotateLeft());
                        continue;
                    }
                    if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                        rc.move(direction.rotateLeft().rotateLeft());
                        continue;
                    }
                    if (rc.canMove(direction.rotateRight())) {
                        rc.move(direction.rotateRight());
                        continue;
                    }
                    if (rc.canMove(direction.rotateRight().rotateRight())) {
                        rc.move(direction.rotateRight().rotateRight());
                        continue;
                    }
                    break;
                }
            } else {
                moveRandomly(rc);
            }
        } else {
            moveRandomly(rc);
        }
    }

    protected static void spreadRandomly(RobotController rc, MapLocation me, MapLocation target) throws GameActionException {
        Direction direction = me.directionTo(target).opposite();
        while (rc.isMovementReady()) {
            boolean moved = false;
            int random = rng.nextInt(6);
            if (random == 0) {
                if (rc.canMove(direction.rotateLeft())) {
                    rc.move(direction.rotateLeft());
                    moved = true;
                }
            } else if (random == 1) {
                if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                    rc.move(direction.rotateLeft().rotateLeft());
                    moved = true;
                }
            } else if (random == 2) {
                if (rc.canMove(direction.rotateRight())) {
                    rc.move(direction.rotateRight());
                    moved = true;
                }
            } else if (random == 3) {
                if (rc.canMove(direction.rotateRight().rotateRight())) {
                    rc.move(direction.rotateRight().rotateRight());
                    moved = true;
                }
            } else {
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    moved = true;
                }
            }
            if (moved == false) {
                break;
            }
        }
    }

    protected static void circleAroundTarget(RobotController rc, MapLocation me, MapLocation target) throws GameActionException {
        Direction direction = me.directionTo(target).rotateLeft();
        if (direction.ordinal() % 2 == 1) {
            direction = direction.rotateLeft();
        }
        if (rc.canMove(direction)) {
            rc.move(direction);
        }
    }

    protected static boolean circleAroundTarget(RobotController rc, MapLocation target, int distance, boolean clockwiseRotation, boolean avoidClouds, boolean avoidWells) throws GameActionException {
        boolean stuck = false;
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction direction = me.directionTo(target);
            robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            if (me.distanceSquaredTo(target) > (int) distance * 1.25) {
                if (clockwiseRotation) {
                    for (int i = 0; i < 2; i++) {
                        direction = direction.rotateLeft();
                        if (canMove(rc, direction, avoidClouds, avoidWells)) {
                            rc.move(direction);
                            stuck = false;
                            break;
                        }
                    }
                } else {
                    for (int i = 0; i < 2; i++) {
                        direction = direction.rotateRight();
                        if (canMove(rc, direction, avoidClouds, avoidWells)) {
                            rc.move(direction);
                            stuck = false;
                            break;
                        }
                    }
                }
            } else if (me.distanceSquaredTo(target) < (int) distance * 0.75) {
                direction = direction.opposite();
                if (canMove(rc, direction, avoidClouds, avoidWells)) {
                    rc.move(direction);
                    stuck = false;
                    continue;
                }
                direction = direction.rotateLeft();
                if (canMove(rc, direction, avoidClouds, avoidWells)) {
                    rc.move(direction);
                    stuck = false;
                    continue;
                }
                direction = direction.rotateRight().rotateRight();
                if (canMove(rc, direction, avoidClouds, avoidWells)) {
                    rc.move(direction);
                    stuck = false;
                    continue;
                }
            } else {
                if (clockwiseRotation) {
                    direction = direction.rotateLeft();
                    for (int i = 0; i < 2; i++) {
                        direction = direction.rotateLeft();
                        if (canMove(rc, direction, avoidClouds, avoidWells)) {
                            rc.move(direction);
                            stuck = false;
                            break;
                        }
                    }
                } else {
                    direction = direction.rotateRight();
                    for (int i = 0; i < 2; i++) {
                        direction = direction.rotateRight();
                        if (canMove(rc, direction, avoidClouds, avoidWells)) {
                            rc.move(direction);
                            stuck = false;
                            break;
                        }
                    }
                }
            }
            if (me.equals(rc.getLocation())) {
                direction = me.directionTo(target);
                if (me.distanceSquaredTo(target) > (int) distance * 1.25) {
                    if (clockwiseRotation) {
                        for (int i = 0; i < 2; i++) {
                            direction = direction.rotateLeft();
                            if (canMove(rc, direction, false, false)) {
                                rc.move(direction);
                                stuck = false;
                                break;
                            }
                        }
                    } else {
                        for (int i = 0; i < 2; i++) {
                            direction = direction.rotateRight();
                            if (canMove(rc, direction, false, false)) {
                                rc.move(direction);
                                stuck = false;
                                break;
                            }
                        }
                    }
                } else if (me.distanceSquaredTo(target) < (int) distance * 0.75) {
                    direction = direction.opposite();
                    if (canMove(rc, direction, false, false)) {
                        rc.move(direction);
                        stuck = false;
                        continue;
                    }
                    direction = direction.rotateLeft();
                    if (canMove(rc, direction, false, false)) {
                        rc.move(direction);
                        stuck = false;
                        continue;
                    }
                    direction = direction.rotateRight().rotateRight();
                    if (canMove(rc, direction, false, false)) {
                        rc.move(direction);
                        stuck = false;
                        continue;
                    }
                } else {
                    if (clockwiseRotation) {
                        direction = direction.rotateLeft();
                        for (int i = 0; i < 2; i++) {
                            direction = direction.rotateLeft();
                            if (canMove(rc, direction, false, false)) {
                                rc.move(direction);
                                stuck = false;
                                break;
                            }
                        }
                    } else {
                        direction = direction.rotateRight();
                        for (int i = 0; i < 2; i++) {
                            direction = direction.rotateRight();
                            if (canMove(rc, direction, false, false)) {
                                rc.move(direction);
                                stuck = false;
                                break;
                            }
                        }
                    }
                }
                clockwiseRotation = !clockwiseRotation;
                if (me.equals(rc.getLocation())) {
                    if (stuck == true) {
                        break;
                    }
                    stuck = true;
                }
            }
        }
        return clockwiseRotation;
    }
    
    protected static Direction[] bug2(RobotController rc, MapLocation dest, Direction lastDirection, boolean clockwiseRotation, boolean avoidClouds, boolean avoidWells, StringBuilder indicatorString) throws GameActionException {
        boolean oldClockwiseRotation = clockwiseRotation;
        int stuck = 0;
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (me.equals(dest)) {
                return new Direction[] { Direction.CENTER, null };
            }
            Direction direction = me.directionTo(dest);
            boolean moved = false;
            robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            if (canMove(rc, direction, avoidClouds, avoidWells) && lastDirection != direction.opposite()) {
                rc.move(direction);
                boolean touchingTheWallBefore = false;
                for (Direction d : DIRECTIONS) {
                    MapLocation translatedMapLocation = me.add(d);
                    if (rc.onTheMap(translatedMapLocation)) {
                        if (!rc.canMove(d)) {
                            touchingTheWallBefore = true;
                        }
                    }
                }
                lastDirection = direction;
                if (touchingTheWallBefore) {
                    clockwiseRotation = !clockwiseRotation;
                }
                continue;
            }
            
            for (int i = 0; i < 7; i++) {
                if (clockwiseRotation) {
                    direction = direction.rotateLeft();
                }
                else {
                    direction = direction.rotateRight();
                }
                if (canMove(rc, direction, avoidClouds, avoidWells) && lastDirection != direction.opposite()) {
                    rc.move(direction);
                    lastDirection = direction;
                    moved = true;
                    break;
                }
            }
            
            if (moved == false) {
                direction = me.directionTo(dest);
                for (int i = 0; i < 7; i++) {
                    if (clockwiseRotation) {
                        direction = direction.rotateLeft();
                    }
                    else {
                        direction = direction.rotateRight();
                    }
                    if (canMove(rc, direction, false, false) && lastDirection != direction.opposite()) {
                        rc.move(direction);
                        lastDirection = direction;
                        moved = true;
                        break;
                    } else if (!rc.onTheMap(me.add(direction))) {
                        lastDirection = Direction.CENTER;
                        clockwiseRotation = !clockwiseRotation;
                        moved = true;
                        stuck++;
                        break;
                    }
                }
                if (moved == false) {
                    lastDirection = Direction.CENTER;
                    break;
                }
            }
            
            if (stuck == 2) {
                break;
            }
        }
        indicatorString.append("BUG-LD=" + DIRABBREV[lastDirection.getDirectionOrderNum()] + "; BUG-CW=" + clockwiseRotation + "; ");
        if (oldClockwiseRotation != clockwiseRotation) {
            return new Direction[] { lastDirection, Direction.CENTER };
        }
        return new Direction[] { lastDirection, null };
    }

    protected static Direction[] bug2retreat(RobotController rc, RobotInfo[] opponentRobotInfo, RobotInfo[] friendlyRobotInfo, MapLocation prioritizedHeadquarters, Direction lastDirection, boolean clockwiseRotation, boolean avoidClouds, boolean avoidWells, StringBuilder indicatorString) throws GameActionException {
        boolean oldClockwiseRotation = clockwiseRotation;
        int stuck = 0;
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction direction = null;
            boolean inCloud = false;
            int bestRobotAmount = 0;
            robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            for (Direction d : DIRECTIONS) {
                if (!canMove(rc, d, false, false)) {
                    continue;
                }
                if (direction == null) {
                    direction = d;
                    inCloud = rc.senseCloud(me.add(d));
                    int robotAmount = 0;
                    for (RobotInfo robot : opponentRobotInfo) {
                        if (Attack.prioritizedRobot(robot.getType()) <= 2) {
                            continue;
                        }
                        if (rc.canSenseLocation(robot.getLocation().add(d.opposite()))) {
                            robotAmount -= 1;
                        }
                        if (rc.canActLocation(robot.getLocation().add(d.opposite()))) {
                            robotAmount -= 1;
                        }
                    }
                    if (friendlyRobotInfo.length <= 15) {
                        for (RobotInfo robot : friendlyRobotInfo) {
                            if (rc.canSenseLocation(robot.getLocation().add(d.opposite()))) {
                                robotAmount += 1;
                            }
                            if (me.distanceSquaredTo(robot.getLocation().add(d.opposite())) < me.distanceSquaredTo(robot.getLocation())) {
                                robotAmount += 1;
                            }
                        }
                    }
                    if (me.distanceSquaredTo(prioritizedHeadquarters.add(d.opposite())) < me.distanceSquaredTo(prioritizedHeadquarters)) {
                        robotAmount += 1;
                    }
                    bestRobotAmount = robotAmount;
                }
                else {
                    if (inCloud) {
                        if (rc.senseCloud(me.add(d))) {
                            int robotAmount = 0;
                            for (RobotInfo robot : opponentRobotInfo) {
                                if (Attack.prioritizedRobot(robot.getType()) <= 2) {
                                    continue;
                                }
                                if (rc.canSenseLocation(robot.getLocation().add(d.opposite()))) {
                                    robotAmount -= 1;
                                }
                                if (rc.canActLocation(robot.getLocation().add(d.opposite()))) {
                                    robotAmount -= 1;
                                }
                            }
                            if (friendlyRobotInfo.length <= 15) {
                                for (RobotInfo robot : friendlyRobotInfo) {
                                    if (rc.canSenseLocation(robot.getLocation().add(d.opposite()))) {
                                        robotAmount += 1;
                                    }
                                    if (me.distanceSquaredTo(robot.getLocation().add(d.opposite())) < me.distanceSquaredTo(robot.getLocation())) {
                                        robotAmount += 1;
                                    }
                                }
                            }
                            if (me.distanceSquaredTo(prioritizedHeadquarters.add(d.opposite())) < me.distanceSquaredTo(prioritizedHeadquarters)) {
                                robotAmount += 1;
                            }
                            if (robotAmount > bestRobotAmount) {
                                direction = d;
                                inCloud = true;
                                bestRobotAmount = robotAmount;
                            }
                        }
                    }
                    else {
                        int robotAmount = 0;
                        for (RobotInfo robot : opponentRobotInfo) {
                            if (Attack.prioritizedRobot(robot.getType()) <= 2) {
                                continue;
                            }
                            if (rc.canSenseLocation(robot.getLocation().add(d.opposite()))) {
                                robotAmount -= 1;
                            }
                            if (rc.canActLocation(robot.getLocation().add(d.opposite()))) {
                                robotAmount -= 1;
                            }
                        }
                        if (friendlyRobotInfo.length <= 15) {
                            for (RobotInfo robot : friendlyRobotInfo) {
                                if (rc.canSenseLocation(robot.getLocation().add(d.opposite()))) {
                                    robotAmount += 1;
                                }
                                if (me.distanceSquaredTo(robot.getLocation().add(d.opposite())) < me.distanceSquaredTo(robot.getLocation())) {
                                    robotAmount += 1;
                                }
                            }
                        }
                        if (me.distanceSquaredTo(prioritizedHeadquarters.add(d.opposite())) < me.distanceSquaredTo(prioritizedHeadquarters)) {
                            robotAmount += 1;
                        }
                        if (robotAmount > bestRobotAmount || rc.senseCloud(me.add(d))) {
                            direction = d;
                            inCloud = rc.senseCloud(me.add(d));
                            bestRobotAmount = robotAmount;
                        }
                    }
                }
            }
            if (direction == null) {
                moveRandomly(rc);
                return new Direction[] { Direction.CENTER, null };
            }
            boolean moved = false;
            if (canMove(rc, direction, avoidClouds, avoidWells)) {
                rc.move(direction);
                boolean touchingTheWallBefore = false;
                for (Direction d : DIRECTIONS) {
                    MapLocation translatedMapLocation = me.add(d);
                    if (rc.onTheMap(translatedMapLocation)) {
                        if (!rc.canMove(d)) {
                            touchingTheWallBefore = true;
                        }
                    }
                }
                lastDirection = direction;
                if (touchingTheWallBefore) {
                    clockwiseRotation = !clockwiseRotation;
                }
                continue;
            }
            for (int i = 0; i < 7; i++) {
                if (clockwiseRotation) {
                    direction = direction.rotateLeft();
                }
                else {
                    direction = direction.rotateRight();
                }
                if (canMove(rc, direction, avoidClouds, avoidWells)) {
                    rc.move(direction);
                    lastDirection = direction;
                    moved = true;
                    break;
                }
            }
            
            if (moved == false) {
                if (clockwiseRotation) {
                    direction = direction.rotateLeft();
                }
                else {
                    direction = direction.rotateRight();
                }
                for (int i = 0; i < 7; i++) {
                    if (clockwiseRotation) {
                        direction = direction.rotateLeft();
                    }
                    else {
                        direction = direction.rotateRight();
                    }
                    if (canMove(rc, direction, false, false)) {
                        rc.move(direction);
                        lastDirection = direction;
                        moved = true;
                        break;
                    } else if (!rc.onTheMap(me.add(direction))) {
                        lastDirection = Direction.CENTER;
                        clockwiseRotation = !clockwiseRotation;
                        moved = true;
                        stuck++;
                        break;
                    }
                }
                if (moved == false) {
                    lastDirection = Direction.CENTER;
                    break;
                }
            }
            
            if (stuck == 2) {
                break;
            }
        }
        indicatorString.append("BUG-LD=" + DIRABBREV[lastDirection.getDirectionOrderNum()] + "; BUG-CW=" + clockwiseRotation + "; ");
        if (oldClockwiseRotation != clockwiseRotation) {
            return new Direction[] { lastDirection, Direction.CENTER };
        }
        return new Direction[] { lastDirection, null };
    }

    private static boolean canMove(RobotController rc, Direction direction, boolean avoidClouds, boolean avoidWells) throws GameActionException {
        MapLocation m = rc.getLocation().add(direction);
        for (RobotInfo r : robotInfo) {
            if (r.getType() == RobotType.HEADQUARTERS) {
                rc.setIndicatorLine(m, r.getLocation(), 255, 0, 0);
                if (r.getLocation().distanceSquaredTo(m) <= RobotType.HEADQUARTERS.actionRadiusSquared) {
                    if (r.getLocation().distanceSquaredTo(rc.getLocation()) >= r.getLocation().distanceSquaredTo(m)) {
                        return false;
                    }
                }
            }
        }
        if (rc.onTheMap(m)) {
            Direction currentDirection = rc.senseMapInfo(m).getCurrentDirection();
            if (rc.canMove(direction) && currentDirection != direction.opposite() && currentDirection != direction.opposite().rotateLeft() && currentDirection != direction.opposite().rotateRight() && !(rc.senseCloud(m) && avoidClouds)) {
                if (!avoidWells) {
                    return true;
                }
                for (Direction d : DIRECTIONS) {
                    MapLocation translatedMapLocation = m.add(d);
                    if (rc.onTheMap(translatedMapLocation) && rc.canSenseLocation(translatedMapLocation)) {
                        if (rc.senseWell(translatedMapLocation) != null) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }
}

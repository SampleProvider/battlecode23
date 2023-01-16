package SPAARK_attack_center;

import battlecode.common.*;

import java.util.Random;

public class Motion {
    private static final Random rng = new Random(2023);
    private static final Direction[] directions = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
    };

    protected static void moveRandomly(RobotController rc) throws GameActionException {
        while (rc.isMovementReady()) {
            Direction direction = directions[rng.nextInt(directions.length)];
            if (rc.canMove(direction)) {
                rc.move(direction);
            }
        }
    }
    protected static void spreadRandomly(RobotController rc, MapLocation me, MapLocation target) throws GameActionException {
        if (rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam()).length <= 10) {
            moveRandomly(rc);
            return;
        }
        Direction direction = me.directionTo(target).opposite();
        while (rc.isMovementReady()) {
            boolean moved = false;
            int random = rng.nextInt(6);
            if (random == 0) {
                if (rc.canMove(direction.rotateLeft())) {
                    rc.move(direction.rotateLeft());
                    moved = true;
                }
            }
            else if (random == 1) {
                if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                    rc.move(direction.rotateLeft().rotateLeft());
                    moved = true;
                }
            }
            else if (random == 2) {
                if (rc.canMove(direction.rotateRight())) {
                    rc.move(direction.rotateRight());
                    moved = true;
                }
            }
            else if (random == 3) {
                if (rc.canMove(direction.rotateRight().rotateRight())) {
                    rc.move(direction.rotateRight().rotateRight());
                    moved = true;
                }
            }
            else {
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    moved = true;
                }
            }
            if (moved == false) {
                Direction d = directions[rng.nextInt(directions.length)];
                if (rc.canMove(d)) {
                    rc.move(d);
                }
            }
        }
    }
    protected static void spreadRandomly(RobotController rc, MapLocation me, MapLocation target, boolean avoidCorners) throws GameActionException {
        if (me.distanceSquaredTo(new MapLocation(0, 0)) <= 25 || me.distanceSquaredTo(new MapLocation(rc.getMapWidth(), 0)) <= 25 || me.distanceSquaredTo(new MapLocation(0, rc.getMapHeight())) <= 25 || me.distanceSquaredTo(new MapLocation(rc.getMapWidth(), rc.getMapHeight())) <= 25) {
            Direction direction = me.directionTo(new MapLocation(rc.getMapWidth() / 2,rc.getMapHeight() / 2));
            while (rc.isMovementReady()) {
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    continue;
                }
                if (rc.canMove(direction.rotateLeft())) {
                    rc.move(direction.rotateLeft());
                    continue;
                }
                if (rc.canMove(direction.rotateRight())) {
                    rc.move(direction.rotateRight());
                    continue;
                }
            }
            return;
        }
        if (rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam()).length <= 10) {
            moveRandomly(rc);
            return;
        }
        Direction direction = me.directionTo(target).opposite();
        while (rc.isMovementReady()) {
            boolean moved = false;
            int random = rng.nextInt(6);
            if (random == 0) {
                if (rc.canMove(direction.rotateLeft())) {
                    rc.move(direction.rotateLeft());
                    moved = true;
                }
            }
            else if (random == 1) {
                if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                    rc.move(direction.rotateLeft().rotateLeft());
                    moved = true;
                }
            }
            else if (random == 2) {
                if (rc.canMove(direction.rotateRight())) {
                    rc.move(direction.rotateRight());
                    moved = true;
                }
            }
            else if (random == 3) {
                if (rc.canMove(direction.rotateRight().rotateRight())) {
                    rc.move(direction.rotateRight().rotateRight());
                    moved = true;
                }
            }
            else {
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    moved = true;
                }
            }
            if (moved == false) {
                Direction d = directions[rng.nextInt(directions.length)];
                if (rc.canMove(d)) {
                    rc.move(d);
                }
            }
        }
    }
    
    protected static void spreadCenter(RobotController rc, MapLocation me) throws GameActionException {
        Direction direction = me.directionTo(new MapLocation(rc.getMapWidth() / 2,rc.getMapHeight() / 2));
        while (rc.isMovementReady()) {
            boolean moved = false;
            int random = rng.nextInt(6);
            if (random == 0) {
                if (rc.canMove(direction.rotateLeft())) {
                    rc.move(direction.rotateLeft());
                    moved = true;
                }
            }
            else if (random == 3) {
                if (rc.canMove(direction.rotateRight())) {
                    rc.move(direction.rotateRight());
                    moved = true;
                }
            }
            else if (random == 1 || random == 2) {
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    moved = true;
                }
            }
            while (moved == false) {
                Direction d = directions[rng.nextInt(directions.length)];
                if (rc.canMove(d)) {
                    rc.move(d);
                    moved = true;
                }
            }
        }
    }
    protected static void spreadEdges(RobotController rc, MapLocation me) throws GameActionException {
        Direction direction = me.directionTo(new MapLocation(rc.getMapWidth() / 2,rc.getMapHeight() / 2)).opposite();
        while (rc.isMovementReady()) {
            boolean moved = false;
            int random = rng.nextInt(6);
            if (random == 0) {
                if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                    rc.move(direction.rotateLeft().rotateLeft());
                    moved = true;
                }
            }
            else if (random == 3) {
                if (rc.canMove(direction.rotateRight().rotateRight())) {
                    rc.move(direction.rotateRight().rotateRight());
                    moved = true;
                }
            }
            else if (random == 1 || random == 2) {
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    moved = true;
                }
            }
            while (moved == false) {
                Direction d = directions[rng.nextInt(directions.length)];
                if (rc.canMove(d)) {
                    rc.move(d);
                    moved = true;
                }
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
    protected static boolean circleAroundTarget(RobotController rc, MapLocation me, MapLocation target, int distance, boolean clockwiseRotation) throws GameActionException {
        Direction direction = me.directionTo(target);
        if (me.distanceSquaredTo(target) > (int) distance * 1.25) {
            if (clockwiseRotation) {
                direction = direction.rotateLeft();
            }
            else {
                direction = direction.rotateRight();
            }
        }
        else if (me.distanceSquaredTo(target) < (int) distance * 0.75) {
            direction = direction.opposite();
        }
        else {
            if (clockwiseRotation) {
                direction = direction.rotateLeft().rotateLeft();
            }
            else {
                direction = direction.rotateRight().rotateRight();
            }
        }
        if (rc.isMovementReady()) {
            if (rc.canMove(direction)) {
                rc.move(direction);
                return clockwiseRotation;
            }
            else {
                return !clockwiseRotation;
            }
        }
        return clockwiseRotation;
    }

    public static void swarm(RobotController rc, MapLocation me, RobotType robotType) throws GameActionException {
        RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared,rc.getTeam().opponent());
        if (robotInfo.length > 0) {
            int adjacentRobots = 0;
            MapLocation closestRobotInfoLocation = null;
            MapLocation adjacentRobotInfoLocation = null;
            for (RobotInfo w : robotInfo) {
                if (w.getType() != robotType) {
                    continue;
                }
                if (me.isAdjacentTo(w.getLocation())) {
                    adjacentRobots += 1;
                    adjacentRobotInfoLocation = w.getLocation();
                    continue;
                }
                if (closestRobotInfoLocation == null) {
                    closestRobotInfoLocation = w.getLocation();
                    continue;
                }
                if (closestRobotInfoLocation.distanceSquaredTo(me) > w.getLocation().distanceSquaredTo(me)) {
                    closestRobotInfoLocation = w.getLocation();
                }
            }
            Direction direction = null;
            if (closestRobotInfoLocation != null) {
                direction = me.directionTo(closestRobotInfoLocation);
            }
            if (adjacentRobots >= 2) {
                direction = me.directionTo(adjacentRobotInfoLocation).opposite();
            }
            if (direction != null){
                while (rc.isMovementReady()) {
                    if (rc.canMove(direction)) {
                        rc.move(direction);
                        continue;
                    }
                    if (rc.canMove(direction.rotateLeft())) {
                        rc.move(direction.rotateLeft());
                        continue;
                    }
                    if (rc.canMove(direction.rotateRight())) {
                        rc.move(direction.rotateRight());
                        continue;
                    }
                    if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                        rc.move(direction.rotateLeft().rotateLeft());
                        continue;
                    }
                    if (rc.canMove(direction.rotateRight().rotateRight())) {
                        rc.move(direction.rotateRight().rotateRight());
                        continue;
                    }
                    if (rc.canMove(direction.rotateLeft().rotateLeft().rotateLeft())) {
                        rc.move(direction.rotateLeft().rotateLeft().rotateLeft());
                        continue;
                    }
                    if (rc.canMove(direction.rotateRight().rotateRight().rotateRight())) {
                        rc.move(direction.rotateRight().rotateRight().rotateRight());
                        continue;
                    }
                    if (rc.canMove(direction.opposite())) {
                        rc.move(direction.opposite());
                        continue;
                    }
                }
            }
            else {
                moveRandomly(rc);
            }
        }
        else {
            // moveRandomly(rc);
        }
    }

    protected static void bug(RobotController rc,MapLocation dest) throws GameActionException {
        Direction lastDirection = Direction.CENTER;
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (me.equals(dest)) {
                return;
            }
            Direction direction = me.directionTo(dest);
            if (rc.canMove(direction)) {
                rc.move(direction);
                lastDirection = direction;
            }
            else {
                rc.setIndicatorString(direction.toString());
                if (rc.canMove(direction.rotateLeft()) && lastDirection != direction.rotateLeft().opposite()) {
                    rc.move(direction.rotateLeft());
                    lastDirection = direction.rotateLeft();
                }
                else if (rc.canMove(direction.rotateLeft().rotateLeft()) && lastDirection != direction.rotateLeft().rotateLeft().opposite()) {
                    rc.move(direction.rotateLeft().rotateLeft());
                    lastDirection = direction.rotateLeft().rotateLeft();
                }
                else if (rc.canMove(direction.rotateLeft().rotateLeft().rotateLeft()) && lastDirection != direction.rotateLeft().rotateLeft().rotateLeft().opposite()) {
                    rc.move(direction.rotateLeft().rotateLeft().rotateLeft());
                    lastDirection = direction.rotateLeft().rotateLeft().rotateLeft();
                }
                else if (rc.canMove(direction.rotateLeft().rotateLeft().rotateLeft().rotateLeft()) && lastDirection != direction.rotateLeft().rotateLeft().rotateLeft().rotateLeft().opposite()) {
                    rc.move(direction.rotateLeft().rotateLeft().rotateLeft().rotateLeft());
                    lastDirection = direction.rotateLeft().rotateLeft().rotateLeft().rotateLeft();
                }
                else if (rc.canMove(direction.rotateRight()) && lastDirection != direction.rotateRight().opposite()) {
                    rc.move(direction.rotateRight());
                    lastDirection = direction.rotateRight();
                }
                else if (rc.canMove(direction.rotateRight().rotateRight()) && lastDirection != direction.rotateRight().rotateRight().opposite()) {
                    rc.move(direction.rotateRight().rotateRight());
                    lastDirection = direction.rotateRight().rotateRight();
                }
                else if (rc.canMove(direction.rotateRight().rotateRight().rotateRight()) && lastDirection != direction.rotateRight().rotateRight().rotateRight().opposite()) {
                    rc.move(direction.rotateRight().rotateRight().rotateRight());
                    lastDirection = direction.rotateRight().rotateRight().rotateRight();
                }
                else if (rc.canMove(direction.opposite()) && lastDirection != direction) {
                    rc.move(direction.opposite());
                    lastDirection = direction.opposite();
                }
            }
        }
    }
    protected static boolean bug(RobotController rc,MapLocation dest, boolean clockwiseRotation) throws GameActionException {
        Direction lastDirection = Direction.CENTER;
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (me.equals(dest)) {
                return clockwiseRotation;
            }
            Direction direction = me.directionTo(dest);
            if (rc.canMove(direction)) {
                rc.move(direction);
                lastDirection = direction;
            }
            else {
                rc.setIndicatorString(direction.toString() + " " + clockwiseRotation);
                if (clockwiseRotation) {
                    if (rc.canMove(direction.rotateLeft()) && lastDirection != direction.rotateLeft().opposite()) {
                        rc.move(direction.rotateLeft());
                        lastDirection = direction.rotateLeft();
                    }
                    else if (rc.canMove(direction.rotateLeft().rotateLeft()) && lastDirection != direction.rotateLeft().rotateLeft().opposite()) {
                        rc.move(direction.rotateLeft().rotateLeft());
                        lastDirection = direction.rotateLeft().rotateLeft();
                    }
                    else if (rc.canMove(direction.rotateLeft().rotateLeft().rotateLeft()) && lastDirection != direction.rotateLeft().rotateLeft().rotateLeft().opposite()) {
                        rc.move(direction.rotateLeft().rotateLeft().rotateLeft());
                        lastDirection = direction.rotateLeft().rotateLeft().rotateLeft();
                    }
                    else if (rc.canMove(direction.opposite()) && lastDirection != direction) {
                        rc.move(direction.opposite());
                        lastDirection = direction.opposite();
                        clockwiseRotation = !clockwiseRotation;
                    }
                    else {
                        clockwiseRotation = !clockwiseRotation;
                    }
                }
                else {
                    if (rc.canMove(direction.rotateRight()) && lastDirection != direction.rotateRight().opposite()) {
                        rc.move(direction.rotateRight());
                        lastDirection = direction.rotateRight();
                    }
                    else if (rc.canMove(direction.rotateRight().rotateRight()) && lastDirection != direction.rotateRight().rotateRight().opposite()) {
                        rc.move(direction.rotateRight().rotateRight());
                        lastDirection = direction.rotateRight().rotateRight();
                    }
                    else if (rc.canMove(direction.rotateRight().rotateRight().rotateRight()) && lastDirection != direction.rotateRight().rotateRight().rotateRight().opposite()) {
                        rc.move(direction.rotateRight().rotateRight().rotateRight());
                        lastDirection = direction.rotateRight().rotateRight().rotateRight();
                    }
                    else if (rc.canMove(direction.opposite()) && lastDirection != direction) {
                        rc.move(direction.opposite());
                        lastDirection = direction.opposite();
                        clockwiseRotation = !clockwiseRotation;
                    }
                    else {
                        clockwiseRotation = !clockwiseRotation;
                    }
                }
            }
            break;
        }
        return clockwiseRotation;
    }
}

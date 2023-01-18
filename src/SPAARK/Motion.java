package SPAARK;

import battlecode.common.*;

import java.util.Random;

public class Motion {
    private static final Random rng = new Random(2023);
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
                moveRandomly(rc);
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
                moveRandomly(rc);
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
            if (moved == false) {
                moveRandomly(rc);
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
            if (moved == false) {
                moveRandomly(rc);
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
            RobotInfo prioritizedRobotInfo = null;
            for (RobotInfo w : robotInfo) {
                if (w.getType() != robotType) {
                    continue;
                }
                if (prioritizedRobotInfo == null) {
                    prioritizedRobotInfo = w;
                    continue;
                }
                if (prioritizedRobotInfo.ID > w.ID) {
                    prioritizedRobotInfo = w;
                }
            }
            Direction direction = null;
            if (prioritizedRobotInfo != null) {
                direction = me.directionTo(prioritizedRobotInfo.getLocation());
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
                    break;
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

    protected static boolean bug(RobotController rc, MapLocation dest, boolean clockwiseRotation, StringBuilder indicatorString) throws GameActionException {
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
                indicatorString.append("BUG-DIR=" + DIRABBREV[direction.getDirectionOrderNum()] + "; BUG-CW=" + clockwiseRotation + "; ");
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
                        // clockwiseRotation = !clockwiseRotation;
                    }
                    else {
                        clockwiseRotation = !clockwiseRotation;
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
                    }
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
        return clockwiseRotation;
    }
    protected static Direction[] bug2(RobotController rc, MapLocation dest, Direction lastDirection, boolean clockwiseRotation, StringBuilder indicatorString) throws GameActionException {
        boolean oldClockwiseRotation = clockwiseRotation;
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (me.equals(dest)) {
                return new Direction[]{Direction.CENTER,null};
            }
            Direction direction = me.directionTo(dest);
            boolean moved = false;
            if (rc.canMove(direction) && lastDirection != direction.opposite()) {
                boolean touchingTheWallBefore = false;
                for (int i = -1;i <= 1;i++) {
                    for (int j = -1;j <= 1;j++) {
                        if (rc.onTheMap(me.translate(i,j))) {
                            if (rc.sensePassability(me.translate(i,j)) == false) {
                                touchingTheWallBefore = true;
                            }
                        }
                    }
                }
                rc.move(direction);
                lastDirection = direction;
                // boolean touchingTheWallAfter = false;
                // for (int i = -1;i <= 1;i++) {
                //     for (int j = -1;j <= 1;j++) {
                //         if (rc.onTheMap(me.translate(i,j))) {
                //             if (rc.sensePassability(me.translate(i,j)) == false) {
                //                 touchingTheWallAfter = true;
                //             }
                //         }
                //     }
                // }
                if (touchingTheWallBefore) {
                    clockwiseRotation = !clockwiseRotation;
                }
                continue;
            }
            if (clockwiseRotation) {
                for (int i = 0;i < 7;i++) {
                    direction = direction.rotateLeft();
                    int f = bug2f(rc, direction, lastDirection);
                    if (f == 1) {
                        lastDirection = direction;
                        moved = true;
                        break;
                    }
                    else if (f == 2) {
                        lastDirection = Direction.CENTER;
                        clockwiseRotation = !clockwiseRotation;
                        moved = true;
                        break;
                    }
                }
            }
            else {
                for (int i = 0;i < 7;i++) {
                    direction = direction.rotateRight();
                    int f = bug2f(rc, direction, lastDirection);
                    if (f == 1) {
                        lastDirection = direction;
                        moved = true;
                        break;
                    }
                    else if (f == 2) {
                        lastDirection = Direction.CENTER;
                        clockwiseRotation = !clockwiseRotation;
                        moved = true;
                        break;
                    }
                }
            }
            if (moved == false) {
                lastDirection = Direction.CENTER;
                break;
            }
        }
        indicatorString.append("BUG-LAST-DIR=" + DIRABBREV[lastDirection.getDirectionOrderNum()] + "; BUG-CW=" + clockwiseRotation + "; ");
        if (oldClockwiseRotation != clockwiseRotation) {
            return new Direction[]{lastDirection,Direction.CENTER};
        }
        return new Direction[]{lastDirection,null};
    }
    private static int bug2f(RobotController rc, Direction direction, Direction lastDirection) throws GameActionException {
        if (rc.canMove(direction) && lastDirection != direction.opposite()) {
            rc.move(direction);
            return 1;
        }
        if (!rc.onTheMap(rc.getLocation().translate(direction.dx, direction.dy))) {
            return 2;
        }
        return 0;
    }
}

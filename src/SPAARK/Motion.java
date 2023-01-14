package SPAARK;

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

    public static void moveRandomly(RobotController rc) throws GameActionException {
        while (rc.isMovementReady()) {
            Direction direction = directions[rng.nextInt(directions.length)];
            if (rc.canMove(direction)) {
                rc.move(direction);
            }
        }
    }
    public static void spreadRandomly(RobotController rc, MapLocation me, MapLocation target) throws GameActionException {
        Direction direction = me.directionTo(target).opposite();
        while (rc.isMovementReady()) {
            boolean moved = false;
            int random = rng.nextInt(4);
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
    public static void circleAroundTarget(RobotController rc, MapLocation me, MapLocation target) throws GameActionException {
        Direction direction = me.directionTo(target).rotateLeft();
        if (direction.ordinal() % 2 == 1) {
            direction = direction.rotateLeft();
        }
        if (rc.canMove(direction)) {
            rc.move(direction);
        }
    }

    public static void bug(RobotController rc,MapInfo[] mapInfo,MapLocation dest) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (me.equals(dest)) {
                return;
            }
            boolean moved = false;
            Direction direction = me.directionTo(dest);
            if (rc.canMove(direction)) {
                rc.move(direction);
                moved = true;
            }
            else {
                switch (direction) {
                    case NORTH:
                        if (dest.x - me.x > 0) {
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                        }
                        else {
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                        }
                        break;
                    case NORTHEAST:
                        if (dest.x - me.x > dest.y - me.y) {
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                        }
                        else {
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                        }
                        break;
                    case EAST:
                        if (dest.y - me.y > 0) {
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                        }
                        else {
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                        }
                        break;
                    case SOUTHEAST:
                        if (dest.x - me.x > me.y - dest.y) {
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                        }
                        else {
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                        }
                        break;
                    case SOUTH:
                        if (dest.x - me.x > 0) {
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                        }
                        else {
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                        }
                        break;
                    case SOUTHWEST:
                        if (me.x - dest.x > me.y - dest.y) {
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                        }
                        else {
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                        }
                        break;
                    case WEST:
                        if (dest.y - me.y > 0) {
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                        }
                        else {
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                        }
                        break;
                    case NORTHWEST:
                        if (me.x - me.y > dest.y - me.y) {
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                        }
                        else {
                            if (rc.canMove(direction.rotateRight())) {
                                rc.move(direction.rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateRight().rotateRight())) {
                                rc.move(direction.rotateRight().rotateRight());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft())) {
                                rc.move(direction.rotateLeft());
                                moved = true;
                            }
                            if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                                rc.move(direction.rotateLeft().rotateLeft());
                                moved = true;
                            }
                        }
                        break;
                    case CENTER:
                        return;
                }
            }
            while (moved == false) {
                direction = directions[rng.nextInt(directions.length)];
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    moved = true;
                }
            }
        }
    }
}

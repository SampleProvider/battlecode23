package launcherCoagulation;

import battlecode.common.*;

public class Attack {

    protected static MapLocation[] launcherAttackLocations = new MapLocation[]{
        new MapLocation(4, 0),
        new MapLocation(3, 1),
        new MapLocation(3, 2),
        new MapLocation(2, 3),
        new MapLocation(1, 3),
        new MapLocation(0, 4),
        new MapLocation(-1, 3),
        new MapLocation(-2, 3),
        new MapLocation(-3, 2),
        new MapLocation(-3, 1),
        new MapLocation(-4, 0),
        new MapLocation(-3, -1),
        new MapLocation(-3, -2),
        new MapLocation(-2, -3),
        new MapLocation(-1, -3),
        new MapLocation(0, -4),
        new MapLocation(1, -3),
        new MapLocation(2, -3),
        new MapLocation(3, -2),
        new MapLocation(3, -1),
    };

    protected static RobotInfo attack(RobotController rc, MapLocation prioritizedHeadquarters, RobotInfo[] robotInfo, boolean attackAll, StringBuilder indicatorString) throws GameActionException {
        if (robotInfo.length > 0) {
            RobotInfo prioritizedRobotInfo = null;
            for (RobotInfo w : robotInfo) {
                if (w.getType() == RobotType.HEADQUARTERS) {
                    continue;
                }
                if (prioritizedRobotInfo == null) {
                    prioritizedRobotInfo = w;
                }
                if (prioritizedRobot(prioritizedRobotInfo.getType()) <= prioritizedRobot(w.getType())) {
                    if (prioritizedRobotInfo.getHealth() > w.getHealth()) {
                        prioritizedRobotInfo = w;
                    }
                }
            }
            if (prioritizedRobotInfo != null && (prioritizedRobot(prioritizedRobotInfo.getType()) >= 3 || attackAll)) {
                if (rc.canAttack(prioritizedRobotInfo.getLocation())) {
                    indicatorString.append("ATK-" + prioritizedRobotInfo.getLocation().toString() + "; ");
                    rc.attack(prioritizedRobotInfo.getLocation());
                }
            }
            return prioritizedRobotInfo;
        } else {
            if (attackAll) {
                if (rc.senseCloud(rc.getLocation())) {
                    MapLocation prioritizedMapLocation = null;
                    for (MapLocation m : launcherAttackLocations) {
                        if (prioritizedMapLocation == null) {
                            prioritizedMapLocation = rc.getLocation().translate(m.x, m.y);
                        }
                        else if (prioritizedHeadquarters.distanceSquaredTo(rc.getLocation().translate(m.x, m.y)) > prioritizedHeadquarters.distanceSquaredTo(prioritizedMapLocation)) {
                            prioritizedMapLocation = rc.getLocation().translate(m.x, m.y);
                        }
                    }
                    if (rc.canAttack(prioritizedMapLocation)) {
                        rc.attack(prioritizedMapLocation);
                    }
                }
                else {
                    MapLocation[] mapInfo = rc.senseNearbyCloudLocations(rc.getType().actionRadiusSquared);
                    if (mapInfo.length == 0) {
                        return null;
                    }
                    MapLocation prioritizedMapLocation = null;
                    for (MapLocation m : mapInfo) {
                        if (prioritizedMapLocation == null) {
                            prioritizedMapLocation = m;
                        }
                        else if (prioritizedHeadquarters.distanceSquaredTo(m) > prioritizedHeadquarters.distanceSquaredTo(prioritizedMapLocation)) {
                            prioritizedMapLocation = m;
                        }
                    }
                    if (rc.canAttack(prioritizedMapLocation)) {
                        rc.attack(prioritizedMapLocation);
                    }
                }
            }
            return null;
        }
    }

    protected static RobotInfo senseOpponent(RobotController rc, RobotInfo[] robotInfo) throws GameActionException {
        if (robotInfo.length > 0) {
            RobotInfo prioritizedRobotInfo = null;
            for (RobotInfo w : robotInfo) {
                if (w.getType() == RobotType.HEADQUARTERS) {
                    continue;
                }
                if (prioritizedRobotInfo == null) {
                    prioritizedRobotInfo = w;
                }
                if (prioritizedRobot(prioritizedRobotInfo.getType()) <= prioritizedRobot(w.getType())) {
                    if (prioritizedRobotInfo.getHealth() > w.getHealth()) {
                        prioritizedRobotInfo = w;
                    }
                }
            }
            return prioritizedRobotInfo;
        } else {
            return null;
        }
    }

    protected static int prioritizedRobot(RobotType robotType) {
        if (robotType == RobotType.DESTABILIZER) {
            return 5;
        }
        if (robotType == RobotType.LAUNCHER) {
            return 4;
        }
        if (robotType == RobotType.BOOSTER) {
            return 3;
        }
        if (robotType == RobotType.CARRIER) {
            return 2;
        }
        if (robotType == RobotType.AMPLIFIER) {
            return 1;
        }
        return 0;
    }
}
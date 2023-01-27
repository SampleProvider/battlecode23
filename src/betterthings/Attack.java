package betterthings;

import battlecode.common.*;

public class Attack {
    protected static RobotInfo attack(RobotController rc, MapLocation prioritizedHeadquarters, RobotInfo[] robotInfo, RobotType robotType, boolean attackAll, StringBuilder indicatorString) throws GameActionException {
        if (robotInfo.length > 0) {
            RobotInfo prioritizedRobotInfo = null;
            MapLocation prioritizedRobotInfoLocation = null;
            for (RobotInfo w : robotInfo) {
                if (w.getType() == RobotType.HEADQUARTERS) {
                    continue;
                }
                if (w.getType() == robotType) {
                    if (prioritizedRobotInfo == null) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    } else if (prioritizedRobotInfo.getHealth() > w.getHealth()) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    }
                } else if (attackAll) {
                    if (prioritizedRobotInfo == null) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    } else if (prioritizedRobotInfo.getType() != robotType && prioritizedRobotInfo.getHealth() > w.getHealth()) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    }
                }
            }
            if (prioritizedRobotInfoLocation != null) {
                if (rc.canAttack(prioritizedRobotInfoLocation)) {
                    indicatorString.append("ATK-" + prioritizedRobotInfoLocation.toString() + "; ");
                    rc.attack(prioritizedRobotInfoLocation);
                }
            }
            return prioritizedRobotInfo;
        } else {
            if (attackAll) {
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
                } else if (prioritizedRobotInfo.getHealth() > w.getHealth()) {
                    prioritizedRobotInfo = w;
                }
            }
            return prioritizedRobotInfo;
        } else {
            return null;
        }
    }
}
package SPAARK_1_24_2023;

import battlecode.common.*;

public class Attack {
    protected static RobotInfo attack(RobotController rc, MapLocation me, RobotInfo[] robotInfo, boolean attackAll, StringBuilder indicatorString) throws GameActionException {
        if (robotInfo.length > 0) {
            RobotInfo prioritizedRobotInfo = null;
            MapLocation prioritizedRobotInfoLocation = null;
            for (RobotInfo w : robotInfo) {
                if (w.getType() == RobotType.HEADQUARTERS) {
                    continue;
                }
                if (prioritizedRobotInfo == null) {
                    prioritizedRobotInfo = w;
                    prioritizedRobotInfoLocation = w.getLocation();
                }
                if (prioritizedRobot(prioritizedRobotInfo.getType()) <= prioritizedRobot(w.getType())) {
                    if (prioritizedRobotInfo.getHealth() > w.getHealth()) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    }
                }
            }
            if (prioritizedRobotInfoLocation != null && (prioritizedRobot(prioritizedRobotInfo.getType()) >= 3 || attackAll)) {
                if (rc.canAttack(prioritizedRobotInfoLocation)) {
                    indicatorString.append("ATK-" + prioritizedRobotInfoLocation.toString() + "; ");
                    rc.attack(prioritizedRobotInfoLocation);
                }
            }
            return prioritizedRobotInfo;
        } else {
            if (attackAll) {
                MapLocation[] mapInfo = rc.senseNearbyCloudLocations(rc.getType().actionRadiusSquared);
                for (MapLocation m : mapInfo) {
                    if (rc.canAttack(m)) {
                        rc.attack(m);
                        return null;
                    }
                }
            }
            return null;
        }
    }

    protected static RobotInfo senseOpponent(RobotController rc, MapLocation me, RobotInfo[] robotInfo) throws GameActionException {
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
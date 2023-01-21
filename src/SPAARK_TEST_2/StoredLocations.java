package SPAARK_TEST_2;

import battlecode.common.*;

public strictfp class StoredLocations {
    protected RobotController rc;

    private WellInfo[] wells = new WellInfo[8];
    private MapLocation[] opponents = new MapLocation[8];
    private MapLocation[] islands = new MapLocation[16];
    
    protected boolean[] removedOpponents = new boolean[GlobalArray.OPPONENTS_LENGTH];
    protected boolean[] removedIslands = new boolean[GlobalArray.OPPONENTS_LENGTH];

    // general location/data parsing/writing
    public StoredLocations(RobotController rc) {
        this.rc = rc;
    }

    public void writeToGlobalArray() throws GameActionException {
        if (!rc.canWriteSharedArray(0, 0)) {
            return;
        }
        for (int i = 0;i < 8;i++) {
            if (wells[i] != null) {
                if (GlobalArray.storeWell(rc, wells[i])) {
                    wells[i] = null;
                }
            }
            else {
                break;
            }
        }
        for (int i = 0;i < 8;i++) {
            if (opponents[i] != null) {
                if (GlobalArray.storeOpponentLocation(rc, opponents[i])) {
                    opponents[i] = null;
                }
            }
        }
        for (int i = 0;i < GlobalArray.OPPONENTS_LENGTH;i++) {
            if (removedOpponents[i]) {
                rc.writeSharedArray(i + GlobalArray.OPPONENTS, 0);
            }
        }
    }

    public boolean storeWell(WellInfo w) {
        for (int i = 0;i < 8;i++) {
            if (wells[i] != null && wells[i].equals(w)) {
                return false;
            }
            else if (wells[i] == null) {
                wells[i] = w;
                return true;
            }
        }
        return false;
    }
    public void detectWells() {
        WellInfo[] wellInfo = rc.senseNearbyWells();
        for (WellInfo w : wellInfo) {
            storeWell(w);
        }
    }
    public boolean storeOpponentLocation(MapLocation m) {
        for (int i = 0;i < 8;i++) {
            if (opponents[i] != null && opponents[i].equals(m)) {
                return false;
            }
            else if (opponents[i] == null) {
                opponents[i] = m;
                return true;
            }
        }
        return false;
    }
    public void removeOpponentLocation(int n) {
        removedOpponents[n] = true;
    }
    public void detectOpponentLocations() throws GameActionException {
        storeOpponentLocation(Attack.senseOpponent(rc, rc.getLocation(), rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent())));
    }
    public boolean storeIslandLocation(MapLocation m) {
        for (int i = 0;i < 8;i++) {
            if (opponents[i] != null && opponents[i].equals(m)) {
                return false;
            }
            else if (opponents[i] == null) {
                opponents[i] = m;
                return true;
            }
        }
        return false;
    }
    public void removeIslandLocation(int n) {
        removedOpponents[n] = true;
    }
    public void detectIslandLocations() throws GameActionException {
        storeOpponentLocation(Attack.senseOpponent(rc, rc.getLocation(), rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent())));
    }
}

package SPAARK;

import battlecode.common.*;

public strictfp class StoredLocations {
    protected RobotController rc;

    private WellInfo[] wells = new WellInfo[8];
    private MapLocation[] opponents = new MapLocation[8];
    private MapLocation[] islands = new MapLocation[16];
    private Team[] islandTeams = new Team[16];
    private int[] islandIds = new int[16];
    
    protected boolean[] removedOpponents = new boolean[GlobalArray.OPPONENTS_LENGTH];
    protected boolean[] removedIslands = new boolean[GlobalArray.ISLANDS_LENGTH];

    // general location/data parsing/writing
    public StoredLocations(RobotController rc) {
        this.rc = rc;
    }

    public void writeToGlobalArray() throws GameActionException {
        if (!rc.canWriteSharedArray(0, 0)) {
            return;
        }
        for (int i = 0; i < 8; i++) {
            if (wells[i] != null) {
                if (GlobalArray.storeWell(rc, wells[i])) {
                    wells[i] = null;
                }
            } else {
                break;
            }
        }
        for (int i = 0; i < 8; i++) {
            if (opponents[i] != null) {
                if (GlobalArray.storeOpponentLocation(rc, opponents[i])) {
                    opponents[i] = null;
                }
            }
        }
        for (int i = 0; i < GlobalArray.OPPONENTS_LENGTH; i++) {
            if (removedOpponents[i]) {
                rc.writeSharedArray(i + GlobalArray.OPPONENTS, 0);
            }
        }
        for (int i = 0;i < 16;i++) {
            if (islands[i] != null) {
                if (GlobalArray.storeIslandLocation(rc, islands[i], islandTeams[i], islandIds[i])) {
                    islands[i] = null;
                    islandTeams[i] = null;
                    islandIds[i] = 0;
                }
            }
        }
    }

    public boolean storeWell(WellInfo w) {
        for (int i = 0; i < 8; i++) {
            if (wells[i] != null && wells[i].equals(w)) {
                return false;
            } else if (wells[i] == null) {
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
        for (int i = 0; i < 8; i++) {
            if (opponents[i] != null && opponents[i].equals(m)) {
                return false;
            } else if (opponents[i] == null) {
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
    public boolean storeIslandLocation(MapLocation m, int id) throws GameActionException {
        for (int i = 0;i < 16;i++) {
            if (islands[i] != null && islandIds[i] == id) {
                return false;
            }
            else if (islands[i] == null) {
                islands[i] = m;
                islandTeams[i] = rc.senseTeamOccupyingIsland(id);
                islandIds[i] = id;
                return true;
            }
        }
        return false;
    }
    public void removeIslandLocation(int n) {
        removedIslands[n] = true;
    }
    public void detectIslandLocations() throws GameActionException {
        int[] islands = rc.senseNearbyIslands();
        for (int id : islands) {
            MapLocation[] islandLocations = rc.senseNearbyIslandLocations(id);
            storeIslandLocation(islandLocations[0], id);
        }
    }
}

package SPAARK_1_23_2023;

import battlecode.common.*;

public strictfp class StoredLocations {
    protected RobotController rc;
    
    public static final int FULL_WELL_TIME = 100;
    
    public static final int MIN_EXISTING_DISTANCE_SQUARED = 16;
    
    protected MapLocation[] headquarters = new MapLocation[0];

    protected WellInfo[] wells = new WellInfo[8];
    protected MapLocation[] opponents = new MapLocation[8];
    protected MapLocation[] islands = new MapLocation[16];
    protected Team[] islandTeams = new Team[16];
    protected boolean[] islandIsOutOfRange = new boolean[16];

    protected MapLocation[] fullWells = new MapLocation[GlobalArray.ADAMANTIUM_WELLS_LENGTH + GlobalArray.MANA_WELLS_LENGTH];
    protected int[] fullWellTimer = new int[GlobalArray.ADAMANTIUM_WELLS_LENGTH + GlobalArray.MANA_WELLS_LENGTH];

    protected boolean[] removedOpponents = new boolean[GlobalArray.OPPONENTS_LENGTH];
    protected boolean[] removedIslands = new boolean[GlobalArray.ISLANDS_LENGTH];

    // general location/data parsing/writing
    public StoredLocations(RobotController rc, MapLocation[] headquarters) {
        this.rc = rc;
        this.headquarters = headquarters;
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
        for (int i = 0; i < 16; i++) {
            if (islands[i] != null) {
                if (GlobalArray.storeIslandLocation(rc, islands[i], islandTeams[i], i, islandIsOutOfRange[i])) {
                    islands[i] = null;
                    islandTeams[i] = null;
                    islandIsOutOfRange[i] = false;
                }
            }
        }
    }

    public boolean storeWell(WellInfo w) {
        for (int i = 0; i < 8; i++) {
            if (wells[i] != null && wells[i].equals(w)) {
                return true;
            } else if (wells[i] == null) {
                wells[i] = w;
                return true;
            }
        }
        return false;
    }

    public void fullWell(MapLocation m) {
        for (MapLocation l : fullWells) {
            if (l != null && l.equals(m)) {
                return;
            }
        }
        for (int i = 0; i < GlobalArray.ADAMANTIUM_WELLS_LENGTH + GlobalArray.MANA_WELLS_LENGTH; i++) {
            if (fullWells[i] == null) {
                fullWells[i] = m;
                fullWellTimer[i] = FULL_WELL_TIME;
                return;
            }
        }
    }

    public boolean isFullWell(MapLocation m) {
        for (MapLocation l : fullWells) {
            if (l != null && l.equals(m)) {
                return true;
            }
        }
        return false;
    }

    public void updateFullWells() {
        for (int i = 0; i < GlobalArray.ADAMANTIUM_WELLS_LENGTH + GlobalArray.MANA_WELLS_LENGTH; i++) {
            fullWellTimer[i] -= 1;
            if (fullWellTimer[i] == 0) {
                fullWells[i] = null;
            }
        }
    }

    public void detectWells() {
        WellInfo[] wellInfo = rc.senseNearbyWells();
        for (WellInfo w : wellInfo) {
            storeWell(w);
        }
    }

    public boolean storeOpponentLocation(MapLocation m) {
        if (m == null) return false;
        for (int i = 0; i < 8; i++) {
            if (opponents[i] != null) {
                if (opponents[i].distanceSquaredTo(m) < MIN_EXISTING_DISTANCE_SQUARED) {
                    return true;
                }
            } else {
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
        // lol cheating a bit here
        if (!rc.canSenseLocation(m) || id >= 32) return false;
        int id2 = id % 16;
        if (id2 == id || islands[id2] == null) {
            islands[id2] = m;
            islandTeams[id2] = rc.senseTeamOccupyingIsland(id);
            if (id2 == id) {
                islandIsOutOfRange[id2] = false;
            }
            else {
                islandIsOutOfRange[id2] = true;
            }
            return true;
        }
        int lowestDistanceID = Integer.MAX_VALUE;
        int lowestDistanceID2 = Integer.MAX_VALUE;
        for (int i = 0; i < headquarters.length; i++) {
            int dist1 = headquarters[i].distanceSquaredTo(m);
            int dist2 = headquarters[i].distanceSquaredTo(islands[id2]);
            if (dist1 < lowestDistanceID) lowestDistanceID = dist1;
            if (dist2 < lowestDistanceID2) lowestDistanceID2 = dist2;
        }
        if (lowestDistanceID2 < lowestDistanceID) {
            islands[id2] = m;
            islandTeams[id2] = rc.senseTeamOccupyingIsland(id);
            islandIsOutOfRange[id2] = true;
            return true;
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

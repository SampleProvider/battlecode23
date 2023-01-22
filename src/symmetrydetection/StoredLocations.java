package symmetrydetection;

import battlecode.common.*;

public strictfp class StoredLocations {
    protected RobotController rc;

    private final int FULL_WELL_TIME = 100;
    
    private final int WELLS_LENGTH = 8;
    private final int OPPONENTS_LENGTH = 8;
    private final int ISLANDS_LENGTH = 16;

    private int symmetry = 0;
    private WellInfo[] wells = new WellInfo[WELLS_LENGTH];
    private MapLocation[] opponents = new MapLocation[OPPONENTS_LENGTH];
    private MapLocation[] islands = new MapLocation[ISLANDS_LENGTH];
    private Team[] islandTeams = new Team[ISLANDS_LENGTH];
    private int[] islandIds = new int[ISLANDS_LENGTH];
    
    protected MapLocation[] fullWells = new MapLocation[GlobalArray.ADAMANTIUM_WELLS_LENGTH + GlobalArray.MANA_WELLS_LENGTH];
    protected int[] fullWellTimer = new int[GlobalArray.ADAMANTIUM_WELLS_LENGTH + GlobalArray.MANA_WELLS_LENGTH];

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
        GlobalArray.storeSymmetry(rc, symmetry);
        for (int i = 0; i < WELLS_LENGTH; i++) {
            if (wells[i] != null) {
                if (GlobalArray.storeWell(rc, wells[i])) {
                    wells[i] = null;
                }
            } else {
                break;
            }
        }
        for (int i = 0; i < OPPONENTS_LENGTH; i++) {
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
        for (int i = 0;i < ISLANDS_LENGTH;i++) {
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
        for (int i = 0; i < WELLS_LENGTH; i++) {
            if (wells[i] != null && wells[i].equals(w)) {
                return false;
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
        for (int i = 0;i < GlobalArray.ADAMANTIUM_WELLS_LENGTH + GlobalArray.MANA_WELLS_LENGTH;i++) {
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
        for (int i = 0;i < GlobalArray.ADAMANTIUM_WELLS_LENGTH + GlobalArray.MANA_WELLS_LENGTH;i++) {
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
        for (int i = 0; i < OPPONENTS_LENGTH; i++) {
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
        for (int i = 0;i < ISLANDS_LENGTH;i++) {
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

    public void storeSymmetry(int _symmetry) {
        symmetry = _symmetry;
    }
}

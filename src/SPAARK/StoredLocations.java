package SPAARK;

import battlecode.common.*;

public strictfp class StoredLocations {
    protected RobotController rc;
    
    public static final int FULL_WELL_TIME = 150;
    
    public static final int MIN_EXISTING_DISTANCE_SQUARED = 16;

    protected MapLocation[] headquarters = new MapLocation[0];

    protected WellInfo[] wells = new WellInfo[8];
    protected MapLocation[] opponents = new MapLocation[8];
    protected MapLocation[] islands = new MapLocation[16];
    protected Team[] islandTeams = new Team[16];
    protected boolean[] islandIsOutOfRange = new boolean[16];

    protected boolean[] storedIslands = new boolean[35];

    protected int symmetry = 0;
    protected int notSymmetry = 0;

    protected MapLocation[] fullWells = new MapLocation[GlobalArray.ADAMANTIUM_WELLS_LENGTH + GlobalArray.MANA_WELLS_LENGTH];
    protected int[] fullWellTimer = new int[GlobalArray.ADAMANTIUM_WELLS_LENGTH + GlobalArray.MANA_WELLS_LENGTH];

    protected boolean[] removedOpponents = new boolean[GlobalArray.OPPONENTS_LENGTH];
    protected boolean[] removedIslands = new boolean[GlobalArray.ISLANDS_LENGTH];

    // general location/data parsing/writing
    public StoredLocations(RobotController rc, MapLocation[] headquarters) {
        this.rc = rc;
        this.headquarters = headquarters;
    }

    public boolean writeToGlobalArray() throws GameActionException {
        if (!rc.canWriteSharedArray(0, 0)) {
            return false;
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
        if (symmetry != 0) {
            GlobalArray globalArray = new GlobalArray();
            globalArray.parseGameState(rc.readSharedArray(GlobalArray.GAMESTATE));
            globalArray.setMapSymmetry(GlobalArray.setBit(0, symmetry - 1, 1));
            rc.writeSharedArray(GlobalArray.GAMESTATE, globalArray.getGameStateNumber());
            symmetry = 0;
        }
        else if (notSymmetry != 0) {
            GlobalArray globalArray = new GlobalArray();
            globalArray.parseGameState(rc.readSharedArray(GlobalArray.GAMESTATE));
            globalArray.setMapSymmetry(GlobalArray.setBit(globalArray.mapSymmetry(), notSymmetry - 1, 0));
            rc.writeSharedArray(GlobalArray.GAMESTATE, globalArray.getGameStateNumber());
            notSymmetry = 0;
        }
        return true;
    }

    public boolean foundNewLocations() throws GameActionException {
        if (symmetry != 0 || notSymmetry != 0) {
            return true;
        }
        MapLocation[] wellLocations = GlobalArray.getKnownWellLocations(rc);
        for (MapLocation m : wellLocations) {
            if (m == null) continue;
            boolean found = true;
            for (WellInfo w : wells) {
                if (w == null ) continue;
                if (w.getMapLocation().equals(m)) {
                    found = false;
                    break;
                };
            }
            if (found) return true;
        }
        MapLocation[] islandLocations = GlobalArray.getKnownIslandLocations(rc);
        for (MapLocation m : islandLocations) {
            if (m == null) continue;
            boolean found = true;
            for (MapLocation m2 : islands) {
                if (m2 == null) continue;
                if (m2.equals(m)) {
                    found = false;
                    break;
                };
            }
            if (found) return true;
        }
        return false;
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
        RobotInfo robot = Attack.senseOpponent(rc, rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent()));
        if (robot != null) {
            storeOpponentLocation(robot.getLocation());
        }
    }

    public boolean storeIslandLocation(MapLocation m, int id) throws GameActionException {
        if (storedIslands[id]) {
            return false;
        }
        // MapLocation[] islands = GlobalArray.getKnownIslandLocations(rc);
        // if (islands[id % 2] != null) {
        //     storedIslands[id] = true;
        //     return false;
        // }
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
            storedIslands[id] = true;
            rc.setIndicatorLine(rc.getLocation(), m, 0, 0, 0);
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
            storedIslands[id] = true;
            rc.setIndicatorLine(rc.getLocation(), m, 0, 0, 0);
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

    public void detectSymmetry() throws GameActionException {
        for (int i = 0; i < headquarters.length; i++) {
            if (rc.canSenseLocation(new MapLocation(rc.getMapWidth() - headquarters[i].x, headquarters[i].y))) {
                RobotInfo robot = rc.senseRobotAtLocation(new MapLocation(rc.getMapWidth() - headquarters[i].x, headquarters[i].y));
                if (robot != null && robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() == rc.getTeam().opponent()) {
                    symmetry = 1;
                    rc.setIndicatorLine(rc.getLocation(), new MapLocation(rc.getMapWidth() - headquarters[i].x, headquarters[i].y), 0, 0, 0);
                }
                else {
                    notSymmetry = 1;
                    rc.setIndicatorLine(rc.getLocation(), new MapLocation(rc.getMapWidth() - headquarters[i].x, headquarters[i].y), 0, 0, 0);
                }
            }
            if (rc.canSenseLocation(new MapLocation(headquarters[i].x, rc.getMapHeight() - headquarters[i].y))) {
                RobotInfo robot = rc.senseRobotAtLocation(new MapLocation(headquarters[i].x, rc.getMapHeight() - headquarters[i].y));
                if (robot != null && robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() == rc.getTeam().opponent()) {
                    symmetry = 2;
                    rc.setIndicatorLine(rc.getLocation(), new MapLocation(headquarters[i].x, rc.getMapHeight() - headquarters[i].y), 0, 0, 0);
                }
                else {
                    notSymmetry = 2;
                    rc.setIndicatorLine(rc.getLocation(), new MapLocation(headquarters[i].x, rc.getMapHeight() - headquarters[i].y), 0, 0, 0);
                }
            }
            if (rc.canSenseLocation(new MapLocation(rc.getMapWidth() - headquarters[i].x, rc.getMapHeight() - headquarters[i].y))) {
                RobotInfo robot = rc.senseRobotAtLocation(new MapLocation(rc.getMapWidth() - headquarters[i].x, rc.getMapHeight() - headquarters[i].y));
                if (robot != null && robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() == rc.getTeam().opponent()) {
                    symmetry = 3;
                    rc.setIndicatorLine(rc.getLocation(), new MapLocation(rc.getMapWidth() - headquarters[i].x, rc.getMapHeight() - headquarters[i].y), 0, 0, 0);
                }
                else {
                    notSymmetry = 3;
                    rc.setIndicatorLine(rc.getLocation(), new MapLocation(rc.getMapWidth() - headquarters[i].x, rc.getMapHeight() - headquarters[i].y), 0, 0, 0);
                }
            }
        }
    }
}
package SPAARK;

import battlecode.common.*;

import java.util.Arrays;
import java.util.ArrayList;

public strictfp class StoredLocations {
    protected RobotController rc;
    private MapLocation center;
    
    public static final int FULL_WELL_TIME = 150;
    
    public static final int MIN_EXISTING_DISTANCE_SQUARED = 16;

    protected MapLocation[] headquarters = new MapLocation[0];

    protected WellInfo[] wells = new WellInfo[8];
    protected MapLocation[] opponents = new MapLocation[8];
    protected MapLocation[] islands = new MapLocation[16];
    protected Team[] islandTeams = new Team[16];
    protected boolean[] islandIsOutOfRange = new boolean[16];

    protected boolean[] storedIslands = new boolean[36];

    private MapLocation[] centerHeadquarters;

    protected int[] symmetry = new int[3];
    protected boolean arrivedAtWell = false;
    
    protected boolean allLeft = true;
    protected boolean allRight = true;
    protected boolean allTop = true;
    protected boolean allBottom = true;
    protected boolean allDiagonal1 = true;
    protected boolean allDiagonal2 = true;

    protected MapLocation[] fullWells = new MapLocation[GlobalArray.ADAMANTIUM_WELLS_LENGTH + GlobalArray.MANA_WELLS_LENGTH];
    protected int[] fullWellTimer = new int[GlobalArray.ADAMANTIUM_WELLS_LENGTH + GlobalArray.MANA_WELLS_LENGTH];

    protected boolean[] removedOpponents = new boolean[GlobalArray.OPPONENTS_LENGTH];
    protected boolean[] removedIslands = new boolean[GlobalArray.ISLANDS_LENGTH];

    // general location/data parsing/writing
    public StoredLocations(RobotController rc, MapLocation[] headquarters) {
        this.rc = rc;
        center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        if (headquarters.length > 0) {
            setHeadquarters(headquarters);
        }
    }

    public void setHeadquarters(MapLocation[] headquarters) {
        this.headquarters = headquarters;
        for (int i = 0;i < headquarters.length;i++) {
            if (headquarters[i].x > center.x) {
                allLeft = false;
            }
            if (headquarters[i].x < center.x) {
                allRight = false;
            }
            if (headquarters[i].y < center.y) {
                allTop = false;
            }
            if (headquarters[i].y > center.y) {
                allBottom = false;
            }
            if (headquarters[i].x < center.x == headquarters[i].y < center.y) {
                allDiagonal2 = false;
            }
            if (headquarters[i].x > center.x == headquarters[i].y > center.y) {
                allDiagonal1 = false;
            }
        }
        centerHeadquarters = Arrays.copyOf(headquarters, headquarters.length);
        MapLocationDistanceToCenterComparator distCompare = new MapLocationDistanceToCenterComparator();
        distCompare.setCenter(center);
        Arrays.sort(centerHeadquarters, distCompare);
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
        GlobalArray globalArray = new GlobalArray();
        globalArray.parseGameState(rc.readSharedArray(GlobalArray.GAMESTATE));
        for (int i = 0; i < 3; i++) {
            if (symmetry[i] == 1) {
                globalArray.setMapSymmetry(GlobalArray.setBit(0, i, 1));
                rc.writeSharedArray(GlobalArray.GAMESTATE, globalArray.getGameStateNumber());
                break;
            }
            else if (symmetry[i] == -1) {
                globalArray.setMapSymmetry(GlobalArray.setBit(globalArray.mapSymmetry(), i, 0));
                rc.writeSharedArray(GlobalArray.GAMESTATE, globalArray.getGameStateNumber());
            }
        }
        return true;
    }

    public boolean foundNewLocations() throws GameActionException {
        for (int i = 0; i < 3; i++) {
            if (symmetry[i] != 0) {
                return true;
            }
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
            for (int j = 0;j < 3;j++) {
                if (inReflectionLine(headquarters[i]) != -1 && inReflectionLine(headquarters[i]) != j) {
                    continue;
                }
                if (rc.canSenseLocation(reflectTarget(headquarters[i], j))) {
                    RobotInfo robot = rc.senseRobotAtLocation(reflectTarget(headquarters[i], j));
                    if (robot != null && robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() == rc.getTeam().opponent()) {
                        symmetry[j] = 1;
                    }
                    else {
                        symmetry[j] = -1;
                    }
                }
            }
        }
        for (int i = 0; i < GlobalArray.MANA_WELLS_LENGTH + GlobalArray.ADAMANTIUM_WELLS_LENGTH; i++) {
            int array = rc.readSharedArray(GlobalArray.MANA_WELLS + i);
            if (!GlobalArray.hasLocation(array)) {
                continue;
            }
            MapLocation target = GlobalArray.parseLocation(array);
            for (int j = 0;j < 3;j++) {
                if (inReflectionLine(target) != -1 && inReflectionLine(target) != j) {
                    continue;
                }
                if (rc.canSenseLocation(reflectTarget(target, j))) {
                    WellInfo well = rc.senseWell(reflectTarget(target, j));
                    if (well != null && well.getResourceType() == (i < GlobalArray.MANA_WELLS_LENGTH ? ResourceType.MANA : ResourceType.ADAMANTIUM)) {
                        symmetry[j] = 1;
                    }
                    else {
                        symmetry[j] = -1;
                    }
                }
            }
        }
    }

    public void updateMapSymmetry(int storedSymmetry) {
        if ((storedSymmetry & 0b1) == 0) {
            symmetry[0] = -1;
        }
        if ((storedSymmetry & 0b10) == 0) {
            symmetry[1] = -1;
        }
        if ((storedSymmetry & 0b100) == 0) {
            symmetry[2] = -1;
        }
        if (symmetry[0] + symmetry[1] == -2) {
            symmetry[2] = 1;
        }
        if (symmetry[1] + symmetry[2] == -2) {
            symmetry[0] = 1;
        }
        if (symmetry[0] + symmetry[2] == -2) {
            symmetry[1] = 1;
        }
    }

    public int getMapSymmetry() {
        for (int i = 0;i < 3;i++) {
            if (symmetry[i] == 1) {
                return i;
            }
        }
        return -1;
    }

    public MapLocation getTarget() throws GameActionException {
        MapLocation target;
        if (arrivedAtWell) {
            int array = rc.readSharedArray(GlobalArray.OPPONENT_HEADQUARTERS);
            if ((array & 0b100) == 4) {
                target = centerHeadquarters[rc.getID() % (headquarters.length)];
            }
            else {
                target = centerHeadquarters[array & 0b11];
            }
        }
        else {
            int array = rc.readSharedArray(GlobalArray.OPPONENT_HEADQUARTERS + 1);
            if (!GlobalArray.hasLocation(array)) {
                return center;
            }
            target = GlobalArray.parseLocation(array);
        }
        for (int i = 0;i < 3;i++) {
            if (symmetry[i] == 1) {
                return reflectTarget(target, i);
            }
        }
        for (int i = 0;i < 3;i++) {
            if (symmetry[i] == -1) {
                if (i != 0) {
                    return reflectTarget(target, 0);
                }
                else if (i != 1) {
                    return reflectTarget(target, 1);
                }
            }
        }
        if ((allLeft || allRight) && !(allTop || allBottom)) {
            return reflectTarget(target, 0);
        }
        if ((allTop || allBottom) && !(allLeft || allRight)) {
            return reflectTarget(target, 2);
        }
        if ((allDiagonal1 || allDiagonal2)) {
            return reflectTarget(target, 1);
        }
        return reflectTarget(target, 0);
    }

    public MapLocation reflectTarget(MapLocation target, int symmetry) {
        if (symmetry == 0) {
            return new MapLocation(rc.getMapWidth() - 1 - target.x, target.y);
        }
        else if (symmetry == 1) {
            return new MapLocation(target.x, rc.getMapHeight() - 1 - target.y);
        }
        else {
            return new MapLocation(rc.getMapWidth() - 1 - target.x, rc.getMapHeight() - 1 - target.y);
        }
    }
    public int inReflectionLine(MapLocation m) {
        if (m.x == rc.getMapWidth() / 2) {
            if (m.y == rc.getMapHeight() / 2) {
                return 2;
            }
            else {
                return 0;
            }
        }
        else {
            if (m.y == rc.getMapHeight() / 2) {
                return 1;
            }
            else {
                return -1;
            }
        }
    }
}
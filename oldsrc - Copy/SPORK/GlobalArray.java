package SPORK;

import java.util.Arrays;

import battlecode.common.*;

public strictfp class GlobalArray {
    public static final int CONVERT_WELL = 0;
    public static final int UPGRADE_WELLS = 1;
    public static final int ELIXIR_HQ_ID = 2;
    public static final int CONVERSION_WELL_ID = 3;

    private static final ResourceType[] resourceTypes = new ResourceType[] {ResourceType.NO_RESOURCE, ResourceType.ADAMANTIUM, ResourceType.MANA, ResourceType.ELIXIR};
    private final int[] currentState = new int[5];
    private boolean changedState = false;

    /*
     * Bits 0-5     x coordinate
     * Bits 6-11    y coordinate
     * Bit 12       presence marker
     * Bits 13-14   resource type (none is headquarters)
     * Bit 15       upgraded well
     */

    // general location/data parsing
    public static boolean hasLocation(int n) {
        return (n >> 12 & 0b1) == 1;
    }
    public static MapLocation parseLocation(int n) {
        return new MapLocation(n & 0b111111, (n >> 6) & 0b111111);
    }
    public static int intifyLocation(MapLocation loc) {
        return 0b1000000000000 | (loc.y << 6) | loc.x;
    }
    public static ResourceType wellType(int n) {
        return resourceTypes[n >> 13 & 0b11];
    }
    public static boolean isUpgradedWell(int n) {
        return n >> 15 == 1;
    }
    public static boolean storeWell(RobotController rc, WellInfo well) throws GameActionException {
        if (rc.canWriteSharedArray(0, 0)) {
            int resType = well.getResourceType().resourceID;
            MapLocation loc = well.getMapLocation();
            if (resType == 2) {
                for (int i = 5; i <= 9; i++) {
                    int arrayWell = rc.readSharedArray(i);
                    if (!hasLocation(arrayWell) || parseLocation(arrayWell).equals(loc)) {
                        rc.writeSharedArray(i, ((well.isUpgraded() ? 1 : 0) << 15) | (resType << 13) | intifyLocation(well.getMapLocation()));
                        return true;
                    }
                }
            } else if (resType == 1) {
                for (int i = 10; i <= 13; i++) {
                    int arrayWell = rc.readSharedArray(i);
                    if (!hasLocation(arrayWell) || parseLocation(arrayWell).equals(loc)) {
                        rc.writeSharedArray(i, ((well.isUpgraded() ? 1 : 0) << 15) | (resType << 13) | intifyLocation(well.getMapLocation()));
                        return true;
                    }
                }
            } else {
                throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Unexpected resource type " + well.getResourceType());
            }
            return false;
        }
        return false;
    }
    public static MapLocation[] getKnownWellLocations(RobotController rc) throws GameActionException {
        MapLocation[] wells = new MapLocation[8];
        for (int i = 0; i < 8; i++) {
            int arrayWell = rc.readSharedArray(i+5);
            if (hasLocation(arrayWell)) {
                wells[i] = parseLocation(arrayWell);
                // wells[j++] = new WellInfo(parseLocation(arrayWell), wellType(arrayWell), null, isUpgradedWell(arrayWell));
            }
        }
        return wells;
    }
    public static MapLocation[] getKnownHeadQuarterLocations(RobotController rc) throws GameActionException {
        MapLocation[] headquarters = new MapLocation[4];
        int hqCount = 0;
        for (int i = 0; i < 4; i++) {
            int arrayHQ = rc.readSharedArray(i+1);
            if (hasLocation(arrayHQ)) {
                headquarters[i] = parseLocation(arrayHQ);
                hqCount++;
            }
        }
        return Arrays.copyOf(headquarters, hqCount);
    }

    /*
     * Bits 0-1     prioritized resource
     * Bit 2        convert well
     * Bit 3        upgrade wells
     * Bits 4-5     id of elixir hq (where to drop elixir)
     * bits 6-9     id of well to convert to elixir
     */

    // read game state
     public int[] parseGameState(int n) {
        currentState[CONVERT_WELL] = n & 0b1; // bit 0
        currentState[UPGRADE_WELLS] = n >> 1 & 0b1; // bit 1
        currentState[ELIXIR_HQ_ID] = n >> 2 & 0b11; // bits 2-3
        currentState[CONVERSION_WELL_ID] = n >> 4 & 0b111; // bits 4-6
        return currentState;
    }
    public boolean convertWell() {
        return currentState[CONVERT_WELL] == 1;
    }
    public int conversionWellId() {
        return currentState[CONVERSION_WELL_ID];
    }
    public boolean upgradeWells() {
        return currentState[UPGRADE_WELLS] == 1;
    }

    // write game state
    public int getGameStateNumber() {
        return currentState[CONVERT_WELL] | currentState[UPGRADE_WELLS] << 1 | currentState[ELIXIR_HQ_ID] << 2 | currentState[CONVERSION_WELL_ID] << 4;
    }
    public boolean changedState() {
        return changedState;
    }
    public void setUpgradeWells(boolean set) {
        changedState = true;
        currentState[UPGRADE_WELLS] = set ? 1 : 0;
    }
    public void setTargetElixirWellHQPair(int wellIndex, int hqIndex) {
        changedState = true;
        currentState[CONVERSION_WELL_ID] = wellIndex;
        currentState[ELIXIR_HQ_ID] = hqIndex;
    }

    // bit operations
    public static int toggleBit(int n, int pos) {
        return n ^ (1 << pos);
    }
    public static int setBit(int n, int pos, int m) {
        if (m == 0) {
            return n & ~(1 << pos);
        }
        else {
            return n | 1 << pos;
        }
    }
}

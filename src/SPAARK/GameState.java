package SPAARK;

import battlecode.common.*;

public strictfp class GameState {
    public static final int PRIORITIZED_RESOURCE = 0;
    public static final int CONVERT_WELL = 1;
    public static final int UPGRADE_WELLS = 2;
    public static final int ELIXIR_HQ = 3;
    public static final int CONVERSION_WELL_ID = 4;

    private static final ResourceType[] resourceTypes = new ResourceType[] {ResourceType.NO_RESOURCE, ResourceType.ADAMANTIUM, ResourceType.MANA, ResourceType.ELIXIR};
    private static final int[] currentState = new int[5];

    /*
     * Bits 0-5     x coordinate
     * Bits 6-11    y coordinate
     * Bit 12       presence marker
     * Bits 13-14   resource type (none is headquarters)
     * Bit 15       upgraded well
     */

    public static boolean hasLocation(int n) {
        return (n >> 12 & 0b1) == 1;
    }
    public static MapLocation parseLocation(int n) {
        return new MapLocation(n & 0b111111, (n >> 6) & 0b111111);
    }
    public static int intifyLocation(MapLocation loc) {
        return 0b1000000000000 | (loc.y << 6) | loc.x;
    }
    public static ResourceType getWellType(int n) {
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

    public static int[] parseGameState(int n) {
        currentState[PRIORITIZED_RESOURCE] = n & 0b11; // bits 0-1
        currentState[CONVERT_WELL] = n >> 2 & 0b1; // bit 2
        currentState[UPGRADE_WELLS] = n >> 2 & 0b1; // bit 2
        currentState[ELIXIR_HQ] = n >> 4 & 0b11; // bits 4-5
        currentState[CONVERSION_WELL_ID] = n >> 6 & 0b11; // bits 6-7
        return currentState;
    }
    public static ResourceType prioritizedResource() {
        return resourceTypes[currentState[PRIORITIZED_RESOURCE]];
    }
    public static boolean convertWell() {
        return currentState[CONVERT_WELL] == 1;
    }
    public static int conversionWellId() {
        return currentState[CONVERSION_WELL_ID];
    }
    public static boolean upgradeWells() {
        return currentState[UPGRADE_WELLS] == 1;
    }
}

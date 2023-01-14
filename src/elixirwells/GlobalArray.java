package elixirwells;

import java.util.Arrays;

import battlecode.common.*;
import battlecode.world.Inventory;

public strictfp class GlobalArray {
    public static final int PRIORITIZED_RESOURCE = 0;
    public static final int CONVERT_WELL = 1;
    public static final int UPGRADE_WELLS = 2;
    public static final int ELIXIR_HQ = 3;
    public static final int CONVERSION_WELL_ID = 4;

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
    public static WellInfo[] getKnownWells(RobotController rc) throws GameActionException {
        WellInfo[] wells = new WellInfo[8];
        int wellCount = 0;
        int j = 0;
        for (int i = 0; i < 8; i++) {
            int arrayWell = rc.readSharedArray(i+5);
            if (hasLocation(arrayWell)) {
                wellCount++;
                wells[j++] = new WellInfo(parseLocation(arrayWell), wellType(arrayWell), new Inventory(), isUpgradedWell(arrayWell));
            }
        }
        return Arrays.copyOf(wells, wellCount);
    }
    public static int intifyHeadQuarters(RobotController rc) throws GameActionException {
        if (rc.getType() != RobotType.HEADQUARTERS) throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Cannot intify " + rc.getType() + " as HeadQuarters");
        int adamantiumAmount = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        return ((adamantiumAmount == 0 ? 0 : ((int) (((double) rc.getResourceAmount(ResourceType.MANA) / adamantiumAmount) * 8))) << 13) | intifyLocation(rc.getLocation());
    }
    public static int manaAdamantiumRatio(int n) {
        return n >> 13;
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
    public int[] parseGameState(int n) {
        currentState[PRIORITIZED_RESOURCE] = n & 0b11; // bits 0-1
        currentState[CONVERT_WELL] = n >> 2 & 0b1; // bit 2
        currentState[UPGRADE_WELLS] = n >> 3 & 0b1; // bit 3
        currentState[ELIXIR_HQ] = n >> 4 & 0b11; // bits 4-5
        currentState[CONVERSION_WELL_ID] = n >> 6 & 0b1111; // bits 6-9
        return currentState;
    }
    public ResourceType prioritizedResource() {
        return resourceTypes[currentState[PRIORITIZED_RESOURCE]];
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

    public int getGameStateNumber() {
        return currentState[PRIORITIZED_RESOURCE] | currentState[CONVERT_WELL] << 2 | currentState[UPGRADE_WELLS] << 3 | currentState[ELIXIR_HQ] << 4 | currentState[CONVERSION_WELL_ID] << 6;
    }
    public boolean changedState() {
        return changedState;
    }
    public void setPrioritizedResource(ResourceType resource) {
        changedState = true;
        currentState[PRIORITIZED_RESOURCE] = resource.resourceID;
    }
    public void setUpgradeWells(boolean set) {
        currentState[UPGRADE_WELLS] = set ? 1 : 0;
    }
    public void setTargetElixirWell(RobotController rc, WellInfo well, int headquarterIndex) throws GameActionException {
        MapLocation loc = well.getMapLocation();
        for (int i = 0; i < 8; i++) {
            int arrayWell = rc.readSharedArray(i+5);
            if (hasLocation(arrayWell) && parseLocation(arrayWell).equals(loc)) {
                currentState[CONVERSION_WELL_ID] = i;
                currentState[ELIXIR_HQ] = headquarterIndex;
                return;
            }
        }
        throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Cannot set target elixir conversion well");
    }
}

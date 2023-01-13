package wellcommtest;

import java.util.HashMap;

import battlecode.common.*;

public strictfp class GameState {
    public static final int PRIORITIZED_RESOURCE = 0;
    public static final int CONVERT_WELL = 1;
    public static final int UPGRADE_WELLS = 2;
    public static final int ELIXIR_HQ = 3;
    public static final int CONVERSION_WELL_ID = 4;

    private final ResourceType[] resourceTypes = new ResourceType[4];
    private final int[] currentState = new int[5];

    public GameState() {
        for (ResourceType r : ResourceType.values()) {
            resourceTypes[r.resourceID] = r;
        }
    }

    /*
     * Bits 0-5     x coordinate
     * Bits 6-11    y coordinate
     * Bit 12       presence marker
     * Bits 13-14   resource type (none is headquarters)
     * Bit 15       upgraded well
     */

    public boolean hasLocation(int n) {
        return (n >> 12 & 0b1) == 1;
    }
    public MapLocation parseLocation(int n) {
        return new MapLocation(n & 0b111111, (n >> 6) & 0b111111);
    }
    public int intifyLocation(MapLocation loc) {
        return 0b1000000000000 | (loc.y << 6) | loc.x;
    }
    public ResourceType getWellType(int n) {
        return resourceTypes[n >> 13 & 0b11];
    }
    public boolean isUpgradedWell(int n) {
        return n >> 15 == 1;
    }
    public boolean storeWell(RobotController rc, WellInfo well) {
        if (rc.canWriteSharedArray(0, 0)) {
            try {
                int resType = well.getResourceType().resourceID;
                MapLocation me = rc.getLocation();
                MapLocation loc = well.getMapLocation();
                int[] wells = new int[8];
                for (int i = 0; i < 8; i++) {
                    wells[i] = rc.readSharedArray(i+5);
                    if (!hasLocation(wells[i])) {
                        rc.writeSharedArray(i+5, ((well.isUpgraded() ? 1 : 0) << 15) | (resType << 13) | intifyLocation(loc));
                        return true;
                    }
                }
                int overwrite = -1;
                int furthest = 0;
                for (int i = 0; i < 8; i++) {
                    int dist = me.distanceSquaredTo(loc);
                    if (dist > furthest) {
                        furthest = dist;
                        overwrite = i+5;
                    }
                }
                if (overwrite > 0) {
                    rc.writeSharedArray(overwrite, ((well.isUpgraded() ? 1 : 0) << 15) | (resType << 13) | intifyLocation(loc));
                    return true;
                }
                return false;
            } catch (GameActionException e) {
                System.out.println("GameActionException at GameState.storeWell");
                e.printStackTrace();
            }
        }
        return false;
    }

    public int[] parseGameState(int n) {
        currentState[PRIORITIZED_RESOURCE] = n & 0b11; // bits 0-1
        currentState[CONVERT_WELL] = n >> 2 & 0b1; // bit 2
        currentState[UPGRADE_WELLS] = n >> 2 & 0b1; // bit 2
        currentState[ELIXIR_HQ] = n >> 4 & 0b11; // bits 4-5
        currentState[CONVERSION_WELL_ID] = n >> 6 & 0b11; // bits 6-7
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
}

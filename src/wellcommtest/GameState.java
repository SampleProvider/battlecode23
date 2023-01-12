package wellcommtest;

import java.util.HashMap;

import battlecode.common.*;

public strictfp class GameState {
    public static final int PRIORITIZED_RESOURCE = 0;
    public static final int CONVERT_WELL = 1;
    public static final int ELIXIR_HQ = 2;
    public static final int CONVERSION_WELL_ID = 3;

    private final ResourceType[] resourceTypes = new ResourceType[4];
    private final int[] currentState = new int[4];

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
     */

    public boolean hasLocation(int n) {
        return (n >> 12 & 0b1) == 1;
    }
    public ResourceType wellType(int n) {
        return resourceTypes[n >> 13 & 0b11];
    }
    public MapLocation parseLocation(int n) {
        return new MapLocation(n & 0b111111, (n >> 6) & 0b111111);
    }
    public int intifyLocation(MapLocation loc) {
        return 0b1000000000000 | (loc.y << 6) | loc.x;
    }

    public int[] parseGameState(int n) {
        currentState[PRIORITIZED_RESOURCE] = n & 0b11; // bits 0-1
        currentState[CONVERT_WELL] = n >> 2 & 0b1; // bit 2
        currentState[ELIXIR_HQ] = n >> 3 & 0b11; // bits 3-4
        currentState[CONVERSION_WELL_ID] = n >> 5 & 0b11; // bits 5-7
        return currentState;
    }
    public ResourceType prioritizedResource() {
        return resourceTypes[currentState[PRIORITIZED_RESOURCE]];
    }
    public boolean convertWell() {
        return currentState[CONVERT_WELL] == 1;
    }
    public int conversionWellId() {
        // store 2 closest mana wells and 2 closest adamantium wells
        return currentState[CONVERSION_WELL_ID];
    }
}

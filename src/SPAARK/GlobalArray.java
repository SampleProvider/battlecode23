package SPAARK;

import java.util.Arrays;

import battlecode.common.*;

public strictfp class GlobalArray {
    public static final int GAMESTATE = 0;
    public static final int HEADQUARTERS = 1;
    public static final int HEADQUARTERS_LENGTH = 4;
    public static final int MANA_WELLS = 5;
    public static final int MANA_WELLS_LENGTH = 6;
    public static final int ADAMANTIUM_WELLS = 12;
    public static final int ADAMANTIUM_WELLS_LENGTH = 6;
    public static final int AMPLIFIERS = 18;
    public static final int AMPLIFIERS_LENGTH = 4;
    public static final int OPPONENTS = 21;
    public static final int OPPONENTS_LENGTH = 4;

    public static final int PRIORITIZED_RESOURCE_HQ1 = 0;
    public static final int PRIORITIZED_RESOURCE_HQ2 = 1;
    public static final int PRIORITIZED_RESOURCE_HQ3 = 2;
    public static final int PRIORITIZED_RESOURCE_HQ4 = 3;
    public static final int CONVERT_WELL = 4;
    public static final int UPGRADE_WELLS = 5;
    public static final int ELIXIR_HQ_ID = 6;
    public static final int CONVERSION_WELL_ID = 7;

    private static final ResourceType[] resourceTypes = new ResourceType[] {ResourceType.NO_RESOURCE, ResourceType.ADAMANTIUM, ResourceType.MANA, ResourceType.ELIXIR};
    private static final int adequateManaThreshold = 15;
    private static final int adequateAdamantiumThreshold = 24;
    
    private final int[] currentState = new int[8];

    /*
     * Bits 0-5     x coordinate
     * Bits 6-11    y coordinate
     * Bit 12       presence marker
     * Bits 13-14   resource type (none is headquarters)
     * Bit 15       upgraded well
     */

    // general location/data parsing/writing
    public static boolean hasLocation(int n) {
        return (n >> 12 & 0b1) == 1;
    }
    public static MapLocation parseLocation(int n) {
        return new MapLocation(n & 0b111111, (n >> 6) & 0b111111);
    }
    public static int intifyLocation(MapLocation loc) {
        return 0b1000000000000 | (loc.y << 6) | loc.x;
    }
    
    // headquarters
    public static int resourceRatio(int n) {
        return n >> 14;
    }
    public static boolean adequateResources(int n) {
        return ((n >> 13) & 0b1) == 1;
    }
    public static void storeHeadquarters(HeadQuarters hq) throws GameActionException {
        int ratio = (hq.adamantium == 0 ? 0 : Math.min((int) ((hq.mana / (hq.adamantium*1.2) * 3) + 1), 3));
        int adequateResources = (((hq.adamantium - hq.lastAdamantium) > adequateAdamantiumThreshold && (hq.mana - hq.lastMana) > adequateManaThreshold) ? 1 : 0);
        hq.rc.writeSharedArray(hq.hqIndex, (ratio << 14) | (adequateResources << 13) | intifyLocation(hq.me));
    }
    public static MapLocation[] getKnownHeadQuarterLocations(RobotController rc) throws GameActionException {
        MapLocation[] headquarters = new MapLocation[HEADQUARTERS_LENGTH];
        int hqCount = 0;
        for (int i = HEADQUARTERS; i < HEADQUARTERS + HEADQUARTERS_LENGTH; i++) {
            int arrayHQ = rc.readSharedArray(i);
            if (hasLocation(arrayHQ)) {
                headquarters[i - HEADQUARTERS] = parseLocation(arrayHQ);
                hqCount++;
            }
        }
        return Arrays.copyOf(headquarters, hqCount);
    }

    // wells
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
                for (int i = MANA_WELLS; i < MANA_WELLS + MANA_WELLS_LENGTH; i++) {
                    int arrayWell = rc.readSharedArray(i);
                    if (!hasLocation(arrayWell) || parseLocation(arrayWell).equals(loc)) {
                        rc.writeSharedArray(i, ((well.isUpgraded() ? 1 : 0) << 15) | (resType << 13) | intifyLocation(well.getMapLocation()));
                        return true;
                    }
                }
            } else if (resType == 1) {
                for (int i = ADAMANTIUM_WELLS; i < ADAMANTIUM_WELLS + ADAMANTIUM_WELLS_LENGTH; i++) {
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
        MapLocation[] wells = new MapLocation[MANA_WELLS_LENGTH + ADAMANTIUM_WELLS_LENGTH];
        for (int i = MANA_WELLS; i < MANA_WELLS + MANA_WELLS_LENGTH + ADAMANTIUM_WELLS_LENGTH; i++) {
            int arrayWell = rc.readSharedArray(i);
            if (hasLocation(arrayWell)) {
                wells[i - MANA_WELLS] = parseLocation(arrayWell);
            }
        }
        return wells;
    }
    
    // opponents
    public static boolean storeOpponentLocation(RobotController rc, MapLocation opponentLocation) throws GameActionException {
        if (rc.canWriteSharedArray(0, 0)) {
            for (int i = OPPONENTS; i < OPPONENTS + OPPONENTS_LENGTH; i++) {
                int arrayOpponentLocation = rc.readSharedArray(i);
                if (!hasLocation(arrayOpponentLocation) || parseLocation(arrayOpponentLocation).equals(opponentLocation)) {
                    rc.writeSharedArray(i, intifyLocation(opponentLocation));
                    return true;
                }
            }
            return false;
        }
        return false;
    }
    public static MapLocation[] getKnownOpponentLocations(RobotController rc) throws GameActionException {
        MapLocation[] opponentLocations = new MapLocation[OPPONENTS_LENGTH];
        for (int i = OPPONENTS; i < OPPONENTS + OPPONENTS_LENGTH; i++) {
            int arrayOpponentLocation = rc.readSharedArray(i);
            if (hasLocation(arrayOpponentLocation)) {
                opponentLocations[i - OPPONENTS] = parseLocation(arrayOpponentLocation);
            }
        }
        return opponentLocations;
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
        currentState[PRIORITIZED_RESOURCE_HQ1] = n & 0b11; // bits 0-1
        currentState[CONVERT_WELL] = n >> 2 & 0b1; // bit 2
        currentState[UPGRADE_WELLS] = n >> 3 & 0b1; // bit 3
        currentState[ELIXIR_HQ_ID] = n >> 4 & 0b11; // bits 4-5
        currentState[CONVERSION_WELL_ID] = n >> 6 & 0b111; // bits 6-8
        return currentState;
    }
    public ResourceType prioritizedResource() {
        return resourceTypes[currentState[PRIORITIZED_RESOURCE_HQ1]];
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
        return (currentState[PRIORITIZED_RESOURCE_HQ1]) | (currentState[CONVERT_WELL] << 2) | (currentState[UPGRADE_WELLS] << 3) | (currentState[ELIXIR_HQ_ID] << 4) |( currentState[CONVERSION_WELL_ID] << 6);
    }
    public void setPrioritizedResource(ResourceType resource) {
        currentState[PRIORITIZED_RESOURCE_HQ1] = resource.resourceID;
    }
    public void setUpgradeWells(boolean set) {
        currentState[UPGRADE_WELLS] = set ? 1 : 0;
    }
    public void setTargetElixirWellHQPair(int wellIndex, int hqIndex) {
        currentState[CONVERSION_WELL_ID] = wellIndex;
        currentState[ELIXIR_HQ_ID] = hqIndex;
    }

    // bit operations
    public static int toggleBit(int n, int pos) {
        return n ^ (1 << pos);
    }
    public static int setBit(int n, int pos, int m) {
        return n & ~(1 << pos) | (m << pos);
    }
}

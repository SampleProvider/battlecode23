package betterspawning;

import java.util.Arrays;

import battlecode.common.*;

public strictfp class GlobalArray {
    public static final int GAMESTATE = 0;
    public static final int HEADQUARTERS = 1;
    public static final int HEADQUARTERS_LENGTH = 4;
    public static final int MANA_WELLS = 5;
    public static final int MANA_WELLS_LENGTH = 6;
    public static final int ADAMANTIUM_WELLS = 11;
    public static final int ADAMANTIUM_WELLS_LENGTH = 6;
    public static final int AMPLIFIERS = 17;
    public static final int AMPLIFIERS_LENGTH = 6;
    public static final int OPPONENTS = 23;
    public static final int OPPONENTS_LENGTH = 8;
    public static final int CARRIERCOUNT = 31;
    public static final int LAUNCHERCOUNT = 32;

    public static final int PRIORITIZED_RESOURCE_HQ1 = 0;
    public static final int PRIORITIZED_RESOURCE_HQ2 = 1;
    public static final int PRIORITIZED_RESOURCE_HQ3 = 2;
    public static final int PRIORITIZED_RESOURCE_HQ4 = 3;
    public static final int CONVERT_WELL = 4;
    public static final int UPGRADE_WELLS = 5;
    public static final int ELIXIR_HQ_ID = 6;
    public static final int CONVERSION_WELL_ID = 7;

    private static final ResourceType[] resourceTypes = new ResourceType[] {ResourceType.NO_RESOURCE, ResourceType.ADAMANTIUM, ResourceType.MANA, ResourceType.ELIXIR};
    
    private final int[] currentState = new int[8];

    /*
     * Bits 0-5     x coordinate
     * Bits 6-11    y coordinate
     * Bit 12       presence marker
     * Wells:
     *  Bits 13-14  resource type
     *  Bit 15      upgraded marker
     * Headquarters:
     *  Bit 13      adequate materials
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
    public static boolean adequateResources(int n) {
        return ((n >> 13) & 0b1) == 1;
    }
    public static void storeHeadquarters(HeadQuarters hq) throws GameActionException {
        int adequateResources = (((hq.adamantium - hq.lastAdamantium) >= 0 && (hq.mana - hq.lastMana) >= 0) ? 1 : 0);
        hq.rc.writeSharedArray(hq.hqIndex, (adequateResources << 13) | intifyLocation(hq.me));
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
     * Bits 0-1     prioritized resource hq 1
     * Bits 2-3     prioritized resource hq 2
     * Bits 4-5     prioritized resource hq 3
     * Bits 6-7     prioritized resource hq 4
     * Bit 8        convert well
     * Bit 9        upgrade wells
     * Bits 10-11   id of elixir hq (where to drop elixir)
     * bits 12-13   id of well to convert to elixir
     */

    // read game state
    public int[] parseGameState(int n) {
        currentState[PRIORITIZED_RESOURCE_HQ1] = (n) & 0b11; // bits 0-1
        currentState[PRIORITIZED_RESOURCE_HQ2] = (n >> 2) & 0b11; // bits 2-3
        currentState[PRIORITIZED_RESOURCE_HQ3] = (n >> 4) & 0b11; // bits 4-5
        currentState[PRIORITIZED_RESOURCE_HQ4] = (n >> 6) & 0b11; // bits 6-7
        currentState[CONVERT_WELL] = (n >> 8) & 0b1; // bit 8
        currentState[UPGRADE_WELLS] = (n >> 9) & 0b1; // bit 9
        currentState[ELIXIR_HQ_ID] = (n >> 10) & 0b11; // bits 10-11
        currentState[CONVERSION_WELL_ID] = (n >> 12) & 0b11; // bits 12-13
        return currentState;
    }
    public ResourceType prioritizedResource(int hqIndex) {
        return resourceTypes[currentState[hqIndex + HEADQUARTERS]];
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
        return (currentState[PRIORITIZED_RESOURCE_HQ1])
            | (currentState[PRIORITIZED_RESOURCE_HQ2] << 2)
            | (currentState[PRIORITIZED_RESOURCE_HQ3] << 4)
            | (currentState[PRIORITIZED_RESOURCE_HQ4] << 6)
            | (currentState[CONVERT_WELL] << 8)
            | (currentState[UPGRADE_WELLS] << 9)
            | (currentState[ELIXIR_HQ_ID] << 10)
            | (currentState[CONVERSION_WELL_ID] << 12);
    }
    public void setPrioritizedResource(ResourceType resource, int hqIndex) {
        currentState[hqIndex - HEADQUARTERS] = resource.resourceID;
    }
    public void setTargetElixirWellHQPair(int wellIndex, int hqIndex) {
        currentState[CONVERT_WELL] = 1;
        currentState[CONVERSION_WELL_ID] = wellIndex;
        currentState[ELIXIR_HQ_ID] = hqIndex;
    }
    public void setUpgradeWells(boolean set) {
        currentState[UPGRADE_WELLS] = set ? 1 : 0;
    }

    // bit operations
    public static int toggleBit(int n, int pos) {
        return n ^ (1 << pos);
    }
    public static int setBit(int n, int pos, int m) {
        return n & ~(1 << pos) | (m << pos);
    }
}

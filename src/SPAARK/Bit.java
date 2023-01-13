package SPAARK;

import battlecode.common.*;

public class Bit {
    public static int toggleBit(int n, int pos) throws GameActionException {
        return n ^ 1 << pos;
    }
    public static int setBit(int n, int pos, int m) throws GameActionException {
        if (m == 0) {
            return n &= ~(1 << pos);
        }
        else {
            return n |= 1 << pos;
        }
    }
}

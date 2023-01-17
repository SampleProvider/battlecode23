package SPAARK_1_15_2023;

import battlecode.common.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) {
        switch (rc.getType()) {
            case HEADQUARTERS:
                new HeadQuarters(rc);
                break;
            case CARRIER:
                new Carrier(rc);
                break;
            case LAUNCHER:
                new Launcher(rc);
                break;
            case AMPLIFIER:
                new Amplifier(rc);
                break;
            case BOOSTER:
                new Booster(rc);
                break;
            case DESTABILIZER:
                new Destabilizer(rc);
                break;
        }
    }
}
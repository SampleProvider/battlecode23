package template;

import battlecode.common.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) {
        System.out.println("Robot " + rc.getType() + " is hp " + rc.getHealth());

        rc.setIndicatorString("Initializing");
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
/*
Shared array:
States           0: [UNUSED]
HQ 1 location    1: 0-5 x location, 6-11 y location, 12 existence of HQ
HQ 2 location    2: 0-5 x location, 6-11 y location, 12 existence of HQ
HQ 2 location    3: 0-5 x location, 6-11 y location, 12 existence of HQ
HQ 4 location    4: 0-5 x location, 6-11 y location, 12 existence of HQ
*/
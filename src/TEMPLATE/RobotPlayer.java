package TEMPLATE;

import battlecode.common.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) {
        System.out.println("Robot " + rc.getType() + " is hp " + rc.getHealth());

        rc.setIndicatorString("Initializing");
        switch (rc.getType()) {
            case HEADQUARTERS:
                HeadQuarters.init(rc);
                HeadQuarters.run(rc);
                break;
            case CARRIER:
                Carrier.init(rc);
                Carrier.run(rc);
                break;
            case LAUNCHER:
                Launcher.init(rc);
                Launcher.run(rc);
                break;
            case AMPLIFIER:
                Amplifier.init(rc);
                Amplifier.run(rc);
                break;
            case BOOSTER:
                Booster.init(rc);
                Booster.run(rc);
                break;
            case DESTABILIZER:
                Destabilizer.init(rc);
                Destabilizer.run(rc);
                break;
        }
    }
}

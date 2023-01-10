package TEMPLATE;

import battlecode.common.*;

public class RobotPlayer {
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        System.out.println("Robot " + rc.getType() + " is hp " + rc.getHealth());

        rc.setIndicatorString("Initializing");
        switch (rc.getType()) {
            case HEADQUARTERS:
                HeadQuarters.run(rc);
                break;
            case CARRIER:
                Carrier.run(rc);
                break;
            case LAUNCHER:
                Launcher.run(rc);
                break;
            case AMPLIFIER:
                Amplifier.run(rc);
                break;
            case BOOSTER:
                Booster.run(rc);
                break;
            case DESTABILIZER:
                Destabilizer.run(rc);
                break;
        }
    }
}

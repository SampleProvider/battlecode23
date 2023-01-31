package launcherCoagulation;

import battlecode.common.*;

public strictfp class FooBar {
    public static final boolean foobar = false;
    public static void foo(RobotController rc) throws GameActionException {
        rc.resign();
        throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "FOO");
    }
    public static void bar(RobotController rc) throws GameActionException {
        rc.disintegrate();
        throw new GameActionException(GameActionExceptionType.IS_NOT_READY, "BAR");
    }
}

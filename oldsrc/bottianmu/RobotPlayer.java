package bottianmu;
import battlecode.common.*;
import java.util.Random;
public strictfp class RobotPlayer {
    private static final Random rng = new Random(20123);
    public static void run(RobotController a){
        if (rng.nextBoolean()) {
            Clock.yield();
        }
        while (true) {
            for(int i = 0;i < a.getMapWidth();i++) {
                for(int j = 0;j < a.getMapHeight();j++) {
                    a.setIndicatorDot(new MapLocation(i,j),255 - i*255/a.getMapWidth(),j*255/a.getMapHeight(),Math.abs(i-j)*255/a.getMapWidth());
                }
            }
            Clock.yield();
        }
}}
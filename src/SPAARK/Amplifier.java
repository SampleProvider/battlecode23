package SPAARK;

import battlecode.common.*;

public strictfp class Amplifier {
    private RobotController rc;

    static int turnCount = 0;
    private int ID;

    public Amplifier(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
            if (rc.readSharedArray(8) == 0) {
                ID = 8;
            }
            else if (rc.readSharedArray(9) == 0) {
                ID = 9;
            }
            else if (rc.readSharedArray(10) == 0) {
                ID = 10;
            }
            else {
                ID = 11;
            }
        } catch (Exception e) {
            System.out.println("Exception at Amplifier constructor");
            e.printStackTrace();
        }
        finally {
            Clock.yield();
        }
        run();
    }
    public void run() {
        while (true) {
            try {
                turnCount++;
                MapLocation position = rc.getLocation();
                rc.writeSharedArray(ID,  0b1000000000000 | (position.y << 6) | position.x);
                RobotInfo[] teamRobots = rc.senseNearbyRobots(-1, rc.getTeam());
                RobotInfo[] otherTeamRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                MapLocation carrierLocation = null;
                for (RobotInfo opponentRobot : otherTeamRobots) {
                    if (opponentRobot.getType() == RobotType.CARRIER) {
                        carrierLocation = opponentRobot.getLocation();
                    }
                }

                Direction[] pathToCarrier = BFS_Not_Hardcoded.run(rc, rc.senseNearbyMapInfos(), carrierLocation);
                for (Direction d : pathToCarrier) {
                    rc.move(d);
                }

            } catch (Exception e) {
                System.out.println("Exception at Amplifier");
                e.printStackTrace();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
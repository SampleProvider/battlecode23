package SPAARK;

import battlecode.common.*;

public strictfp class Amplifier {
    private RobotController rc;

    static int turnCount = 0;
    private int ID = 0;

    public Amplifier(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
            rc.writeSharedArray(14, 0b1111111111111111); //Indicates no target, should be only one signal amplifier at a time (for now)
        } catch (GameActionException e) {
            System.out.println("GameActionException at Amplifier constructor");
            e.printStackTrace();
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
                RobotInfo[] teamRobots = rc.senseNearbyRobots(-1, rc.getTeam());
                RobotInfo[] otherTeamRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                MapLocation carrierLocation = null;
                MapLocation toWrite = null; //Sometimes a write is unnecessary
                for (RobotInfo opponentRobot : otherTeamRobots) {
                    if (opponentRobot.getType() == RobotType.CARRIER) {
                        carrierLocation = opponentRobot.getLocation();
                        toWrite = carrierLocation;
                        break;
                    }
                }

                if (toWrite) {
                    //Write before running expensive BFS
                    rc.writeSharedArray(14,  0b1000000000000 | (toWrite.y << 6) | toWrite.x);
                }

                Direction[] pathToCarrier = BFS_Not_Hardcoded.run(rc, rc.senseNearbyMapInfos(), carrierLocation);
                for (Direction d : pathToCarrier) {
                    rc.move(d);
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException at Amplifier");
                e.printStackTrace();
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
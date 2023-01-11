package template;

import battlecode.common.*;

public strictfp class Booster {
    private RobotController rc;

    static int turnCount = 0;

    public Booster(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
            throw new GameActionException(null, null);
        } catch (GameActionException e) {
            System.out.println("GameActionException at Booster constructor");
            e.printStackTrace();
        }
        catch (Exception e) {
            System.out.println("Exception at Booster constructor");
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
                // code
                turnCount++;
                throw new GameActionException(null, null);
            } catch (GameActionException e) {
                System.out.println("GameActionException at Booster");
                e.printStackTrace();
            }
            catch (Exception e) {
                System.out.println("Exception at Booster");
                e.printStackTrace();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
package template;

import battlecode.common.*;

public strictfp class Launcher {
    RobotController rc;

    private int turnCount = 0;

    public Launcher(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
            throw new GameActionException(null, null);
        } catch (GameActionException e) {
            System.out.println("GameActionException at Launcher constructor");
            e.printStackTrace();
        }
        catch (Exception e) {
            System.out.println("Exception at Launcher constructor");
            e.printStackTrace();
        }
        finally {
            Clock.yield();
        }
        run();
    }
    private void run() {
        while (true) {
            try {
                // code
                turnCount++;
                throw new GameActionException(null, null);
            } catch (GameActionException e) {
                System.out.println("GameActionException at Launcher");
                e.printStackTrace();
            }
            catch (Exception e) {
                System.out.println("Exception at Launcher");
                e.printStackTrace();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
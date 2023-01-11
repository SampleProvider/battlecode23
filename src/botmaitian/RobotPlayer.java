package botmaitian;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Queue;
import java.lang.StringBuilder;

public strictfp class RobotPlayer {
    static int turnCount = 0;
    static final Random rng = new Random(2023);
    static final Direction[] directions = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
    };
    static MapInfo[] mapInfo;
    static WellInfo[] wellInfo;
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

        while(true){
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()) {
                    case HEADQUARTERS:     runHeadquarters(rc);  break;
                    case CARRIER:      runCarrier(rc);   break;
                    case LAUNCHER: runLauncher(rc); break;
                    case BOOSTER: // Examplefuncsplayer doesn't use any of these robot types below.
                    case DESTABILIZER: // You might want to give them a try!
                    case AMPLIFIER:       break;
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }
    static void runHeadquarters(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        // if(wellInfo.length > 1 && rng.nextInt(3) == 1){
        //     WellInfo well_one = wellInfo[1];
        //     Direction dir = me.directionTo(well_one.getMapLocation());
        //     if (rc.canMove(dir)) 
        //         rc.move(dir);
        // }
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation newLoc = rc.getLocation().add(dir);
        if (rc.canBuildAnchor(Anchor.STANDARD)) {
            // If we can build an anchor do it!
            rc.buildAnchor(Anchor.STANDARD);
            rc.setIndicatorString("Building anchor! " + rc.getAnchor());
        }
        if (rng.nextBoolean()) {
            // Let's try to build a carrier.
            rc.setIndicatorString("Trying to build a carrier");
            if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                rc.buildRobot(RobotType.CARRIER, newLoc);
            }
        } else {
            // Let's try to build a launcher.
            rc.setIndicatorString("Trying to build a launcher");
            if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                rc.buildRobot(RobotType.LAUNCHER, newLoc);
            }
        }
    }
    static void runCarrier(RobotController rc) throws GameActionException {
        if (rc.getAnchor() != null) {
            // If I have an anchor singularly focus on getting it to the first island I see
            int[] islands = rc.senseNearbyIslands();
            Set<MapLocation> islandLocs = new HashSet<>();
            for (int id : islands) {
                MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
                islandLocs.addAll(Arrays.asList(thisIslandLocs));
            }
            if (islandLocs.size() > 0) {
                MapLocation islandLocation = islandLocs.iterator().next();
                rc.setIndicatorString("Moving my anchor towards " + islandLocation);
                while (!rc.getLocation().equals(islandLocation)) {
                    Direction dir = rc.getLocation().directionTo(islandLocation);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                }
                if (rc.canPlaceAnchor()) {
                    rc.setIndicatorString("Huzzah, placed anchor!");
                    rc.placeAnchor();
                }
            }
        }
        // Try to gather from squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation wellLocation = new MapLocation(me.x + dx, me.y + dy);
                if (rc.canCollectResource(wellLocation, -1)) {
                    if (rng.nextBoolean()) {
                        rc.collectResource(wellLocation, -1);
                        rc.setIndicatorString("Collecting, now have, AD:" + 
                            rc.getResourceAmount(ResourceType.ADAMANTIUM) + 
                            " MN: " + rc.getResourceAmount(ResourceType.MANA) + 
                            " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
                    }
                }
            }
        }
        // Occasionally try out the carriers attack
        if(rng.nextInt(20) == 1){
            RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (enemyRobots.length > 0) {
                if (rc.canAttack(enemyRobots[0].location)) {
                    rc.attack(enemyRobots[0].location);
                }
            }
        }
        wellBFS(rc);
        // If we can see a well, move towards it
        // WellInfo[] wells = rc.senseNearbyWells();
        // if (wells.length > 1 && rng.nextInt(3) == 1){
        //     WellInfo well_one = wells[1];
        //     Direction dir = me.directionTo(well_one.getMapLocation());
        //     if (rc.canMove(dir)) 
        //         rc.move(dir);
        // }
        // Also try to move randomly.
        // Direction dir = directions[rng.nextInt(directions.length)];
        // if(rc.canMove(dir)){
        //     rc.move(dir);
        // }
    }
    static void runLauncher(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length >= 0) {
            // MapLocation toAttack = enemies[0].location;
            MapLocation toAttack = rc.getLocation().add(Direction.EAST);

            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");        
                rc.attack(toAttack);
            }
        }

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
    static void BFS(RobotController rc) throws GameActionException {
        mapInfo = rc.senseNearbyMapInfos();
        wellInfo = rc.senseNearbyWells();
        int visionRadius = (int) Math.sqrt(rc.getType().visionRadiusSquared);
        int visionDiameter = visionRadius * 2 + 1;
        int[][] range = new int[visionDiameter][visionDiameter];
        Queue<Integer> queue = new LinkedList<Integer>();
        queue.add(visionRadius * visionDiameter + visionRadius);
        while(queue.size() > 0){
            int c = queue.poll();
            int x = c % visionDiameter;
            int y = (int) c / visionDiameter;
            for(int dy = -1;dy <= 1;dy++){
                for(int dx = -1;dx <= 1;dx++){
                    if(dx == 0 && dy == 0){
                        continue;
                    }
                    if(Math.pow(x + dx - visionRadius,2) + Math.pow(y + dy - visionRadius,2) > rc.getType().visionRadiusSquared){
                        return;
                    }
                    if(range[x + dx][y + dy] == 0){
                        range[x + dx][y + dy] = range[x][y] + 1;
                        queue.add(x + dx + visionDiameter * (y + dy));
                    }
                }
            }
        }
        for(WellInfo well : wellInfo){
            
        }
    }
    static void wellBFS(RobotController rc) throws GameActionException {
        mapInfo = rc.senseNearbyMapInfos();
        wellInfo = rc.senseNearbyWells();
        if(wellInfo.length > 0){
            MapLocation me = rc.getLocation();
            MapLocation dest = wellInfo[0].getMapLocation();
            int visionRadius = (int) Math.sqrt(rc.getType().visionRadiusSquared);
            int visionDiameter = visionRadius * 2 + 1;
            int[][] range = new int[visionDiameter][visionDiameter];
            // int[][] map = new int[visionDiameter][visionDiameter];
            // for(MapInfo m : mapInfo){
            //     MapLocation mapLocation = m.getMapLocation();
            //     if(m.)
            //     map[mapLocation.x - me.x][mapLocation.y - me.y]
            // }
            Queue<Integer> queue = new LinkedList<Integer>();
            queue.add(dest.x - me.x + visionRadius + visionDiameter * (dest.y - me.y + visionRadius));
            while(queue.size() > 0){
                int c = queue.poll();
                int x = c % visionDiameter;
                int y = (int) c / visionDiameter;
                for(int dy = -1;dy <= 1;dy++){
                    for(int dx = -1;dx <= 1;dx++){
                        if(dx == 0 && dy == 0){
                            continue;
                        }
                        if(Math.pow(x + dx - visionRadius,2) + Math.pow(y + dy - visionRadius,2) > rc.getType().visionRadiusSquared){
                            continue;
                        }
                        // try{
                            if(rc.sensePassability(new MapLocation(me.x + x + dx - visionRadius,me.y + y + dy - visionRadius)) == false){
                                continue;
                            }
                        // }
                        // catch(Exception error){
                        //     continue;
                        // }
                        if(range[x + dx][y + dy] == 0){
                            range[x + dx][y + dy] = range[x][y] + 1;
                            queue.add(x + dx + visionDiameter * (y + dy));
                            if(x + dx == visionRadius && y + dy == visionRadius){
                                int direction = -dx - dy * 3 + 4;
                                if(direction > 4){
                                    direction -= 1;
                                }
                                if(rc.canMove(directions[direction])){
                                    rc.move(directions[direction]);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

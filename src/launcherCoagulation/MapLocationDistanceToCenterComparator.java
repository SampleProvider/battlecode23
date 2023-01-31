package launcherCoagulation;

import battlecode.common.*;

import java.util.Comparator;

public class MapLocationDistanceToCenterComparator implements Comparator<MapLocation> {
    private MapLocation center;

    public void setCenter(MapLocation center) {
        this.center = center;
    }

    public int compare(MapLocation a, MapLocation b) throws IllegalArgumentException {
        if (center == null) throw new IllegalArgumentException("\"center\" must be set.");
        return a.distanceSquaredTo(center) - b.distanceSquaredTo(center);
    }
}
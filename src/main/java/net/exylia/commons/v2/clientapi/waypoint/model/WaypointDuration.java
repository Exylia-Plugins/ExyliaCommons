package net.exylia.commons.v2.clientapi.waypoint.model;

public final class WaypointDuration {

    private static final WaypointDuration PERMANENT = new WaypointDuration(0);

    private final int seconds;

    private WaypointDuration(int seconds) {
        this.seconds = seconds;
    }

    public static WaypointDuration permanent() {
        return PERMANENT;
    }

    public static WaypointDuration of(int seconds) {
        if (seconds <= 0) return PERMANENT;
        return new WaypointDuration(seconds);
    }

    public int getSeconds() { return seconds; }

    public boolean isPermanent() { return seconds <= 0; }
}

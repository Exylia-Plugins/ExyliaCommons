package net.exylia.commons.v2.clientapi.waypoint.model;

public final class WaypointColor {

    private static final WaypointColor CHROMA = new WaypointColor(255, 255, 255, 255, true);

    private final int r, g, b, alpha;
    private final boolean chroma;

    private WaypointColor(int r, int g, int b, int alpha, boolean chroma) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.alpha = alpha;
        this.chroma = chroma;
    }

    public static WaypointColor of(int r, int g, int b) {
        return new WaypointColor(r, g, b, 255, false);
    }

    public static WaypointColor of(int r, int g, int b, int alpha) {
        return new WaypointColor(r, g, b, alpha, false);
    }

    public static WaypointColor fromHex(String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        int r = Integer.parseInt(clean.substring(0, 2), 16);
        int g = Integer.parseInt(clean.substring(2, 4), 16);
        int b = Integer.parseInt(clean.substring(4, 6), 16);
        return new WaypointColor(r, g, b, 255, false);
    }

    public static WaypointColor chroma() {
        return CHROMA;
    }

    public int getR() { return r; }
    public int getG() { return g; }
    public int getB() { return b; }
    public int getAlpha() { return alpha; }
    public boolean isChroma() { return chroma; }

    public java.awt.Color toAwtColor() {
        if (chroma) return java.awt.Color.WHITE;
        return new java.awt.Color(r, g, b, alpha);
    }
}

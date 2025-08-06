package net.exylia.commons.item.config;

import lombok.Getter;

/**
 * Configuration for radius-based actions
 */
@Getter
public class RadiusConfiguration {

    private final double radius;
    private final boolean affectSelf;
    private final boolean onlyPlayers;
    private final boolean requireLineOfSight;
    private final int maxTargets;

    public RadiusConfiguration(double radius, boolean affectSelf, boolean onlyPlayers,
                               boolean requireLineOfSight, int maxTargets) {
        this.radius = Math.max(0.0, radius);
        this.affectSelf = affectSelf;
        this.onlyPlayers = onlyPlayers;
        this.requireLineOfSight = requireLineOfSight;
        this.maxTargets = Math.max(-1, maxTargets); // -1 means no limit
    }

    /**
     * Creates a default radius configuration
     */
    public static RadiusConfiguration defaultConfig() {
        return new RadiusConfiguration(5.0, false, true, false, -1);
    }

    /**
     * Creates a radius configuration from action config values
     */
    public static RadiusConfiguration fromActionConfig(ItemConfiguration config) {
        double radius = config.getActionConfigDouble("radius", 5.0);
        boolean affectSelf = config.getActionConfigBoolean("affect-self", false);
        boolean onlyPlayers = config.getActionConfigBoolean("only-players", true);
        boolean requireLineOfSight = config.getActionConfigBoolean("require-line-of-sight", false);
        int maxTargets = config.getActionConfigInt("max-targets", -1);

        return new RadiusConfiguration(radius, affectSelf, onlyPlayers, requireLineOfSight, maxTargets);
    }

    /**
     * Checks if this configuration has a valid radius
     */
    public boolean hasRadius() {
        return radius > 0.0;
    }

    /**
     * Checks if there's a limit on the number of targets
     */
    public boolean hasMaxTargets() {
        return maxTargets > 0;
    }

    @Override
    public String toString() {
        return "RadiusConfiguration{" +
                "radius=" + radius +
                ", affectSelf=" + affectSelf +
                ", onlyPlayers=" + onlyPlayers +
                ", requireLineOfSight=" + requireLineOfSight +
                ", maxTargets=" + maxTargets +
                '}';
    }
}
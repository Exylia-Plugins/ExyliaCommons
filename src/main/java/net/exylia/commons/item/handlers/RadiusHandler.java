package net.exylia.commons.item.handlers;

import net.exylia.commons.item.config.RadiusConfiguration;
import net.exylia.commons.item.config.TriggerType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Centralized handler for radius-based entity processing
 */
public class RadiusHandler {

    /**
     * Processes entities within radius based on configuration and trigger type
     */
    public static List<LivingEntity> getEntitiesInRadius(Player sourcePlayer, RadiusConfiguration config,
                                                         TriggerType triggerType, Player targetPlayer) {

        Location center = determineCenter(sourcePlayer, triggerType, targetPlayer);

        if (center == null || !config.hasRadius()) {
            return new ArrayList<>();
        }

        // Get all entities in radius
        List<Entity> nearbyEntities = center.getWorld()
                .getNearbyEntities(center, config.getRadius(), config.getRadius(), config.getRadius())
                .stream()
                .filter(createEntityFilter(sourcePlayer, config, triggerType, targetPlayer))
                .collect(Collectors.toList());

        // Convert to LivingEntity and sort by distance
        List<LivingEntity> livingEntities = nearbyEntities.stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .sorted(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(center)))
                .collect(Collectors.toList());

        // Apply line of sight filter if required
        if (config.isRequireLineOfSight()) {
            livingEntities = livingEntities.stream()
                    .filter(entity -> hasLineOfSight(center, entity.getLocation()))
                    .collect(Collectors.toList());
        }

        // Apply max targets limit
        if (config.hasMaxTargets()) {
            livingEntities = livingEntities.stream()
                    .limit(config.getMaxTargets())
                    .collect(Collectors.toList());
        }

        return livingEntities;
    }

    /**
     * Determines the center location for radius calculations based on trigger type
     */
    private static Location determineCenter(Player sourcePlayer, TriggerType triggerType, Player targetPlayer) {
        return switch (triggerType) {
            case RADIUS, IMMEDIATE, AFTER_CONSUME, ON_PROJECTILE_LAUNCH -> sourcePlayer.getLocation();
            case ON_HIT_PLAYER, ON_PROJECTILE_HIT -> targetPlayer != null ? targetPlayer.getLocation() : sourcePlayer.getLocation();
        };
    }

    /**
     * Creates an entity filter based on configuration and trigger type
     */
    private static Predicate<Entity> createEntityFilter(Player sourcePlayer, RadiusConfiguration config,
                                                        TriggerType triggerType, Player targetPlayer) {
        return entity -> {
            // Basic type filter
            if (config.isOnlyPlayers() && !(entity instanceof Player)) {
                return false;
            }

            if (!(entity instanceof LivingEntity)) {
                return false;
            }

            // Self-targeting filter
            if (!config.isAffectSelf() && entity.equals(sourcePlayer)) {
                return false;
            }

            // For hit-based triggers, don't include the original target in radius calculations
            // (they're already handled separately)
            if ((triggerType == TriggerType.ON_HIT_PLAYER || triggerType == TriggerType.ON_PROJECTILE_HIT)
                    && targetPlayer != null && entity.equals(targetPlayer)) {
                return false;
            }

            return true;
        };
    }

    /**
     * Checks if there's line of sight between two locations
     */
    private static boolean hasLineOfSight(Location from, Location to) {
        if (from.getWorld() == null || to.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return false;
        }

        try {
            // Add small offset to avoid ground collision
            Location fromEye = from.clone().add(0, 1.6, 0);
            Location toEye = to.clone().add(0, 1.6, 0);

            return fromEye.getWorld().rayTraceBlocks(fromEye,
                    toEye.toVector().subtract(fromEye.toVector()),
                    fromEye.distance(toEye)) == null;
        } catch (Exception e) {
            // If raytracing fails, assume line of sight exists
            return true;
        }
    }

    /**
     * Processes a list of entities with a given processor
     */
    public static void processEntities(List<LivingEntity> entities, EntityProcessor processor) {
        if (entities == null || processor == null) {
            return;
        }

        entities.forEach(processor::process);
    }

    /**
     * Gets entities for a specific action execution - FIXED VERSION
     */
    public static List<LivingEntity> getEntitiesForAction(Player sourcePlayer,
                                                          RadiusConfiguration config,
                                                          TriggerType triggerType,
                                                          Player targetPlayer) {

        List<LivingEntity> entities = new ArrayList<>();

        // Always include the target player for hit-based triggers
        if (triggerType.requiresTargetPlayer() && targetPlayer != null) {
            entities.add(targetPlayer);
        }

        // Add radius entities if radius is configured
        if (config.hasRadius()) {
            List<LivingEntity> radiusEntities = getEntitiesInRadius(sourcePlayer, config, triggerType, targetPlayer);
            entities.addAll(radiusEntities);
        }
        // FIXED: For non-radius triggers, handle the default behavior
        else {
            switch (triggerType) {
                case IMMEDIATE, AFTER_CONSUME, ON_PROJECTILE_LAUNCH -> {
                    // Default behavior: affect self if affectSelf is true, otherwise do nothing
                    // (This allows actions to work even without radius configuration)
                    if (config.isAffectSelf()) {
                        entities.add(sourcePlayer);
                    }
                    // IMPORTANT: If affectSelf is false and no radius, the action should still execute
                    // but might not have entities to process. Let the action handle this case.
                }
                case RADIUS -> {
                    // Radius trigger without radius config should affect self
                    if (config.isAffectSelf()) {
                        entities.add(sourcePlayer);
                    }
                }
                // Hit-based triggers are already handled above
            }
        }

        return entities;
    }

    @FunctionalInterface
    public interface EntityProcessor {
        void process(LivingEntity entity);
    }
}
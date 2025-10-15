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

public class RadiusHandler {

    public static List<LivingEntity> getEntitiesInRadius(Player sourcePlayer, RadiusConfiguration config,
                                                         TriggerType triggerType, Player targetPlayer) {

        Location center = determineCenter(sourcePlayer, triggerType, targetPlayer);

        if (center == null || !config.hasRadius()) {
            return new ArrayList<>();
        }

        List<Entity> nearbyEntities = center.getWorld()
                .getNearbyEntities(center, config.getRadius(), config.getRadius(), config.getRadius())
                .stream()
                .filter(createEntityFilter(sourcePlayer, config, triggerType, targetPlayer))
                .collect(Collectors.toList());

        List<LivingEntity> livingEntities = nearbyEntities.stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .sorted(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(center)))
                .collect(Collectors.toList());

        if (config.isRequireLineOfSight()) {
            livingEntities = livingEntities.stream()
                    .filter(entity -> hasLineOfSight(center, entity.getLocation()))
                    .collect(Collectors.toList());
        }

        if (config.hasMaxTargets()) {
            livingEntities = livingEntities.stream()
                    .limit(config.getMaxTargets())
                    .collect(Collectors.toList());
        }

        return livingEntities;
    }

    private static Location determineCenter(Player sourcePlayer, TriggerType triggerType, Player targetPlayer) {
        return switch (triggerType) {
            case RADIUS, IMMEDIATE, AFTER_CONSUME, ON_PROJECTILE_LAUNCH, HOLD -> sourcePlayer.getLocation();
            case ON_HIT_PLAYER, ON_MULTIPLE_HIT_PLAYER, ON_PROJECTILE_HIT -> targetPlayer != null ? targetPlayer.getLocation() : sourcePlayer.getLocation();
        };
    }

    private static Predicate<Entity> createEntityFilter(Player sourcePlayer, RadiusConfiguration config,
                                                        TriggerType triggerType, Player targetPlayer) {
        return entity -> {
             
            if (config.isOnlyPlayers() && !(entity instanceof Player)) {
                return false;
            }

            if (!(entity instanceof LivingEntity)) {
                return false;
            }

            if (!config.isAffectSelf() && entity.equals(sourcePlayer)) {
                return false;
            }

            if ((triggerType == TriggerType.ON_HIT_PLAYER || triggerType == TriggerType.ON_PROJECTILE_HIT)
                    && targetPlayer != null && entity.equals(targetPlayer)) {
                return false;
            }

            return true;
        };
    }

    private static boolean hasLineOfSight(Location from, Location to) {
        if (from.getWorld() == null || to.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return false;
        }

        try {
             
            Location fromEye = from.clone().add(0, 1.6, 0);
            Location toEye = to.clone().add(0, 1.6, 0);

            return fromEye.getWorld().rayTraceBlocks(fromEye,
                    toEye.toVector().subtract(fromEye.toVector()),
                    fromEye.distance(toEye)) == null;
        } catch (Exception e) {
             
            return true;
        }
    }

    public static void processEntities(List<LivingEntity> entities, EntityProcessor processor) {
        if (entities == null || processor == null) {
            return;
        }

        entities.forEach(processor::process);
    }

    public static List<LivingEntity> getEntitiesForAction(Player sourcePlayer,
                                                          RadiusConfiguration config,
                                                          TriggerType triggerType,
                                                          Player targetPlayer) {

        List<LivingEntity> entities = new ArrayList<>();

        if (triggerType.requiresTargetPlayer() && targetPlayer != null) {
            entities.add(targetPlayer);
        }

        if (config.hasRadius()) {
            List<LivingEntity> radiusEntities = getEntitiesInRadius(sourcePlayer, config, triggerType, targetPlayer);
            entities.addAll(radiusEntities);
        }
         
        else {
            switch (triggerType) {
                case IMMEDIATE, AFTER_CONSUME, ON_PROJECTILE_LAUNCH -> {
                     
                    if (config.isAffectSelf()) {
                        entities.add(sourcePlayer);
                    }
                     
                }
                case RADIUS -> {
                     
                    if (config.isAffectSelf()) {
                        entities.add(sourcePlayer);
                    }
                }
                 
            }
        }

        return entities;
    }

    @FunctionalInterface
    public interface EntityProcessor {
        void process(LivingEntity entity);
    }
}

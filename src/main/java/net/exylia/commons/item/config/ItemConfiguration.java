package net.exylia.commons.item.config;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuración de un item interactivo almacenada en memoria
 * ACTUALIZADO: Nuevo sistema de regiones y soporte para TriggerType
 */
@Getter
public class ItemConfiguration {

    // Propiedades visuales
    private final String material;
    private final String name;
    private final List<String> lore;
    private final int amount;
    private final boolean glowing;
    private final boolean hideAttributes;
    private final int slot; // solo para items de lobby

    // Comportamiento
    private final List<String> commands;
    private final String action;
    private final boolean consumeOnUse;
    private final boolean cancelEvent;
    private final boolean stackable;

    // Sistema de usos
    private final int maxUses;

    // Sistema de cooldown
    private final double cooldownSeconds;

    // Efectos
    private final String soundOnUse;
    private final String particlesOnUse;
    private final String fireworkOnUse;

    // Configuración de acciones
    private final Map<String, Object> actionConfig;

    // Restricciones de movimiento
    private final boolean allowMovement;
    private final boolean allowShiftClick;
    private final boolean allowDrop;
    private final boolean allowSwapToOffhand;
    private final boolean allowNumberKeys;

    // Placeholders
    private final boolean usePlaceholders;

    // Sistema de regiones
    private final RegionFilterType regionType;
    private final RegionCheckerType regionChecker;
    private final List<RegionEntry> regionEntries;
    private final List<String> regionList;
    private final Map<String, Double> regionCooldowns;

    // NUEVO: Tipo de trigger
    private final TriggerType triggerType;

    ItemConfiguration(ItemConfigurationBuilder builder) {
        this.material = builder.material;
        this.name = builder.name;
        this.lore = new ArrayList<>(builder.lore);
        this.amount = builder.amount;
        this.glowing = builder.glowing;
        this.hideAttributes = builder.hideAttributes;
        this.slot = builder.slot;
        this.commands = new ArrayList<>(builder.commands);
        this.action = builder.action;
        this.consumeOnUse = builder.consumeOnUse;
        this.cancelEvent = builder.cancelEvent;
        this.stackable = builder.stackable;
        this.maxUses = builder.maxUses;
        this.cooldownSeconds = builder.cooldownSeconds;

        this.soundOnUse = builder.soundOnUse;
        this.particlesOnUse = builder.particlesOnUse;
        this.fireworkOnUse = builder.fireworkOnUse;

        this.actionConfig = new HashMap<>(builder.actionConfig);
        this.usePlaceholders = builder.usePlaceholders;

        this.allowMovement = builder.allowMovement;
        this.allowShiftClick = builder.allowShiftClick;
        this.allowDrop = builder.allowDrop;
        this.allowSwapToOffhand = builder.allowSwapToOffhand;
        this.allowNumberKeys = builder.allowNumberKeys;

        this.regionType = builder.regionType;
        this.regionChecker = builder.regionChecker;
        this.regionEntries = new ArrayList<>(builder.regionEntries);
        this.regionList = new ArrayList<>(builder.regionList);
        this.regionCooldowns = new HashMap<>(builder.regionCooldowns);

        this.triggerType = builder.triggerType;
    }

    public boolean hasRegionConfiguration() {
        return regionType != RegionFilterType.NONE && !regionEntries.isEmpty();
    }

    public boolean canUseWithChecker(List<String> playerRegions, World playerWorld, String highestPriorityRegion) {
        if (!hasRegionConfiguration()) {
            return true;
        }

        return switch (regionChecker) {
            case PRIORITY -> canUseWithPriorityCheck(highestPriorityRegion, playerWorld);
            case CONTAINS -> canUseWithContainsCheck(playerRegions, playerWorld);
        };
    }

    private boolean canUseWithPriorityCheck(String highestPriorityRegion, World playerWorld) {
        if (highestPriorityRegion == null) {
            return regionType == RegionFilterType.BLACKLIST;
        }

        boolean isInConfiguredRegion = regionEntries.stream()
                .anyMatch(entry -> entry.matches(highestPriorityRegion, playerWorld));

        return switch (regionType) {
            case WHITELIST -> isInConfiguredRegion;
            case BLACKLIST -> !isInConfiguredRegion;
            case NONE -> true;
        };
    }

    private boolean canUseWithContainsCheck(List<String> playerRegions, World playerWorld) {
        if (playerRegions.isEmpty()) {
            return regionType == RegionFilterType.BLACKLIST;
        }

        boolean hasMatchingRegion = playerRegions.stream()
                .anyMatch(regionName -> regionEntries.stream()
                        .anyMatch(entry -> entry.matches(regionName, playerWorld)));

        return switch (regionType) {
            case WHITELIST -> hasMatchingRegion;
            case BLACKLIST -> !hasMatchingRegion;
            case NONE -> true;
        };
    }

    public double getCooldownForRegion(String regionName) {
        return regionCooldowns.getOrDefault(regionName, cooldownSeconds);
    }

    public double getHighestCooldownForRegions(List<String> regionNames) {
        if (regionNames.isEmpty()) {
            return cooldownSeconds;
        }

        return regionNames.stream()
                .mapToDouble(this::getCooldownForRegion)
                .max()
                .orElse(cooldownSeconds);
    }

    public boolean hasRegionCooldowns() {
        return !regionCooldowns.isEmpty();
    }

    public boolean hasSound() {
        return soundOnUse != null && !soundOnUse.trim().isEmpty();
    }

    public boolean hasParticles() {
        return particlesOnUse != null && !particlesOnUse.trim().isEmpty();
    }

    public boolean hasFirework() {
        return (fireworkOnUse != null && !fireworkOnUse.trim().isEmpty());
    }

    public boolean hasEffects() {
        return hasSound() || hasParticles() || hasFirework();
    }

    public boolean hasCooldown() {
        return cooldownSeconds > 0.0;
    }

    @SuppressWarnings("unchecked")
    public <T> T getActionConfigValue(String key, T defaultValue) {
        Object value = actionConfig.get(key);
        if (value != null) {
            try {
                return (T) value;
            } catch (ClassCastException e) {
                return convertActionConfigValue(value, defaultValue);
            }
        }
        return defaultValue;
    }

    public int getActionConfigInt(String key, int defaultValue) {
        Object value = actionConfig.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public double getActionConfigDouble(String key, double defaultValue) {
        Object value = actionConfig.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public boolean getActionConfigBoolean(String key, boolean defaultValue) {
        Object value = actionConfig.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            return str.equals("true") || str.equals("yes") || str.equals("1");
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return defaultValue;
    }

    public String getActionConfigString(String key, String defaultValue) {
        Object value = actionConfig.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    public List<String> getActionConfigListString(String key, List<String> defaultValue) {
        Object value = actionConfig.get(key);

        if (value == null) {
            return defaultValue != null ? new ArrayList<>(defaultValue) : new ArrayList<>();
        }

        if (value instanceof List<?>) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }

        if (value instanceof String stringValue) {
            if (stringValue.trim().isEmpty()) {
                return defaultValue != null ? new ArrayList<>(defaultValue) : new ArrayList<>();
            }

            List<String> result = new ArrayList<>();
            String[] parts = stringValue.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        }

        List<String> result = new ArrayList<>();
        result.add(value.toString());
        return result;
    }

    public List<String> getActionConfigListString(String key) {
        return getActionConfigListString(key, new ArrayList<>());
    }

    public boolean hasActionConfig(String key) {
        return actionConfig.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertActionConfigValue(Object value, T defaultValue) {
        if (defaultValue instanceof Integer && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        }
        if (defaultValue instanceof Double && value instanceof Number) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        }
        if (defaultValue instanceof Boolean) {
            if (value instanceof String) {
                String str = ((String) value).toLowerCase();
                return (T) Boolean.valueOf(str.equals("true") || str.equals("yes") || str.equals("1"));
            }
        }
        if (defaultValue instanceof String) {
            return (T) value.toString();
        }
        return defaultValue;
    }

    public List<String> getRegionNames() {
        return regionEntries.stream()
                .map(RegionEntry::getRegionName)
                .collect(Collectors.toList());
    }

    public List<RegionEntry> getRegionEntries() {
        return new ArrayList<>(regionEntries);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ItemConfigurationBuilder {
    }

    @Override
    public String toString() {
        return "ItemConfiguration{" +
                "material='" + material + '\'' +
                ", name='" + name + '\'' +
                ", commands=" + commands.size() +
                ", action='" + action + '\'' +
                ", maxUses=" + maxUses +
                ", cooldownSeconds=" + cooldownSeconds +
                ", actionConfig=" + actionConfig.size() + " keys" +
                ", stackable=" + stackable +
                ", hasSound=" + hasSound() +
                ", hasParticles=" + hasParticles() +
                ", hasFirework=" + hasFirework() +
                ", regionType=" + regionType +
                ", regionChecker=" + regionChecker +
                ", regionEntries=" + regionEntries.size() + " region entries" +
                ", regionCooldowns=" + regionCooldowns.size() + " region cooldowns" +
                ", triggerType=" + triggerType +
                '}';
    }
}
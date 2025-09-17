package net.exylia.commons.item.config;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class ItemConfiguration {

    private final String material;
    private final String name;
    private final String displayName;
    private final List<String> lore;
    private final int amount;
    private final boolean glowing;
    private final boolean hideAttributes;
    private final int slot;

    private final List<String> commands;
    private final String action;
    private final boolean consumeOnUse;
    private final boolean cancelEvent;
    private final boolean stackable;

    private final int maxUses;
    private final double cooldownSeconds;

    private final ConfigurationSection effectsOnUse;

    private final Map<String, Object> actionConfig;

    private final boolean allowMovement;
    private final boolean allowShiftClick;
    private final boolean allowDrop;
    private final boolean allowSwapToOffhand;
    private final boolean allowNumberKeys;

    private final boolean usePlaceholders;

    private final RegionFilterType regionType;
    private final RegionCheckerType regionChecker;
    private final List<RegionEntry> regionEntries;
    private final List<String> regionList;
    private final Map<String, Double> regionCooldowns;

    private final WorldFilterType worldType;
    private final List<WorldEntry> worldEntries;
    private final List<String> worldList;
    private final Map<String, Double> worldCooldowns;

    private final TriggerType triggerType;
    private final String forceId;
    
    private final Map<Enchantment, Integer> enchantments;
    
    private final long expirationTimeMillis;
    private final String expirationBehavior;

    ItemConfiguration(ItemConfigurationBuilder builder) {
        this.material = builder.material;
        this.name = builder.name;
        this.displayName = builder.displayName;
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

        this.effectsOnUse = builder.effectsOnUse;

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

        this.worldType = builder.worldType;
        this.worldEntries = new ArrayList<>(builder.worldEntries);
        this.worldList = new ArrayList<>(builder.worldList);
        this.worldCooldowns = new HashMap<>(builder.worldCooldowns);

        this.triggerType = builder.triggerType;
        this.forceId = builder.forceId;
        
        this.enchantments = new HashMap<>(builder.enchantments);
        
        this.expirationTimeMillis = builder.expirationTimeMillis;
        this.expirationBehavior = builder.expirationBehavior;
    }

    public boolean hasDisplayName() {
        return displayName != null && !displayName.trim().isEmpty();
    }

    public boolean hasForceId() {
        return forceId != null && !forceId.trim().isEmpty();
    }

    public String getEffectiveId(String originalId) {
        return hasForceId() ? forceId : originalId;
    }

    public boolean hasRegionConfiguration() {
        return regionType != RegionFilterType.NONE && !regionEntries.isEmpty();
    }

    public boolean canUseWithChecker(List<String> playerRegions, World playerWorld, String highestPriorityRegion) {
        if (!canUseInWorld(playerWorld)) {
            return false;
        }

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

    public boolean hasEffects() {
        return effectsOnUse != null;
    }

    public boolean hasCooldown() {
        return cooldownSeconds > 0.0;
    }

    public boolean isAllowMovement() { return allowMovement; }
    public boolean isAllowShiftClick() { return allowShiftClick; }
    public boolean isAllowDrop() { return allowDrop; }
    public boolean isAllowSwapToOffhand() { return allowSwapToOffhand; }
    public boolean isAllowNumberKeys() { return allowNumberKeys; }

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

    @Deprecated
    @SuppressWarnings("unchecked")
    public Map<String, Object> getActionConfigSection(String sectionKey) {
        Object value = actionConfig.get(sectionKey);
        if (value instanceof Map) {
            return new HashMap<>((Map<String, Object>) value);
        }

        if (value != null && value.getClass().getSimpleName().equals("MemorySection")) {
            try {
                java.lang.reflect.Method getValuesMethod = value.getClass().getMethod("getValues", boolean.class);
                Object result = getValuesMethod.invoke(value, false);
                if (result instanceof Map) {
                    return new HashMap<>((Map<String, Object>) result);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error al procesar MemorySection: " + e.getMessage());
            }
        }

        return new HashMap<>();
    }

    public ConfigurationSection getActionConfigurationSection(String sectionKey) {
        Object value = actionConfig.get(sectionKey);
        if (value instanceof ConfigurationSection) {
            return (ConfigurationSection) value;
        }
        return null;
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

    public boolean hasWorldConfiguration() {
        return worldType != WorldFilterType.NONE && !worldEntries.isEmpty();
    }

    public boolean canUseInWorld(World world) {
        if (!hasWorldConfiguration()) {
            return true;
        }

        if (world == null) {
            return worldType == WorldFilterType.BLACKLIST;
        }

        boolean isInConfiguredWorld = worldEntries.stream()
                .anyMatch(entry -> entry.matches(world));

        return switch (worldType) {
            case WHITELIST -> isInConfiguredWorld;
            case BLACKLIST -> !isInConfiguredWorld;
            case NONE -> true;
        };
    }

    public double getCooldownForWorld(String worldName) {
        return worldCooldowns.getOrDefault(worldName, cooldownSeconds);
    }

    public double getCooldownForWorld(World world) {
        return world != null ? getCooldownForWorld(world.getName()) : cooldownSeconds;
    }

    public boolean hasWorldCooldowns() {
        return !worldCooldowns.isEmpty();
    }

    public List<String> getWorldNames() {
        return new ArrayList<>(worldList);
    }

    public List<WorldEntry> getWorldEntries() {
        return new ArrayList<>(worldEntries);
    }
    
    public Map<Enchantment, Integer> getEnchantments() {
        return new HashMap<>(enchantments);
    }
    
    public boolean hasEnchantments() {
        return !enchantments.isEmpty();
    }
    
    public boolean hasEnchantment(Enchantment enchantment) {
        return enchantments.containsKey(enchantment);
    }
    
    public int getEnchantmentLevel(Enchantment enchantment) {
        return enchantments.getOrDefault(enchantment, 0);
    }
    
    public long getExpirationTimeMillis() {
        return expirationTimeMillis;
    }
    
    public boolean hasExpiration() {
        return expirationTimeMillis > 0;
    }
    
    public String getExpirationBehavior() {
        return expirationBehavior;
    }

    public int getHitCount() {
        return getActionConfigInt("hit-count", 5);
    }

    public int getHitPeriod() {
        return getActionConfigInt("hit-period", 40);
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
                ", displayName='" + displayName + '\'' +
                ", commands=" + commands.size() +
                ", action='" + action + '\'' +
                ", maxUses=" + maxUses +
                ", cooldownSeconds=" + cooldownSeconds +
                ", actionConfig=" + actionConfig.size() + " keys" +
                ", stackable=" + stackable +
                ", hasEffects=" + hasEffects() +
                ", regionType=" + regionType +
                ", regionChecker=" + regionChecker +
                ", regionEntries=" + regionEntries.size() + " region entries" +
                ", regionCooldowns=" + regionCooldowns.size() + " region cooldowns" +
                ", worldType=" + worldType +
                ", worldEntries=" + worldEntries.size() + " world entries" +
                ", worldCooldowns=" + worldCooldowns.size() + " world cooldowns" +
                ", triggerType=" + triggerType +
                ", forceId='" + forceId + '\'' +
                '}';
    }
}
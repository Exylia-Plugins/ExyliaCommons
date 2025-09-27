package net.exylia.commons.item.cooldown;

import lombok.Builder;
import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CooldownConfiguration {

    @Builder.Default
    private final int maxItemsInCooldown = -1;

    @Builder.Default
    private final double globalCooldownSeconds = -1.0;

    @Builder.Default
    private final Map<String, Double> globalCooldownByWorld = new HashMap<>();

    @Builder.Default
    private final Map<String, Double> globalCooldownByRegion = new HashMap<>();

    @Builder.Default
    private final long saveIntervalTicks = 1200L;

    @Builder.Default
    private final long cleanupIntervalTicks = 6000L;

    @Builder.Default
    private final String dataFileName = "cooldowns.json";

    public boolean hasMaxItemsLimit() {
        return maxItemsInCooldown > 0;
    }

    public boolean hasGlobalCooldown() {
        return globalCooldownSeconds > 0 || !globalCooldownByWorld.isEmpty() || !globalCooldownByRegion.isEmpty();
    }

    public double getGlobalCooldownForWorld(String worldName) {
        return globalCooldownByWorld.getOrDefault(worldName, globalCooldownSeconds);
    }

    public double getGlobalCooldownForRegion(String regionId) {
        return globalCooldownByRegion.getOrDefault(regionId, globalCooldownSeconds);
    }

    public double getEffectiveGlobalCooldown(org.bukkit.entity.Player player) {
        if (player == null || !hasGlobalCooldown()) {
            return -1.0;
        }

        String worldName = player.getWorld().getName();
        double worldCooldown = getGlobalCooldownForWorld(worldName);

        if (net.exylia.commons.utils.WorldGuardUtils.isWorldGuardAvailable()) {
            java.util.List<String> regions = net.exylia.commons.utils.WorldGuardUtils.getRegionsAtPlayer(player);
            for (String regionId : regions) {
                if (globalCooldownByRegion.containsKey(regionId)) {
                    return globalCooldownByRegion.get(regionId);
                }
            }
        }

        return worldCooldown;
    }

    public static CooldownConfiguration getDefault() {
        return CooldownConfiguration.builder().build();
    }

    public static CooldownConfiguration withMaxItems(int maxItems) {
        return CooldownConfiguration.builder()
                .maxItemsInCooldown(maxItems)
                .build();
    }

    public static CooldownConfiguration withGlobalCooldown(double globalCooldownSeconds) {
        return CooldownConfiguration.builder()
                .globalCooldownSeconds(globalCooldownSeconds)
                .build();
    }

    public static CooldownConfiguration withMaxItemsAndGlobalCooldown(int maxItems, double globalCooldownSeconds) {
        return CooldownConfiguration.builder()
                .maxItemsInCooldown(maxItems)
                .globalCooldownSeconds(globalCooldownSeconds)
                .build();
    }

    public static CooldownConfiguration fromConfiguration(ConfigurationSection config) {
        if (config == null) {
            return getDefault();
        }

        CooldownConfigurationBuilder builder = CooldownConfiguration.builder();

        if (config.contains("max-items")) {
            builder.maxItemsInCooldown(config.getInt("max-items", -1));
        }

        if (config.contains("global-cooldown")) {
            Object globalCooldown = config.get("global-cooldown");
            if (globalCooldown instanceof Number) {
                builder.globalCooldownSeconds(((Number) globalCooldown).doubleValue());
            } else if (globalCooldown instanceof ConfigurationSection) {
                ConfigurationSection globalSection = (ConfigurationSection) globalCooldown;

                if (globalSection.contains("default")) {
                    builder.globalCooldownSeconds(globalSection.getDouble("default", -1.0));
                }

                Map<String, Double> worldCooldowns = new HashMap<>();
                if (globalSection.contains("worlds")) {
                    ConfigurationSection worldsSection = globalSection.getConfigurationSection("worlds");
                    if (worldsSection != null) {
                        for (String worldName : worldsSection.getKeys(false)) {
                            worldCooldowns.put(worldName, worldsSection.getDouble(worldName));
                        }
                    }
                }
                builder.globalCooldownByWorld(worldCooldowns);

                Map<String, Double> regionCooldowns = new HashMap<>();
                if (globalSection.contains("regions")) {
                    ConfigurationSection regionsSection = globalSection.getConfigurationSection("regions");
                    if (regionsSection != null) {
                        for (String regionId : regionsSection.getKeys(false)) {
                            regionCooldowns.put(regionId, regionsSection.getDouble(regionId));
                        }
                    }
                }
                builder.globalCooldownByRegion(regionCooldowns);
            }
        }

        if (config.contains("save-interval-ticks")) {
            builder.saveIntervalTicks(config.getLong("save-interval-ticks", 1200L));
        }

        if (config.contains("cleanup-interval-ticks")) {
            builder.cleanupIntervalTicks(config.getLong("cleanup-interval-ticks", 6000L));
        }

        if (config.contains("data-file-name")) {
            builder.dataFileName(config.getString("data-file-name", "cooldowns.json"));
        }

        return builder.build();
    }
}
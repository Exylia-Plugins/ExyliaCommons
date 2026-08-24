package net.exylia.commons.v2.reload.core;

import net.exylia.commons.v2.action.core.ActionManager;
import net.exylia.commons.v2.clan.integration.OptionalClassCache;
import net.exylia.commons.v2.hologram.core.HologramManager;
import net.exylia.commons.v2.placeholders.registry.PlaceholderRegistry;
import net.exylia.commons.v2.region.RegionManager;
import net.exylia.commons.v2.reload.api.ReloadableSystem;
import net.exylia.commons.v2.reload.detector.SystemAvailability;
import net.exylia.commons.v2.scoreboard.core.ScoreboardManager;
import net.exylia.commons.v2.visual.core.VisualManager;

import java.util.Map;

public class SystemDetector {

    public SystemAvailability detectAll(Map<String, ReloadableSystem> systems) {
        SystemAvailability.Builder builder = new SystemAvailability.Builder();

        for (Map.Entry<String, ReloadableSystem> entry : systems.entrySet()) {
            String systemName = entry.getKey();
            ReloadableSystem system = entry.getValue();

            try {
                if (system.isAvailable()) {
                    builder.available(systemName);
                } else {
                    builder.unavailable(systemName, "Not initialized");
                }
            } catch (IllegalStateException e) {
                builder.unavailable(systemName, "Not initialized");
            } catch (Exception e) {
                builder.unavailable(systemName, "Error: " + e.getMessage());
            }
        }

        return builder.build();
    }

    @Deprecated
    public SystemAvailability detectAll() {
        SystemAvailability.Builder builder = new SystemAvailability.Builder();

        detectDatabaseV2(builder);
        detectRedis(builder);
        detectScoreboardManager(builder);
        detectHologramManager(builder);
        detectActionManager(builder);
        detectRegionManager(builder);
        detectPlaceholderRegistry(builder);
        detectVisualManager(builder);

        builder.check("FormatterRegistry", true, "Always available");
        builder.check("ColorSystem", true, "Always available");
        builder.check("ConfigSystem", true, "Always available");

        return builder.build();
    }

    private void detectDatabaseV2(SystemAvailability.Builder builder) {
        try {
            net.exylia.commons.v2.database.core.DatabaseManager.getInstance();
            builder.available("DatabaseV2");
        } catch (IllegalStateException e) {
            builder.unavailable("DatabaseV2", "Not initialized");
        } catch (Exception e) {
            builder.unavailable("DatabaseV2", "Error: " + e.getMessage());
        }
    }

    private void detectRedis(SystemAvailability.Builder builder) {
        try {
            if (OptionalClassCache.resolve("redis.clients.jedis.Jedis") == null) {
                builder.unavailable("Redis", "Jedis not in classpath");
                return;
            }
            net.exylia.commons.v2.redis.SimpleRedis instance = net.exylia.commons.v2.redis.SimpleRedis.getInstance();
            if (instance != null && instance.isConnected()) {
                builder.available("Redis");
            } else {
                builder.unavailable("Redis", "Not connected");
            }
        } catch (Exception e) {
            builder.unavailable("Redis", "Error: " + e.getMessage());
        }
    }

    private void detectScoreboardManager(SystemAvailability.Builder builder) {
        try {
            ScoreboardManager instance = ScoreboardManager.getInstance();
            if (instance != null) {
                builder.available("ScoreboardManager");
            } else {
                builder.unavailable("ScoreboardManager", "Instance is null");
            }
        } catch (IllegalStateException e) {
            builder.unavailable("ScoreboardManager", "Not initialized");
        } catch (Exception e) {
            builder.unavailable("ScoreboardManager", "Error: " + e.getMessage());
        }
    }

    private void detectHologramManager(SystemAvailability.Builder builder) {
        try {
            HologramManager instance = HologramManager.getInstance();
            if (instance != null) {
                builder.available("HologramManager");
            } else {
                builder.unavailable("HologramManager", "Instance is null");
            }
        } catch (IllegalStateException e) {
            builder.unavailable("HologramManager", "Not initialized");
        } catch (Exception e) {
            builder.unavailable("HologramManager", "Error: " + e.getMessage());
        }
    }

    private void detectActionManager(SystemAvailability.Builder builder) {
        try {
            if (ActionManager.isInitialized()) {
                builder.available("ActionManager");
            } else {
                builder.unavailable("ActionManager", "Not initialized");
            }
        } catch (Exception e) {
            builder.unavailable("ActionManager", "Error: " + e.getMessage());
        }
    }

    private void detectRegionManager(SystemAvailability.Builder builder) {
        try {
            RegionManager instance = RegionManager.getInstance();
            if (instance != null) {
                builder.available("RegionManager");
            } else {
                builder.unavailable("RegionManager", "Instance is null");
            }
        } catch (IllegalStateException e) {
            builder.unavailable("RegionManager", "Not initialized");
        } catch (Exception e) {
            builder.unavailable("RegionManager", "Error: " + e.getMessage());
        }
    }

    private void detectPlaceholderRegistry(SystemAvailability.Builder builder) {
        try {
            PlaceholderRegistry instance = PlaceholderRegistry.getInstance();
            if (instance != null) {
                builder.available("PlaceholderRegistry");
            } else {
                builder.unavailable("PlaceholderRegistry", "Instance is null");
            }
        } catch (IllegalStateException e) {
            builder.unavailable("PlaceholderRegistry", "Not initialized");
        } catch (Exception e) {
            builder.unavailable("PlaceholderRegistry", "Error: " + e.getMessage());
        }
    }

    private void detectVisualManager(SystemAvailability.Builder builder) {
        try {
            VisualManager instance = VisualManager.getInstance();
            if (instance != null) {
                builder.available("VisualManager");
            } else {
                builder.unavailable("VisualManager", "Instance is null");
            }
        } catch (IllegalStateException e) {
            builder.unavailable("VisualManager", "Not initialized");
        } catch (Exception e) {
            builder.unavailable("VisualManager", "Error: " + e.getMessage());
        }
    }
}

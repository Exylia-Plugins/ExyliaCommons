package net.exylia.commons.v2.reload.core;

import net.exylia.commons.database.DatabaseManager;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.v2.reload.detector.SystemAvailability;
import net.exylia.commons.v2.action.core.ActionManager;
import net.exylia.commons.v2.hologram.core.HologramManager;
import net.exylia.commons.v2.placeholders.registry.PlaceholderRegistry;
import net.exylia.commons.v2.region.RegionManager;
import net.exylia.commons.v2.scoreboard.core.ScoreboardManager;
import net.exylia.commons.v2.visual.core.VisualManager;

public class SystemDetector {

    public SystemAvailability detectAll() {
        SystemAvailability.Builder builder = new SystemAvailability.Builder();

        builder.check("ConfigSystem", true, "Always available");

        detectDatabaseV1(builder);
        detectDatabaseV2(builder);
        detectRedis(builder);
        detectScoreboardManager(builder);
        detectHologramManager(builder);
        detectActionManager(builder);
        detectRegionManager(builder);
        detectPlaceholderRegistry(builder);
        detectPlaceholderSystemManager(builder);
        detectVisualManager(builder);

        builder.check("FormatterRegistry", true, "Always available");
        builder.check("ColorSystem", true, "Always available");

        return builder.build();
    }

    private void detectDatabaseV1(SystemAvailability.Builder builder) {
        try {
            DatabaseManager instance = DatabaseManager.getInstance();
            if (instance != null && instance.isConnected()) {
                builder.available("DatabaseV1");
            } else {
                builder.unavailable("DatabaseV1", "Not connected");
            }
        } catch (Exception e) {
            builder.unavailable("DatabaseV1", "Not initialized: " + e.getMessage());
        }
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
            Class.forName("redis.clients.jedis.Jedis");
            net.exylia.commons.v2.redis.SimpleRedis instance = net.exylia.commons.v2.redis.SimpleRedis.getInstance();
            if (instance != null && instance.isConnected()) {
                builder.available("Redis");
            } else {
                builder.unavailable("Redis", "Not connected");
            }
        } catch (ClassNotFoundException e) {
            builder.unavailable("Redis", "Jedis not in classpath");
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

    private void detectPlaceholderSystemManager(SystemAvailability.Builder builder) {
        try {
            PlaceholderSystemManager instance = PlaceholderSystemManager.getInstance();
            if (instance != null) {
                builder.available("PlaceholderSystemManager");
            } else {
                builder.unavailable("PlaceholderSystemManager", "Instance is null");
            }
        } catch (Exception e) {
            builder.unavailable("PlaceholderSystemManager", "Error: " + e.getMessage());
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

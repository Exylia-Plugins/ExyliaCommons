package net.exylia.commons.v2.lifecycle;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.chat.core.ChatInputManager;
import net.exylia.commons.v2.config.ConfigInitializer;
import net.exylia.commons.v2.config.schema.ConfigSchemaRegistry;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.config.DebugDefaults;
import net.exylia.commons.v2.formatter.FormattersDefaults;
import net.exylia.commons.v2.reward.core.RewardManager;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.sequence.SequenceListener;
import net.exylia.commons.v2.sequence.preview.EffectPreview;
import net.exylia.commons.v2.ui.selector.impl.reward.action.RewardEditorActionRegistrar;
import net.exylia.commons.v2.utils.PlayerUtils;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.exylia.commons.v2.visual.core.VisualManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;


public class SystemBootstrapper {

    public void initializeCoreSystemsAsync(ExyliaPlugin plugin) {
        initializeCoreSystemsAsync((JavaPlugin) plugin);
    }

    public void initializeCoreSystemsAsync(JavaPlugin plugin) {
        try {
            deleteDirectory(new File(plugin.getDataFolder(), "menus/admin"));
            ConfigInitializer.initConfigs(plugin);

            ConfigSchemaRegistry.ensureDefaults(DebugDefaults.class);
            ConfigSchemaRegistry.ensureDefaults(FormattersDefaults.class);

            TaskAPI.initialize(plugin);

            DebugAPI.logLibDebug("Initializing Messages...");
            ConfigInitializer.initMessages();

            DebugAPI.logLibDebug("Reloading DebugConfig cache...");
            net.exylia.commons.v2.debug.config.DebugConfig.reload();

            DebugAPI.logLibDebug("Initializing ColorAPI...");
            ColorAPI.initialize(plugin);

            DebugAPI.logLibDebug("Initializing VisualManager...");
            VisualManager.getInstance().initialize(plugin);

            DebugAPI.logLibDebug("Initializing PlayerUtils...");
            PlayerUtils.initialize(plugin);

            DebugAPI.logLibDebug("Initializing Placeholders...");
            Placeholders.initialize(plugin);

            DebugAPI.logLibDebug("Initializing ChatInputManager...");
            ChatInputManager.init(plugin);

            DebugAPI.logLibDebug("Initializing RewardManager...");
            RewardManager.getInstance().initialize(plugin);

            DebugAPI.logLibDebug("Initializing ActionAPI...");
            ActionAPI.initialize(plugin);

            DebugAPI.logLibDebug("Registering RewardEditor actions...");
            RewardEditorActionRegistrar.register(plugin);

            EffectPreview.init(plugin);
            DebugAPI.logLibDebug("Registering SequenceListener...");
            plugin.getServer().getPluginManager().registerEvents(new SequenceListener(), plugin);

            DebugAPI.logLibInfo("Core systems initialized successfully");
        } catch (Exception e) {
            DebugAPI.logLibError("Error initializing core systems: " + e.getMessage());
            throw new RuntimeException("Failed to initialize core systems", e);
        }
    }

    public void checkOptionalDependencies() {
        try {
            Class.forName("redis.clients.jedis.Jedis");
        } catch (ClassNotFoundException ignored) {
        }

        checkDatabaseDrivers();
    }

    private void checkDatabaseDrivers() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ignored) {
            }
        }

        try {
            Class.forName("com.mongodb.client.MongoClient");
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName("com.zaxxer.hikari.HikariDataSource");
        } catch (ClassNotFoundException ignored) {
        }
    }

    private void deleteDirectory(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}

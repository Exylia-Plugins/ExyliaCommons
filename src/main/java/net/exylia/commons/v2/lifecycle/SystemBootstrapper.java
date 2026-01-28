package net.exylia.commons.v2.lifecycle;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.skull.SkullManager;
import net.exylia.commons.utils.visuals.ActionBarUtils;
import net.exylia.commons.utils.visuals.BossbarUtils;
import net.exylia.commons.utils.visuals.TitleUtils;
import net.exylia.commons.v2.config.ConfigInitializer;
import net.exylia.commons.v2.config.schema.ConfigSchemaRegistry;
import net.exylia.commons.v2.database.config.DatabaseDefaults;
import net.exylia.commons.v2.debug.config.DebugDefaults;
import net.exylia.commons.v2.discord.config.DiscordConfig;
import net.exylia.commons.v2.formatter.FormattersDefaults;
import net.exylia.commons.v2.tasks.config.TasksDefaults;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.exylia.commons.v2.visual.config.ColorDefaults;
import net.exylia.commons.v2.visual.core.VisualManager;

import static net.exylia.commons.utils.DebugUtils.*;


public class SystemBootstrapper {

    public void initializeCoreSystemsAsync(ExyliaPlugin plugin) {
        try {
            logInternalDebug("Initializing Configs...");
            ConfigInitializer.initConfigs(plugin);

            logInternalDebug("Loading config schemas...");
            ConfigSchemaRegistry.ensureDefaults(DebugDefaults.class);
            ConfigSchemaRegistry.ensureDefaults(FormattersDefaults.class);
            ConfigSchemaRegistry.ensureDefaults(DatabaseDefaults.class);
            ConfigSchemaRegistry.ensureDefaults(TasksDefaults.class);
            ConfigSchemaRegistry.ensureDefaults(ColorDefaults.class);
            ConfigSchemaRegistry.ensureDefaults(DiscordConfig.class);

            logInternalDebug("Initializing Messages...");
            ConfigInitializer.initMessages();

            logInternalDebug("Reloading DebugConfig cache...");
            net.exylia.commons.v2.debug.config.DebugConfig.reload();

            logInternalDebug("Initializing ColorAPI...");
            ColorAPI.initialize(plugin);

            logInternalDebug("Initializing VisualManager...");
            VisualManager.getInstance().initialize(plugin);

            logInternalDebug("Initializing PlaceholderSystemManager...");
            PlaceholderSystemManager.initialize(plugin);

            logInternalDebug("Initializing AdapterFactory...");
            AdapterFactory.initialize(plugin);

            logInternalDebug("Initializing ActionBarUtils...");
            ActionBarUtils.init(plugin);

            logInternalDebug("Initializing BossbarUtils...");
            BossbarUtils.init(plugin);

            logInternalDebug("Initializing TitleUtils...");
            TitleUtils.init(plugin);

            logInternalDebug("Initializing SkullManager...");
            SkullManager.initialize(plugin);

            logInternalInfo("Core systems initialized successfully");
        } catch (Exception e) {
            logInternalError("Error initializing core systems: " + e.getMessage());
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
}

package net.exylia.commons;

import com.hapangama.SunLicenseAPI;
import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.command.CommandManager;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.config.ConfigurationSystem;
import net.exylia.commons.config.ConfigBase;
import net.exylia.commons.config.base.MainConfigBase;
import net.exylia.commons.config.base.MessagesBase;
import net.exylia.commons.configSimple.ConfigInitializer;
import net.exylia.commons.database.DatabaseManager;
import net.exylia.commons.license.SunLicenseUtil;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.redis.RedisIntegration;
import net.exylia.commons.utils.*;
import net.exylia.commons.utils.skull.SkullManager;
import net.exylia.commons.utils.visuals.ActionBarUtils;
import net.exylia.commons.utils.visuals.BossbarUtils;
import net.exylia.commons.utils.visuals.TitleUtils;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.exylia.commons.utils.DebugUtils.*;

public abstract class ExyliaPlugin extends JavaPlugin {

    private static boolean initialized = false;
    private static final Set<ExyliaPlugin> registeredPlugins = new HashSet<>();
    @Getter
    private static ExyliaPlugin instance;

    private BukkitAudiences adventure;
    private ConfigurationSystem configSystem;
    private ReloadManager reloadManager;
    @Getter
    @Setter
    private SunLicenseAPI api;

    public abstract int getProductID();

    @Override
    public final void onEnable() {
        try {
            this.adventure = BukkitAudiences.create(this);
            this.reloadManager = new ReloadManager(this);
            registeredPlugins.add(this);

            if (!initialized) {
                initializeExylia();
                instance = this;
                initialized = true;
            }
            initializeConfigurationSystem();

            SunLicenseUtil licenseManager = new SunLicenseUtil(this);

            if (!licenseManager.initializeLicense()) {
                getServer().getPluginManager().disablePlugin(this);
            }
            if (api == null) {
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            api.validate();

            Bukkit.getScheduler().runTask(this, this::enablePlugin);
        } catch (Exception e) {
            DebugUtils.logInternalError("License validation failed: " + e.getMessage());
            DebugUtils.logInternalError("You need support? Join our Discord: https://discord.exylia.net/");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public final void onDisable() {
        registeredPlugins.remove(this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
            TitleUtils.cancelAllTitles(player);
            ActionBarUtils.cancelAllActionBars(player);
            BossbarUtils.cancelAllBossBars(player);
        }
        onExyliaDisable();

        if (configSystem != null) {
            configSystem.shutdown();
        }

        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }

        if (registeredPlugins.isEmpty()) {
            shutdownExylia();
            initialized = false;
        }

        logInternalInfo("Plugin Exylia deshabilitado: " + getDescription().getName());
    }

    private void enablePlugin() {
        try {
            onExyliaEnable();
        } catch (Exception e) {
            logInternalError("Error habilitando plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initializeConfigurationSystem() {
        try {
            configSystem = new ConfigurationSystem(this);

            Class<? extends ConfigBase>[] pluginConfigClasses = getConfigurationClasses();

            List<Class<? extends ConfigBase>> allConfigClasses = new ArrayList<>();

            allConfigClasses.add(MainConfigBase.class);
            allConfigClasses.add(MessagesBase.class);

            if (pluginConfigClasses != null && pluginConfigClasses.length > 0) {
                 
                for (Class<? extends ConfigBase> pluginClass : pluginConfigClasses) {

                    if (MainConfigBase.class.isAssignableFrom(pluginClass) && !pluginClass.equals(MainConfigBase.class)) {
                         
                        allConfigClasses.removeIf(cls -> cls.equals(MainConfigBase.class));
                    }

                    if (MessagesBase.class.isAssignableFrom(pluginClass) && !pluginClass.equals(MessagesBase.class)) {
                         
                        allConfigClasses.removeIf(cls -> cls.equals(MessagesBase.class));
                    }

                    allConfigClasses.add(pluginClass);
                }
            }

            Class<? extends ConfigBase>[] finalConfigClasses = allConfigClasses.toArray(new Class[0]);

            configSystem.initialize(finalConfigClasses);
            setupConfigurationListeners();

            ConfigManager.init(configSystem, finalConfigClasses);
            TimeFormatter.init();
            DateFormatter.init();
            
            ColorUtils.initializePresets(this, getCustomColorPresets());
        } catch (Exception e) {
            logInternalError("Error inicializando sistema de configuración: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void setupConfigurationListeners() {
        configSystem.addReloadListener(new ConfigurationSystem.ConfigReloadListener() {
            @Override
            public void onConfigReload(String fileName) {
                onConfigurationFileReload(fileName);
            }

            @Override
            public void onAllConfigsReload() {
                onAllConfigurationsReload();
            }
        });
    }

    protected abstract void onExyliaEnable();

    protected abstract void onExyliaDisable();

    protected abstract Class<? extends ConfigBase>[] getConfigurationClasses();

    protected Map<String, String> getCustomColorPresets() {
        return new LinkedHashMap<>();
    }

    public final ReloadManager getReloadManager() {
        return reloadManager;
    }

    public final CompletableFuture<ReloadResult> reloadAllAsync() {
        return reloadManager.reloadAllAsync();
    }

    public final CompletableFuture<ReloadResult> reloadAllAsync(org.bukkit.command.CommandSender sender) {
        ReloadResult.sendStartMessage(sender);
        return reloadManager.reloadAllAsync()
                .thenApply(result -> {
                    org.bukkit.Bukkit.getScheduler().runTask(this, () ->
                        ReloadResult.sendDetailedReloadResult(sender, result));
                    return result;
                });
    }

    public final CompletableFuture<ReloadResult> reloadAllAsync(long timeoutSeconds) {
        return reloadManager.reloadAllAsync(timeoutSeconds);
    }

    public final CompletableFuture<ReloadResult> reloadAllAsync(org.bukkit.command.CommandSender sender, long timeoutSeconds) {
        ReloadResult.sendStartMessage(sender);
        return reloadManager.reloadAllAsync(timeoutSeconds)
                .thenApply(result -> {
                    org.bukkit.Bukkit.getScheduler().runTask(this, () ->
                        ReloadResult.sendDetailedReloadResult(sender, result));
                    return result;
                });
    }

    public final CompletableFuture<ReloadResult> reloadConfigurationAsync() {
        return reloadManager.reloadConfigurationAsync();
    }

    public final CompletableFuture<ReloadResult> reloadDatabaseAsync() {
        return reloadManager.reloadDatabaseAsync();
    }

    public final CompletableFuture<ReloadResult> reloadRedisAsync() {
        return reloadManager.reloadRedisAsync();
    }

    public final CompletableFuture<ReloadResult> reloadPluginAsync() {
        return reloadManager.reloadPluginAsync();
    }

    protected void onDatabaseReload() {
         
    }

    protected void onRedisReload() {
         
    }

    protected void onPluginReload() {
         
    }

    protected void onConfigurationFileReload(String fileName) {
         
    }

    protected void onAllConfigurationsReload() {
         
    }

    final void callDatabaseReloadHook() {
        onDatabaseReload();
    }

    final void callRedisReloadHook() {
        onRedisReload();
    }

    final void callPluginReloadHook() {
        onPluginReload();
    }

    final void callConfigurationFileReloadHook(String fileName) {
        onConfigurationFileReload(fileName);
    }

    final void callAllConfigurationsReloadHook() {
        onAllConfigurationsReload();
    }

    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Attempted to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExyliaPlugin> T getExyliaPlugin(Class<T> pluginClass) {
        for (ExyliaPlugin plugin : registeredPlugins) {
            if (pluginClass.isInstance(plugin)) {
                return (T) plugin;
            }
        }
        return null;
    }

    public static boolean isPlaceholderAPIEnabled() {
        return Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    private void initializeExylia() {
        try {
            ConfigInitializer.init(this);
            PlaceholderSystemManager.initialize(this);
            AdapterFactory.initialize(this);
            ActionBarUtils.init(this);
            BossbarUtils.init(this);
            TitleUtils.init(this);
            SkullManager.initialize(this);
        } catch (Exception e) {
            logInternalInfo("Error inicializando un sistema: " + e.getMessage());
        }
        checkOptionalDependencies();
    }

    private void checkOptionalDependencies() {
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

    private void shutdownExylia() {
        if (DatabaseManager.getInstance() != null) {
            DatabaseManager.getInstance().shutdown();
        }
        RedisIntegration.shutdownRedis();
        ColorUtils.shutdown();
        OldColorUtils.shutdown();
        AdapterFactory.close();
    }
}

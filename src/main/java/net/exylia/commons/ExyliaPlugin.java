package net.exylia.commons;

import net.exylia.commons.database.DatabaseManager;
import net.exylia.commons.license.LicenseManager;
import net.exylia.commons.placeholders.PlaceholderRegistry;
import net.exylia.commons.redis.RedisIntegration;
import net.exylia.commons.utils.*;
import net.exylia.commons.wizard.LocationWizardManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.exylia.commons.utils.DebugUtils.*;

public abstract class ExyliaPlugin extends JavaPlugin {

    // ===== STATIC FIELDS =====
    private static boolean initialized = false;
    private static final Set<ExyliaPlugin> registeredPlugins = new HashSet<>();
    private static ExyliaPlugin instance;

    // ===== INSTANCE FIELDS =====
    private BukkitAudiences adventure;
    private LicenseManager licenseManager;
    private final boolean requiresLicense;

    // ===== CONSTRUCTOR =====
    protected ExyliaPlugin(boolean requiresLicense) {
        this.requiresLicense = requiresLicense;
    }

    // ===== BUKKIT LIFECYCLE =====
    @Override
    public final void onEnable() {
        this.adventure = BukkitAudiences.create(this);
        registeredPlugins.add(this);

        if (!initialized) {
            initializeExylia();
            instance = this;
            initialized = true;
        }

        licenseManager = new LicenseManager(this, requiresLicense);
        licenseManager.initializeAndVerify()
                .thenRun(() -> Bukkit.getScheduler().runTask(this, this::enablePlugin))
                .exceptionally(throwable -> {
                    Bukkit.getScheduler().runTask(this, () -> {
                        logError("License verification failed: " + throwable.getMessage());
                        getServer().getPluginManager().disablePlugin(this);
                    });
                    return null;
                });
    }

    @Override
    public final void onDisable() {
        registeredPlugins.remove(this);
        onExyliaDisable();

        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }

        if (registeredPlugins.isEmpty()) {
            shutdownExylia();
            initialized = false;
        }

        logInfo("Plugin Exylia deshabilitado: " + getDescription().getName());
    }

    private void enablePlugin() {
        try {
            onExyliaEnable();
            logSuccess("Plugin Exylia habilitado correctamente: " + getDescription().getName());
        } catch (Exception e) {
            logError("Error habilitando plugin: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    // ===== ABSTRACT METHODS (PARA IMPLEMENTAR) =====
    protected abstract void onExyliaEnable();
    protected abstract void onExyliaDisable();

    // ===== RELOAD METHODS =====

    public final boolean reloadAll() {
        try {
            reloadConfig();
            if (!reloadDatabase()) {
                logError("Error en reload de base de datos");
                return false;
            }
            try {
                onPluginReload();
            } catch (Exception e) {
                logError("Error en reload personalizado: " + e.getMessage());
                return false;
            }

            logSuccess("=== RELOAD COMPLETED ===");
            return true;

        } catch (Exception e) {
            logError("Error durante reload completo: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * SIMPLIFICADO: Reload de base de datos ahora es completamente automático
     */
    public final boolean reloadDatabase() {
        try {
            if (DatabaseManager.getInstance() != null) {
                boolean success = DatabaseManager.getInstance().performCompleteReload();

                if (success) {
                    onDatabaseReload();
                    return true;
                } else {
                    logError("Fallo en reload automático de base de datos");
                    return false;
                }
            } else {
                DatabaseManager.initialize(this);

                if (DatabaseManager.getInstance().isConnected()) {
                    onDatabaseReload();
                    return true;
                } else {
                    logError("Error: No se pudo inicializar la base de datos");
                    return false;
                }
            }
        } catch (Exception e) {
            logError("Error crítico en reload de base de datos: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ===== HOOKS OPCIONALES PARA LOS PLUGINS =====

    protected void onDatabaseReload() {
    }

    protected void onPluginReload() {
    }

    // ===== LICENSE METHODS =====
    protected final boolean shouldOperate() {
        return licenseManager != null && licenseManager.isVerified();
    }

    protected final void requireLicense(String feature) {
        if (licenseManager == null || !licenseManager.isVerified()) {
            throw new SecurityException("La función '" + feature + "' requiere una licencia válida");
        }
    }

    // ===== ADVENTURE API =====
    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Attempted to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    // ===== STATIC UTILITY METHODS =====
    @SuppressWarnings("unchecked")
    public static <T extends ExyliaPlugin> T getExyliaPlugin(Class<T> pluginClass) {
        for (ExyliaPlugin plugin : registeredPlugins) {
            if (pluginClass.isInstance(plugin)) {
                return (T) plugin;
            }
        }
        return null;
    }

    public static ExyliaPlugin getInstance() {
        return instance;
    }

    public static boolean isPlaceholderAPIEnabled() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    // ===== PRIVATE INITIALIZATION METHODS =====
    private void initializeExylia() {
        try {
            AdapterFactory.initialize(this);
            ActionBarUtils.init(this);
            BossbarUtils.init(this);
            TitleUtils.init(this);
        } catch (Exception e) {
            logInfo("Error inicializando un sistema: " + e.getMessage());
        }
        checkOptionalDependencies();
        logInfo("Núcleo Exylia inicializado correctamente");
    }

    private void checkOptionalDependencies() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            logInfo("PlaceholderAPI detectado.");
        }
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
        logInfo("Limpiando recursos globales de Exylia");
        try {
            if (DatabaseManager.getInstance() != null) {
                DatabaseManager.getInstance().shutdown();
            }
        } catch (Exception e) {
            logInfo("Error cerrando sistema de base de datos: " + e.getMessage());
        }
        try {
            if (LocationWizardManager.getInstance() != null) {
                LocationWizardManager.getInstance().cleanup();
            }
        } catch (Exception e) {
            logInfo("Error limpiando wizards: " + e.getMessage());
        }
        RedisIntegration.shutdownRedis();
        ColorUtils.shutdown();
        OldColorUtils.shutdown();
        AdapterFactory.close();
        PlaceholderRegistry.clear();
    }
}
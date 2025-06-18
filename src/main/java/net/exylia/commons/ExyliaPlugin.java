package net.exylia.commons;

import net.exylia.commons.command.BungeeMessageSender;
import net.exylia.commons.database.DatabaseManager;
import net.exylia.commons.item.ItemManager;
import net.exylia.commons.item.cooldown.CooldownManager;
import net.exylia.commons.menu.MenuActionManager;
import net.exylia.commons.menu.MenuManager;
import net.exylia.commons.placeholders.PlaceholderRegistry;
import net.exylia.commons.redis.RedisIntegration;
import net.exylia.commons.utils.*;
import net.exylia.commons.wizard.LocationWizardManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

import static net.exylia.commons.utils.DebugUtils.logInfo;

public abstract class ExyliaPlugin extends JavaPlugin {
    private static boolean initialized = false;
    private static final Set<ExyliaPlugin> registeredPlugins = new HashSet<>();
    private BukkitAudiences adventure;
    private static ExyliaPlugin instance;

    @Override
    public final void onEnable() {
        this.adventure = BukkitAudiences.create(this);
        registeredPlugins.add(this);

        if (!initialized) {
            initializeExylia();
            instance = this;
            initialized = true;
        }

        onExyliaEnable();
        logInfo("Plugin Exylia habilitado correctamente: " + getDescription().getName());
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

    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Attempted to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    public BukkitAudiences getAudience() {
        return adventure();
    }

    private void initializeExylia() {
        try {
            AdapterFactory.initialize(this);
//            MenuManager.initialize(this);
//            ItemManager.initialize(this);
//            ConfirmationManager.initialize(this);
//            BungeeMessageSender.initialize(this);
//            LocationWizardManager.initialize(this);
//            DatabaseManager.initialize(this);
//            RedisIntegration.initializeRedis(this);
//            CooldownManager.initialize(this);
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
        // Verificar H2
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ignored) {
        }

        // Verificar MySQL
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
        MenuActionManager.unregisterPluginActions(this);
    }

    /**
     * Obtiene la instancia de un plugin Exylia registrado por su clase.
     *
     * @param pluginClass La clase del plugin que se desea obtener
     * @return La instancia del plugin o null si no está registrado
     */
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

    /**
     * Verifica si Redis está disponible y funcionando
     */
    public static boolean isRedisAvailable() {
        try {
            return net.exylia.commons.redis.RedisManager.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica si el sistema de base de datos está disponible
     */
    public static boolean isDatabaseAvailable() {
        try {
            return DatabaseManager.getInstance() != null && DatabaseManager.getInstance().isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene el manager de base de datos
     */
    public static DatabaseManager getDatabaseManager() {
        return DatabaseManager.getInstance();
    }

    protected abstract void onExyliaEnable();
    protected abstract void onExyliaDisable();
}
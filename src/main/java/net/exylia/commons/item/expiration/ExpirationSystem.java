package net.exylia.commons.item.expiration;

import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Sistema principal de expiración automática de items
 * Se encarga de inicializar y coordinar todos los componentes
 */
public class ExpirationSystem {

    private static ExpirationSystem instance;
    private final JavaPlugin plugin;
    private ExpirationManager manager;
    private ExpirationListener listener;
    private ExpirationConfig config;
    private boolean initialized = false;

    private ExpirationSystem(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicializa el sistema de expiración
     */
    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new ExpirationSystem(plugin);
        }
        instance.start();
    }

    /**
     * Obtiene la instancia del sistema
     */
    public static ExpirationSystem getInstance() {
        return instance;
    }

    /**
     * Inicia todos los componentes del sistema
     */
    public void start() {
        if (initialized) {
            return;
        }

        try {
            // Inicializar configuración
            config = new ExpirationConfig(plugin);
            config.loadFromConfig();
            
            // Inicializar manager
            ExpirationManager.initialize(plugin);
            manager = ExpirationManager.getInstance();
            
            // Aplicar configuración al manager
            config.applyToManager();
            
            // Registrar listener
            listener = new ExpirationListener();
            Bukkit.getPluginManager().registerEvents(listener, plugin);
            manager.start();
            initialized = true;
            DebugUtils.logInternalInfo("Sistema de expiración automática iniciado correctamente");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error al inicializar el sistema de expiración: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Detiene el sistema de expiración
     */
    public void stop() {
        if (!initialized) {
            return;
        }

        try {
            if (manager != null) {
                manager.stop();
            }
            
            initialized = false;
            DebugUtils.logInternalInfo("Sistema de expiración automática detenido");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error al detener el sistema de expiración: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Recarga la configuración
     */
    public void reload() {
        if (!initialized) {
            return;
        }

        try {
            config.loadFromConfig();
            config.applyToManager();
            DebugUtils.logInternalInfo("Configuración de expiración recargada");
        } catch (Exception e) {
            plugin.getLogger().severe("Error al recargar configuración de expiración: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Getters para acceso a componentes
    public ExpirationManager getManager() { return manager; }
    public ExpirationListener getListener() { return listener; }
    public ExpirationConfig getConfig() { return config; }
    public boolean isInitialized() { return initialized; }
}
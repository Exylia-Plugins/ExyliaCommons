package net.exylia.commons.item.expiration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Configuración para el sistema de expiración automática
 */
public class ExpirationConfig {
    
    private final JavaPlugin plugin;
    private final String configSection = "item-expiration";
    
    // Valores por defecto
    private boolean enabled = true;
    private double checkIntervalSeconds = 5.0; // 5 segundos
    private boolean enableDebugMessages = false;
    private boolean notifyOnRemoval = true;
    private boolean notifyOnDisable = true;
    private boolean notifyOnTransform = true;
    private boolean checkOnPlayerJoin = true;
    private boolean checkOnInventoryClick = true;
    private boolean updatePlaceholders = true;

    public ExpirationConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Carga la configuración desde el archivo config.yml
     */
    public void loadFromConfig() {
        FileConfiguration config = plugin.getConfig();
        
        // Crear sección por defecto si no existe
        if (!config.contains(configSection)) {
            setDefaults();
            plugin.saveConfig();
            return;
        }
        
        String section = configSection + ".";
        
        enabled = config.getBoolean(section + "enabled", enabled);
        checkIntervalSeconds = config.getDouble(section + "check-interval-seconds", checkIntervalSeconds);
        enableDebugMessages = config.getBoolean(section + "debug-messages", enableDebugMessages);
        notifyOnRemoval = config.getBoolean(section + "notifications.on-removal", notifyOnRemoval);
        notifyOnDisable = config.getBoolean(section + "notifications.on-disable", notifyOnDisable);
        notifyOnTransform = config.getBoolean(section + "notifications.on-transform", notifyOnTransform);
        checkOnPlayerJoin = config.getBoolean(section + "triggers.on-player-join", checkOnPlayerJoin);
        checkOnInventoryClick = config.getBoolean(section + "triggers.on-inventory-click", checkOnInventoryClick);
        updatePlaceholders = config.getBoolean(section + "update-placeholders", updatePlaceholders);

        // Validaciones
        if (checkIntervalSeconds < 0.1) {
            checkIntervalSeconds = 0.1;
        }
    }

    /**
     * Establece valores por defecto en la configuración
     */
    public void setDefaults() {
        FileConfiguration config = plugin.getConfig();
        String section = configSection + ".";
        
        config.set(section + "enabled", enabled);
        config.set(section + "check-interval-seconds", checkIntervalSeconds);
        config.set(section + "debug-messages", enableDebugMessages);
        config.set(section + "notifications.on-removal", notifyOnRemoval);
        config.set(section + "notifications.on-disable", notifyOnDisable);
        config.set(section + "notifications.on-transform", notifyOnTransform);
        config.set(section + "triggers.on-player-join", checkOnPlayerJoin);
        config.set(section + "triggers.on-inventory-click", checkOnInventoryClick);
        config.set(section + "update-placeholders", updatePlaceholders);
        
        // Agregar comentarios
        config.setComments(configSection, java.util.Arrays.asList(
            "Configuración del sistema de expiración automática de items",
            "Este sistema verifica periódicamente los inventarios de los jugadores",
            "y aplica automáticamente los comportamientos de expiración configurados"
        ));
    }

    /**
     * Guarda la configuración actual al archivo
     */
    public void saveToConfig() {
        FileConfiguration config = plugin.getConfig();
        String section = configSection + ".";
        
        config.set(section + "enabled", enabled);
        config.set(section + "check-interval-seconds", checkIntervalSeconds);
        config.set(section + "debug-messages", enableDebugMessages);
        config.set(section + "notifications.on-removal", notifyOnRemoval);
        config.set(section + "notifications.on-disable", notifyOnDisable);
        config.set(section + "notifications.on-transform", notifyOnTransform);
        config.set(section + "triggers.on-player-join", checkOnPlayerJoin);
        config.set(section + "triggers.on-inventory-click", checkOnInventoryClick);
        config.set(section + "update-placeholders", updatePlaceholders);
        
        plugin.saveConfig();
    }

    /**
     * Aplica la configuración al ExpirationManager
     */
    public void applyToManager() {
        ExpirationManager manager = ExpirationManager.getInstance();
        if (manager != null) {
            manager.setEnabled(enabled);
            manager.setCheckIntervalSeconds(checkIntervalSeconds);
            manager.setEnableDebugMessages(enableDebugMessages);
            manager.setUpdatePlaceholders(updatePlaceholders);
        }
    }

    // Getters y Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public double getCheckIntervalSeconds() { return checkIntervalSeconds; }
    public void setCheckIntervalSeconds(double checkIntervalSeconds) { 
        this.checkIntervalSeconds = Math.max(0.1, checkIntervalSeconds); 
    }

    public boolean isEnableDebugMessages() { return enableDebugMessages; }
    public void setEnableDebugMessages(boolean enableDebugMessages) { this.enableDebugMessages = enableDebugMessages; }

    public boolean isNotifyOnRemoval() { return notifyOnRemoval; }
    public void setNotifyOnRemoval(boolean notifyOnRemoval) { this.notifyOnRemoval = notifyOnRemoval; }

    public boolean isNotifyOnDisable() { return notifyOnDisable; }
    public void setNotifyOnDisable(boolean notifyOnDisable) { this.notifyOnDisable = notifyOnDisable; }

    public boolean isNotifyOnTransform() { return notifyOnTransform; }
    public void setNotifyOnTransform(boolean notifyOnTransform) { this.notifyOnTransform = notifyOnTransform; }

    public boolean isCheckOnPlayerJoin() { return checkOnPlayerJoin; }
    public void setCheckOnPlayerJoin(boolean checkOnPlayerJoin) { this.checkOnPlayerJoin = checkOnPlayerJoin; }

    public boolean isCheckOnInventoryClick() { return checkOnInventoryClick; }
    public void setCheckOnInventoryClick(boolean checkOnInventoryClick) { this.checkOnInventoryClick = checkOnInventoryClick; }

    public boolean isUpdatePlaceholders() { return updatePlaceholders; }
    public void setUpdatePlaceholders(boolean updatePlaceholders) { this.updatePlaceholders = updatePlaceholders; }
}
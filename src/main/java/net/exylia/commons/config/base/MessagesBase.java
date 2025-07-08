package net.exylia.commons.config.base;

import net.exylia.commons.config.*;
import net.exylia.commons.placeholders.ExyliaContext;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Configuración base para messages.yml que contiene métodos comunes de mensajería
 * Esta clase debe ser extendida por Messages en cada plugin
 */
@ConfigFile(value = "messages", required = true)
public class MessagesBase extends ConfigBase {

    // ===== IMPLEMENTACIÓN DE MessageConfig =====

    @Override
    public ConfigurationSystem getSystem() {
        return ConfigManager.getSystem();
    }

    public FileConfiguration file() {
        return getActiveFileConfiguration();
    }

    // ===== MÉTODOS ESTÁTICOS PRINCIPALES =====

    /**
     * Obtiene un mensaje simple
     */
    public static Component get(String path) {
        return getActiveInstance().getSystem().getMessage(path);
    }

    /**
     * Obtiene un mensaje con reemplazos
     */
    public static Component get(String path, Object... replacements) {
        return getActiveInstance().getSystem().getMessage(path, replacements);
    }

    /**
     * Obtiene un mensaje con contexto
     */
    public static Component getWithContext(String path, Object context) {
        return getActiveInstance().getSystem().message(path).withContext(ExyliaContext.of(context)).build();
    }

    /**
     * Obtiene un mensaje para un jugador específico
     */
    public static Component forPlayer(String path, Player player) {
        return getActiveInstance().getSystem().message(path).forPlayer(player).build();
    }

    /**
     * Obtiene un mensaje para un jugador con contexto
     */
    public static Component forPlayerContext(String path, Player player, Object context) {
        return getActiveInstance().getSystem().message(path)
                .forPlayer(player)
                .withContext(ExyliaContext.of(context))
                .build();
    }

    /**
     * Obtiene un MessageBuilder para configuración avanzada
     */
    public static ConfigurationSystem.MessageBuilder message(String path) {
        return getActiveInstance().getSystem().message(path);
    }

    /**
     * Obtiene el texto crudo de un mensaje
     */
    public static String getRaw(String path) {
        return getActiveInstance().file().getString(path, path);
    }

    /**
     * Obtiene el texto crudo con valor por defecto
     */
    public static String getRaw(String path, String defaultValue) {
        return getActiveInstance().file().getString(path, defaultValue);
    }

    // ===== MÉTODOS INTERNOS =====

    /**
     * Obtiene la instancia activa de MessagesBase o su extensión
     */
    private static MessagesBase getActiveInstance() {
        // Primero intentar obtener la extensión del plugin actual
        try {
            for (Class<?> clazz : ConfigManager.getSystem().getConfigInstances().keySet()) {
                if (MessagesBase.class.isAssignableFrom(clazz) && !clazz.equals(MessagesBase.class)) {
                    return (MessagesBase) ConfigManager.getSystem().getConfigInstances().get(clazz);
                }
            }
        } catch (Exception e) {
            // Si falla, usar la instancia base
        }

        // Fallback a la instancia base
        try {
            return ConfigManager.get(MessagesBase.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo obtener la instancia de MessagesBase");
        }
    }

    /**
     * Obtiene el FileConfiguration activo (de la extensión o base)
     */
    private FileConfiguration getActiveFileConfiguration() {
        try {
            // Intentar obtener desde la extensión
            MessagesBase activeInstance = getActiveInstance();
            if (activeInstance != this) {
                return activeInstance.getConfig();
            }
            return getConfig();
        } catch (Exception e) {
            return getConfig();
        }
    }
}
package net.exylia.commons.config;

import net.exylia.commons.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.exylia.commons.utils.DebugUtils.logInfo;

/**
 * Manejador de configuraciones para plugins de Exylia
 * Permite administrar múltiples archivos de configuración YAML con sistema de presets de colores global
 */
public class ConfigManager {
    private final JavaPlugin plugin;
    private static final Map<String, FileConfiguration> configs = new HashMap<>();
    private String prefix = "";

    /**
     * Constructor del ConfigManager
     * @param plugin El plugin que utiliza esta configuración
     * @param files Lista de nombres de archivo (sin extensión) a cargar
     */
    public ConfigManager(JavaPlugin plugin, List<String> files) {
        this.plugin = plugin;
        ColorUtils.initializePresets(plugin);
        for (String file : files) {
            loadConfig(file);
        }
        if (configs.containsKey("messages")) {
            this.prefix = getConfig("messages").getString("prefix", "");
        }
    }

    /**
     * Carga un archivo de configuración
     * @param fileName Nombre del archivo sin extensión
     */
    private void loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName + ".yml");
        if (!file.exists()) {
            plugin.saveResource(fileName + ".yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(fileName, config);
    }

    /**
     * Obtiene un mensaje personalizado con soporte para presets de colores y Components
     * Soporta tanto String como Component como valores de reemplazo
     * @param path La ruta del mensaje en el archivo de mensajes
     * @param replacements Los reemplazos de placeholders (String placeholder, Object value, ...)
     * @return El componente del mensaje personalizado
     */
    public Component getMessage(String path, Object... replacements) {
        String message = getConfig("messages").getString(path, "{error}" + path + " not found in messages.yml");

        // Aplicar prefix antes de convertir a Component
        message = applyPrefix(message);

        // Convertir el mensaje base a Component
        Component component = ColorUtils.parse(message);

        // Aplicar replacements usando TextReplacementConfig
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = replacements[i].toString();
            Object value = replacements[i + 1];

            Component replacement;
            if (value instanceof Component) {
                replacement = (Component) value;
            } else {
                // Si es String u otro objeto, parsearlo con ColorUtils
                replacement = ColorUtils.parse(value.toString());
            }

            component = component.replaceText(
                    TextReplacementConfig.builder()
                            .match(placeholder)
                            .replacement(replacement)
                            .build()
            );
        }

        return component;
    }

    /**
     * Obtiene un mensaje personalizado con soporte para presets de colores (versión original)
     * @param path La ruta del mensaje en el archivo de mensajes
     * @param replacements Los reemplazos de placeholders (solo String)
     * @return El componente del mensaje personalizado
     */
    public Component getMessageString(String path, String... replacements) {
        String message = getConfig("messages").getString(path, "{error}" + path + " not found in messages.yml");

        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }

        message = applyPrefix(message);
        return ColorUtils.parse(message);
    }

    /**
     * Obtiene un mensaje personalizado con soporte para presets de colores
     * @param path La ruta del mensaje en el archivo de mensajes
     * @return El componente del mensaje personalizado
     */
    public Component getMessage(String path) {
        String message = getConfig("messages").getString(path, "{error}" + path + " not found in messages.yml");
        message = applyPrefix(message);
        return ColorUtils.parse(message);
    }

    /**
     * Obtiene un mensaje como string (sin convertir a Component) con presets aplicados
     * Útil para casos donde necesitas el string procesado
     * @param path La ruta del mensaje en el archivo de mensajes
     * @param replacements Los reemplazos de placeholders
     * @return El string del mensaje con presets y placeholders aplicados
     */
    public String getMessageStringOnly(String path, String... replacements) {
        String message = getConfig("messages").getString(path, "{error}" + path + " not found in messages.yml");

        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }

        message = applyPrefix(message);
        return ColorUtils.parseToString(message);
    }

    /**
     * Obtiene un mensaje como string (sin convertir a Component) con presets aplicados
     * @param path La ruta del mensaje en el archivo de mensajes
     * @return El string del mensaje con presets aplicados
     */
    public String getMessageStringOnly(String path) {
        String message = getConfig("messages").getString(path, "{error}" + path + " not found in messages.yml");
        message = applyPrefix(message);
        return ColorUtils.parseToString(message);
    }

    /**
     * Versión avanzada que permite especificar si usar Components o String processing
     * @param path La ruta del mensaje
     * @param useComponentReplacement Si usar Component replacement (true) o String replacement (false)
     * @param replacements Los reemplazos
     * @return El componente del mensaje
     */
    public Component getMessage(String path, boolean useComponentReplacement, Object... replacements) {
        if (useComponentReplacement) {
            return getMessage(path, replacements);
        } else {
            String[] stringReplacements = new String[replacements.length];
            for (int i = 0; i < replacements.length; i++) {
                stringReplacements[i] = replacements[i].toString();
            }
            return getMessageString(path, stringReplacements);
        }
    }

    /**
     * Aplica placeholders al mensaje
     * @param message El mensaje original
     * @return El mensaje con placeholders reemplazados
     */
    private String applyPrefix(String message) {
        return message.replace("%prefix%", prefix);
    }

    /**
     * Obtiene una configuración por nombre de archivo
     * @param fileName Nombre del archivo sin extensión
     * @return El objeto FileConfiguration correspondiente
     */
    public FileConfiguration getConfig(String fileName) {
        return configs.get(fileName);
    }

    /**
     * Obtiene la configuración principal
     * @return El objeto FileConfiguration correspondiente
     */
    public FileConfiguration getConfig() {
        return getConfig("config");
    }

    /**
     * Recarga una configuración específica
     * @param fileName Nombre del archivo sin extensión
     */
    public void reloadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(fileName, config);

        // Si se recarga colors.yml, actualizar presets en ColorUtils
        if ("colors".equals(fileName)) {
            ColorUtils.reloadPresets();
        }
    }

    /**
     * Recarga todas las configuraciones
     */
    public void reloadAllConfigs() {
        // Primero recargar colors.yml para actualizar presets
        if (configs.containsKey("colors")) {
            ColorUtils.reloadPresets();
        }

        for (String fileName : configs.keySet()) {
            reloadConfig(fileName);
        }

        if (configs.containsKey("messages")) {
            this.prefix = getConfig("messages").getString("prefix", "");
        }
    }

    /**
     * Guarda una configuración específica en su archivo correspondiente
     * @param fileName Nombre del archivo sin extensión
     * @throws IOException Si ocurre un error al guardar el archivo
     */
    public void saveConfig(String fileName) throws IOException {
        FileConfiguration config = configs.get(fileName);
        if (config == null) {
            throw new IllegalArgumentException("No existe la configuración: " + fileName);
        }

        File file = new File(plugin.getDataFolder(), fileName + ".yml");
        try {
            config.save(file);
            logInfo("Configuración " + fileName + ".yml guardada exitosamente");
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando configuración " + fileName + ".yml: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Guarda una configuración específica en su archivo correspondiente (versión sin excepción)
     * @param fileName Nombre del archivo sin extensión
     * @return true si se guardó exitosamente, false si ocurrió un error
     */
    public boolean saveConfigSafe(String fileName) {
        try {
            saveConfig(fileName);
            return true;
        } catch (IOException | IllegalArgumentException e) {
            plugin.getLogger().severe("Error guardando configuración " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Guarda todas las configuraciones cargadas
     * @throws IOException Si ocurre un error al guardar algún archivo
     */
    public void saveAllConfigs() throws IOException {
        for (String fileName : configs.keySet()) {
            saveConfig(fileName);
        }
        logInfo("Todas las configuraciones han sido guardadas");
    }

    /**
     * Guarda todas las configuraciones cargadas (versión sin excepción)
     * @return true si todas se guardaron exitosamente, false si alguna falló
     */
    public boolean saveAllConfigsSafe() {
        boolean allSaved = true;
        for (String fileName : configs.keySet()) {
            if (!saveConfigSafe(fileName)) {
                allSaved = false;
            }
        }
        return allSaved;
    }
}
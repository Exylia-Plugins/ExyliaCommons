package net.exylia.commons.config;

import net.exylia.commons.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.exylia.commons.utils.DebugUtils.logInfo;

/**
 * Manejador de configuraciones para plugins de Exylia
 * Permite administrar múltiples archivos de configuración YAML con sistema de presets de colores
 */
public class ConfigManager {
    private final JavaPlugin plugin;
    private static final Map<String, FileConfiguration> configs = new HashMap<>();
    private static final Map<String, String> colorPresets = new HashMap<>();
    private String prefix = "";

    // Patrón para detectar presets: {preset_name}
    private static final Pattern PRESET_PATTERN = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

    /**
     * Constructor del ConfigManager
     * @param plugin El plugin que utiliza esta configuración
     * @param files Lista de nombres de archivo (sin extensión) a cargar
     */
    public ConfigManager(JavaPlugin plugin, List<String> files) {
        this.plugin = plugin;
        loadColorPresets();
        for (String file : files) {
            loadConfig(file);
        }
        if (configs.containsKey("messages")) {
            this.prefix = getConfig("messages").getString("prefix", "");
        }
    }

    private void loadColorPresets() {
        File configFile = new File(plugin.getDataFolder(), "colors.yml");

        if (!configFile.exists()) {
            createDefaultColorPresets(configFile);
        }

        // Cargar presets en memoria
        FileConfiguration colorConfig = YamlConfiguration.loadConfiguration(configFile);
        colorPresets.clear();

        for (String key : colorConfig.getKeys(false)) {
            String value = colorConfig.getString(key);
            if (value != null) {
                colorPresets.put(key.toLowerCase(), value);
            }
        }
        logInfo("Se cargaron " + colorPresets.size() + " presets de colores.");
    }

    private void createDefaultColorPresets(File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Colores principales
            config.set("primary", "<#8a51c4>");
            config.set("secondary", "<#aa76de>");
            config.set("secondary_light", "<#b48fd9>");

            // Colores de texto
            config.set("letters", "<#e7cfff>");
            config.set("letters_black", "<#a89ab5>");

            // Colores de estados
            config.set("error", "<#a33b53>");
            config.set("success", "<#8fffc1>");
            config.set("success_light", "<#a1ffc3>");
            config.set("warning", "<#ff9500>");
            config.set("warning_light", "<#ffd2a8>");
            config.set("info", "<#59a4ff>");
            config.set("info_light", "<#7db7ff>");

            // Colores adicionales comunes
            config.set("accent", "<#ff6b9d>");
            config.set("neutral", "<#6c757d>");
            config.set("highlight", "<#ffd700>");
            config.set("muted", "<#868e96>");

            // Gradientes (para usar con MiniMessage)
            config.set("gradient_primary", "<gradient:#8a51c4:#aa76de>");
            config.set("gradient_success", "<gradient:#8fffc1:#a1ffc3>");
            config.set("gradient_warning", "<gradient:#ff9500:#ffd2a8>");
            config.set("gradient_error", "<gradient:#a33b53:#ff6b9d>");

            config.save(configFile);
            logInfo("Archivo colors.yml creado con presets por defecto");

        } catch (IOException e) {
            plugin.getLogger().severe("Error creando archivo colors.yml: " + e.getMessage());
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
     * Obtiene un mensaje personalizado con soporte para presets de colores
     * @param path La ruta del mensaje en el archivo de mensajes
     * @param replacements Los reemplazos de placeholders
     * @return El componente del mensaje personalizado
     */
    public Component getMessage(String path, String... replacements) {
        String message = getConfig("messages").getString(path, "{error}" + path + " not found in messages.yml");

        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }

        message = applyPrefix(message);
        message = applyColorPresets(message);

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
        message = applyColorPresets(message);
        return ColorUtils.parse(message);
    }

    /**
     * Aplica los presets de colores al mensaje
     * @param message El mensaje con presets {preset_name}
     * @return El mensaje con presets reemplazados por códigos de color
     */
    public static String applyColorPresets(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        Matcher matcher = PRESET_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String presetName = matcher.group(1).toLowerCase();
            String colorCode = colorPresets.get(presetName);

            if (colorCode != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(colorCode));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        return result.toString();
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

        // Si se recarga colors.yml, actualizar presets
        if ("colors".equals(fileName)) {
            loadColorPresets();
        }
    }

    /**
     * Recarga todas las configuraciones
     */
    public void reloadAllConfigs() {
        // Primero recargar colors.yml para actualizar presets
        if (configs.containsKey("colors")) {
            loadColorPresets();
        }

        for (String fileName : configs.keySet()) {
            reloadConfig(fileName);
        }

        if (configs.containsKey("messages")) {
            this.prefix = getConfig("messages").getString("prefix", "");
        }
    }

    /**
     * Obtiene un preset de color específico
     * @param presetName Nombre del preset
     * @return El código de color del preset, o null si no existe
     */
    public String getColorPreset(String presetName) {
        return colorPresets.get(presetName.toLowerCase());
    }

    /**
     * Obtiene todos los presets de colores disponibles
     * @return Mapa con todos los presets de colores
     */
    public Map<String, String> getAllColorPresets() {
        return new HashMap<>(colorPresets);
    }

    /**
     * Aplica presets de colores a un mensaje personalizado (útil para otros usos)
     * @param message Mensaje con presets {preset_name}
     * @return Mensaje con presets aplicados
     */
    public String applyPresetsToString(String message) {
        return applyColorPresets(message);
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
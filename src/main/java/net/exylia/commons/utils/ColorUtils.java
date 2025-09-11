package net.exylia.commons.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.DebugUtils.logInternalInfo;
import static net.exylia.commons.utils.DebugUtils.logInternalSuccess;

/**
 * Utilidades optimizadas para manejar colores y componentes de texto
 * utilizando MiniMessage con caché para mejorar el rendimiento
 * Incluye soporte global para presets de colores
 */
public class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Cache<String, Component> COMPONENT_CACHE = new Cache<>(1800000, 500, 300000);

    // Sistema de presets global
    private static final Map<String, String> colorPresets = new HashMap<>();
    private static final Pattern PRESET_PATTERN = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static JavaPlugin pluginInstance;
    private static boolean presetsInitialized = false;
    
    // Cache específico para strings procesados (antes de Component)
    private static final Cache<String, String> PROCESSED_STRING_CACHE = new Cache<>(1800000, 300, 300000);
    
    // Sistema de transformación de fuentes
    private static final Map<Character, Character> SMALL_FONT_MAP = new HashMap<>();
    static {
        initializeSmallFontMap();
    }

    /**
     * Inicializa el sistema de presets de colores
     * @param plugin Instancia del plugin para acceder a los archivos
     */
    public static void initializePresets(JavaPlugin plugin) {
        pluginInstance = plugin;
        loadColorPresets();
        presetsInitialized = true;
    }

    /**
     * Inicializa el sistema de presets de colores con presets personalizados
     * @param plugin Instancia del plugin para acceder a los archivos
     * @param customPresets Map con presets personalizados del plugin
     */
    public static void initializePresets(JavaPlugin plugin, Map<String, String> customPresets) {
        pluginInstance = plugin;
        
        // Si hay presets personalizados, usarlos como base
        if (customPresets != null && !customPresets.isEmpty()) {
            colorPresets.clear();
            
            // Añadir presets personalizados primero
            for (Map.Entry<String, String> entry : customPresets.entrySet()) {
                String key = entry.getKey().toLowerCase();
                String value = entry.getValue();
                if (key != null && value != null) {
                    colorPresets.put(key, value);
                }
            }
            
            // Crear/actualizar archivo colors.yml con los presets personalizados
            createCustomColorPresetsFile(customPresets);
            
        } else {
            // Si no hay presets personalizados, usar la lógica normal
            loadColorPresets();
        }
        
        presetsInitialized = true;
    }

    /**
     * Carga los presets de colores desde colors.yml
     */
    private static void loadColorPresets() {
        loadColorPresetsFromFile(true);
    }

    /**
     * Carga los presets de colores desde colors.yml
     * @param createIfNotExists Si crear el archivo con presets por defecto si no existe
     */
    private static void loadColorPresetsFromFile(boolean createIfNotExists) {
        if (pluginInstance == null) {
            return;
        }

        File configFile = new File(pluginInstance.getDataFolder(), "colors.yml");

        if (!configFile.exists()) {
            if (createIfNotExists) {
                createDefaultColorPresets(configFile);
            } else {
                // No existe archivo y no se debe crear, usar solo presets personalizados
                logInternalInfo("colors.yml not found, using only custom presets.");
                return;
            }
        }

        // Cargar presets desde archivo (sin limpiar los existentes si ya hay custom)
        FileConfiguration colorConfig = YamlConfiguration.loadConfiguration(configFile);
        
        if (createIfNotExists) {
            colorPresets.clear(); // Solo limpiar si no hay presets personalizados
        }

        for (String key : colorConfig.getKeys(false)) {
            String value = colorConfig.getString(key);
            if (value != null) {
                String lowerKey = key.toLowerCase();
                // Solo añadir si no existe ya (para no sobrescribir presets personalizados)
                if (!colorPresets.containsKey(lowerKey)) {
                    colorPresets.put(lowerKey, value);
                }
            }
        }
        logInternalSuccess("Loaded " + colorPresets.size() + " color presets.");
    }

    /**
     * Crea el archivo colors.yml con presets personalizados del plugin
     */
    private static void createCustomColorPresetsFile(Map<String, String> customPresets) {
        if (pluginInstance == null) {
            return;
        }

        File configFile = new File(pluginInstance.getDataFolder(), "colors.yml");
        
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Limpiar contenido existente
            for (String key : config.getKeys(false)) {
                config.set(key, null);
            }

            // Añadir presets personalizados
            for (Map.Entry<String, String> entry : customPresets.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    config.set(key, value);
                }
            }

            config.save(configFile);
        } catch (IOException e) {
            if (pluginInstance != null) {
                pluginInstance.getLogger().severe("ColorUtils: Error creando archivo colors.yml: " + e.getMessage());
            }
        }
    }

    /**
     * Crea el archivo colors.yml con presets por defecto
     */
    private static void createDefaultColorPresets(File configFile) {
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
            logInternalInfo("ColorUtils: Archivo colors.yml creado con presets por defecto");

        } catch (IOException e) {
            if (pluginInstance != null) {
                pluginInstance.getLogger().severe("ColorUtils: Error creando archivo colors.yml: " + e.getMessage());
            }
        }
    }

    /**
     * Aplica los presets de colores al mensaje
     * @param message El mensaje con presets {preset_name}
     * @return El mensaje con presets reemplazados por códigos de color
     */
    public static String applyColorPresets(String message) {
        if (message == null || message.isEmpty() || !presetsInitialized) {
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
     * Traduce códigos de color a componentes Adventure con caché y soporte para presets
     *
     * @param message Mensaje con códigos de color y presets
     * @return Componente con colores y formato aplicados
     */
    public static Component parse(String message) {
        if (message == null) {
            return Component.empty();
        }

        // Usar cache para evitar reprocesamiento
        return COMPONENT_CACHE.get(message, key -> {
            String processed = applyColorPresets(key); // Aplicar presets primero
            processed = preprocessColorCodes(processed); // Aplicar códigos de color
            processed = applyFontTransformation(processed); // Aplicar transformación de fuente al final
            return MINI_MESSAGE.deserialize(processed)
                    .decoration(TextDecoration.ITALIC, false);
        });
    }

    /**
     * Traduce códigos de color a componentes Adventure de forma asíncrona
     * @param message Mensaje con códigos de color y presets
     * @return CompletableFuture con el componente procesado
     */
    public static CompletableFuture<Component> parseAsync(String message) {
        return CompletableFuture.supplyAsync(() -> parse(message));
    }

    /**
     * Traduce una lista de mensajes a componentes
     * @param messages Lista de mensajes con códigos de color y presets
     * @return Lista de componentes procesados
     */
    public static List<Component> parse(List<String> messages) {
        return messages.stream()
                .map(ColorUtils::parse)
                .collect(Collectors.toList());
    }

    /**
     * Traduce una lista de mensajes asíncronamente
     * @param messages Lista de mensajes con códigos de color y presets
     * @return CompletableFuture con la lista de componentes
     */
    public static CompletableFuture<List<Component>> parseAsync(List<String> messages) {
        return CompletableFuture.supplyAsync(() -> parse(messages));
    }

    /**
     * Convierte un string con presets a string con códigos de color (sin convertir a Component)
     * Útil para cuando necesitas el string procesado pero no el Component
     * @param message Mensaje con presets y códigos de color
     * @return String con presets aplicados y códigos de color preprocesados
     */
    public static String parseToString(String message) {
        if (message == null) {
            return "";
        }

        String processed = applyColorPresets(message); // Aplicar presets primero
        processed = preprocessColorCodes(processed); // Aplicar códigos de color
        return applyFontTransformation(processed); // Aplicar transformación de fuente al final
    }

    /**
     * Preprocesa códigos de color ampersand (&) a formato MiniMessage - OPTIMIZADO
     * @param message Mensaje con códigos de color
     * @return Mensaje con códigos convertidos a formato MiniMessage
     */
    private static String preprocessColorCodes(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // Usar cache para evitar reprocesamiento del mismo string
        return PROCESSED_STRING_CACHE.get(message, key -> {
            String result = key.replace('§', '&');

            // Usar patrón compilado para códigos hex
            result = COLOR_CODE_PATTERN.matcher(result).replaceAll("<#$1>");

            // Batch replacement para códigos simples - más eficiente que múltiples replace()
            StringBuilder sb = new StringBuilder(result.length() + 32);
            for (int i = 0; i < result.length(); i++) {
                char c = result.charAt(i);
                
                if (c == '&' && i + 1 < result.length()) {
                    char next = result.charAt(i + 1);
                    String replacement = getColorCodeReplacement(next);
                    if (replacement != null) {
                        sb.append(replacement);
                        i++; // Skip the next character
                        continue;
                    }
                }
                sb.append(c);
            }
            
            return sb.toString();
        });
    }
    
    /**
     * Obtiene el reemplazo MiniMessage para un código de color específico
     * @param code El código de color (0-9, a-f, k-r)
     * @return El reemplazo MiniMessage, o null si no hay reemplazo
     */
    private static String getColorCodeReplacement(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    /**
     * Normaliza un código de color a formato MiniMessage
     * @param input Código de color en cualquier formato soportado
     * @return Código de color en formato MiniMessage
     */
    public static String normalizeColor(String input) {
        if (input == null || input.isBlank()) {
            return "<#ffffff>";
        }

        input = input.trim().toLowerCase();

        // Si ya está en formato MiniMessage <#ffffff>
        if (input.matches("<#[0-9a-f]{6}>")) {
            return input;
        }

        // Si es un código hexadecimal en varios formatos (ffffff, #ffffff, &#ffffff)
        if (input.matches("#?[0-9a-f]{6}") || input.matches("&#[0-9a-f]{6}")) {
            return "<#" + input.replace("#", "").replace("&", "") + ">";
        }

        // Si es un código de color con "&" (ej: &f)
        return switch (input) {
            case "&0" -> "<black>";
            case "&1" -> "<dark_blue>";
            case "&2" -> "<dark_green>";
            case "&3" -> "<dark_aqua>";
            case "&4" -> "<dark_red>";
            case "&5" -> "<dark_purple>";
            case "&6" -> "<gold>";
            case "&7" -> "<gray>";
            case "&8" -> "<dark_gray>";
            case "&9" -> "<blue>";
            case "&a" -> "<green>";
            case "&b" -> "<aqua>";
            case "&c" -> "<red>";
            case "&d" -> "<light_purple>";
            case "&e" -> "<yellow>";
            case "&f" -> "<white>";
            default -> "<#ffffff>";
        };
    }

    /**
     * Quita todos los códigos de color y formato de un string
     * @param message Mensaje con códigos de color
     * @return String sin códigos de color ni formato
     */
    public static String stripColors(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // Convertir § a & para normalizar
        message = message.replace('§', '&');

        // Quitar códigos hexadecimales &#ffffff
        message = message.replaceAll("&#[0-9a-fA-F]{6}", "");

        // Quitar códigos MiniMessage <#ffffff>
        message = message.replaceAll("<#[0-9a-fA-F]{6}>", "");

        // Quitar códigos MiniMessage con nombres <color>
        message = message.replaceAll("</?(?:black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)>", "");

        // Quitar códigos de formato MiniMessage
        message = message.replaceAll("</?(?:obfuscated|bold|strikethrough|underlined|italic|reset)>", "");

        // Quitar códigos simples &x (colores y formato)
        message = message.replaceAll("&[0-9a-fA-FklmnoprKLMNOPR]", "");

        // Quitar otros códigos MiniMessage comunes
        message = message.replaceAll("</?(?:b|i|u|st|obf|r)>", "");

        // Quitar presets sin procesar
        message = message.replaceAll("\\{[a-zA-Z_][a-zA-Z0-9_]*\\}", "");

        return message;
    }
    public static String stripColors(Component component) {
        if (component == null) {
            return "";
        }

        // Serializar el componente a texto plano
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
    public static String getColorPreset(String presetName) {
        return colorPresets.get(presetName.toLowerCase());
    }
    public static Map<String, String> getAllColorPresets() {
        return new HashMap<>(colorPresets);
    }
    public static void reloadPresets() {
        if (presetsInitialized) {
            loadColorPresets();
            // Limpiar cache para que los nuevos presets se apliquen
            COMPONENT_CACHE.clear();
        }
    }
    public static boolean arePresetsInitialized() {
        return presetsInitialized;
    }
    public static void addCustomColorPreset(String name, String colorCode) {
        if (name != null && colorCode != null) {
            colorPresets.put(name.toLowerCase(), colorCode);
            // Limpiar cache para que los nuevos presets se apliquen
            COMPONENT_CACHE.clear();
            PROCESSED_STRING_CACHE.clear();
        }
    }

    public static void addCustomColorPresets(Map<String, String> customPresets) {
        if (customPresets != null && !customPresets.isEmpty()) {
            for (Map.Entry<String, String> entry : customPresets.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    colorPresets.put(key.toLowerCase(), value);
                }
            }
            // Limpiar cache para que los nuevos presets se apliquen
            COMPONENT_CACHE.clear();
            PROCESSED_STRING_CACHE.clear();
        }
    }

    public static void clearCache() {
        COMPONENT_CACHE.clear();
    }

    private static void initializeSmallFontMap() {
        String normal = "abcdefghijklmnopqrstuvwxyz";
        String small = "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";
        
        for (int i = 0; i < normal.length(); i++) {
            SMALL_FONT_MAP.put(normal.charAt(i), small.charAt(i));
            SMALL_FONT_MAP.put(Character.toUpperCase(normal.charAt(i)), small.charAt(i));
        }
    }

    public static String applyFontTransformation(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        try {
            String automaticFont = net.exylia.commons.config.base.MainConfigBase.textAutomaticFont();
            boolean forceUpperCase = net.exylia.commons.config.base.MainConfigBase.textForceInUpperCase();
            
            if (!"small".equals(automaticFont)) {
                return message;
            }

            int len = message.length();
            StringBuilder result = new StringBuilder(len);
            boolean insideTag = false;
            int tagDepth = 0;
            
            for (int i = 0; i < len; i++) {
                char c = message.charAt(i);
                
                if (c == '<' && !insideTag) {
                    int closingIndex = findTagEnd(message, i);
                    if (closingIndex != -1) {
                        insideTag = true;
                        tagDepth = 1;
                    }
                } else if (c == '<' && insideTag) {
                    tagDepth++;
                } else if (c == '>' && insideTag) {
                    tagDepth--;
                    if (tagDepth <= 0) {
                        insideTag = false;
                        tagDepth = 0;
                    }
                }
                
                if (c == '&' && i + 1 < len && !insideTag) {
                    char nextChar = message.charAt(i + 1);
                    if (isColorCode(nextChar)) {
                        result.append(c);
                        i++;
                        result.append(nextChar);
                        continue;
                    }
                }
                
                if (insideTag) {
                    result.append(c);
                } else {
                    Character transformed = SMALL_FONT_MAP.get(forceUpperCase ? Character.toUpperCase(c) : c);
                    result.append(transformed != null ? transformed : c);
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            return message;
        }
    }
    
    private static int findTagEnd(String message, int startIndex) {
        int depth = 0;
        for (int i = startIndex; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean isColorCode(char c) {
        return (c >= '0' && c <= '9') || 
               (c >= 'a' && c <= 'f') || 
               (c >= 'A' && c <= 'F') ||
               c == 'k' || c == 'l' || c == 'm' || c == 'n' || 
               c == 'o' || c == 'p' || c == 'r' || 
               c == 'K' || c == 'L' || c == 'M' || c == 'N' || 
               c == 'O' || c == 'P' || c == 'R' || c == '#';
    }

    public static void shutdown() {
        COMPONENT_CACHE.shutdown();
        PROCESSED_STRING_CACHE.shutdown();
        colorPresets.clear();
        presetsInitialized = false;
        pluginInstance = null;
    }

    public static String generateRandomHexColor() {
        int red, green, blue;
        do {
            red = ThreadLocalRandom.current().nextInt(50, 220);
            green = ThreadLocalRandom.current().nextInt(50, 220);
            blue = ThreadLocalRandom.current().nextInt(50, 220);
        } while (isTooLight(red, green, blue) || isTooDark(red, green, blue));

        return String.format("<#%02x%02x%02x>", red, green, blue);
    }

    private static boolean isTooLight(int red, int green, int blue) {
        return (red + green + blue) > 600;
    }

    private static boolean isTooDark(int red, int green, int blue) {
        return (red + green + blue) < 200;
    }
}
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
    private static JavaPlugin pluginInstance;
    private static boolean presetsInitialized = false;
    
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
     * Carga los presets de colores desde colors.yml
     */
    private static void loadColorPresets() {
        if (pluginInstance == null) {
            return;
        }

        File configFile = new File(pluginInstance.getDataFolder(), "colors.yml");

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
        logInternalSuccess("Loaded " + colorPresets.size() + " color presets.");
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
     * Preprocesa códigos de color ampersand (&) a formato MiniMessage
     * @param message Mensaje con códigos de color
     * @return Mensaje con códigos convertidos a formato MiniMessage
     */
    private static String preprocessColorCodes(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        message = message.replace('§', '&');

        // Convertir códigos &# a <#hexcode>
        message = message.replaceAll("&#([0-9a-fA-F]{6})", "<#$1>");

        // Convertir códigos simples &x a sus equivalentes MiniMessage
        message = message.replace("&0", "<black>");
        message = message.replace("&1", "<dark_blue>");
        message = message.replace("&2", "<dark_green>");
        message = message.replace("&3", "<dark_aqua>");
        message = message.replace("&4", "<dark_red>");
        message = message.replace("&5", "<dark_purple>");
        message = message.replace("&6", "<gold>");
        message = message.replace("&7", "<gray>");
        message = message.replace("&8", "<dark_gray>");
        message = message.replace("&9", "<blue>");
        message = message.replace("&a", "<green>");
        message = message.replace("&b", "<aqua>");
        message = message.replace("&c", "<red>");
        message = message.replace("&d", "<light_purple>");
        message = message.replace("&e", "<yellow>");
        message = message.replace("&f", "<white>");

        // Convertir códigos de formato
        message = message.replace("&k", "<obfuscated>");
        message = message.replace("&l", "<bold>");
        message = message.replace("&m", "<strikethrough>");
        message = message.replace("&n", "<underlined>");
        message = message.replace("&o", "<italic>");
        message = message.replace("&r", "<reset>");

        return message;
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

    /**
     * Quita todos los colores y formato de un Component y lo convierte a string plano
     * @param component Componente con colores y formato
     * @return String sin colores ni formato
     */
    public static String stripColors(Component component) {
        if (component == null) {
            return "";
        }

        // Serializar el componente a texto plano
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Obtiene un preset de color específico
     * @param presetName Nombre del preset
     * @return El código de color del preset, o null si no existe
     */
    public static String getColorPreset(String presetName) {
        return colorPresets.get(presetName.toLowerCase());
    }

    /**
     * Obtiene todos los presets de colores disponibles
     * @return Mapa con todos los presets de colores
     */
    public static Map<String, String> getAllColorPresets() {
        return new HashMap<>(colorPresets);
    }

    /**
     * Recarga los presets de colores desde el archivo
     */
    public static void reloadPresets() {
        if (presetsInitialized) {
            loadColorPresets();
            // Limpiar cache para que los nuevos presets se apliquen
            COMPONENT_CACHE.clear();
        }
    }

    /**
     * Verifica si los presets están inicializados
     * @return true si los presets están inicializados
     */
    public static boolean arePresetsInitialized() {
        return presetsInitialized;
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

            StringBuilder result = new StringBuilder();
            boolean insideColorCode = false;
            
            for (int i = 0; i < message.length(); i++) {
                char c = message.charAt(i);
                
                // Detectar inicio de códigos de color MiniMessage
                if (c == '<' && !insideColorCode) {
                    // Verificar si es un código de color válido
                    int closingIndex = message.indexOf('>', i);
                    if (closingIndex != -1) {
                        String tag = message.substring(i, closingIndex + 1);
                        // Verificar si es un código de color hexadecimal, nombre de color o formato
                        if (tag.matches("<#[0-9a-fA-F]{6}>") || 
                            tag.matches("</?(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white|obfuscated|bold|strikethrough|underlined|italic|reset|b|i|u|st|obf|r)>") ||
                            tag.startsWith("<gradient:") || tag.equals("</gradient>") ||
                            tag.startsWith("<rainbow") || tag.equals("</rainbow>") ||
                            tag.startsWith("<transition:") || tag.equals("</transition>")) {
                            insideColorCode = true;
                        }
                    }
                }
                
                // Detectar códigos de color ampersand
                if (c == '&' && i + 1 < message.length() && !insideColorCode) {
                    char nextChar = message.charAt(i + 1);
                    // Códigos simples (&0-9, &a-f, &k-o, &r) o hexadecimales (&#ffffff)
                    if ((nextChar >= '0' && nextChar <= '9') || 
                        (nextChar >= 'a' && nextChar <= 'f') || 
                        (nextChar >= 'A' && nextChar <= 'F') ||
                        "klmnoprKLMNOPR".indexOf(nextChar) != -1 ||
                        nextChar == '#') {
                        insideColorCode = true;
                    }
                }
                
                if (insideColorCode) {
                    result.append(c);
                    
                    // Detectar final de códigos MiniMessage
                    if (c == '>') {
                        insideColorCode = false;
                    }
                    // Detectar final de códigos ampersand (después de 2 caracteres para códigos simples)
                    else if (message.charAt(Math.max(0, i - 1)) == '&' && 
                             "0123456789abcdefklmnoprABCDEFKLMNOPR".indexOf(c) != -1) {
                        insideColorCode = false;
                    }
                    // Detectar final de códigos hexadecimales ampersand (&#ffffff)
                    else if (i >= 7 && message.substring(Math.max(0, i - 7), i + 1).matches("&#[0-9a-fA-F]{6}")) {
                        insideColorCode = false;
                    }
                } else {
                    // Aplicar transformación de fuente solo fuera de códigos de color
                    Character transformed = SMALL_FONT_MAP.get(forceUpperCase ? Character.toUpperCase(c) : c);
                    result.append(transformed != null ? transformed : c);
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            return message;
        }
    }

    public static void shutdown() {
        COMPONENT_CACHE.shutdown();
        colorPresets.clear();
        presetsInitialized = false;
        pluginInstance = null;
    }
}
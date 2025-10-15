package net.exylia.commons.utils;

import net.exylia.commons.config.base.MainConfigBase;
import net.exylia.commons.configSimple.Configs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
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

public class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Cache<String, Component> COMPONENT_CACHE = new Cache<>(1800000, 500, 300000);

    private static final Map<String, String> colorPresets = new HashMap<>();
    private static final Pattern PRESET_PATTERN = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static JavaPlugin pluginInstance;
    private static boolean presetsInitialized = false;
    
    private static final Cache<String, String> PROCESSED_STRING_CACHE = new Cache<>(1800000, 300, 300000);
    
    private static final Map<String, String> SMALL_FONT_MAP = new HashMap<>();
    private static final Map<String, String> FRAKTUR_FONT_MAP = new HashMap<>();
    private static final Map<String, String> BOLD_FRAKTUR_FONT_MAP = new HashMap<>();
    private static final Map<String, String> SCRIPT_FONT_MAP = new HashMap<>();
    private static final Map<String, String> DOUBLE_STRUCK_FONT_MAP = new HashMap<>();
    private static final Map<String, String> SQUARED_FONT_MAP = new HashMap<>();
    private static final Map<String, String> BOLD_FONT_MAP = new HashMap<>();
    private static final Map<String, String> ITALIC_FONT_MAP = new HashMap<>();
    private static final Map<String, String> BOLD_ITALIC_FONT_MAP = new HashMap<>();
    private static final Map<String, String> MONOSPACE_FONT_MAP = new HashMap<>();
    private static final Map<String, String> NEGATIVE_SQUARED_FONT_MAP = new HashMap<>();

    static {
        initializeFontMaps();
    }

    public static void initializePresets(JavaPlugin plugin) {
        pluginInstance = plugin;
        loadColorPresets();
        presetsInitialized = true;
    }

    public static void initializePresets(JavaPlugin plugin, Map<String, String> customPresets) {
        pluginInstance = plugin;
        
        if (customPresets != null && !customPresets.isEmpty()) {
            colorPresets.clear();
            
            for (Map.Entry<String, String> entry : customPresets.entrySet()) {
                String key = entry.getKey().toLowerCase();
                String value = entry.getValue();
                if (key != null && value != null) {
                    colorPresets.put(key, value);
                }
            }
            
            createCustomColorPresetsFile(customPresets);
            
        } else {
             
            loadColorPresets();
        }
        
        presetsInitialized = true;
    }

    private static void loadColorPresets() {
        loadColorPresetsFromFile(true);
    }

    private static void loadColorPresetsFromFile(boolean createIfNotExists) {
        if (pluginInstance == null) {
            return;
        }

        File configFile = new File(pluginInstance.getDataFolder(), "colors.yml");

        if (!configFile.exists()) {
            if (createIfNotExists) {
                createDefaultColorPresets(configFile);
            } else {
                 
                logInternalInfo("colors.yml not found, using only custom presets.");
                return;
            }
        }

        FileConfiguration colorConfig = YamlConfiguration.loadConfiguration(configFile);
        
        if (createIfNotExists) {
            colorPresets.clear();  
        }

        for (String key : colorConfig.getKeys(false)) {
            String value = colorConfig.getString(key);
            if (value != null) {
                String lowerKey = key.toLowerCase();
                 
                if (!colorPresets.containsKey(lowerKey)) {
                    colorPresets.put(lowerKey, value);
                }
            }
        }
        logInternalSuccess("Loaded " + colorPresets.size() + " color presets.");
    }

    private static void createCustomColorPresetsFile(Map<String, String> customPresets) {
        if (pluginInstance == null) {
            return;
        }

        File configFile = new File(pluginInstance.getDataFolder(), "colors.yml");
        
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            for (String key : config.getKeys(false)) {
                config.set(key, null);
            }

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

    private static void createDefaultColorPresets(File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            config.set("primary", "<#8a51c4>");
            config.set("secondary", "<#aa76de>");
            config.set("secondary_light", "<#b48fd9>");

            config.set("letters", "<#e7cfff>");
            config.set("letters_black", "<#a89ab5>");

            config.set("error", "<#a33b53>");
            config.set("success", "<#8fffc1>");
            config.set("success_light", "<#a1ffc3>");
            config.set("warning", "<#ff9500>");
            config.set("warning_light", "<#ffd2a8>");
            config.set("info", "<#59a4ff>");
            config.set("info_light", "<#7db7ff>");

            config.set("accent", "<#ff6b9d>");
            config.set("neutral", "<#6c757d>");
            config.set("highlight", "<#ffd700>");
            config.set("muted", "<#868e96>");

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

    public static Component parse(String message) {
        if (message == null) {
            return Component.empty();
        }

        return COMPONENT_CACHE.get(message, key -> {
            String processed = applyColorPresets(key);
            processed = preprocessColorCodes(processed);
            String automaticFont = Configs.string("text.automatic-font");
            boolean forceUpperCase = Configs.bool("text.force-in-upper-case");
            processed = applyFontTransformation(processed, automaticFont, forceUpperCase);
            return MINI_MESSAGE.deserialize(processed)
                    .decoration(TextDecoration.ITALIC, false);
        });
    }

    public static CompletableFuture<Component> parseAsync(String message) {
        return CompletableFuture.supplyAsync(() -> parse(message));
    }

    public static List<Component> parse(List<String> messages) {
        return messages.stream()
                .map(ColorUtils::parse)
                .collect(Collectors.toList());
    }

    public static CompletableFuture<List<Component>> parseAsync(List<String> messages) {
        return CompletableFuture.supplyAsync(() -> parse(messages));
    }

    public static String parseToString(String message) {
        if (message == null) {
            return "";
        }

        String processed = applyColorPresets(message);  
        processed = preprocessColorCodes(processed);  
        String automaticFont = Configs.string("text.automatic-font");
        boolean forceUpperCase = Configs.bool("text.force-in-upper-case");
        return applyFontTransformation(processed, automaticFont, forceUpperCase);  
    }

    private static String preprocessColorCodes(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        return PROCESSED_STRING_CACHE.get(message, key -> {
            String result = key.replace('§', '&');

            result = COLOR_CODE_PATTERN.matcher(result).replaceAll("<#$1>");

            StringBuilder sb = new StringBuilder(result.length() + 32);
            for (int i = 0; i < result.length(); i++) {
                char c = result.charAt(i);
                
                if (c == '&' && i + 1 < result.length()) {
                    char next = result.charAt(i + 1);
                    String replacement = getColorCodeReplacement(next);
                    if (replacement != null) {
                        sb.append(replacement);
                        i++;  
                        continue;
                    }
                }
                sb.append(c);
            }
            
            return sb.toString();
        });
    }
    
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

    public static String normalizeColor(String input) {
        if (input == null || input.isBlank()) {
            return "<#ffffff>";
        }

        input = input.trim().toLowerCase();

        if (input.matches("<#[0-9a-f]{6}>")) {
            return input;
        }

        if (input.matches("#?[0-9a-f]{6}") || input.matches("&#[0-9a-f]{6}")) {
            return "<#" + input.replace("#", "").replace("&", "") + ">";
        }

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

    public static String stripColors(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        message = message.replace('§', '&');

        message = message.replaceAll("&#[0-9a-fA-F]{6}", "");

        message = message.replaceAll("<#[0-9a-fA-F]{6}>", "");

        message = message.replaceAll("</?(?:black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)>", "");

        message = message.replaceAll("</?(?:obfuscated|bold|strikethrough|underlined|italic|reset)>", "");

        message = message.replaceAll("&[0-9a-fA-FklmnoprKLMNOPR]", "");

        message = message.replaceAll("</?(?:b|i|u|st|obf|r)>", "");

        message = message.replaceAll("\\{#[0-9a-fA-F]{6}[^}]*}", "");

        message = message.replaceAll("\\{[a-zA-Z_][a-zA-Z0-9_]*\\}", "");

        return message;
    }
    public static String stripColors(Component component) {
        if (component == null) {
            return "";
        }

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
             
            COMPONENT_CACHE.clear();
        }
    }
    public static boolean arePresetsInitialized() {
        return presetsInitialized;
    }
    public static void addCustomColorPreset(String name, String colorCode) {
        if (name != null && colorCode != null) {
            colorPresets.put(name.toLowerCase(), colorCode);
             
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
             
            COMPONENT_CACHE.clear();
            PROCESSED_STRING_CACHE.clear();
        }
    }

    public static void clearCache() {
        COMPONENT_CACHE.clear();
    }

    private static void initializeFontMaps() {
        String[] normal = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
        String[] normalUpper = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

        String[] small = {"ᴀ", "ʙ", "ᴄ", "ᴅ", "ᴇ", "ғ", "ɢ", "ʜ", "ɪ", "ᴊ", "ᴋ", "ʟ", "ᴍ", "ɴ", "ᴏ", "ᴘ", "ǫ", "ʀ", "s", "ᴛ", "ᴜ", "ᴠ", "ᴡ", "x", "ʏ", "ᴢ"};

        String[] fraktur = {"\uD835\uDD1E", "\uD835\uDD1F", "\uD835\uDD20", "\uD835\uDD21", "\uD835\uDD22", "\uD835\uDD23", "\uD835\uDD24", "\uD835\uDD25", "\uD835\uDD26", "\uD835\uDD27", "\uD835\uDD28", "\uD835\uDD29", "\uD835\uDD2A", "\uD835\uDD2B", "\uD835\uDD2C", "\uD835\uDD2D", "\uD835\uDD2E", "\uD835\uDD2F", "\uD835\uDD30", "\uD835\uDD31", "\uD835\uDD32", "\uD835\uDD33", "\uD835\uDD34", "\uD835\uDD35", "\uD835\uDD36", "\uD835\uDD37"};

        String[] boldFraktur = {"𝖆", "𝖇", "𝖈", "𝖉", "𝖊", "𝖋", "𝖌", "𝖍", "𝖎", "𝖏", "𝖐", "𝖑", "𝖒", "𝖓", "𝖔", "𝖕", "𝖖", "𝖗", "𝖘", "𝖙", "𝖚", "𝖛", "𝖜", "𝖝", "𝖞", "𝖟"};

        String[] script = {"𝓪", "𝓫", "𝓬", "𝓭", "𝓮", "𝓯", "𝓰", "𝓱", "𝓲", "𝓳", "𝓴", "𝓵", "𝓶", "𝓷", "𝓸", "𝓹", "𝓺", "𝓻", "𝓼", "𝓽", "𝓾", "𝓿", "𝔀", "𝔁", "𝔂", "𝔃"};

        String[] doubleStruck = {"𝕒", "𝕓", "𝕔", "𝕕", "𝕖", "𝕗", "𝕘", "𝕙", "𝕚", "𝕛", "𝕜", "𝕝", "𝕞", "𝕟", "𝕠", "𝕡", "𝕢", "𝕣", "𝕤", "𝕥", "𝕦", "𝕧", "𝕨", "𝕩", "𝕪", "𝕫"};

        String[] squared = {"🄰", "🄱", "🄲", "🄳", "🄴", "🄵", "🄶", "🄷", "🄸", "🄹", "🄺", "🄻", "🄼", "🄽", "🄾", "🄿", "🅀", "🅁", "🅂", "🅃", "🅄", "🅅", "🅆", "🅇", "🅈", "🅉"};

        String[] bold = {"𝐚", "𝐛", "𝐜", "𝐝", "𝐞", "𝐟", "𝐠", "𝐡", "𝐢", "𝐣", "𝐤", "𝐥", "𝐦", "𝐧", "𝐨", "𝐩", "𝐪", "𝐫", "𝐬", "𝐭", "𝐮", "𝐯", "𝐰", "𝐱", "𝐲", "𝐳"};

        String[] italic = {"𝘢", "𝘣", "𝘤", "𝘥", "𝘦", "𝘧", "𝘨", "𝘩", "𝘪", "𝘫", "𝘬", "𝘭", "𝘮", "𝘯", "𝘰", "𝘱", "𝘲", "𝘳", "𝘴", "𝘵", "𝘶", "𝘷", "𝘸", "𝘹", "𝘺", "𝘻"};

        String[] boldItalic = {"𝙖", "𝙗", "𝙘", "𝙙", "𝙚", "𝙛", "𝙜", "𝙝", "𝙞", "𝙟", "𝙠", "𝙡", "𝙢", "𝙣", "𝙤", "𝙥", "𝙦", "𝙧", "𝙨", "𝙩", "𝙪", "𝙫", "𝙬", "𝙭", "𝙮", "𝙯"};

        String[] monospace = {"𝚊", "𝚋", "𝚌", "𝚍", "𝚎", "𝚏", "𝚐", "𝚑", "𝚒", "𝚓", "𝚔", "𝚕", "𝚖", "𝚗", "𝚘", "𝚙", "𝚚", "𝚛", "𝚜", "𝚝", "𝚞", "𝚟", "𝚠", "𝚡", "𝚢", "𝚣"};

        String[] negativeSquared = {"🅰", "🅱", "🅲", "🅳", "🅴", "🅵", "🅶", "🅷", "🅸", "🅹", "🅺", "🅻", "🅼", "🅽", "🅾", "🅿", "🆀", "🆁", "🆂", "🆃", "🆄", "🆅", "🆆", "🆇", "🆈", "🆉"};

        for (int i = 0; i < 26; i++) {
            String normalChar = normal[i];
            String upperChar = normalUpper[i];

            SMALL_FONT_MAP.put(normalChar, small[i]);
            SMALL_FONT_MAP.put(upperChar, small[i]);

            FRAKTUR_FONT_MAP.put(normalChar, fraktur[i]);
            FRAKTUR_FONT_MAP.put(upperChar, fraktur[i]);

            BOLD_FRAKTUR_FONT_MAP.put(normalChar, boldFraktur[i]);
            BOLD_FRAKTUR_FONT_MAP.put(upperChar, boldFraktur[i]);

            SCRIPT_FONT_MAP.put(normalChar, script[i]);
            SCRIPT_FONT_MAP.put(upperChar, script[i]);

            DOUBLE_STRUCK_FONT_MAP.put(normalChar, doubleStruck[i]);
            DOUBLE_STRUCK_FONT_MAP.put(upperChar, doubleStruck[i]);

            BOLD_FONT_MAP.put(normalChar, bold[i]);
            BOLD_FONT_MAP.put(upperChar, bold[i]);

            ITALIC_FONT_MAP.put(normalChar, italic[i]);
            ITALIC_FONT_MAP.put(upperChar, italic[i]);

            BOLD_ITALIC_FONT_MAP.put(normalChar, boldItalic[i]);
            BOLD_ITALIC_FONT_MAP.put(upperChar, boldItalic[i]);

            MONOSPACE_FONT_MAP.put(normalChar, monospace[i]);
            MONOSPACE_FONT_MAP.put(upperChar, monospace[i]);

            SQUARED_FONT_MAP.put(normalChar, squared[i]);
            SQUARED_FONT_MAP.put(upperChar, squared[i]);

            NEGATIVE_SQUARED_FONT_MAP.put(normalChar, negativeSquared[i]);
            NEGATIVE_SQUARED_FONT_MAP.put(upperChar, negativeSquared[i]);
        }
    }

    public static String applyFontTransformation(String message, String font, boolean forceUpperCase) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        try {
            Map<String, String> fontMap = getFontMap(font);
            if (fontMap == null) {
                return message;
            }

            StringBuilder result = new StringBuilder();
            boolean insideTag = false;
            boolean insideBrackets = false;
            int tagDepth = 0;

            for (int i = 0; i < message.length(); i++) {
                char c = message.charAt(i);

                if (c == '[' && !insideTag) {
                    insideBrackets = true;
                } else if (c == ']' && !insideTag) {
                    insideBrackets = false;
                }

                if (c == '<' && !insideTag && !insideBrackets) {
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

                if (c == '&' && i + 1 < message.length() && !insideTag && !insideBrackets) {
                    char nextChar = message.charAt(i + 1);
                    if (isColorCode(nextChar)) {
                        result.append(c);
                        i++;
                        result.append(nextChar);
                        continue;
                    }
                }

                if (insideTag || insideBrackets) {
                    result.append(c);
                } else {
                    String targetChar = String.valueOf(forceUpperCase && isBasicLetter(c) ? Character.toUpperCase(c) : c);
                    String transformed = fontMap.get(targetChar);
                    result.append(transformed != null ? transformed : targetChar);
                }
            }

            return result.toString();
        } catch (Exception e) {
            return message;
        }
    }

    private static Map<String, String> getFontMap(String fontType) {
        if (fontType == null) {
            return null;
        }

        return switch (fontType.toLowerCase()) {
            case "small" -> SMALL_FONT_MAP;
            case "fraktur" -> FRAKTUR_FONT_MAP;
            case "bold_fraktur" -> BOLD_FRAKTUR_FONT_MAP;
            case "script" -> SCRIPT_FONT_MAP;
            case "double_struck" -> DOUBLE_STRUCK_FONT_MAP;
            case "squared" -> SQUARED_FONT_MAP;
            case "bold" -> BOLD_FONT_MAP;
            case "italic" -> ITALIC_FONT_MAP;
            case "bold_italic" -> BOLD_ITALIC_FONT_MAP;
            case "monospace" -> MONOSPACE_FONT_MAP;
            case "negative_squared" -> NEGATIVE_SQUARED_FONT_MAP;
            default -> null;
        };
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

    private static boolean isBasicLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
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

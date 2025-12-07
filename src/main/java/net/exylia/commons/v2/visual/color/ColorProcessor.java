package net.exylia.commons.v2.visual.color;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.commons.v2.config.Configs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ColorProcessor {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern HEX_AMPERSAND = Pattern.compile("&#[0-9a-fA-F]{6}");
    private static final Pattern HEX_BRACKETS = Pattern.compile("<#[0-9a-fA-F]{6}>");
    private static final Pattern NAMED_COLORS = Pattern.compile("</?(?:black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)>");
    private static final Pattern FORMATTING = Pattern.compile("</?(?:obfuscated|bold|strikethrough|underlined|italic|reset)>");
    private static final Pattern LEGACY_COLORS = Pattern.compile("&[0-9a-fA-FklmnoprKLMNOPR]");
    private static final Pattern SHORT_TAGS = Pattern.compile("</?(?:b|i|u|st|obf|r)>");
    private static final Pattern HEX_BRACES = Pattern.compile("\\{#[0-9a-fA-F]{6}[^}]*}");
    private static final Pattern PRESETS = Pattern.compile("\\{[a-zA-Z_][a-zA-Z0-9_]*\\}");

    private static final Cache<String, String> STRIP_CACHE = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .recordStats()
            .build();

    public static Component parseToComponent(String message) {
        if (message == null) {
            return Component.empty();
        }

        String processed = processString(message);
        return MINI_MESSAGE.deserialize(processed)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static String processString(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String processed = ColorPresetManager.getInstance().applyColorPresets(message);

        processed = preprocessColorCodes(processed);

        String automaticFont = null;
        boolean forceUpperCase = false;

        try {
            automaticFont = Configs.string("text.automatic-font");
            forceUpperCase = Configs.bool("text.force-in-upper-case");
        } catch (Exception ignored) {
        }

        if (automaticFont != null && !automaticFont.isEmpty()) {
            processed = FontTransformer.transform(processed, automaticFont, forceUpperCase);
        }

        return processed;
    }

    private static String preprocessColorCodes(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String result = message.replace('§', '&');

        result = COLOR_CODE_PATTERN.matcher(result).replaceAll("<#$1>");

        StringBuilder sb = new StringBuilder(result.length() + 32);
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);

            if (c == '&' && i + 1 < result.length()) {
                char next = result.charAt(i + 1);
                String replacement = getLegacyReplacement(next);
                if (replacement != null) {
                    sb.append(replacement);
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }

        return sb.toString();
    }

    private static String getLegacyReplacement(char code) {
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

    public static String stripColors(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String cached = STRIP_CACHE.getIfPresent(message);
        if (cached != null) {
            return cached;
        }

        String result = message.replace('§', '&');

        result = HEX_AMPERSAND.matcher(result).replaceAll("");
        result = HEX_BRACKETS.matcher(result).replaceAll("");
        result = NAMED_COLORS.matcher(result).replaceAll("");
        result = FORMATTING.matcher(result).replaceAll("");
        result = LEGACY_COLORS.matcher(result).replaceAll("");
        result = SHORT_TAGS.matcher(result).replaceAll("");
        result = HEX_BRACES.matcher(result).replaceAll("");
        result = PRESETS.matcher(result).replaceAll("");

        STRIP_CACHE.put(message, result);
        return result;
    }

    public static String stripColors(Component component) {
        if (component == null) {
            return "";
        }
        return PlainTextComponentSerializer.plainText().serialize(component);
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

    public static void clearCache() {
        STRIP_CACHE.invalidateAll();
    }
}

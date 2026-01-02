package net.exylia.commons.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OldColorUtils {

    private static final Cache<String, String> LEGACY_CACHE = new Cache<>(1800000, 500, 300000);
    private static final Cache<String, String> LEGACY_HEX_CACHE = new Cache<>(1800000, 500, 300000);

    private static final Pattern MINIMESSAGE_GRADIENT = Pattern.compile("<gradient:([^>]+)>([^<]+)</gradient>");
    private static final Pattern MINIMESSAGE_HEX = Pattern.compile("<#([0-9a-fA-F]{6})>");
    private static final Pattern MINIMESSAGE_COLOR = Pattern.compile("<(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)>");
    private static final Pattern MINIMESSAGE_FORMATTING = Pattern.compile("<(bold|b|italic|i|underlined|u|strikethrough|st|obfuscated|obf)>");
    private static final Pattern LEGACY_GRADIENT = Pattern.compile("<#([0-9a-fA-F]{6})>(.*?)</#([0-9a-fA-F]{6})>");
    private static final Pattern FORMAT_CODE_PATTERN = Pattern.compile("(&[klmnor])");

    public static String parseOld(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        return LEGACY_CACHE.get(message, key -> {
            String processed = key;

            processed = parseOldNormalColors(processed);

            processed = processed.replaceAll("<#([0-9a-fA-F]{6})>", "<#$1>");
            processed = processed.replaceAll("</#[0-9a-fA-F]{6}>", "&r");

            Pattern pattern = Pattern.compile("(&[0-9a-fA-F])(&[klmnor])+|(&[klmnor])+(&[0-9a-fA-F])");
            Matcher matcher = pattern.matcher(processed);

            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String match = matcher.group();

                String colorCode = "";
                Matcher colorMatcher = Pattern.compile("&[0-9a-fA-F]").matcher(match);
                if (colorMatcher.find()) {
                    colorCode = colorMatcher.group();
                }

                List<String> formatCodes = new ArrayList<>();
                Matcher formatMatcher = Pattern.compile("&[klmnor]").matcher(match);
                while (formatMatcher.find()) {
                    formatCodes.add(formatMatcher.group());
                }

                StringBuilder replacement = new StringBuilder();
                if (!colorCode.isEmpty()) {
                    replacement.append(colorCode);
                }
                for (String formatCode : formatCodes) {
                    replacement.append(formatCode);
                }

                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
            }
            matcher.appendTail(sb);
            processed = sb.toString();

            processed = processed.replaceAll("(&[klmnor]+)(<#[0-9a-fA-F]{6}>)", "$2$1");

            return ChatColor.translateAlternateColorCodes('&', GradientUtils.applyGradientsAndHex(processed));
        });
    }

    public static String parseOldNormalColors(String processed){
        processed = processed
                 
                .replace("<black>", "&0")
                .replace("<dark_blue>", "&1")
                .replace("<dark_green>", "&2")
                .replace("<dark_aqua>", "&3")
                .replace("<dark_red>", "&4")
                .replace("<dark_purple>", "&5")
                .replace("<gold>", "&6")
                .replace("<gray>", "&7")
                .replace("<dark_gray>", "&8")
                .replace("<blue>", "&9")
                .replace("<green>", "&a")
                .replace("<aqua>", "&b")
                .replace("<red>", "&c")
                .replace("<light_purple>", "&d")
                .replace("<yellow>", "&e")
                .replace("<white>", "&f")

                .replace("<obfuscated>", "&k")
                .replace("<bold>", "&l")
                .replace("<strikethrough>", "&m")
                .replace("<underlined>", "&n")
                .replace("<italic>", "&o")
                .replace("</italic>", "&r")
                .replace("<reset>", "&r")
                .replace("<!italic>", "");

        processed = processed
                .replace("</black>", "&r")
                .replace("</dark_blue>", "&r")
                .replace("</dark_green>", "&r")
                .replace("</dark_aqua>", "&r")
                .replace("</dark_red>", "&r")
                .replace("</dark_purple>", "&r")
                .replace("</gold>", "&r")
                .replace("</gray>", "&r")
                .replace("</dark_gray>", "&r")
                .replace("</blue>", "&r")
                .replace("</green>", "&r")
                .replace("</aqua>", "&r")
                .replace("</red>", "&r")
                .replace("</light_purple>", "&r")
                .replace("</yellow>", "&r")
                .replace("</white>", "&r")
                .replace("</obfuscated>", "&r")
                .replace("</bold>", "&r")
                .replace("</strikethrough>", "&r")
                .replace("</underlined>", "&r")
                .replace("</reset>", "&r");
        return processed;
    }

    public static void clearCache() {
        LEGACY_CACHE.clear();
        LEGACY_HEX_CACHE.clear();
    }

    public static void shutdown() {
        LEGACY_CACHE.shutdown();
        LEGACY_HEX_CACHE.shutdown();
    }

    public static String convertMiniMessageToLegacyHex(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        return LEGACY_HEX_CACHE.get(message, key -> {
            String processed = key;
            processed = convertMiniMessageFormatting(processed);
            processed = convertMiniMessageGradients(processed);
            processed = convertLegacyGradients(processed);
            processed = convertMiniMessageColors(processed);
            return processed;
        });
    }

    public static String convertToLegacyHex(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        return LEGACY_HEX_CACHE.get("legacy_" + message, key -> {
            String processed = message;

            processed = convertLegacyGradients(processed);
            processed = convertBracketHexToLegacy(processed);
            processed = parseOldNormalColorsToLegacy(processed);

            return processed;
        });
    }

    private static String convertMiniMessageGradients(String message) {
        int startIndex = 0;
        StringBuilder result = new StringBuilder();

        while (true) {
            int gradientStart = message.indexOf("<gradient:", startIndex);
            if (gradientStart == -1) {
                result.append(message.substring(startIndex));
                break;
            }

            result.append(message, startIndex, gradientStart);

            int gradientTagEnd = message.indexOf(">", gradientStart);
            if (gradientTagEnd == -1) {
                result.append(message.substring(gradientStart));
                break;
            }

            int gradientEnd = message.indexOf("</gradient>", gradientTagEnd);
            String content;
            int nextStart;

            if (gradientEnd == -1) {
                content = message.substring(gradientTagEnd + 1);
                nextStart = message.length();
            } else {
                content = message.substring(gradientTagEnd + 1, gradientEnd);
                nextStart = gradientEnd + 11;
            }

            String gradientTag = message.substring(gradientStart + 10, gradientTagEnd);

            String[] colorParts = gradientTag.split(":");
            List<Color> colors = new ArrayList<>();

            for (String colorPart : colorParts) {
                colorPart = colorPart.trim();
                if (colorPart.startsWith("#")) {
                    try {
                        colors.add(Color.decode(colorPart));
                    } catch (Exception e) {
                    }
                }
            }

            if (colors.size() >= 2) {
                String gradient = createMultiColorLegacyGradient(content, colors);
                result.append(gradient);
            } else {
                result.append(content);
            }

            startIndex = nextStart;
            if (startIndex >= message.length()) {
                break;
            }
        }

        return result.toString();
    }

    private static String convertLegacyGradients(String message) {
        Matcher matcher = LEGACY_GRADIENT.matcher(message);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String startHex = matcher.group(1);
            String content = matcher.group(2);
            String endHex = matcher.group(3);

            Color startColor = Color.decode("#" + startHex);
            Color endColor = Color.decode("#" + endHex);
            String gradient = createLegacyGradient(content, startColor, endColor);

            matcher.appendReplacement(sb, Matcher.quoteReplacement(gradient));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String createMultiColorLegacyGradient(String text, List<Color> colors) {
        if (colors.size() < 2) {
            return text;
        }

        if (colors.size() == 2) {
            return createLegacyGradient(text, colors.get(0), colors.get(1));
        }

        StringBuilder builder = new StringBuilder();

        Matcher formatMatcher = FORMAT_CODE_PATTERN.matcher(text);
        StringBuilder plainText = new StringBuilder();
        List<String> formatCodes = new ArrayList<>();

        int lastEnd = 0;
        while (formatMatcher.find()) {
            plainText.append(text, lastEnd, formatMatcher.start());
            formatCodes.add(formatMatcher.group());
            lastEnd = formatMatcher.end();
        }
        plainText.append(text.substring(lastEnd));

        String plainString = plainText.toString();
        String formats = String.join("", formatCodes);

        int length = plainString.length();
        if (length == 0) {
            return text;
        }

        int colorCount = colors.size();
        for (int i = 0; i < length; i++) {
            double ratio = length > 1 ? (double) i / (length - 1) : 0;

            double scaledRatio = ratio * (colorCount - 1);
            int colorIndex = (int) scaledRatio;
            double localRatio = scaledRatio - colorIndex;

            if (colorIndex >= colorCount - 1) {
                colorIndex = colorCount - 2;
                localRatio = 1.0;
            }

            Color startColor = colors.get(colorIndex);
            Color endColor = colors.get(colorIndex + 1);

            int red = (int) (startColor.getRed() * (1 - localRatio) + endColor.getRed() * localRatio);
            int green = (int) (startColor.getGreen() * (1 - localRatio) + endColor.getGreen() * localRatio);
            int blue = (int) (startColor.getBlue() * (1 - localRatio) + endColor.getBlue() * localRatio);

            builder.append(String.format("&#%02x%02x%02x", red, green, blue));

            if (!formats.isEmpty()) {
                builder.append(formats);
            }

            builder.append(plainString.charAt(i));
        }

        return builder.toString();
    }

    private static String createLegacyGradient(String text, Color startColor, Color endColor) {
        StringBuilder builder = new StringBuilder();

        Matcher formatMatcher = FORMAT_CODE_PATTERN.matcher(text);
        StringBuilder plainText = new StringBuilder();
        List<String> formatCodes = new ArrayList<>();

        int lastEnd = 0;
        while (formatMatcher.find()) {
            plainText.append(text, lastEnd, formatMatcher.start());
            formatCodes.add(formatMatcher.group());
            lastEnd = formatMatcher.end();
        }
        plainText.append(text.substring(lastEnd));

        String plainString = plainText.toString();
        String formats = String.join("", formatCodes);

        int length = plainString.length();
        if (length == 0) {
            return text;
        }

        for (int i = 0; i < length; i++) {
            double ratio = length > 1 ? (double) i / (length - 1) : 0;

            int red = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
            int green = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
            int blue = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);

            builder.append(String.format("&#%02x%02x%02x", red, green, blue));

            if (!formats.isEmpty()) {
                builder.append(formats);
            }

            builder.append(plainString.charAt(i));
        }

        return builder.toString();
    }

    private static String convertMiniMessageColors(String message) {
        message = MINIMESSAGE_HEX.matcher(message).replaceAll("&#$1");

        message = MINIMESSAGE_COLOR.matcher(message).replaceAll(match -> {
            String colorName = match.group(1);
            return getLegacyColorCode(colorName);
        });

        message = message.replaceAll("</gradient>", "");
        message = message.replaceAll("</(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)>", "&r");
        message = message.replaceAll("<reset>", "&r");
        message = message.replaceAll("<r>", "&r");

        return message;
    }

    private static String convertMiniMessageFormatting(String message) {
        message = message.replaceAll("<bold>|<b>", "&l");
        message = message.replaceAll("<italic>|<i>", "&o");
        message = message.replaceAll("<underlined>|<u>", "&n");
        message = message.replaceAll("<strikethrough>|<st>", "&m");
        message = message.replaceAll("<obfuscated>|<obf>", "&k");

        message = message.replaceAll("</(bold|b|italic|i|underlined|u|strikethrough|st|obfuscated|obf)>", "&r");

        return message;
    }

    private static String convertBracketHexToLegacy(String message) {
        return message.replaceAll("<#([0-9a-fA-F]{6})>", "&#$1");
    }

    private static String parseOldNormalColorsToLegacy(String message) {
        return message
                .replace("<black>", "&0")
                .replace("<dark_blue>", "&1")
                .replace("<dark_green>", "&2")
                .replace("<dark_aqua>", "&3")
                .replace("<dark_red>", "&4")
                .replace("<dark_purple>", "&5")
                .replace("<gold>", "&6")
                .replace("<gray>", "&7")
                .replace("<dark_gray>", "&8")
                .replace("<blue>", "&9")
                .replace("<green>", "&a")
                .replace("<aqua>", "&b")
                .replace("<red>", "&c")
                .replace("<light_purple>", "&d")
                .replace("<yellow>", "&e")
                .replace("<white>", "&f")

                .replace("<obfuscated>", "&k")
                .replace("<bold>", "&l")
                .replace("<strikethrough>", "&m")
                .replace("<underlined>", "&n")
                .replace("<italic>", "&o")
                .replace("<reset>", "&r")

                .replace("</black>", "&r")
                .replace("</dark_blue>", "&r")
                .replace("</dark_green>", "&r")
                .replace("</dark_aqua>", "&r")
                .replace("</dark_red>", "&r")
                .replace("</dark_purple>", "&r")
                .replace("</gold>", "&r")
                .replace("</gray>", "&r")
                .replace("</dark_gray>", "&r")
                .replace("</blue>", "&r")
                .replace("</green>", "&r")
                .replace("</aqua>", "&r")
                .replace("</red>", "&r")
                .replace("</light_purple>", "&r")
                .replace("</yellow>", "&r")
                .replace("</white>", "&r")
                .replace("</obfuscated>", "&r")
                .replace("</bold>", "&r")
                .replace("</strikethrough>", "&r")
                .replace("</underlined>", "&r")
                .replace("</italic>", "&r");
    }

    private static String getLegacyColorCode(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "black" -> "&0";
            case "dark_blue" -> "&1";
            case "dark_green" -> "&2";
            case "dark_aqua" -> "&3";
            case "dark_red" -> "&4";
            case "dark_purple" -> "&5";
            case "gold" -> "&6";
            case "gray" -> "&7";
            case "dark_gray" -> "&8";
            case "blue" -> "&9";
            case "green" -> "&a";
            case "aqua" -> "&b";
            case "red" -> "&c";
            case "light_purple" -> "&d";
            case "yellow" -> "&e";
            case "white" -> "&f";
            default -> "";
        };
    }
}

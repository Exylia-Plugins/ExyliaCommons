package net.exylia.commons.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OldColorUtils {

    private static final Cache<String, String> LEGACY_CACHE = new Cache<>(1800000, 500, 300000);

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
    }

    public static void shutdown() {
        LEGACY_CACHE.shutdown();
    }
}

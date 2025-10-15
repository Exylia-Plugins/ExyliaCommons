package net.exylia.commons.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradientUtils {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>(.*?)</#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern FORMAT_PATTERN = Pattern.compile("(&[klmnor])");

    public static String applyGradientsAndHex(String message) {
        message = applyHexColors(message);
        return applyGradients(message);
    }

    private static String applyHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            String hexColor = toChatColor("#" + hexCode);
            message = message.replace("<#" + hexCode + ">", hexColor);
        }
        return message;
    }

    private static String applyGradients(String message) {
        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String startColor = matcher.group(1);
            String content = matcher.group(2);
            String endColor = matcher.group(3);

            String gradient = createGradient(content, Color.decode("#" + startColor), Color.decode("#" + endColor));
            matcher.appendReplacement(sb, gradient);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String createGradient(String text, Color startColor, Color endColor) {
        StringBuilder builder = new StringBuilder();

        Matcher formatMatcher = FORMAT_PATTERN.matcher(text);
        StringBuilder plainText = new StringBuilder();
        StringBuilder formatBuilder = new StringBuilder();

        int lastEnd = 0;
        while (formatMatcher.find()) {
            plainText.append(text, lastEnd, formatMatcher.start());
            formatBuilder.append(formatMatcher.group());
            lastEnd = formatMatcher.end();
        }
        plainText.append(text.substring(lastEnd));

        String plainString = plainText.toString();
        String formats = formatBuilder.toString();

        int length = plainString.length();
        for (int i = 0; i < length; i++) {
            double ratio = (double) i / (length - 1);
            if (Double.isNaN(ratio)) ratio = 0;

            int red = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
            int green = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
            int blue = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);

            builder.append(toChatColor(String.format("#%02x%02x%02x", red, green, blue)));

            if (!formats.isEmpty()) {
                builder.append(formats);
            }

            builder.append(plainString.charAt(i));
        }

        return builder.toString();
    }

    public static String toChatColor(String hex) {
        return ChatColor.of(hex).toString();
    }
}

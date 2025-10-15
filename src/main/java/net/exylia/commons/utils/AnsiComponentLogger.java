package net.exylia.commons.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.function.Function;
import java.util.regex.Pattern;

public class AnsiComponentLogger {
    private static final Pattern HEX_PATTERN = Pattern.compile(
            "§#([0-9a-fA-F]{6})"
    );

    private static final String RGB_ANSI = "\u001B[38;2;%d;%d;%dm";
    private static final String RESET = "\u001B[0m";

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .character('§')
            .hexCharacter('#')
            .build();

    private static final Function<Component, String> converter =
            supportsAnsi() ? AnsiComponentLogger::convertHexColors : AnsiComponentLogger::stripHexColors;

    public static String convert(final Component component) {
        return converter.apply(component);
    }

    public static String convertHexColors(final Component input) {
        String serialized = SERIALIZER.serialize(input);
        return HEX_PATTERN.matcher(serialized).replaceAll(result -> {
            final int hex = Integer.decode("0x" + result.group().substring(2));
            final int red = hex >> 16 & 0xFF;
            final int green = hex >> 8 & 0xFF;
            final int blue = hex & 0xFF;
            return String.format(RGB_ANSI, red, green, blue);
        }) + RESET;
    }

    private static String stripHexColors(final Component input) {
        String serialized = SERIALIZER.serialize(input);
        return HEX_PATTERN.matcher(serialized).replaceAll("");
    }

    private static boolean supportsAnsi() {
        String osName = System.getProperty("os.name").toLowerCase();
        String term = System.getenv("TERM");

        return !osName.contains("win") ||
                (term != null && !term.equals("dumb"));
    }

    public static String convertLegacyColors(final Component input) {
        String serialized = SERIALIZER.serialize(input);
        return convertLegacyColorCodes(serialized);
    }

    private static String convertLegacyColorCodes(String input) {
         
        return input
                .replace("§0", "\u001B[30m")    
                .replace("§1", "\u001B[34m")    
                .replace("§2", "\u001B[32m")    
                .replace("§3", "\u001B[36m")    
                .replace("§4", "\u001B[31m")    
                .replace("§5", "\u001B[35m")    
                .replace("§6", "\u001B[33m")    
                .replace("§7", "\u001B[37m")    
                .replace("§8", "\u001B[90m")    
                .replace("§9", "\u001B[94m")    
                .replace("§a", "\u001B[92m")    
                .replace("§b", "\u001B[96m")    
                .replace("§c", "\u001B[91m")    
                .replace("§d", "\u001B[95m")    
                .replace("§e", "\u001B[93m")    
                .replace("§f", "\u001B[97m")    
                + RESET;
    }
}

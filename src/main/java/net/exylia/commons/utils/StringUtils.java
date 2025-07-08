package net.exylia.commons.utils;

public class StringUtils {
    /**
     * Convierte CamelCase a kebab-case
     * Ejemplo: SaveSchematic -> save-schematic
     */
    public static String camelCaseToKebabCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(input.charAt(0)));

        for (int i = 1; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('-');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}

package net.exylia.commons.items.utils;

public class PlaceholderDetector {

    public static boolean contains(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.contains("%") || text.contains("{") || text.contains("<");
    }

    public static boolean containsPercent(String text) {
        return text != null && text.contains("%");
    }

    public static boolean containsBraces(String text) {
        return text != null && text.contains("{");
    }

    public static boolean containsAngleBrackets(String text) {
        return text != null && text.contains("<");
    }
}

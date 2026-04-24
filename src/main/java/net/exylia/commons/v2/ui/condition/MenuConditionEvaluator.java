package net.exylia.commons.v2.ui.condition;

import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

public final class MenuConditionEvaluator {

    private MenuConditionEvaluator() {}

    public static boolean evaluate(String condition, Player player, PlaceholderContext context) {
        if (condition == null || condition.isBlank()) return true;

        String resolved = Placeholders.process(condition.trim(), player, context);

        if (resolved.contains(" != "))         return evalEquality(resolved, " != ", false);
        if (resolved.contains(" == "))         return evalEquality(resolved, " == ", true);
        if (resolved.contains(" <= "))         return evalNumeric(resolved, " <= ");
        if (resolved.contains(" >= "))         return evalNumeric(resolved, " >= ");
        if (resolved.contains(" < "))          return evalNumeric(resolved, " < ");
        if (resolved.contains(" > "))          return evalNumeric(resolved, " > ");
        if (resolved.contains(" contains "))   return evalContains(resolved);
        if (resolved.contains(" startsWith ")) return evalStartsWith(resolved);
        if (resolved.contains(" endsWith "))   return evalEndsWith(resolved);

        return Boolean.parseBoolean(resolved);
    }

    private static boolean evalEquality(String expr, String op, boolean expectEqual) {
        String[] parts = expr.split(java.util.regex.Pattern.quote(op), 2);
        if (parts.length != 2) return false;
        boolean equal = parts[0].trim().equalsIgnoreCase(parts[1].trim());
        return expectEqual ? equal : !equal;
    }

    private static boolean evalNumeric(String expr, String op) {
        String[] parts = expr.split(java.util.regex.Pattern.quote(op), 2);
        if (parts.length != 2) return false;
        try {
            double left = Double.parseDouble(parts[0].trim());
            double right = Double.parseDouble(parts[1].trim());
            return switch (op.trim()) {
                case "<"  -> left < right;
                case ">"  -> left > right;
                case "<=" -> left <= right;
                case ">=" -> left >= right;
                default   -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean evalContains(String expr) {
        String[] parts = expr.split(java.util.regex.Pattern.quote(" contains "), 2);
        if (parts.length != 2) return false;
        return parts[0].trim().contains(parts[1].trim());
    }

    private static boolean evalStartsWith(String expr) {
        String[] parts = expr.split(java.util.regex.Pattern.quote(" startsWith "), 2);
        if (parts.length != 2) return false;
        return parts[0].trim().startsWith(parts[1].trim());
    }

    private static boolean evalEndsWith(String expr) {
        String[] parts = expr.split(java.util.regex.Pattern.quote(" endsWith "), 2);
        if (parts.length != 2) return false;
        return parts[0].trim().endsWith(parts[1].trim());
    }
}

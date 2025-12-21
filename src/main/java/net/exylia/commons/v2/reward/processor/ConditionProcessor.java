package net.exylia.commons.v2.reward.processor;

import net.exylia.commons.v2.placeholders.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

public class ConditionProcessor {

    public static boolean evaluate(String condition, Player player, PlaceholderContext context) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        String processed = Placeholders.process(condition, player, context);

        return evaluateExpression(processed);
    }

    private static boolean evaluateExpression(String expression) {
        expression = expression.trim();

        if (expression.equalsIgnoreCase("true")) {
            return true;
        }
        if (expression.equalsIgnoreCase("false")) {
            return false;
        }

        if (expression.contains(">=")) {
            return evaluateComparison(expression, ">=");
        }
        if (expression.contains("<=")) {
            return evaluateComparison(expression, "<=");
        }
        if (expression.contains(">")) {
            return evaluateComparison(expression, ">");
        }
        if (expression.contains("<")) {
            return evaluateComparison(expression, "<");
        }
        if (expression.contains("==")) {
            return evaluateEquality(expression);
        }
        if (expression.contains("!=")) {
            return evaluateInequality(expression);
        }

        return false;
    }

    private static boolean evaluateComparison(String expression, String operator) {
        String[] parts = expression.split(operator);
        if (parts.length != 2) {
            return false;
        }

        try {
            double left = Double.parseDouble(parts[0].trim());
            double right = Double.parseDouble(parts[1].trim());

            return switch (operator) {
                case ">=" -> left >= right;
                case "<=" -> left <= right;
                case ">" -> left > right;
                case "<" -> left < right;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean evaluateEquality(String expression) {
        String[] parts = expression.split("==");
        if (parts.length != 2) {
            return false;
        }

        String left = parts[0].trim();
        String right = parts[1].trim();

        return left.equals(right);
    }

    private static boolean evaluateInequality(String expression) {
        String[] parts = expression.split("!=");
        if (parts.length != 2) {
            return false;
        }

        String left = parts[0].trim();
        String right = parts[1].trim();

        return !left.equals(right);
    }
}

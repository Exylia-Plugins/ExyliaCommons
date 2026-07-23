package net.exylia.commons.v2.expression.api;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.expression.core.ExpressionEvaluator;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

public final class ExpressionAPI {

    private ExpressionAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static double evaluate(String formula, Player player, PlaceholderContext context, double defaultValue) {
        if (formula == null || formula.isBlank()) return defaultValue;

        String resolved = Placeholders.process(formula, player, context);

        return evaluate(resolved, defaultValue);
    }

    public static double evaluate(String formula, double defaultValue) {
        if (formula == null || formula.isBlank()) return defaultValue;

        try {
            return ExpressionEvaluator.evaluate(formula);
        } catch (Exception e) {
            DebugAPI.logLibError(DebugCategory.GENERAL, "Failed to evaluate expression '" + formula + "': " + e.getMessage());
            return defaultValue;
        }
    }
}

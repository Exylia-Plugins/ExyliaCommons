package net.exylia.commons.v2.effect.api;

import net.exylia.commons.v2.effect.core.EffectExecutor;
import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

import java.util.List;

public final class EffectAPI {
    private EffectAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void execute(Player player, EffectEntry entry) {
        EffectExecutor.execute(player, entry);
    }

    public static void execute(Player player, EffectEntry entry, PlaceholderContext context) {
        EffectExecutor.execute(player, entry, context);
    }

    public static void execute(Player player, List<EffectEntry> entries) {
        EffectExecutor.execute(player, entries);
    }

    public static void execute(Player player, List<EffectEntry> entries, PlaceholderContext context) {
        EffectExecutor.execute(player, entries, context);
    }
}

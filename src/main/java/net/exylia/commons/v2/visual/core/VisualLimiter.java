package net.exylia.commons.v2.visual.core;

import org.bukkit.entity.Player;

import java.util.Map;

import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public final class VisualLimiter {
    private static final int DEFAULT_MAX_PER_PLAYER = 20;
    private static final Map<VisualType, Integer> TYPE_LIMITS = Map.of(
            VisualType.ACTIONBAR, 5,
            VisualType.BOSSBAR, 10,
            VisualType.TITLE, 3,
            VisualType.PARTICLE, 100,
            VisualType.SOUND, 50,
            VisualType.MESSAGE, 50,
            VisualType.EFFECT, 20,
            VisualType.FIREWORK, 10
    );

    private VisualLimiter() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean canAdd(Player player, VisualType type) {
        VisualRegistry registry = VisualRegistry.getInstance();

        int total = registry.countByPlayer(player);
        if (total >= DEFAULT_MAX_PER_PLAYER) {
            logInternalWarn("Player " + player.getName() + " has reached max visual limit: " + total);
            return false;
        }

        int typeCount = registry.countByPlayerAndType(player, type);
        int limit = TYPE_LIMITS.getOrDefault(type, 10);

        if (typeCount >= limit) {
            logInternalWarn("Player " + player.getName() + " has reached " + type + " limit: " + typeCount);
            return false;
        }

        return true;
    }

    public static int getLimit(VisualType type) {
        return TYPE_LIMITS.getOrDefault(type, 10);
    }

    public static int getDefaultMaxPerPlayer() {
        return DEFAULT_MAX_PER_PLAYER;
    }
}

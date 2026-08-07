package net.exylia.commons.v2.ui.selector.impl.effect;

import net.exylia.commons.v2.effect.model.EffectEntry;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EffectClipboard {
    private static final ConcurrentHashMap<UUID, EffectEntry> CLIPBOARD = new ConcurrentHashMap<>();

    private EffectClipboard() {}

    public static void copy(Player player, EffectEntry entry) {
        if (entry != null) CLIPBOARD.put(player.getUniqueId(), entry.copy());
    }

    public static EffectEntry paste(Player player) {
        EffectEntry entry = CLIPBOARD.get(player.getUniqueId());
        return entry == null ? null : entry.copy();
    }

    public static boolean has(Player player) {
        return CLIPBOARD.containsKey(player.getUniqueId());
    }

    public static void clear(Player player) {
        CLIPBOARD.remove(player.getUniqueId());
    }
}

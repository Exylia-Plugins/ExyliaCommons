package net.exylia.commons.v2.ui.selector.impl.loot;

import net.exylia.commons.v2.loot.model.LootEntry;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LootClipboard {

    private static final ConcurrentHashMap<UUID, LootEntry> clipboard = new ConcurrentHashMap<>();

    private LootClipboard() {}

    public static void copy(Player player, LootEntry entry) {
        clipboard.put(player.getUniqueId(), entry.copy());
    }

    public static LootEntry paste(Player player) {
        LootEntry copied = clipboard.get(player.getUniqueId());
        return copied != null ? copied.copy() : null;
    }

    public static boolean has(Player player) {
        return clipboard.containsKey(player.getUniqueId());
    }

    public static void clear(Player player) {
        clipboard.remove(player.getUniqueId());
    }
}

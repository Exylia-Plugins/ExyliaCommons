package net.exylia.commons.v2.ui.selector.impl.loot;

import net.exylia.commons.v2.loot.model.LootEntry;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LootListClipboard {

    private static final ConcurrentHashMap<UUID, List<LootEntry>> clipboard = new ConcurrentHashMap<>();

    private LootListClipboard() {}

    public static void copy(Player player, List<LootEntry> entries) {
        List<LootEntry> copy = new ArrayList<>();
        for (LootEntry entry : entries) {
            copy.add(entry.copy());
        }
        clipboard.put(player.getUniqueId(), copy);
    }

    public static List<LootEntry> paste(Player player) {
        List<LootEntry> stored = clipboard.get(player.getUniqueId());
        if (stored == null) return null;
        List<LootEntry> copy = new ArrayList<>();
        for (LootEntry entry : stored) {
            copy.add(entry.copy());
        }
        return copy;
    }

    public static boolean has(Player player) {
        return clipboard.containsKey(player.getUniqueId());
    }

    public static int size(Player player) {
        List<LootEntry> stored = clipboard.get(player.getUniqueId());
        return stored != null ? stored.size() : 0;
    }

    public static void clear(Player player) {
        clipboard.remove(player.getUniqueId());
    }
}

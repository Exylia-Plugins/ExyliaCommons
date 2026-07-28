package net.exylia.commons.v2.ui.selector.impl.reward;

import net.exylia.commons.v2.reward.model.RewardEntry;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RewardListClipboard {

    private static final ConcurrentHashMap<UUID, List<RewardEntry>> clipboard = new ConcurrentHashMap<>();

    private RewardListClipboard() {}

    public static void copy(Player player, List<RewardEntry> entries) {
        List<RewardEntry> copy = new ArrayList<>();
        for (RewardEntry entry : entries) {
            copy.add(entry.copy());
        }
        clipboard.put(player.getUniqueId(), copy);
    }

    public static List<RewardEntry> paste(Player player) {
        List<RewardEntry> stored = clipboard.get(player.getUniqueId());
        if (stored == null) return null;
        List<RewardEntry> copy = new ArrayList<>();
        for (RewardEntry entry : stored) {
            copy.add(entry.copy());
        }
        return copy;
    }

    public static boolean has(Player player) {
        return clipboard.containsKey(player.getUniqueId());
    }

    public static int size(Player player) {
        List<RewardEntry> stored = clipboard.get(player.getUniqueId());
        return stored != null ? stored.size() : 0;
    }

    public static void clear(Player player) {
        clipboard.remove(player.getUniqueId());
    }
}

package net.exylia.commons.v2.ui.selector.impl.reward;

import net.exylia.commons.v2.reward.model.RewardEntry;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RewardClipboard {

    private static final ConcurrentHashMap<UUID, RewardEntry> clipboard = new ConcurrentHashMap<>();

    private RewardClipboard() {}

    public static void copy(Player player, RewardEntry entry) {
        clipboard.put(player.getUniqueId(), entry.copy());
    }

    public static RewardEntry paste(Player player) {
        RewardEntry copied = clipboard.get(player.getUniqueId());
        return copied != null ? copied.copy() : null;
    }

    public static boolean has(Player player) {
        return clipboard.containsKey(player.getUniqueId());
    }

    public static void clear(Player player) {
        clipboard.remove(player.getUniqueId());
    }
}

package net.exylia.commons.v2.ui.selector.impl.namedcommand;

import net.exylia.commons.v2.namedcommand.model.NamedCommandEntry;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NamedCommandClipboard {

    private static final ConcurrentHashMap<UUID, NamedCommandEntry> clipboard = new ConcurrentHashMap<>();

    private NamedCommandClipboard() {}

    public static void copy(Player player, NamedCommandEntry entry) {
        clipboard.put(player.getUniqueId(), entry.copy());
    }

    public static NamedCommandEntry paste(Player player) {
        NamedCommandEntry copied = clipboard.get(player.getUniqueId());
        return copied != null ? copied.copy() : null;
    }

    public static boolean has(Player player) {
        return clipboard.containsKey(player.getUniqueId());
    }

    public static void clear(Player player) {
        clipboard.remove(player.getUniqueId());
    }
}

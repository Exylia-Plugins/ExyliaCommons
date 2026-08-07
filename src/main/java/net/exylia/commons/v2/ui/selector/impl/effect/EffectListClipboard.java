package net.exylia.commons.v2.ui.selector.impl.effect;

import net.exylia.commons.v2.effect.model.EffectEntry;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EffectListClipboard {
    private static final ConcurrentHashMap<UUID, List<EffectEntry>> CLIPBOARD = new ConcurrentHashMap<>();

    private EffectListClipboard() {}

    public static void copy(Player player, List<EffectEntry> entries) {
        List<EffectEntry> copy = new ArrayList<>();
        if (entries != null) entries.forEach(entry -> copy.add(entry.copy()));
        CLIPBOARD.put(player.getUniqueId(), copy);
    }

    public static List<EffectEntry> paste(Player player) {
        List<EffectEntry> entries = CLIPBOARD.get(player.getUniqueId());
        if (entries == null) return null;
        List<EffectEntry> copy = new ArrayList<>();
        entries.forEach(entry -> copy.add(entry.copy()));
        return copy;
    }

    public static boolean has(Player player) {
        return CLIPBOARD.containsKey(player.getUniqueId());
    }

    public static int size(Player player) {
        List<EffectEntry> entries = CLIPBOARD.get(player.getUniqueId());
        return entries == null ? 0 : entries.size();
    }

    public static void clear(Player player) {
        CLIPBOARD.remove(player.getUniqueId());
    }
}

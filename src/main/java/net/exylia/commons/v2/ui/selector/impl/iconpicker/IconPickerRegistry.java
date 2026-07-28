package net.exylia.commons.v2.ui.selector.impl.iconpicker;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class IconPickerRegistry {

    private static final IconPickerRegistry INSTANCE = new IconPickerRegistry();

    private final ConcurrentHashMap<UUID, IconPickerSession> sessions = new ConcurrentHashMap<>();

    private IconPickerRegistry() {}

    public static IconPickerRegistry getInstance() {
        return INSTANCE;
    }

    public void put(IconPickerSession session) {
        sessions.put(session.getPlayer().getUniqueId(), session);
    }

    public IconPickerSession get(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void remove(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public boolean has(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }
}

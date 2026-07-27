package net.exylia.commons.v2.ui.selector.impl.namedcommand;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NamedCommandEditorRegistry {

    private static final NamedCommandEditorRegistry INSTANCE = new NamedCommandEditorRegistry();

    private final ConcurrentHashMap<UUID, NamedCommandEditorSession> sessions = new ConcurrentHashMap<>();

    private NamedCommandEditorRegistry() {}

    public static NamedCommandEditorRegistry getInstance() {
        return INSTANCE;
    }

    public void put(NamedCommandEditorSession session) {
        sessions.put(session.getPlayer().getUniqueId(), session);
    }

    public NamedCommandEditorSession get(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void remove(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public boolean has(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }
}

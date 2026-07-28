package net.exylia.commons.v2.ui.selector.impl.loot;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LootEditorRegistry {

    private static final LootEditorRegistry INSTANCE = new LootEditorRegistry();

    private final ConcurrentHashMap<UUID, LootEditorSession> sessions = new ConcurrentHashMap<>();

    private LootEditorRegistry() {}

    public static LootEditorRegistry getInstance() {
        return INSTANCE;
    }

    public void put(LootEditorSession session) {
        sessions.put(session.getPlayer().getUniqueId(), session);
    }

    public LootEditorSession get(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void remove(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public boolean has(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }
}

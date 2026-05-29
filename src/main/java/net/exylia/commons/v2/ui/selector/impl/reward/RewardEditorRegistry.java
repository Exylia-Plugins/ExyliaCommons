package net.exylia.commons.v2.ui.selector.impl.reward;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RewardEditorRegistry {

    private static final RewardEditorRegistry INSTANCE = new RewardEditorRegistry();

    private final ConcurrentHashMap<UUID, RewardEditorSession> sessions = new ConcurrentHashMap<>();

    private RewardEditorRegistry() {}

    public static RewardEditorRegistry getInstance() {
        return INSTANCE;
    }

    public void put(RewardEditorSession session) {
        sessions.put(session.getPlayer().getUniqueId(), session);
    }

    public RewardEditorSession get(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void remove(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public boolean has(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }
}

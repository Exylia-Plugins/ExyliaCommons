package net.exylia.commons.v2.ui.selector.impl.effect;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EffectEditorRegistry {
    private static final EffectEditorRegistry INSTANCE = new EffectEditorRegistry();
    private final ConcurrentHashMap<UUID, EffectEditorSession> sessions = new ConcurrentHashMap<>();

    private EffectEditorRegistry() {}

    public static EffectEditorRegistry getInstance() {
        return INSTANCE;
    }

    public void put(EffectEditorSession session) {
        sessions.put(session.getPlayer().getUniqueId(), session);
    }

    public EffectEditorSession get(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void remove(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public boolean has(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }
}

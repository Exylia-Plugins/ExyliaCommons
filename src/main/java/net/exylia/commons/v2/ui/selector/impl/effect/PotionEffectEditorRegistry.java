package net.exylia.commons.v2.ui.selector.impl.effect;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PotionEffectEditorRegistry {

    private static final PotionEffectEditorRegistry INSTANCE = new PotionEffectEditorRegistry();

    private final ConcurrentHashMap<UUID, PotionEffectEditorSession> sessions = new ConcurrentHashMap<>();

    private PotionEffectEditorRegistry() {}

    public static PotionEffectEditorRegistry getInstance() {
        return INSTANCE;
    }

    public void put(PotionEffectEditorSession session) {
        sessions.put(session.getPlayer().getUniqueId(), session);
    }

    public PotionEffectEditorSession get(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void remove(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public boolean has(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }
}

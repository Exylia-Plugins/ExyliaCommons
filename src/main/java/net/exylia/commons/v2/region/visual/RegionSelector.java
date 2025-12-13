package net.exylia.commons.v2.region.visual;

import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RegionSelector {
    private static RegionSelector instance;

    private final Map<UUID, SelectionSession> activeSessions;

    private RegionSelector() {
        this.activeSessions = new ConcurrentHashMap<>();
    }

    public static RegionSelector getInstance() {
        if (instance == null) {
            instance = new RegionSelector();
        }
        return instance;
    }

    public SelectionSession showSelector(Player player, Region region, Color color) {
        stopSession(player);

        SelectionSession session = new SelectionSession(player, region, color);
        activeSessions.put(player.getUniqueId(), session);
        session.start();

        return session;
    }

    public SelectionSession showSelector(Player player, Region region) {
        return showSelector(player, region, Color.AQUA);
    }

    public void stopSession(Player player) {
        SelectionSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.stop();
        }
    }

    public Optional<SelectionSession> getSession(Player player) {
        return Optional.ofNullable(activeSessions.get(player.getUniqueId()));
    }

    public boolean hasActiveSession(Player player) {
        SelectionSession session = activeSessions.get(player.getUniqueId());
        return session != null && session.isActive();
    }

    public void stopAllSessions() {
        for (SelectionSession session : activeSessions.values()) {
            session.stop();
        }
        activeSessions.clear();
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public void cleanup() {
        stopAllSessions();
    }
}

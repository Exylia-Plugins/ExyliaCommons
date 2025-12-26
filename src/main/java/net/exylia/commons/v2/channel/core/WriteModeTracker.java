package net.exylia.commons.v2.channel.core;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WriteModeTracker {

    private final ConcurrentHashMap<UUID, String> playerWriteModes;

    public WriteModeTracker() {
        this.playerWriteModes = new ConcurrentHashMap<>();
    }

    public void setWriteMode(UUID playerId, String channelId) {
        playerWriteModes.put(playerId, channelId);
    }

    public void clearWriteMode(UUID playerId) {
        playerWriteModes.remove(playerId);
    }

    public Optional<String> getWriteMode(UUID playerId) {
        return Optional.ofNullable(playerWriteModes.get(playerId));
    }

    public boolean isInWriteMode(UUID playerId) {
        return playerWriteModes.containsKey(playerId);
    }

    public void clearAllForChannel(String channelId) {
        playerWriteModes.entrySet().removeIf(entry -> entry.getValue().equals(channelId));
    }

    public void cleanup(UUID playerId) {
        clearWriteMode(playerId);
    }

    public void clearAll() {
        playerWriteModes.clear();
    }

    public int getActiveCount() {
        return playerWriteModes.size();
    }
}

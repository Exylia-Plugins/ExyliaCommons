package net.exylia.commons.v2.database.cache;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class SessionMetadata {
    private final UUID sessionId;
    private final long startTime;
    private long lastActivityTime;
    private boolean dirty;
    private final Map<String, Object> customData;

    public SessionMetadata(UUID sessionId) {
        this.sessionId = sessionId;
        this.startTime = System.currentTimeMillis();
        this.lastActivityTime = this.startTime;
        this.dirty = false;
        this.customData = new HashMap<>();
    }

    public SessionMetadata(UUID sessionId, Map<String, Object> customData) {
        this.sessionId = sessionId;
        this.startTime = System.currentTimeMillis();
        this.lastActivityTime = this.startTime;
        this.dirty = false;
        this.customData = customData != null ? new HashMap<>(customData) : new HashMap<>();
    }

    public void updateActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }

    public long getSessionDurationMillis() {
        return System.currentTimeMillis() - startTime;
    }

    public long getIdleTimeMillis() {
        return System.currentTimeMillis() - lastActivityTime;
    }
}

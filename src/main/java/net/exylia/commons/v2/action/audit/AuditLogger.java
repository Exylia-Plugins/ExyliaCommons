package net.exylia.commons.v2.action.audit;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.utils.DebugUtils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AuditLogger {
    private final Queue<AuditEntry> entries;
    private final int maxEntries;
    @Getter
    @Setter
    private boolean enabled;

    public AuditLogger() {
        this(1000);
    }

    public AuditLogger(int maxEntries) {
        this.entries = new ConcurrentLinkedQueue<>();
        this.maxEntries = maxEntries;
        this.enabled = false;
    }

    public void log(AuditEntry entry) {
        if (!enabled) {
            return;
        }

        entries.offer(entry);

        while (entries.size() > maxEntries) {
            entries.poll();
        }

        DebugUtils.logInternalInfo("Action audit: " + entry.getActionId() + " by " + entry.getPlayerId());
    }

    public Queue<AuditEntry> getEntries() {
        return new ConcurrentLinkedQueue<>(entries);
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }
}

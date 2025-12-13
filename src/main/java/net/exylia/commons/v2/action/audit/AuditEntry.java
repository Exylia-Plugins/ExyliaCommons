package net.exylia.commons.v2.action.audit;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.action.model.ActionSource;

import java.util.UUID;

@Getter
@Builder
public class AuditEntry {
    private final UUID entryId;
    private final String actionId;
    private final UUID playerId;
    private final ActionSource source;
    private final long timestamp;
    private final String arguments;
    private final boolean success;
    private final String errorMessage;

    public static class AuditEntryBuilder {
        private UUID entryId = UUID.randomUUID();
        private long timestamp = System.currentTimeMillis();
    }
}

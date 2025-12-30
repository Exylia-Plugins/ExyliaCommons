package net.exylia.commons.v2.action.pipeline;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.action.audit.AuditEntry;
import net.exylia.commons.v2.action.audit.AuditLogger;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;

@RequiredArgsConstructor
public class LoggingMiddleware implements Middleware {
    private final AuditLogger auditLogger;

    @Override
    public void execute(Action action, ActionContext context) {
        if (action.getMetadata().isAuditable() && auditLogger.isEnabled()) {
            AuditEntry entry = AuditEntry.builder()
                    .actionId(action.getMetadata().getFullId())
                    .playerId(context.getPlayer().getUniqueId())
                    .source(context.getSource())
                    .arguments(context.getArguments() != null ? context.getArguments().getRaw() : "")
                    .success(true)
                    .build();

            auditLogger.log(entry);

            DebugAPI.logLibDebug(DebugCategory.ACTION, "Audit logged for action: " + action.getMetadata().getFullId() +
                    " by player: " + context.getPlayer().getName());
        }
    }

    @Override
    public int getPriority() {
        return 200;
    }
}

package net.exylia.commons.v2.action.model;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

@Getter
@Builder(toBuilder = true)
public class ActionMetadata {
    private final String id;
    private final String namespace;
    @Builder.Default
    private final String description = "";
    @Builder.Default
    private final String category = "general";
    private final JavaPlugin owner;
    @Builder.Default
    private final ExecutionMode executionMode = ExecutionMode.SYNC;
    @Builder.Default
    private final ActionPriority priority = ActionPriority.NORMAL;
    private final String permission;
    @Builder.Default
    private final long cooldownMillis = 0;
    @Builder.Default
    private final int rateLimit = 0;
    @Builder.Default
    private final boolean auditable = false;
    @Builder.Default
    private final Set<String> aliases = new HashSet<>();

    public String getFullId() {
        if (namespace != null && !namespace.isEmpty()) {
            return namespace + ":" + id;
        }
        return id;
    }
}

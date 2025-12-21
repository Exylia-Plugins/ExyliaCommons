package net.exylia.commons.v2.command.model;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class CommandContext {
    private final Player player;

    @Builder.Default
    private final PlaceholderContext placeholderContext = PlaceholderContext.create();

    @Builder.Default
    private final Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    private final UUID executionId = UUID.randomUUID();

    @Builder.Default
    private final long executionStartTime = System.currentTimeMillis();

    @Builder.Default
    private final boolean silent = false;

    @Builder.Default
    private final boolean ignorePermissions = false;
}

package net.exylia.commons.placeholdersV2.resolver;

import net.exylia.commons.placeholdersV2.context.PlaceholderContext;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface ContextPlaceholderResolver {
    Object resolve(PlaceholderContext context, Player player);

    static ContextPlaceholderResolver of(ContextPlaceholderResolver resolver) {
        return resolver;
    }
}

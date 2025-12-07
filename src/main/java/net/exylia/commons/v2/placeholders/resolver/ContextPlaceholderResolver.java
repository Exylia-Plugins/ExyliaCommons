package net.exylia.commons.v2.placeholders.resolver;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface ContextPlaceholderResolver {
    Object resolve(PlaceholderContext context, Player player);

    static ContextPlaceholderResolver of(ContextPlaceholderResolver resolver) {
        return resolver;
    }
}

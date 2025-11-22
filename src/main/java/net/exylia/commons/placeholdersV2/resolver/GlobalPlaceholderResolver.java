package net.exylia.commons.placeholdersV2.resolver;

import net.exylia.commons.placeholdersV2.context.PlaceholderContext;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface GlobalPlaceholderResolver {
    Object resolve();

    static GlobalPlaceholderResolver of(GlobalPlaceholderResolver resolver) {
        return resolver;
    }
}

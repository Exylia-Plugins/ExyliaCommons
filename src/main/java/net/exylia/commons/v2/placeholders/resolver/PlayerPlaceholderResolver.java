package net.exylia.commons.v2.placeholders.resolver;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlayerPlaceholderResolver {
    Object resolve(Player player);

    static PlayerPlaceholderResolver of(PlayerPlaceholderResolver resolver) {
        return resolver;
    }
}

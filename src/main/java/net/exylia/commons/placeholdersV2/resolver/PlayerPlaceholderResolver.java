package net.exylia.commons.placeholdersV2.resolver;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlayerPlaceholderResolver {
    Object resolve(Player player);

    static PlayerPlaceholderResolver of(PlayerPlaceholderResolver resolver) {
        return resolver;
    }
}

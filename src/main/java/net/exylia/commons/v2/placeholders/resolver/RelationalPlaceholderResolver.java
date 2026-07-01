package net.exylia.commons.v2.placeholders.resolver;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface RelationalPlaceholderResolver {
    Object resolve(Player requester, Player target, PlaceholderContext context);
}

package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.config.VisualConfig;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public interface VisualRenderer<T extends VisualConfig> {
    CompletableFuture<Void> renderAsync(Player player, T config, PlaceholderContext context);

    void cleanup(Player player, String visualId);
}

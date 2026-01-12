package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.config.VisualConfig;
import org.bukkit.entity.Player;

public interface VisualRenderer<T extends VisualConfig> {
    void render(Player player, T config, PlaceholderContext context);

    void cleanup(Player player, String visualId);
}

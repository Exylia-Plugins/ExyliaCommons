package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.v2.visual.cache.CacheManager;
import net.exylia.commons.v2.visual.config.ActionBarConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Collection;

public class ActionBarRenderer implements VisualRenderer<ActionBarConfig> {
    private static final ActionBarRenderer INSTANCE = new ActionBarRenderer();

    private ActionBarRenderer() {
    }

    public static ActionBarRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void render(Player player, ActionBarConfig config, PlaceholderContext context) {
        if (!player.isOnline()) return;

        Component component = CacheManager.getInstance()
                .processAndParse(config.getText(), player, context);
        player.sendActionBar(component);
    }

    public void renderBatch(Collection<Player> players, ActionBarConfig config, PlaceholderContext context) {
        Component component = CacheManager.getInstance()
                .processAndParse(config.getText(), null, context);

        for (Player player : players) {
            if (player.isOnline()) {
                player.sendActionBar(component);
            }
        }
    }

    @Override
    public void cleanup(Player player, String visualId) {
        if (player.isOnline()) {
            player.sendActionBar(Component.empty());
        }
    }
}

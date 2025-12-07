package net.exylia.commons.v2.visual.component;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface VisualComponent<T> {
    CompletableFuture<String> send(Player player, ConfigurationSection section);

    CompletableFuture<String> send(Player player, ConfigurationSection section, PlaceholderContext context);

    CompletableFuture<String> send(Player player, ConfigurationSection section, Consumer<PlaceholderContext> contextBuilder);

    default boolean isEnabled(ConfigurationSection section) {
        return section != null && section.getBoolean("enabled", true);
    }
}

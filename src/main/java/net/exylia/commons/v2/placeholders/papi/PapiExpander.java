package net.exylia.commons.v2.placeholders.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.placeholders.registry.PlaceholderRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PapiExpander extends PlaceholderExpansion {
    private final String identifier;
    private final PlaceholderRegistry registry;

    public PapiExpander(String identifier) {
        this.identifier = identifier;
        this.registry = PlaceholderRegistry.getInstance();
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "ExyliaCommons";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "2.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        Object result = registry.resolve(params, player, new PlaceholderContext().withPlayer(player));
        return result != null ? result.toString() : "";
    }
}

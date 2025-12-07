package net.exylia.commons.v2.placeholders.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.placeholders.registry.PlaceholderRegistryV2;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PapiExpanderV2 extends PlaceholderExpansion {
    private final String identifier;
    private final PlaceholderRegistryV2 registry;

    public PapiExpanderV2(String identifier) {
        this.identifier = identifier;
        this.registry = PlaceholderRegistryV2.getInstance();
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

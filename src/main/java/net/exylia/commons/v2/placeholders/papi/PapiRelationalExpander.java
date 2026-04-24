package net.exylia.commons.v2.placeholders.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.placeholders.registry.PlaceholderRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PapiRelationalExpander extends PlaceholderExpansion implements Relational {
    private final String identifier;

    public PapiRelationalExpander(String identifier) {
        this.identifier = identifier;
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
    public String onPlaceholderRequest(Player requester, Player target, @NotNull String params) {
        if (requester == null || target == null) return null;
        if (!PlaceholderRegistry.isInitialized()) return null;
        Object result = PlaceholderRegistry.getInstance().resolveRelational(
                params, requester, target,
                new PlaceholderContext().withPlayer(requester)
        );
        return result != null ? result.toString() : null;
    }
}

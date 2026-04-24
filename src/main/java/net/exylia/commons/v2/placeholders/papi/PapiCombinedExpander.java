package net.exylia.commons.v2.placeholders.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.placeholders.registry.PlaceholderRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PapiCombinedExpander extends PlaceholderExpansion implements Relational {
    private final String identifier;
    private volatile boolean handleNormal = false;
    private volatile boolean handleRelational = false;

    public PapiCombinedExpander(String identifier) {
        this.identifier = identifier;
    }

    public void enableNormal() {
        this.handleNormal = true;
    }

    public void enableRelational() {
        this.handleRelational = true;
    }

    public void disableNormal() {
        this.handleNormal = false;
    }

    public void disableRelational() {
        this.handleRelational = false;
    }

    public boolean isFullyDisabled() {
        return !handleNormal && !handleRelational;
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
        if (!handleNormal || !PlaceholderRegistry.isInitialized()) return null;
        Object result = PlaceholderRegistry.getInstance().resolve(
                params, player, new PlaceholderContext().withPlayer(player)
        );
        return result != null ? result.toString() : null;
    }

    @Override
    public String onPlaceholderRequest(Player requester, Player target, @NotNull String params) {
        if (!handleRelational || requester == null || target == null || !PlaceholderRegistry.isInitialized()) return null;
        Object result = PlaceholderRegistry.getInstance().resolveRelational(
                params, requester, target,
                new PlaceholderContext().withPlayer(requester)
        );
        return result != null ? result.toString() : null;
    }
}

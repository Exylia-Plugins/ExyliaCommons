package net.exylia.commons.v2.placeholders.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.placeholders.registry.PlaceholderRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PapiExpander extends PlaceholderExpansion {
    private final String identifier;

    public PapiExpander(String identifier) {
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
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        net.exylia.commons.v2.debug.api.DebugAPI.logLibDebug(net.exylia.commons.v2.debug.core.DebugCategory.PLACEHOLDER,
            "[PapiExpander:" + identifier + "] called params='" + params + "' player=" + (player != null ? player.getName() : "null") + " registryOk=" + PlaceholderRegistry.isInitialized());
        if (!PlaceholderRegistry.isInitialized()) return "";
        PlaceholderRegistry registry = PlaceholderRegistry.getInstance();
        net.exylia.commons.v2.debug.api.DebugAPI.logLibDebug(net.exylia.commons.v2.debug.core.DebugCategory.PLACEHOLDER,
            "[PapiExpander:" + identifier + "] stats=" + registry.getStats());
        Object result = registry.resolve(params, player, new PlaceholderContext().withPlayer(player));
        net.exylia.commons.v2.debug.api.DebugAPI.logLibDebug(net.exylia.commons.v2.debug.core.DebugCategory.PLACEHOLDER,
            "[PapiExpander:" + identifier + "] result=" + result);
        return result != null ? result.toString() : "";
    }
}

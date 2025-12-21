package net.exylia.commons.v2.visual.instance;

import lombok.Getter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class GlobalCountdownContext {
    private final GlobalCountdownInstance<?> instance;
    private final PlaceholderContext placeholderContext;
    private final long ticksRemaining;
    private final long secondsRemaining;
    private final double progress;

    public GlobalCountdownContext(GlobalCountdownInstance<?> instance, PlaceholderContext context) {
        this.instance = instance;
        this.placeholderContext = context;
        this.ticksRemaining = instance.getTicksRemaining();
        this.secondsRemaining = (ticksRemaining + 19) / 20;
        this.progress = instance.getInitialDurationTicks() > 0
                ? (double) ticksRemaining / instance.getInitialDurationTicks()
                : 0.0;
    }

    public Set<Player> getCurrentViewers() {
        return instance.getViewers().stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toSet());
    }

    public void broadcast(String message) {
        getCurrentViewers().forEach(p -> p.sendMessage(message));
    }

    public int getViewerCount() {
        return getCurrentViewers().size();
    }

    public boolean hasViewers() {
        return !instance.getViewers().isEmpty();
    }
}

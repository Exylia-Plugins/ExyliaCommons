package net.exylia.commons.v2.region.visual;

import lombok.Getter;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class SelectionSession {
    private final UUID sessionId;
    private final Player player;
    private final Region region;
    private final Color color;
    private final ParticleRenderer renderer;
    private final long createdAt;

    private ScheduledTask renderTask;
    private volatile boolean active;

    public SelectionSession(Player player, Region region, Color color) {
        this.sessionId = UUID.randomUUID();
        this.player = player;
        this.region = region;
        this.color = color;
        this.renderer = new ParticleRenderer();
        this.createdAt = System.currentTimeMillis();
        this.active = true;
    }

    public void start() {
        if (renderTask != null) {
            return;
        }

        double optimalStep = renderer.calculateOptimalStep(region.getVolume());

        renderTask = Schedulers.syncTimer(() -> {
            if (!active || !player.isOnline()) {
                stop();
                return;
            }

            renderer.renderBorders(region, player, color, optimalStep);
        }, 5L, 5L);
    }

    public void stop() {
        active = false;
        if (renderTask != null) {
            renderTask.cancel();
            renderTask = null;
        }
    }

    public long getDuration() {
        return System.currentTimeMillis() - createdAt;
    }

    public boolean isActive() {
        return active && renderTask != null;
    }

    @Override
    public String toString() {
        return String.format("SelectionSession{player=%s, region=%s, color=%s, active=%b, duration=%dms}",
                player.getName(), region.getId(), color, active, getDuration());
    }
}

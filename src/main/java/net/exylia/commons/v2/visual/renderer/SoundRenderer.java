package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.async.SchedulerManager;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.config.SoundConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class SoundRenderer implements VisualRenderer<SoundConfig> {
    private static final SoundRenderer INSTANCE = new SoundRenderer();
    private static final double NEARBY_RADIUS = 16.0;

    private SoundRenderer() {
    }

    public static SoundRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public CompletableFuture<Void> renderAsync(Player player, SoundConfig config, PlaceholderContext context) {
        return CompletableFuture.runAsync(() -> {
            Location location = determineLocation(player, config);
            Collection<Player> targets = determineTargets(player, config);

            SchedulerManager.getInstance().runSync(() -> {
                for (Player target : targets) {
                    if (target.isOnline()) {
                        target.playSound(location, config.getSound(), config.getVolume(), config.getPitch());
                    }
                }
            });
        }, AsyncExecutor.getInstance().getGeneralExecutor());
    }

    @Override
    public void cleanup(Player player, String visualId) {
    }

    private Location determineLocation(Player player, SoundConfig config) {
        return switch (config.getScope()) {
            case PLAYER -> player.getLocation();
            case NEARBY -> player.getLocation();
            case LOCATION -> config.getLocation() != null ? config.getLocation() : player.getLocation();
        };
    }

    private Collection<Player> determineTargets(Player player, SoundConfig config) {
        Collection<Player> targets = new ArrayList<>();

        switch (config.getScope()) {
            case PLAYER -> targets.add(player);
            case NEARBY -> {
                Location loc = player.getLocation();
                double radiusSquared = NEARBY_RADIUS * NEARBY_RADIUS;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getWorld().equals(loc.getWorld()) &&
                            online.getLocation().distanceSquared(loc) <= radiusSquared) {
                        targets.add(online);
                    }
                }
            }
            case LOCATION -> {
                if (config.getLocation() != null) {
                    Location loc = config.getLocation();
                    double radiusSquared = NEARBY_RADIUS * NEARBY_RADIUS;
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (online.getWorld().equals(loc.getWorld()) &&
                                online.getLocation().distanceSquared(loc) <= radiusSquared) {
                            targets.add(online);
                        }
                    }
                }
            }
        }

        return targets;
    }
}

package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.config.SoundConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;

public class SoundRenderer implements VisualRenderer<SoundConfig> {
    private static final SoundRenderer INSTANCE = new SoundRenderer();
    private static final double NEARBY_RADIUS = 16.0;

    private SoundRenderer() {
    }

    public static SoundRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void render(Player player, SoundConfig config, PlaceholderContext context) {
        if (config.getSound() == null) return;
        Collection<Player> targets = determineTargets(player, config);

        if (config.getScope() == SoundConfig.SoundScope.PLAYER) {
            for (Player target : targets) {
                if (target.isOnline()) {
                    target.playSound(target, config.getSound(), config.getVolume(), config.getPitch());
                }
            }
        } else {
            Location location = determineLocation(player, config);
            for (Player target : targets) {
                if (target.isOnline()) {
                    target.playSound(location, config.getSound(), config.getVolume(), config.getPitch());
                }
            }
        }
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

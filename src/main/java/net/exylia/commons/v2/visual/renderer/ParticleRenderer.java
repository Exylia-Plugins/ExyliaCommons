package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.compat.ParticleCompat;
import net.exylia.commons.v2.visual.config.ParticleConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;

public class ParticleRenderer implements VisualRenderer<ParticleConfig> {
    private static final ParticleRenderer INSTANCE = new ParticleRenderer();
    private static final double NEARBY_RADIUS = 16.0;

    private ParticleRenderer() {
    }

    public static ParticleRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void render(Player player, ParticleConfig config, PlaceholderContext context) {
        Location location = determineLocation(player, config);
        Collection<Player> targets = determineTargets(player, config);

        if (ParticleCompat.isDustParticle(config.getParticle()) && config.getColor() != null) {
            Particle.DustOptions dustOptions = new Particle.DustOptions(
                    org.bukkit.Color.fromRGB(
                            config.getColor().getRed(),
                            config.getColor().getGreen(),
                            config.getColor().getBlue()
                    ),
                    config.getDustSize()
            );
            for (Player target : targets) {
                if (target.isOnline()) {
                    target.spawnParticle(
                            config.getParticle(),
                            location,
                            config.getCount(),
                            config.getOffsetX(),
                            config.getOffsetY(),
                            config.getOffsetZ(),
                            config.getExtra(),
                            dustOptions
                    );
                }
            }
        } else {
            for (Player target : targets) {
                if (target.isOnline()) {
                    target.spawnParticle(
                            config.getParticle(),
                            location,
                            config.getCount(),
                            config.getOffsetX(),
                            config.getOffsetY(),
                            config.getOffsetZ(),
                            config.getExtra()
                    );
                }
            }
        }
    }

    @Override
    public void cleanup(Player player, String visualId) {
    }

    private Location determineLocation(Player player, ParticleConfig config) {
        return switch (config.getScope()) {
            case PLAYER -> player.getLocation().add(0, 1, 0);
            case NEARBY -> player.getLocation().add(0, 1, 0);
            case LOCATION -> config.getLocation() != null ? config.getLocation() : player.getLocation().add(0, 1, 0);
        };
    }

    private Collection<Player> determineTargets(Player player, ParticleConfig config) {
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

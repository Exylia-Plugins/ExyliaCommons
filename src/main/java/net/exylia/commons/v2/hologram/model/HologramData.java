package net.exylia.commons.v2.hologram.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HologramData {

    private String id;
    private String world;
    private double x;
    private double y;
    private double z;
    private List<String> lines;
    private boolean persistent;
    private boolean perPlayer;
    private double viewDistance;
    private boolean enabled;

    public static HologramData fromHologram(Hologram hologram) {
        return new HologramData(
            hologram.getId(),
            hologram.getLocation().getWorld().getName(),
            hologram.getLocation().getX(),
            hologram.getLocation().getY(),
            hologram.getLocation().getZ(),
            hologram.getLines().stream()
                .map(HologramLine::getText)
                .collect(Collectors.toList()),
            hologram.isPersistent(),
            hologram.isPerPlayer(),
            hologram.getViewDistance(),
            hologram.isEnabled()
        );
    }

    public Hologram toHologram(JavaPlugin plugin) {
        Location location = new Location(
            Bukkit.getWorld(world),
            x, y, z
        );

        if (location.getWorld() == null) {
            throw new IllegalStateException("World not found: " + world);
        }

        List<HologramLine> hologramLines = lines.stream()
            .map(HologramLine::new)
            .collect(Collectors.toList());

        return new Hologram(
            id,
            location,
            hologramLines,
            HologramProperties.defaultProperties(),
            HologramConfig.defaultConfig(),
            persistent,
            perPlayer,
            null,
            viewDistance,
            plugin,
                enabled
        );
    }
}

package net.exylia.commons.v2.hologram.persistence;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.database.annotation.*;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.hologram.model.Hologram;
import net.exylia.commons.v2.hologram.model.HologramConfig;
import net.exylia.commons.v2.hologram.model.HologramLine;
import net.exylia.commons.v2.hologram.model.HologramProperties;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Table(name = "holograms")
@Indexes({
        @Index(name = "idx_world", fields = {"world"}),
        @Index(name = "idx_persistent", fields = {"persistent"})
})
public class HologramEntity extends Entity {

    @Column(name = "id", primaryKey = true)
    private String id;

    @Column(name = "world")
    private String world;

    @Column(name = "x")
    private double x;

    @Column(name = "y")
    private double y;

    @Column(name = "z")
    private double z;

    @Column(name = "lines", serializationType = SerializationType.JSON)
    private List<String> lines;

    @Column(name = "properties", serializationType = SerializationType.JSON)
    private HologramProperties properties;

    @Column(name = "config", serializationType = SerializationType.JSON)
    private HologramConfig config;

    @Column(name = "persistent")
    private boolean persistent;

    @Column(name = "per_player")
    private boolean perPlayer;

    @Column(name = "view_distance")
    private double viewDistance;

    @Column(name = "enabled")
    private boolean enabled;

    @Override
    public Object getId() {
        return id;
    }

    public static HologramEntity fromHologram(Hologram hologram) {
        HologramEntity entity = new HologramEntity();
        entity.setId(hologram.getId());
        entity.setWorld(hologram.getLocation().getWorld().getName());
        entity.setX(hologram.getLocation().getX());
        entity.setY(hologram.getLocation().getY());
        entity.setZ(hologram.getLocation().getZ());
        entity.setLines(hologram.getLines().stream()
                .map(HologramLine::getText)
                .collect(Collectors.toList()));
        entity.setProperties(hologram.getProperties());
        entity.setConfig(hologram.getConfig());
        entity.setPersistent(hologram.isPersistent());
        entity.setPerPlayer(hologram.isPerPlayer());
        entity.setViewDistance(hologram.getViewDistance());
        return entity;
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
                properties != null ? properties : HologramProperties.defaultProperties(),
                config != null ? config : HologramConfig.defaultConfig(),
                persistent,
                perPlayer,
                null,
                viewDistance,
                plugin,
                enabled
        );
    }
}

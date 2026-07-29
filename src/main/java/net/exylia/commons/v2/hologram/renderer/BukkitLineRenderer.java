package net.exylia.commons.v2.hologram.renderer;

import net.exylia.commons.v2.hologram.model.HologramLine;
import net.exylia.commons.v2.hologram.model.HologramProperties;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.UUID;

/**
 * Fallback {@link LineRenderer} used when PacketEvents is not installed.
 * Spawns a single real Display entity (Text/Item/BlockDisplay depending on
 * the line's {@link net.exylia.commons.v2.hologram.model.HologramType}) and
 * controls per-player visibility with the vanilla
 * {@link Player#showEntity}/{@link Player#hideEntity} API.
 */
public class BukkitLineRenderer implements LineRenderer {

    private static final double DUPLICATE_CHECK_RADIUS = 0.1;

    private final JavaPlugin plugin;
    private final HologramLine line;
    private final Set<UUID> hiddenFor = ConcurrentHashMap.newKeySet();

    private volatile Display entity;
    private volatile HologramProperties properties;
    private volatile Component text;

    public BukkitLineRenderer(JavaPlugin plugin, Location location, HologramLine line, HologramProperties properties) {
        this.plugin = plugin;
        this.line = line;
        this.properties = properties;
        this.text = line.getComponent() != null ? line.getComponent() : Component.text(
                line.getText() != null ? line.getText() : "");
        spawnEntity(location);
    }

    private void spawnEntity(Location location) {
        cleanupDuplicates(location);

        EntityType bukkitType = switch (line.getType()) {
            case TEXT -> EntityType.TEXT_DISPLAY;
            case ITEM -> EntityType.ITEM_DISPLAY;
            case BLOCK -> EntityType.BLOCK_DISPLAY;
        };

        Entity spawned = location.getWorld().spawnEntity(location, bukkitType);
        this.entity = (Display) spawned;
        entity.setPersistent(false);
        entity.setInvulnerable(true);
        entity.setGravity(false);
        applyProperties();

        // Hidden from everyone by default; HologramListener/visibility layer
        // decides who gets shown via spawnFor().
    }

    private void cleanupDuplicates(Location location) {
        if (location.getWorld() == null) return;
        Collection<Entity> nearby = location.getWorld()
                .getNearbyEntities(location, DUPLICATE_CHECK_RADIUS, DUPLICATE_CHECK_RADIUS, DUPLICATE_CHECK_RADIUS);
        nearby.stream()
                .filter(e -> e instanceof Display)
                .filter(Entity::isValid)
                .forEach(Entity::remove);
    }

    @Override
    public void spawnFor(Player player) {
        if (entity == null || !entity.isValid()) return;
        hiddenFor.remove(player.getUniqueId());
        player.showEntity(plugin, entity);
    }

    @Override
    public void despawnFor(Player player) {
        if (entity == null || !entity.isValid()) return;
        hiddenFor.add(player.getUniqueId());
        player.hideEntity(plugin, entity);
    }

    @Override
    public void despawnForAll() {
        if (entity != null && entity.isValid()) {
            Tasks.at(entity, () -> {
                if (entity.isValid()) entity.remove();
            });
        }
    }

    @Override
    public boolean isViewing(Player player) {
        return entity != null && entity.isValid() && !hiddenFor.contains(player.getUniqueId());
    }

    @Override
    public void updateText(Component text) {
        this.text = text;
        if (entity instanceof TextDisplay textDisplay && entity.isValid()) {
            Tasks.at(entity, () -> {
                if (entity.isValid()) textDisplay.text(text);
            });
        }
    }

    @Override
    public void updateProperties(HologramProperties properties) {
        this.properties = properties;
        if (entity != null && entity.isValid()) {
            Tasks.at(entity, this::applyProperties);
        }
    }

    @Override
    public void teleport(Location location) {
        if (entity != null && entity.isValid()) {
            entity.teleportAsync(location);
        }
    }

    @Override
    public void rotateTo(float angleRadians) {
        if (entity == null || !entity.isValid()) return;
        Tasks.at(entity, () -> {
            if (!entity.isValid()) return;
            entity.setInterpolationDuration(2);
            entity.setInterpolationDelay(-1);
            Transformation current = entity.getTransformation();
            entity.setTransformation(new Transformation(
                    current.getTranslation(),
                    new Quaternionf().rotateY(angleRadians),
                    current.getScale(),
                    current.getRightRotation()
            ));
        });
    }

    @Override
    public Location getLocation() {
        return entity != null ? entity.getLocation() : null;
    }

    @Override
    public boolean isValid() {
        return entity != null && entity.isValid();
    }

    private void applyProperties() {
        HologramProperties props = properties;
        entity.setBillboard(props.getBillboard());
        entity.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0f, 0f, 1f, 0f),
                new Vector3f(props.getScaleX(), props.getScaleY(), props.getScaleZ()),
                new AxisAngle4f(0f, 0f, 1f, 0f)
        ));
        entity.setGlowing(props.isGlowing());
        if (props.getGlowColorOverride() != null) {
            entity.setGlowColorOverride(props.getGlowColorOverride());
        }
        if (props.getBrightness() >= 0) {
            entity.setBrightness(new Display.Brightness(
                    (props.getBrightness() >> 4) & 0xF,
                    props.getBrightness() & 0xF
            ));
        }

        if (entity instanceof TextDisplay textDisplay) {
            textDisplay.text(text);
            textDisplay.setAlignment(props.getAlignment());
            textDisplay.setShadowed(props.isShadow());
            textDisplay.setSeeThrough(props.isSeeThrough());
            textDisplay.setLineWidth(props.getLineWidth());
            textDisplay.setTextOpacity(props.getTextOpacity());
            textDisplay.setDefaultBackground(props.isDefaultBackground());

            if (props.getBackgroundColor() != null) {
                int alpha = props.getBackgroundAlpha();
                org.bukkit.Color bg = props.getBackgroundColor();
                textDisplay.setBackgroundColor(alpha < 255
                        ? org.bukkit.Color.fromARGB(alpha, bg.getRed(), bg.getGreen(), bg.getBlue())
                        : bg);
            } else if (props.getBackgroundAlpha() == 0) {
                textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            }
        } else if (entity instanceof ItemDisplay itemDisplay) {
            itemDisplay.setItemStack(line.getItem());
            if (line.getItemTransform() != null) {
                itemDisplay.setItemDisplayTransform(line.getItemTransform());
            }
        } else if (entity instanceof BlockDisplay blockDisplay) {
            if (line.getBlock() != null) {
                blockDisplay.setBlock(line.getBlock());
            }
        }
    }
}

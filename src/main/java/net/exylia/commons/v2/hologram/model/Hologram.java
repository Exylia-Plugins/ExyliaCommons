package net.exylia.commons.v2.hologram.model;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.hologram.entity.HologramDisplayEntity;
import net.exylia.commons.v2.hologram.exception.HologramException;
import net.exylia.commons.v2.hologram.visibility.VisibilityCondition;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class Hologram {
    private final String id;
    private final Location location;
    private final List<HologramLine> lines;
    private final HologramProperties properties;
    private final HologramConfig config;
    private final boolean persistent;
    private final boolean perPlayer;
    private final VisibilityCondition visibilityCondition;
    private final double viewDistance;
    private final JavaPlugin plugin;

    private final Map<UUID, TextDisplay> playerDisplays = new ConcurrentHashMap<>();
    @Getter
    private TextDisplay globalDisplay;
    private final AtomicBoolean spawned = new AtomicBoolean(false);
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    @Setter
    private PlaceholderContext placeholderContext;

    public Hologram(String id, Location location, List<HologramLine> lines,
                    HologramProperties properties, HologramConfig config,
                    boolean persistent, boolean perPlayer,
                    VisibilityCondition visibilityCondition, double viewDistance,
                    JavaPlugin plugin, boolean enabled) {
        this.id = id;
        this.location = location;
        this.lines = lines;
        this.properties = properties;
        this.config = config;
        this.persistent = persistent;
        this.perPlayer = perPlayer;
        this.visibilityCondition = visibilityCondition;
        this.viewDistance = viewDistance;
        this.plugin = plugin;
        this.enabled.set(enabled);
    }

    public void spawn() {
        if (!Tasks.isRegionThread(location)) {
            throw new IllegalStateException("Hologram must be spawned on the region thread for its location");
        }

        if (!enabled.get()) {
            return;
        }

        if (spawned.get()) {
            return;
        }

        try {
            if (perPlayer) {
                spawnForOnlinePlayers();
            } else {
                spawnGlobal();
            }
            spawned.set(true);
        } catch (Exception e) {
            throw new HologramException.HologramSpawnException("Failed to spawn hologram " + id, e);
        }
    }

    private void spawnGlobal() {
        HologramDisplayEntity.cleanupDuplicateEntities(location);

        TextDisplay display = (TextDisplay) location.getWorld()
                .spawnEntity(location, EntityType.TEXT_DISPLAY);

        display.text(buildComponent(null));
        display.setPersistent(false);
        applyProperties(display, properties);

        globalDisplay = display;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canSee(player)) {
                player.hideEntity(plugin, display);
            }
        }
    }

    private void spawnForOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (canSee(player)) {
                spawnForPlayer(player);
            }
        }
    }

    public void spawnForPlayer(Player player) {
        if (!canSee(player)) {
            return;
        }

        HologramDisplayEntity.cleanupDuplicateEntities(location);

        TextDisplay display = (TextDisplay) location.getWorld()
                .spawnEntity(location, EntityType.TEXT_DISPLAY);

        display.text(buildComponent(player));
        display.setPersistent(false);
        applyProperties(display, properties);

        player.showEntity(plugin, display);
        playerDisplays.put(player.getUniqueId(), display);
    }

    private Component buildComponent(Player player) {
        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            HologramLine line = lines.get(i);
            String processed = placeholderContext != null
                    ? Placeholders.process(line.getText(), player, placeholderContext)
                    : (player != null ? Placeholders.process(line.getText(), player) : Placeholders.process(line.getText()));
            Component parsed = ColorAPI.parse(processed + "<reset>");
            if (i == 0) {
                result = parsed;
            } else {
                result = result.append(Component.newline()).append(parsed);
            }
        }
        return result;
    }

    private void applyProperties(TextDisplay display, HologramProperties props) {
        display.setBillboard(props.getBillboard());
        display.setAlignment(props.getAlignment());

        org.bukkit.util.Transformation transformation = display.getTransformation();
        transformation.getScale().set(props.getScaleX(), props.getScaleY(), props.getScaleZ());
        display.setTransformation(transformation);

        display.setShadowed(props.isShadow());
        display.setSeeThrough(props.isSeeThrough());
        display.setLineWidth(props.getLineWidth());

        if (props.getBackgroundColor() != null) {
            Color bgColor = props.getBackgroundColor();
            int alpha = props.getBackgroundAlpha();
            if (alpha < 255) {
                Color colorWithAlpha = Color.fromARGB(alpha, bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue());
                display.setBackgroundColor(colorWithAlpha);
            } else {
                display.setBackgroundColor(bgColor);
            }
        } else if (props.getBackgroundAlpha() == 0) {
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        }

        display.setTextOpacity(props.getTextOpacity());
        display.setDefaultBackground(props.isDefaultBackground());

        if (props.getBrightness() >= 0) {
            display.setBrightness(new Display.Brightness(
                    (props.getBrightness() >> 4) & 0xF,
                    props.getBrightness() & 0xF
            ));
        }
    }

    public CompletableFuture<Void> updateAsync() {
        if (!spawned.get()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            if (perPlayer) {
                updatePerPlayer();
            } else {
                updateGlobal();
            }
        });
    }

    private void updateGlobal() {
        TextDisplay display = globalDisplay;
        if (display == null || !display.isValid()) return;
        Tasks.at(display, () -> {
            if (display.isValid()) {
                display.text(buildComponent(null));
            }
        });
    }

    private void updatePerPlayer() {
        List<UUID> toRemove = new ArrayList<>();

        playerDisplays.forEach((playerId, display) -> {
            Player player = Bukkit.getPlayer(playerId);

            if (player == null || !player.isOnline()) {
                toRemove.add(playerId);
                if (display != null && display.isValid()) {
                    Tasks.at(display, () -> removeEntity(display));
                }
                return;
            }

            if (display != null && display.isValid()) {
                Tasks.at(display, () -> {
                    if (display.isValid()) {
                        display.text(buildComponent(player));
                    }
                });
            } else if (display != null) {
                toRemove.add(playerId);
            }
        });

        toRemove.forEach(playerDisplays::remove);
    }

    public void despawn() {
        if (!spawned.get()) {
            return;
        }

        if (perPlayer) {
            playerDisplays.values().forEach(this::removeEntity);
            playerDisplays.clear();
        } else {
            removeEntity(globalDisplay);
            globalDisplay = null;
        }

        spawned.set(false);
    }

    public void markUnspawned() {
        if (!spawned.getAndSet(false)) {
            return;
        }

        if (perPlayer) {
            playerDisplays.clear();
        } else {
            globalDisplay = null;
        }
    }

    public boolean isEntityValid() {
        if (perPlayer) {
            return spawned.get() && !playerDisplays.isEmpty();
        }
        return spawned.get() && globalDisplay != null && globalDisplay.isValid();
    }

    private void removeEntity(TextDisplay entity) {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    public void teleport(Location newLocation) {
        boolean worldChanged = location.getWorld() != null
                && !location.getWorld().equals(newLocation.getWorld());

        this.location.setWorld(newLocation.getWorld());
        this.location.setX(newLocation.getX());
        this.location.setY(newLocation.getY());
        this.location.setZ(newLocation.getZ());
        this.location.setYaw(newLocation.getYaw());
        this.location.setPitch(newLocation.getPitch());

        if (!spawned.get()) return;

        if (worldChanged) {
            despawn();
            spawn();
            return;
        }

        if (perPlayer) {
            playerDisplays.values().forEach(display -> {
                if (display != null && display.isValid()) {
                    display.teleportAsync(newLocation);
                }
            });
        } else if (globalDisplay != null && globalDisplay.isValid()) {
            globalDisplay.teleportAsync(newLocation);
        }
    }

    public void setLine(int index, String text) {
        if (index < 0 || index >= lines.size()) {
            throw new IndexOutOfBoundsException("Line index out of bounds: " + index);
        }
        HologramLine oldLine = lines.get(index);
        lines.set(index, new HologramLine(text, oldLine.getComponent(), oldLine.getProperties()));
    }

    public void addLine(String text) {
        lines.add(new HologramLine(text));
        if (spawned.get()) {
            despawn();
            spawn();
        }
    }

    public void removeLine(int index) {
        if (index < 0 || index >= lines.size()) {
            throw new IndexOutOfBoundsException("Line index out of bounds: " + index);
        }
        lines.remove(index);
        if (spawned.get()) {
            despawn();
            spawn();
        }
    }

    public boolean canSee(Player player) {
        if (!player.getWorld().equals(location.getWorld())) {
            return false;
        }

        double distanceSquared = player.getLocation().distanceSquared(location);
        if (distanceSquared > (viewDistance * viewDistance)) {
            return false;
        }

        if (visibilityCondition != null) {
            return visibilityCondition.canSee(player, this);
        }

        return true;
    }

    public void showTo(Player player) {
        if (!perPlayer) {
            return;
        }

        if (!playerDisplays.containsKey(player.getUniqueId())) {
            Tasks.at(location, () -> spawnForPlayer(player));
        }
    }

    public void hideFrom(Player player) {
        if (!perPlayer) {
            return;
        }

        TextDisplay display = playerDisplays.remove(player.getUniqueId());
        if (display != null && display.isValid()) {
            Tasks.at(display, () -> removeEntity(display));
        }
    }

    public void cleanupPlayer(UUID playerId) {
        TextDisplay display = playerDisplays.remove(playerId);
        if (display != null && display.isValid()) {
            Tasks.at(display, () -> removeEntity(display));
        }
    }

    public boolean isSpawned() {
        return spawned.get();
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void enable() {
        enabled.set(true);
    }

    public void disable() {
        if (!enabled.get()) {
            return;
        }
        enabled.set(false);
        if (spawned.get()) {
            despawn();
        }
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            enable();
        } else {
            disable();
        }
    }

    public int getLineCount() {
        return lines.size();
    }
}

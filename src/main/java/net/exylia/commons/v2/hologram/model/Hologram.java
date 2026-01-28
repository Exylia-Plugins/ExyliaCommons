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

    private final Map<UUID, List<TextDisplay>> playerDisplays = new ConcurrentHashMap<>();
    @Getter
    private final List<TextDisplay> globalDisplays = Collections.synchronizedList(new ArrayList<>());
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
        if (!Tasks.isMain()) {
            throw new IllegalStateException("Hologram must be spawned on main thread");
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
        Location currentLoc = location.clone();

        for (HologramLine line : lines) {
            HologramDisplayEntity.cleanupDuplicateEntities(currentLoc);

            TextDisplay display = (TextDisplay) location.getWorld()
                    .spawnEntity(currentLoc, EntityType.TEXT_DISPLAY);

            String processed = placeholderContext != null
                ? Placeholders.process(line.getText(), null, placeholderContext)
                : Placeholders.process(line.getText());
            Component component = ColorAPI.parse(processed);
            display.text(component);
            display.setPersistent(false);

            HologramProperties lineProps = line.getPropertiesOrDefault(properties);
            applyProperties(display, lineProps);

            globalDisplays.add(display);
            currentLoc.add(0, lineProps.getLineSpacing(), 0);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canSee(player)) {
                for (TextDisplay display : globalDisplays) {
                    player.hideEntity(plugin, display);
                }
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

        Location currentLoc = location.clone();
        List<TextDisplay> displays = new ArrayList<>();

        for (HologramLine line : lines) {
            HologramDisplayEntity.cleanupDuplicateEntities(currentLoc);

            TextDisplay display = (TextDisplay) location.getWorld()
                    .spawnEntity(currentLoc, EntityType.TEXT_DISPLAY);

            String processed = placeholderContext != null
                ? Placeholders.process(line.getText(), player, placeholderContext)
                : Placeholders.process(line.getText(), player);
            Component component = ColorAPI.parse(processed);
            display.text(component);
            display.setPersistent(false);

            HologramProperties lineProps = line.getPropertiesOrDefault(properties);
            applyProperties(display, lineProps);

            player.showEntity(plugin, display);

            displays.add(display);
            currentLoc.add(0, lineProps.getLineSpacing(), 0);
        }

        playerDisplays.put(player.getUniqueId(), displays);
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
            display.setBrightness(new org.bukkit.entity.Display.Brightness(
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
        Tasks.sync(() -> {
            for (int i = 0; i < lines.size() && i < globalDisplays.size(); i++) {
                HologramLine line = lines.get(i);
                TextDisplay display = globalDisplays.get(i);

                if (display != null && display.isValid()) {
                    String processed = placeholderContext != null
                        ? Placeholders.process(line.getText(), null, placeholderContext)
                        : Placeholders.process(line.getText());
                    Component component = ColorAPI.parse(processed);
                    display.text(component);
                }
            }
        });
    }

    private void updatePerPlayer() {
        Tasks.sync(() -> {
            List<UUID> toRemove = new ArrayList<>();

            playerDisplays.forEach((playerId, displays) -> {
                Player player = Bukkit.getPlayer(playerId);

                if (player == null || !player.isOnline()) {
                    toRemove.add(playerId);
                    displays.forEach(this::removeEntity);
                    return;
                }

                for (int i = 0; i < lines.size() && i < displays.size(); i++) {
                    HologramLine line = lines.get(i);
                    TextDisplay display = displays.get(i);

                    if (display != null && display.isValid()) {
                        String processed = placeholderContext != null
                            ? Placeholders.process(line.getText(), player, placeholderContext)
                            : Placeholders.process(line.getText(), player);
                        Component component = ColorAPI.parse(processed);
                        display.text(component);
                    } else if (display != null && !display.isValid()) {
                        toRemove.add(playerId);
                    }
                }
            });

            toRemove.forEach(playerDisplays::remove);
        });
    }

    public void despawn() {
        if (!spawned.get()) {
            return;
        }

        if (perPlayer) {
            playerDisplays.values().forEach(displays ->
                    displays.forEach(this::removeEntity));
            playerDisplays.clear();
        } else {
            globalDisplays.forEach(this::removeEntity);
            globalDisplays.clear();
        }

        spawned.set(false);
    }

    private void removeEntity(TextDisplay entity) {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    public void teleport(Location newLocation) {
        boolean wasSpawned = spawned.get();
        if (wasSpawned) {
            despawn();
        }

        this.location.setWorld(newLocation.getWorld());
        this.location.setX(newLocation.getX());
        this.location.setY(newLocation.getY());
        this.location.setZ(newLocation.getZ());
        this.location.setYaw(newLocation.getYaw());
        this.location.setPitch(newLocation.getPitch());

        if (wasSpawned) {
            spawn();
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

        List<TextDisplay> displays = playerDisplays.remove(player.getUniqueId());
        if (displays != null) {
            Tasks.sync(() -> displays.forEach(this::removeEntity));
        }
    }

    public void cleanupPlayer(UUID playerId) {
        List<TextDisplay> displays = playerDisplays.remove(playerId);
        if (displays != null) {
            displays.forEach(this::removeEntity);
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

package net.exylia.commons.v2.hologram.model;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.hologram.exception.HologramException;
import net.exylia.commons.v2.hologram.renderer.LineRenderer;
import net.exylia.commons.v2.hologram.renderer.LineRendererFactory;
import net.exylia.commons.v2.hologram.visibility.VisibilityCondition;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A hologram: a vertically-stacked set of {@link HologramLine}s (each one
 * TEXT, ITEM or BLOCK) rendered as a group at a location.
 * <p>
 * Each line owns its own {@link LineRenderer}, which is packet-based (no
 * server-side entity — see {@code PacketLineRenderer}) whenever PacketEvents
 * is available, and transparently falls back to a real Bukkit Display entity
 * otherwise ({@code BukkitLineRenderer}). Callers never need to know which
 * one is in use.
 */
@Getter
public class Hologram {
    private static final double DEFAULT_LINE_GAP = 0.25;

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

    /** One renderer per line, in the same order as {@link #lines}. */
    private final List<LineRenderer> renderers = new ArrayList<>();
    /**
     * Players currently receiving spawn packets / seeing the real entity for
     * this hologram. Tracked regardless of {@link #perPlayer}: every viewer
     * needs an explicit spawn/destroy at the protocol level, the flag only
     * changes whether the rendered text content differs per player.
     */
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

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
        this.lines = new ArrayList<>(lines);
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
            createRenderers();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (canSee(player)) {
                    spawnForPlayerInternal(player);
                }
            }

            spawned.set(true);
        } catch (Exception e) {
            throw new HologramException.HologramSpawnException("Failed to spawn hologram " + id, e);
        }
    }

    private void createRenderers() {
        renderers.clear();
        List<Location> lineLocations = computeLineLocations();
        for (int i = 0; i < lines.size(); i++) {
            HologramLine line = lines.get(i);
            HologramProperties lineProps = line.getPropertiesOrDefault(properties);
            LineRenderer renderer = LineRendererFactory.create(plugin, lineLocations.get(i), line, lineProps);

            // A freshly-created renderer's text field is the line's raw,
            // unprocessed source (placeholders/color tags untouched). For
            // shared (non-perPlayer) TEXT lines the resolved content is the
            // same for every viewer, so resolve it once here rather than
            // leaving literal "{highlight}%foo%" visible to whoever spawns
            // it first, until the next scheduled updateAsync() tick.
            if (!perPlayer && line.isText()) {
                renderer.updateText(buildLineComponent(line, null));
            }

            renderers.add(renderer);
        }
    }

    /**
     * Stacks lines downward from the anchor location, each separated by
     * {@link #DEFAULT_LINE_GAP} blocks (matching the vertical text spacing
     * professional hologram plugins use), so multi-line holograms read
     * top-to-bottom in creation order.
     */
    private List<Location> computeLineLocations() {
        List<Location> result = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            result.add(location.clone().add(0, -i * DEFAULT_LINE_GAP, 0));
        }
        return result;
    }

    public void spawnForPlayer(Player player) {
        if (!canSee(player)) {
            return;
        }
        spawnForPlayerInternal(player);
    }

    private void spawnForPlayerInternal(Player player) {
        for (int i = 0; i < renderers.size(); i++) {
            LineRenderer renderer = renderers.get(i);
            HologramLine line = lines.get(i);

            if (perPlayer && line.isText()) {
                // Shared (non-perPlayer) TEXT renderers already carry
                // resolved content set in createRenderers()/updateAsync();
                // only per-player renderers need resolving right before
                // this specific viewer's first spawn packet, since their
                // text depends on that viewer's placeholder context.
                Component component = buildLineComponent(line, player);
                renderer.spawnFor(player);
                renderer.updateTextFor(player, component);
            } else {
                renderer.spawnFor(player);
            }
        }
        viewers.add(player.getUniqueId());
    }

    private Component buildLineComponent(HologramLine line, Player player) {
        if (!line.isText()) {
            return null;
        }
        String source = line.getText();
        if (source == null) {
            return line.getComponent();
        }
        String processed = placeholderContext != null
                ? Placeholders.process(source, player, placeholderContext)
                : (player != null ? Placeholders.process(source, player) : Placeholders.process(source));
        return ColorAPI.parse(processed + "<reset>");
    }

    public CompletableFuture<Void> updateAsync() {
        if (!spawned.get()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            for (int i = 0; i < lines.size(); i++) {
                HologramLine line = lines.get(i);
                if (!line.isText()) continue;
                LineRenderer renderer = renderers.get(i);

                if (perPlayer) {
                    for (UUID viewerId : viewers) {
                        Player player = Bukkit.getPlayer(viewerId);
                        if (player == null || !player.isOnline()) continue;
                        Component component = buildLineComponent(line, player);
                        Tasks.at(location, () -> renderer.updateTextFor(player, component));
                    }
                } else {
                    Component component = buildLineComponent(line, null);
                    Tasks.at(location, () -> renderer.updateText(component));
                }
            }
        });
    }

    public void despawn() {
        if (!spawned.get()) {
            return;
        }

        for (LineRenderer renderer : renderers) {
            renderer.despawnForAll();
        }
        renderers.clear();
        viewers.clear();

        spawned.set(false);
    }

    public void markUnspawned() {
        if (!spawned.getAndSet(false)) {
            return;
        }
        renderers.clear();
        viewers.clear();
    }

    public boolean isEntityValid() {
        return spawned.get() && !renderers.isEmpty() && renderers.get(0).isValid();
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

        List<Location> lineLocations = computeLineLocations();
        for (int i = 0; i < renderers.size() && i < lineLocations.size(); i++) {
            renderers.get(i).teleport(lineLocations.get(i));
        }
    }

    public void setLine(int index, String text) {
        if (index < 0 || index >= lines.size()) {
            throw new IndexOutOfBoundsException("Line index out of bounds: " + index);
        }
        HologramLine oldLine = lines.get(index);
        HologramLine newLine = new HologramLine(text, oldLine.getComponent(), oldLine.getProperties());
        lines.set(index, newLine);

        if (spawned.get() && index < renderers.size()) {
            Component component = buildLineComponent(newLine, null);
            renderers.get(index).updateText(component);
        }
    }

    public void addLine(String text) {
        lines.add(new HologramLine(text));
        if (spawned.get()) {
            despawn();
            spawn();
        }
    }

    public void addLine(HologramLine line) {
        lines.add(line);
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

    /**
     * Applies a background color override to the first TEXT line of this
     * hologram (or does nothing if there is none). Useful for dynamic
     * status colors (e.g. a payload cart's capture progress).
     */
    public void setBackgroundColor(Color color) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).isText() && i < renderers.size()) {
                HologramProperties updated = properties.withBackgroundColor(color);
                renderers.get(i).updateProperties(updated);
                return;
            }
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
        if (!viewers.contains(player.getUniqueId())) {
            Tasks.at(location, () -> spawnForPlayerInternal(player));
        }
    }

    public void hideFrom(Player player) {
        viewers.remove(player.getUniqueId());
        for (LineRenderer renderer : renderers) {
            if (renderer.isViewing(player)) {
                Tasks.at(location, () -> renderer.despawnFor(player));
            }
        }
    }

    public void cleanupPlayer(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        viewers.remove(playerId);
        if (player == null) return;
        for (LineRenderer renderer : renderers) {
            if (renderer.isViewing(player)) {
                Tasks.at(location, () -> renderer.despawnFor(player));
            }
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

    /**
     * Returns an immutable snapshot of the players currently viewing this
     * hologram.
     */
    public Set<UUID> getPlayerViewerIds() {
        return Collections.unmodifiableSet(viewers);
    }
}

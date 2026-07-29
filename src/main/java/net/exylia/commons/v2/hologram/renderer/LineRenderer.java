package net.exylia.commons.v2.hologram.renderer;

import net.exylia.commons.v2.hologram.model.HologramLine;
import net.exylia.commons.v2.hologram.model.HologramProperties;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Renders a single {@link HologramLine} to players. Implementations may be
 * packet-only (no server-side entity, see {@code PacketLineRenderer}) or
 * backed by a real Bukkit Display entity (see {@code BukkitLineRenderer}),
 * transparently chosen by {@link LineRendererFactory} depending on whether
 * PacketEvents is available.
 * <p>
 * A single renderer instance owns exactly one visual row at one location and
 * tracks which players currently have it spawned client-side.
 */
public interface LineRenderer {

    /**
     * Spawns this line for a single viewer (sends the relevant packets, or
     * shows the shared entity to that player).
     */
    void spawnFor(Player player);

    /**
     * Despawns this line for a single viewer.
     */
    void despawnFor(Player player);

    /**
     * Despawns this line for every player currently viewing it.
     */
    void despawnForAll();

    /**
     * Whether the given player currently has this line spawned.
     */
    boolean isViewing(Player player);

    /**
     * Updates the rendered text for every current viewer (only meaningful
     * for TEXT lines; a no-op for ITEM/BLOCK lines).
     */
    void updateText(Component text);

    /**
     * Updates the rendered text for a single viewer only, letting the same
     * hologram line show different content to different players
     * (per-player mode). Packet-based renderers send this as a unicast
     * metadata packet, so it works perfectly. The real-entity fallback
     * (used only when PacketEvents is unavailable) cannot diverge a single
     * shared entity's state per-viewer, so it degrades to updating the text
     * for everyone currently viewing this line.
     */
    default void updateTextFor(Player player, Component text) {
        updateText(text);
    }

    /**
     * Re-applies the given properties (billboard, scale, glow, etc) and
     * re-sends metadata to all current viewers.
     */
    void updateProperties(HologramProperties properties);

    /**
     * Teleports this line to a new location, re-sending position packets (or
     * moving the real entity) to all current viewers.
     */
    void teleport(Location location);

    /**
     * Rotates the line around the Y axis (used for spinning ITEM/BLOCK
     * lines, e.g. floating power-up icons). No-op for TEXT lines, whose
     * rotation is normally handled by the billboard mode instead.
     */
    default void rotateTo(float angleRadians) {
    }

    Location getLocation();

    boolean isValid();
}

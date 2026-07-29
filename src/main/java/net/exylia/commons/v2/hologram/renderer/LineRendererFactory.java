package net.exylia.commons.v2.hologram.renderer;

import net.exylia.commons.v2.hologram.model.HologramLine;
import net.exylia.commons.v2.hologram.model.HologramProperties;
import net.exylia.commons.v2.hologram.protocol.PacketHologramSupport;
import net.exylia.commons.v2.hologram.protocol.PacketLineRenderer;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Chooses the right {@link LineRenderer} implementation: packet-based
 * (preferred, no server-side entity) when PacketEvents is available on the
 * server, or a real Bukkit Display entity otherwise. This keeps every
 * consumer of the hologram API working regardless of whether their plugin
 * declares PacketEvents as a dependency.
 */
public final class LineRendererFactory {

    private LineRendererFactory() {
    }

    public static LineRenderer create(JavaPlugin plugin, Location location, HologramLine line, HologramProperties properties) {
        if (PacketHologramSupport.isAvailable()) {
            return new PacketLineRenderer(location, line, properties);
        }
        return new BukkitLineRenderer(plugin, location, line, properties);
    }
}

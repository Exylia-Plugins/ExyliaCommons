package net.exylia.commons.v2.hologram.protocol;

/**
 * Soft-dependency gate for PacketEvents.
 * <p>
 * The hologram system renders entirely through packets when PacketEvents is
 * present on the server (no real entity ever spawns, so nothing can be left
 * orphaned in the world after a crash/rough restart). When PacketEvents is
 * not installed, the system transparently falls back to real Bukkit Display
 * entities (see {@link net.exylia.commons.v2.hologram.renderer.EntityHologramRenderer}),
 * so every consuming plugin keeps working regardless of whether it declares
 * PacketEvents as a dependency.
 * <p>
 * No class outside this {@code protocol} package may import PacketEvents
 * types directly. Everything that touches the PacketEvents API must go
 * through classes in this package, and callers must check
 * {@link #isAvailable()} before using them, exactly like the existing
 * {@code PacketEventsSupport} guard used elsewhere in Commons.
 */
public final class PacketHologramSupport {

    private static final boolean CLASSES_PRESENT = computeClassesPresent();

    private PacketHologramSupport() {
    }

    private static boolean computeClassesPresent() {
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            Class.forName("io.github.retrooper.packetevents.util.SpigotReflectionUtil");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Whether packet-based rendering can be used right now. Evaluated live
     * (not cached beyond the classpath check) since the {@code packetevents}
     * plugin may enable after this library's classes are first touched,
     * depending on each consumer's plugin.yml load order.
     */
    public static boolean isAvailable() {
        if (!CLASSES_PRESENT) {
            return false;
        }
        try {
            return org.bukkit.Bukkit.getPluginManager().isPluginEnabled("packetevents");
        } catch (Throwable t) {
            return false;
        }
    }
}

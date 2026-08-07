package net.exylia.commons.v2.scoreboard.protocol;

import org.bukkit.Bukkit;

/**
 * Runtime gate for the PacketEvents implementation. Scoreboards deliberately
 * do not have a Bukkit/NMS fallback: this package is packet-only by design.
 */
public final class PacketScoreboardSupport {

    private static final String LOG_PREFIX = "[Scoreboard] [PacketScoreboardSupport] ";
    private static final boolean CLASSES_PRESENT = classesPresent();

    private PacketScoreboardSupport() {
    }

    private static boolean classesPresent() {
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            Class.forName("com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams");
            Class.forName("com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore");
            Bukkit.getLogger().info(LOG_PREFIX + "PacketEvents classes found on classpath.");
            return true;
        } catch (Throwable t) {
            Bukkit.getLogger().warning(LOG_PREFIX + "PacketEvents classes NOT found on classpath: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return false;
        }
    }

    public static boolean isAvailable() {
        if (!CLASSES_PRESENT) {
            Bukkit.getLogger().warning(LOG_PREFIX + "isAvailable()=false -> PacketEvents classes are not present (dependency missing at compile/runtime).");
            return false;
        }
        try {
            boolean pluginEnabled = Bukkit.getPluginManager().isPluginEnabled("packetevents");
            if (!pluginEnabled) {
                Bukkit.getLogger().warning(LOG_PREFIX + "isAvailable()=false -> the 'packetevents' plugin is NOT enabled on this server. Install/enable it and add it to depend: in plugin.yml.");
                return false;
            }
            Object api = com.github.retrooper.packetevents.PacketEvents.getAPI();
            if (api == null) {
                Bukkit.getLogger().warning(LOG_PREFIX + "isAvailable()=false -> PacketEvents.getAPI() returned null (PacketEvents not initialized yet?).");
                return false;
            }
            return true;
        } catch (Throwable t) {
            Bukkit.getLogger().warning(LOG_PREFIX + "isAvailable()=false -> exception checking PacketEvents availability: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return false;
        }
    }
}

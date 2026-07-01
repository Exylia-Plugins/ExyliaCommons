package net.exylia.commons.v2.chat.detect;

import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.entity.Player;

public final class DialogCapabilityDetector {

    private static final boolean PACKET_EVENTS_AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            available = true;
            DebugAPI.logLibDebug("[Dialog] PacketEvents available — dialog support enabled");
        } catch (ClassNotFoundException ignored) {
            DebugAPI.logLibDebug("[Dialog] PacketEvents NOT available — dialog support disabled");
        }
        PACKET_EVENTS_AVAILABLE = available;
    }

    private DialogCapabilityDetector() {}

    public static boolean isServerDialogCapable() {
        return PACKET_EVENTS_AVAILABLE;
    }

    public static boolean isClientDialogCapable(Player player) {
        if (!PACKET_EVENTS_AVAILABLE) {
            DebugAPI.logLibDebug("[Dialog] Client check skipped for " + player.getName() + " — PacketEvents unavailable");
            return false;
        }
        return DialogClientChecker.isCapable(player);
    }

    public static boolean canUseDialog(Player player) {
        return PACKET_EVENTS_AVAILABLE && isClientDialogCapable(player);
    }
}

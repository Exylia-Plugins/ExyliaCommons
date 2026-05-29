package net.exylia.commons.v2.chat.detect;

import net.exylia.commons.v2.chat.config.ChatInputDefaults;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.entity.Player;

public final class BedrockDetector {

    private static final boolean FLOODGATE_AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            available = true;
            DebugAPI.logLibDebug("[Bedrock] Floodgate available — native Bedrock detection enabled");
        } catch (ClassNotFoundException ignored) {
            DebugAPI.logLibDebug("[Bedrock] Floodgate NOT available — username prefix fallback active");
        }
        FLOODGATE_AVAILABLE = available;
    }

    private BedrockDetector() {}

    public static boolean isFloodgateAvailable() {
        return FLOODGATE_AVAILABLE;
    }

    public static boolean isBedrockPlayer(Player player) {
        java.util.UUID uuid = player.getUniqueId();
        if (FLOODGATE_AVAILABLE) {
            org.geysermc.floodgate.api.FloodgateApi api = org.geysermc.floodgate.api.FloodgateApi.getInstance();
            if (api.isFloodgatePlayer(uuid) || api.getPlayer(uuid) != null) {
                DebugAPI.logLibDebug("[Bedrock] Floodgate confirmed " + player.getName() + " as Bedrock");
                return true;
            }
            if (uuid.getMostSignificantBits() == 0L) {
                DebugAPI.logLibDebug("[Bedrock] Floodgate API missed " + player.getName() + " but UUID=" + uuid + " matches Floodgate format — treating as Bedrock (no BEDROCK handler)");
                return true;
            }
            DebugAPI.logLibDebug("[Bedrock] Floodgate: " + player.getName() + " is Java (uuid=" + uuid + ")");
            return false;
        }
        String prefix = ChatInputDefaults.ChatInput.Bedrock.PREFIX;
        boolean result = prefix != null && !prefix.isEmpty() && player.getName().startsWith(prefix);
        DebugAPI.logLibDebug("[Bedrock] Prefix check for " + player.getName() + " prefix=\"" + prefix + "\" = " + result);
        return result;
    }

    public static boolean isFloodgateConfirmed(Player player) {
        if (!FLOODGATE_AVAILABLE) return false;
        org.geysermc.floodgate.api.FloodgateApi api = org.geysermc.floodgate.api.FloodgateApi.getInstance();
        java.util.UUID uuid = player.getUniqueId();
        return api.isFloodgatePlayer(uuid) || api.getPlayer(uuid) != null;
    }
}

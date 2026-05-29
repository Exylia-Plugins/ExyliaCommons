package net.exylia.commons.v2.chat.detect;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.entity.Player;

final class DialogClientChecker {

    private DialogClientChecker() {}

    static boolean isCapable(Player player) {
        try {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user == null) {
                DebugAPI.logLibDebug("[Dialog] Client check: PacketEvents user null for " + player.getName());
                return false;
            }
            ClientVersion version = user.getClientVersion();
            if (version == null) {
                DebugAPI.logLibDebug("[Dialog] Client check: clientVersion null for " + player.getName());
                return false;
            }
            boolean capable = version.isNewerThanOrEquals(ClientVersion.V_1_21_6);
            DebugAPI.logLibDebug("[Dialog] Client " + player.getName() + " version=" + version + " capable=" + capable);
            return capable;
        } catch (Exception e) {
            DebugAPI.logLibDebug("[Dialog] Client version check error for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
}

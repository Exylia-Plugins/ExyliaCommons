package net.exylia.commons.v2.ui.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.kyori.adventure.text.Component;
import net.exylia.commons.v2.compat.InventoryViewCompat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class InventoryTitleUpdater {

    private InventoryTitleUpdater() {}

    public static void updateTitle(Player player, Inventory inventory, Component title) {
        try {
            Inventory topInventory = InventoryViewCompat.getTopInventory(player);
            if (topInventory != null && topInventory != inventory) {
                DebugAPI.logLibDebug(DebugCategory.UI, "Title update skipped for " + player.getName() + ": inventory mismatch");
                return;
            }

            int containerId = ContainerIdTracker.getContainerId(player.getUniqueId());
            if (containerId == -1) {
                DebugAPI.logLibDebug(DebugCategory.UI, "Title update skipped for " + player.getName() + ": containerId=-1 (PacketEvents listener not registered?)");
                return;
            }

            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user == null) {
                DebugAPI.logLibDebug(DebugCategory.UI, "Title update skipped for " + player.getName() + ": PacketEvents user is null");
                return;
            }

            int inventoryType = getInventoryTypeId(inventory);

            WrapperPlayServerOpenWindow packet = new WrapperPlayServerOpenWindow(
                    containerId,
                    inventoryType,
                    title
            );

            user.sendPacket(packet);
            player.updateInventory();
            DebugAPI.logLibDebug(DebugCategory.UI, "Title updated for " + player.getName() + " (containerId=" + containerId + ", type=" + inventoryType + ")");
        } catch (Exception e) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Title update failed for " + player.getName() + ": " + e.getMessage());
        }
    }

    private static int getInventoryTypeId(Inventory inventory) {
        int size = inventory.getSize();
        return switch (size) {
            case 9 -> 0;   // GENERIC_9X1
            case 18 -> 1;  // GENERIC_9X2
            case 27 -> 2;  // GENERIC_9X3
            case 36 -> 3;  // GENERIC_9X4
            case 45 -> 4;  // GENERIC_9X5
            case 54 -> 5;  // GENERIC_9X6
            default -> 2;  // Default to 9x3
        };
    }
}

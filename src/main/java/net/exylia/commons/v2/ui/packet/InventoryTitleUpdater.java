package net.exylia.commons.v2.ui.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;

public final class InventoryTitleUpdater {

    private InventoryTitleUpdater() {}

    public static void updateTitle(Player player, Inventory inventory, Component title) {
        try {
            Object view = player.getOpenInventory();
            Method getTopInventory = view.getClass().getMethod("getTopInventory");
            Inventory topInventory = (Inventory) getTopInventory.invoke(view);
            if (topInventory != inventory) {
                return;
            }

            int containerId = ContainerIdTracker.getContainerId(player.getUniqueId());
            if (containerId == -1) {
                return;
            }

            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user == null) {
                return;
            }

            int inventoryType = getInventoryTypeId(inventory);

            WrapperPlayServerOpenWindow packet = new WrapperPlayServerOpenWindow(
                    containerId,
                    inventoryType,
                    title
            );

            user.sendPacket(packet);
        } catch (Exception ignored) {
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

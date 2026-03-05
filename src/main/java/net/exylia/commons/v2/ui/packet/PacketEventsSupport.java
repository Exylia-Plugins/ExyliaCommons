package net.exylia.commons.v2.ui.packet;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class PacketEventsSupport {

    private static final boolean AVAILABLE;

    static {
        boolean found;
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            found = true;
        } catch (ClassNotFoundException e) {
            found = false;
        }
        AVAILABLE = found;
    }

    private PacketEventsSupport() {}

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static void updateTitle(Player player, Inventory inventory, Component title) {
        if (!AVAILABLE) {
            return;
        }
        InventoryTitleUpdater.updateTitle(player, inventory, title);
    }
}

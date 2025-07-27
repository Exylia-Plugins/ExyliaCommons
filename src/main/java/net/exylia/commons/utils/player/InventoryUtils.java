package net.exylia.commons.utils.player;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public class InventoryUtils {
    public static void simulateItemDrops(Player player) {
        Location deathLocation = player.getLocation();
        PlayerInventory inventory = player.getInventory();

        ItemStack[] allContents = inventory.getContents();
        for (ItemStack item : allContents) {
            if (item != null && item.getType() != Material.AIR) {
                Location dropLocation = deathLocation.clone().add(
                        (Math.random() - 0.5) * 1,
                        0.5,
                        (Math.random() - 0.5) * 1
                );
                deathLocation.getWorld().dropItemNaturally(dropLocation, item);
            }
        }

        player.getInventory().clear();
    }
}

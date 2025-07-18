package net.exylia.commons.utils.player;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryUtils {
    /**
     * Simula manualmente la caída de items cuando un jugador muere
     */
    public static void simulateItemDrops(Player player) {
        Location deathLocation = player.getLocation();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                Location dropLocation = deathLocation.clone().add(
                        (Math.random() - 0.5) * 2, // -1 a +1 en X
                        0.5, // Altura fija
                        (Math.random() - 0.5) * 2  // -1 a +1 en Z
                );
                deathLocation.getWorld().dropItemNaturally(dropLocation, item);
            }
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack armorPiece : armor) {
            if (armorPiece != null && armorPiece.getType() != Material.AIR) {
                Location dropLocation = deathLocation.clone().add(
                        (Math.random() - 0.5) * 2,
                        0.5,
                        (Math.random() - 0.5) * 2
                );
                deathLocation.getWorld().dropItemNaturally(dropLocation, armorPiece);
            }
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && offHand.getType() != Material.AIR) {
            Location dropLocation = deathLocation.clone().add(
                    (Math.random() - 0.5) * 2,
                    0.5,
                    (Math.random() - 0.5) * 2
            );

            deathLocation.getWorld().dropItemNaturally(dropLocation, offHand);
        }
    }
}

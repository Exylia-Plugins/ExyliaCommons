package net.exylia.commons.utils.player;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class TeleportUtils {

    public static void teleportToGround(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();

        if (world == null) return;

        Block block = loc.getBlock();
        Block blockBelow = loc.clone().add(0, -0.1, 0).getBlock();

        if (block.getType() == Material.AIR && blockBelow.getType().isSolid()) {
            return;
        }

        Location groundLoc = loc.clone();

        for (int y = loc.getBlockY(); y > Math.max(world.getMinHeight(), loc.getBlockY() - 100); y--) {
            groundLoc.setY(y);

            if (groundLoc.getBlock().getType() == Material.AIR &&
                    groundLoc.clone().add(0, -1, 0).getBlock().getType().isSolid()) {

                groundLoc.setY(y + 0.1);
                groundLoc.setPitch(loc.getPitch());
                groundLoc.setYaw(loc.getYaw());
                player.teleport(groundLoc);
                return;
            }
        }
    }
}

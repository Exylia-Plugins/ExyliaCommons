package net.exylia.commons.selection.utils;

import net.exylia.commons.selection.model.Selection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SelectionUtils {

    public static List<Block> getBlocks(Selection selection) {
        List<Block> blocks = new ArrayList<>();

        if (!selection.isComplete()) {
            return blocks;
        }

        Location min = selection.getMinimumPoint();
        Location max = selection.getMaximumPoint();

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    blocks.add(min.getWorld().getBlockAt(x, y, z));
                }
            }
        }

        return blocks;
    }

    public static List<Block> getBlocksByType(Selection selection, Material material) {
        List<Block> blocks = new ArrayList<>();

        for (Block block : getBlocks(selection)) {
            if (block.getType() == material) {
                blocks.add(block);
            }
        }

        return blocks;
    }

    public static int countBlocksByType(Selection selection, Material material) {
        return getBlocksByType(selection, material).size();
    }

    public static boolean canModify(Player player, Selection selection) {
        if (!selection.isComplete()) {
            return false;
        }

        if (!player.hasPermission("exylia.selection.modify")) {
            return false;
        }

        long volume = selection.getVolume();
        return volume <= getMaxVolume(player);
    }

    public static long getMaxVolume(Player player) {
        if (player.hasPermission("exylia.selection.unlimited")) {
            return Long.MAX_VALUE;
        }

        if (player.hasPermission("exylia.selection.large")) {
            return 1000000;  
        }

        if (player.hasPermission("exylia.selection.medium")) {
            return 100000;  
        }

        return 10000;  
    }

    public static String formatSelectionInfo(Selection selection) {
        if (!selection.isComplete()) {
            return "Selección incompleta";
        }

        Location min = selection.getMinimumPoint();
        Location max = selection.getMaximumPoint();

        return String.format(
                "Selección [%s] - Desde: %d,%d,%d Hasta: %d,%d,%d (Volumen: %d bloques)",
                selection.getSelectionId(),
                min.getBlockX(), min.getBlockY(), min.getBlockZ(),
                max.getBlockX(), max.getBlockY(), max.getBlockZ(),
                selection.getVolume()
        );
    }

    public static boolean intersects(Selection selection1, Selection selection2) {
        if (!selection1.isComplete() || !selection2.isComplete()) {
            return false;
        }

        if (!selection1.getPos1().getWorld().equals(selection2.getPos1().getWorld())) {
            return false;
        }

        Location min1 = selection1.getMinimumPoint();
        Location max1 = selection1.getMaximumPoint();
        Location min2 = selection2.getMinimumPoint();
        Location max2 = selection2.getMaximumPoint();

        return min1.getBlockX() <= max2.getBlockX() && max1.getBlockX() >= min2.getBlockX() &&
                min1.getBlockY() <= max2.getBlockY() && max1.getBlockY() >= min2.getBlockY() &&
                min1.getBlockZ() <= max2.getBlockZ() && max1.getBlockZ() >= min2.getBlockZ();
    }

    public static long getIntersectionVolume(Selection selection1, Selection selection2) {
        if (!intersects(selection1, selection2)) {
            return 0;
        }

        Location min1 = selection1.getMinimumPoint();
        Location max1 = selection1.getMaximumPoint();
        Location min2 = selection2.getMinimumPoint();
        Location max2 = selection2.getMaximumPoint();

        int minX = Math.max(min1.getBlockX(), min2.getBlockX());
        int maxX = Math.min(max1.getBlockX(), max2.getBlockX());
        int minY = Math.max(min1.getBlockY(), min2.getBlockY());
        int maxY = Math.min(max1.getBlockY(), max2.getBlockY());
        int minZ = Math.max(min1.getBlockZ(), min2.getBlockZ());
        int maxZ = Math.min(max1.getBlockZ(), max2.getBlockZ());

        return (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public static void expandSelection(Selection selection, int amount) {
        if (!selection.isComplete()) {
            return;
        }

        Location min = selection.getMinimumPoint();
        Location max = selection.getMaximumPoint();

        min.add(-amount, -amount, -amount);
        max.add(amount, amount, amount);

        selection.setPos1(min);
        selection.setPos2(max);
    }

    public static void contractSelection(Selection selection, int amount) {
        if (!selection.isComplete()) {
            return;
        }

        Location min = selection.getMinimumPoint();
        Location max = selection.getMaximumPoint();

        min.add(amount, amount, amount);
        max.add(-amount, -amount, -amount);

        if (min.getBlockX() > max.getBlockX() ||
                min.getBlockY() > max.getBlockY() ||
                min.getBlockZ() > max.getBlockZ()) {
            return;  
        }

        selection.setPos1(min);
        selection.setPos2(max);
    }
}

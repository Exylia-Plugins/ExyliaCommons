package net.exylia.commons.v2.items.utils;

import net.exylia.commons.ui.items.provider.CustomItemManager;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.skull.SkullManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import static net.exylia.commons.utils.skull.SkullUtils.*;

public class ItemStackUtils {

    public static ItemStack createFromString(String materialString) {
        if (materialString == null || materialString.isEmpty()) {
            return new ItemStack(Material.STONE);
        }

        if (materialString.startsWith("headbase-")) {
            String base64 = materialString.substring(9);
            return createSkullFromTexture(base64);
        }

        if (materialString.startsWith("headurl-")) {
            String url = materialString.substring(8);
            return createSkullFromUrl(url);
        }

        if (materialString.startsWith("playerhead-")) {
            String playerName = materialString.substring(11);
            ItemStack cachedSkull = createPlayerSkull(playerName);
            if (isRealPlayerSkull(cachedSkull, playerName)) {
                return cachedSkull;
            }
            return cachedSkull;
        }

        if (materialString.contains(":")) {
            DebugUtils.logInternalDebug("ItemStackUtils: Attempting to load custom item: " + materialString);
            ItemStack customItem = CustomItemManager.getInstance().getCustomItem(materialString);
            if (customItem != null) {
                DebugUtils.logInternalDebug("ItemStackUtils: Successfully loaded custom item: " + materialString);
                return customItem.clone();
            }
            DebugUtils.logInternalWarn("ItemStackUtils: Custom item not found: " + materialString + ", falling back to STONE");
        }

        try {
            Material material = Material.valueOf(materialString.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            return new ItemStack(Material.STONE);
        }
    }

    private static boolean isRealPlayerSkull(ItemStack skull, String expectedPlayerName) {
        if (skull.getType() != Material.PLAYER_HEAD) {
            return false;
        }

        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) {
            return false;
        }

        try {
            return isPlayerSkullCached(expectedPlayerName);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isPlayerSkullCached(String playerName) {
        try {
            return SkullManager.getInstance().isPlayerCached(playerName);
        } catch (Exception e) {
            return false;
        }
    }

    public static void updateMaterial(ItemStack itemStack, String materialString) {
        ItemStack newStack = createFromString(materialString);
        itemStack.setType(newStack.getType());
        itemStack.setData(newStack.getData());
    }

    public static boolean isValidMaterial(String materialString) {
        if (materialString == null || materialString.isEmpty()) {
            return false;
        }

        if (materialString.startsWith("headbase-") || materialString.startsWith("headurl-") ||
            materialString.startsWith("playerhead-")) {
            return true;
        }

        if (materialString.contains(":")) {
            return CustomItemManager.getInstance().getCustomItem(materialString) != null;
        }

        try {
            Material.valueOf(materialString.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

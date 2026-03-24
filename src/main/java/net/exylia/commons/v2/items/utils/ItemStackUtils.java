package net.exylia.commons.v2.items.utils;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.items.integration.CustomItemManager;
import net.exylia.commons.v2.items.skull.SkullParser;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.skull.api.SkullAPI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ItemStackUtils {

    public static ItemStack createFromString(String materialString) {
        if (materialString == null || materialString.isEmpty()) {
            DebugAPI.logLibWarn("ItemStackUtils: Material string is null or empty, falling back to STONE");
            return new ItemStack(Material.STONE);
        }

        if (SkullParser.isSkullString(materialString)) {
            return SkullParser.parse(materialString, null, PlaceholderContext.create());
        }

        if (materialString.startsWith("headbase-")) {
            String base64 = materialString.substring(9);
            return SkullAPI.fromTexture(base64);
        }

        if (materialString.startsWith("headurl-")) {
            String url = materialString.substring(8);
            return SkullAPI.fromTextureURL(url);
        }

        if (materialString.startsWith("playerhead-")) {
            String playerName = materialString.substring(11);
            return SkullAPI.fromPlayer(playerName);
        }

        if (materialString.contains(":")) {
            DebugAPI.logLibDebug("ItemStackUtils: Attempting to load custom item: " + materialString);
            ItemStack customItem = CustomItemManager.getInstance().getCustomItem(materialString);
            if (customItem != null) {
                DebugAPI.logLibDebug("ItemStackUtils: Successfully loaded custom item: " + materialString);
                return customItem.clone();
            }
            DebugAPI.logLibWarn("ItemStackUtils: Custom item not found: " + materialString + ", falling back to STONE");
        }

        try {
            Material material = Material.valueOf(materialString.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            DebugAPI.logLibWarn("ItemStackUtils: Unknown material '" + materialString + "', falling back to STONE");
            return new ItemStack(Material.STONE);
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

        if (SkullParser.isSkullString(materialString)) {
            return true;
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

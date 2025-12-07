package net.exylia.commons.v2.items.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class NBTManager {

    private static JavaPlugin plugin;

    public static void setPlugin(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    public static void applyCustomNBT(ItemStack itemStack, Map<String, String> customNBT) {
        if (itemStack == null || customNBT == null || customNBT.isEmpty()) {
            return;
        }

        if (plugin == null) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();

        for (Map.Entry<String, String> entry : customNBT.entrySet()) {
            setNBTValue(container, entry.getKey(), entry.getValue());
        }

        itemStack.setItemMeta(meta);
    }

    private static void setNBTValue(PersistentDataContainer container, String key, String value) {
        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            container.set(namespacedKey, PersistentDataType.STRING, value);
        } catch (Exception ignored) {
        }
    }

    public static String getNBTValue(ItemStack itemStack, String key) {
        if (itemStack == null || plugin == null) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean hasNBT(ItemStack itemStack, String key) {
        return getNBTValue(itemStack, key) != null;
    }
}

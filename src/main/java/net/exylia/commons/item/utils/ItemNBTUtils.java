package net.exylia.commons.item.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemNBTUtils {

    public static String getNBTString(ItemStack item, JavaPlugin plugin, String key) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
    }

    public static void setNBTString(ItemStack item, JavaPlugin plugin, String key, String value) {
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        if (value != null) {
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
        } else {
            meta.getPersistentDataContainer().remove(namespacedKey);
        }

        item.setItemMeta(meta);
    }

    public static int getNBTInt(ItemStack item, JavaPlugin plugin, String key, int defaultValue) {
        if (item == null || !item.hasItemMeta()) return defaultValue;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return defaultValue;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().getOrDefault(namespacedKey, PersistentDataType.INTEGER, defaultValue);
    }

    public static void setNBTInt(ItemStack item, JavaPlugin plugin, String key, int value) {
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
    }

    public static long getNBTLong(ItemStack item, JavaPlugin plugin, String key, long defaultValue) {
        if (item == null || !item.hasItemMeta()) return defaultValue;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return defaultValue;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().getOrDefault(namespacedKey, PersistentDataType.LONG, defaultValue);
    }

    public static void setNBTLong(ItemStack item, JavaPlugin plugin, String key, long value) {
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.LONG, value);
        item.setItemMeta(meta);
    }

    public static boolean hasNBTValue(ItemStack item, JavaPlugin plugin, String key, PersistentDataType<?, ?> type) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().has(namespacedKey, type);
    }

    public static void removeNBTValue(ItemStack item, JavaPlugin plugin, String key) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        meta.getPersistentDataContainer().remove(namespacedKey);
        item.setItemMeta(meta);
    }
}

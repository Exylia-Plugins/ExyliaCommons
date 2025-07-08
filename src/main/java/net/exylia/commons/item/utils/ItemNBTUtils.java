package net.exylia.commons.item.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utilidades para manejar datos NBT en items
 */
public class ItemNBTUtils {

    /**
     * Obtiene un valor string desde NBT
     */
    public static String getNBTString(ItemStack item, JavaPlugin plugin, String key) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
    }

    /**
     * Establece un valor string en NBT
     */
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

    /**
     * Obtiene un valor int desde NBT
     */
    public static int getNBTInt(ItemStack item, JavaPlugin plugin, String key, int defaultValue) {
        if (item == null || !item.hasItemMeta()) return defaultValue;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return defaultValue;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().getOrDefault(namespacedKey, PersistentDataType.INTEGER, defaultValue);
    }

    /**
     * Establece un valor int en NBT
     */
    public static void setNBTInt(ItemStack item, JavaPlugin plugin, String key, int value) {
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
    }

    /**
     * Obtiene un valor long desde NBT
     */
    public static long getNBTLong(ItemStack item, JavaPlugin plugin, String key, long defaultValue) {
        if (item == null || !item.hasItemMeta()) return defaultValue;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return defaultValue;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().getOrDefault(namespacedKey, PersistentDataType.LONG, defaultValue);
    }

    /**
     * Establece un valor long en NBT
     */
    public static void setNBTLong(ItemStack item, JavaPlugin plugin, String key, long value) {
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.LONG, value);
        item.setItemMeta(meta);
    }

    /**
     * Verifica si existe un valor NBT
     */
    public static boolean hasNBTValue(ItemStack item, JavaPlugin plugin, String key, PersistentDataType<?, ?> type) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().has(namespacedKey, type);
    }

    /**
     * Remueve un valor NBT
     */
    public static void removeNBTValue(ItemStack item, JavaPlugin plugin, String key) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        meta.getPersistentDataContainer().remove(namespacedKey);
        item.setItemMeta(meta);
    }
}
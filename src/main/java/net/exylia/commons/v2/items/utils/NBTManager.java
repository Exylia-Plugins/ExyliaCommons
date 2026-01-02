package net.exylia.commons.v2.items.utils;

import lombok.Setter;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class NBTManager {

    @Setter
    private static JavaPlugin plugin;

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

            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                container.set(namespacedKey, PersistentDataType.BOOLEAN, Boolean.parseBoolean(value));
                return;
            }

            try {
                if (!value.contains(".")) {
                    int intValue = Integer.parseInt(value);
                    container.set(namespacedKey, PersistentDataType.INTEGER, intValue);
                    return;
                }
            } catch (NumberFormatException ignored) {
            }

            try {
                double doubleValue = Double.parseDouble(value);
                container.set(namespacedKey, PersistentDataType.DOUBLE, doubleValue);
                return;
            } catch (NumberFormatException ignored) {
            }

            container.set(namespacedKey, PersistentDataType.STRING, value);
        } catch (Exception ignored) {
        }
    }

    public static String getString(ItemStack itemStack, String key) {
        return getString(itemStack, key, null);
    }

    public static String getString(ItemStack itemStack, String key, String defaultValue) {
        if (itemStack == null || plugin == null) {
            return defaultValue;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return defaultValue;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            String value = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Integer getInt(ItemStack itemStack, String key) {
        return getInt(itemStack, key, null);
    }

    public static Integer getInt(ItemStack itemStack, String key, Integer defaultValue) {
        if (itemStack == null || plugin == null) {
            return defaultValue;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return defaultValue;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            Integer value = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.INTEGER);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Double getDouble(ItemStack itemStack, String key) {
        return getDouble(itemStack, key, null);
    }

    public static Double getDouble(ItemStack itemStack, String key, Double defaultValue) {
        if (itemStack == null || plugin == null) {
            return defaultValue;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return defaultValue;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            Double value = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.DOUBLE);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Boolean getBoolean(ItemStack itemStack, String key) {
        return getBoolean(itemStack, key, null);
    }

    public static Boolean getBoolean(ItemStack itemStack, String key, Boolean defaultValue) {
        if (itemStack == null || plugin == null) {
            return defaultValue;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return defaultValue;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            Byte value = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.BYTE);
            if (value != null) {
                return value == 1;
            }
            Boolean boolValue = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.BOOLEAN);
            return boolValue != null ? boolValue : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Long getLong(ItemStack itemStack, String key) {
        return getLong(itemStack, key, null);
    }

    public static Long getLong(ItemStack itemStack, String key, Long defaultValue) {
        if (itemStack == null || plugin == null) {
            return defaultValue;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return defaultValue;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            Long value = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.LONG);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Float getFloat(ItemStack itemStack, String key) {
        return getFloat(itemStack, key, null);
    }

    public static Float getFloat(ItemStack itemStack, String key, Float defaultValue) {
        if (itemStack == null || plugin == null) {
            return defaultValue;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return defaultValue;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            Float value = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.FLOAT);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Deprecated
    public static String getNBTValue(ItemStack itemStack, String key) {
        return getString(itemStack, key);
    }

    public static boolean hasNBT(ItemStack itemStack, String key) {
        if (itemStack == null || plugin == null) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            return meta.getPersistentDataContainer().has(namespacedKey);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setString(ItemStack itemStack, String key, String value) {
        if (itemStack == null || plugin == null || key == null || value == null) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
            itemStack.setItemMeta(meta);
        } catch (Exception ignored) {
        }
    }

    public static void setInt(ItemStack itemStack, String key, int value) {
        if (itemStack == null || plugin == null || key == null) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.INTEGER, value);
            itemStack.setItemMeta(meta);
        } catch (Exception ignored) {
        }
    }

    public static void setDouble(ItemStack itemStack, String key, double value) {
        if (itemStack == null || plugin == null || key == null) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.DOUBLE, value);
            itemStack.setItemMeta(meta);
        } catch (Exception ignored) {
        }
    }

    public static void setBoolean(ItemStack itemStack, String key, boolean value) {
        if (itemStack == null || plugin == null || key == null) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.BOOLEAN, value);
            itemStack.setItemMeta(meta);
        } catch (Exception ignored) {
        }
    }

    public static void setLong(ItemStack itemStack, String key, long value) {
        if (itemStack == null || plugin == null || key == null) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.LONG, value);
            itemStack.setItemMeta(meta);
        } catch (Exception ignored) {
        }
    }

    public static void setFloat(ItemStack itemStack, String key, float value) {
        if (itemStack == null || plugin == null || key == null) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.FLOAT, value);
            itemStack.setItemMeta(meta);
        } catch (Exception ignored) {
        }
    }

    public static void remove(ItemStack itemStack, String key) {
        if (itemStack == null || plugin == null || key == null) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        try {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().remove(namespacedKey);
            itemStack.setItemMeta(meta);
        } catch (Exception ignored) {
        }
    }
}

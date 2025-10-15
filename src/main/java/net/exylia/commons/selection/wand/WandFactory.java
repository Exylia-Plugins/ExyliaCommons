package net.exylia.commons.selection.wand;

import net.exylia.commons.selection.model.WandConfig;
import net.exylia.commons.selection.model.SelectionType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class WandFactory {
    private static final String WAND_KEY = "exylia_selection_wand";
    private static final String SELECTION_ID_KEY = "exylia_selection_id";
    private static final String SELECTION_TYPE_KEY = "exylia_selection_type";

    private final JavaPlugin plugin;
    private final NamespacedKey wandKey;
    private final NamespacedKey selectionIdKey;
    private final NamespacedKey selectionTypeKey;

    public WandFactory(JavaPlugin plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, WAND_KEY);
        this.selectionIdKey = new NamespacedKey(plugin, SELECTION_ID_KEY);
        this.selectionTypeKey = new NamespacedKey(plugin, SELECTION_TYPE_KEY);
    }

    public ItemStack createWand() {
        return createWand(new WandConfig());
    }

    public ItemStack createWand(String selectionId) {
        return createWand(new WandConfig().selectionId(selectionId));
    }

    public ItemStack createWand(WandConfig config) {
        ItemStack wand = new ItemStack(config.getMaterial());
        ItemMeta meta = wand.getItemMeta();

        if (meta != null) {
             
            meta.displayName(config.getDisplayName());
            meta.lore(config.getLore());

            if (config.isUnbreakable()) {
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            }

            if (config.isEnchanted()) {
                meta.addEnchant(Enchantment.LUCK, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (config.getCustomModelData() > 0) {
                meta.setCustomModelData(config.getCustomModelData());
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(selectionIdKey, PersistentDataType.STRING, config.getSelectionId());
            meta.getPersistentDataContainer().set(selectionTypeKey, PersistentDataType.STRING, config.getSelectionType().name());

            wand.setItemMeta(meta);
        }

        return wand;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public String getSelectionId(ItemStack wand) {
        if (!isWand(wand)) {
            return null;
        }

        ItemMeta meta = wand.getItemMeta();
        return meta.getPersistentDataContainer().get(selectionIdKey, PersistentDataType.STRING);
    }

    public SelectionType getSelectionType(ItemStack wand) {
        if (!isWand(wand)) {
            return null;
        }

        ItemMeta meta = wand.getItemMeta();
        String typeName = meta.getPersistentDataContainer().get(selectionTypeKey, PersistentDataType.STRING);

        try {
            return SelectionType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return SelectionType.CUBOID;
        }
    }
}

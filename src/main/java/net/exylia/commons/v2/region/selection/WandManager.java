package net.exylia.commons.v2.region.selection;

import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class WandManager {
    private static final String WAND_KEY = "exylia_region_wand_v2";
    private static final String SELECTION_ID_KEY = "exylia_region_selection_id";

    private final JavaPlugin plugin;
    private final NamespacedKey wandKey;
    private final NamespacedKey selectionIdKey;

    public WandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, WAND_KEY);
        this.selectionIdKey = new NamespacedKey(plugin, SELECTION_ID_KEY);
    }

    public ItemStack createWand() {
        return createWand("region-selection");
    }

    public ItemStack createWand(String selectionId) {
        return createWand(selectionId, Material.GOLDEN_AXE);
    }

    public ItemStack createWand(String selectionId, Material material) {
        ItemStack wand = new ItemStack(material);
        ItemMeta meta = wand.getItemMeta();

        if (meta != null) {
            meta.displayName(ColorAPI.parse("{primary}&l✦ REGION SELECTOR V2 ✦"));
            meta.lore(List.of(
                    ColorAPI.parse("{success}• {letters}Position 1 {info}→ {success}Left-click"),
                    ColorAPI.parse("{error}• {letters}Position 2 {info}→ {error}Right-click"),
                    ColorAPI.parse(""),
                    ColorAPI.parse("{info}• {letters}Create regions with /region create")
            ));

            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(selectionIdKey, PersistentDataType.STRING, selectionId);

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
}

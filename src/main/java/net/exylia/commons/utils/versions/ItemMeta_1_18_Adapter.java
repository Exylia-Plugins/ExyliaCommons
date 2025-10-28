package net.exylia.commons.utils.versions;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemMeta_1_18_Adapter implements ItemMetaAdapter {

    @Override
    public void setDisplayName(ItemMeta meta, Component name) {
        meta.displayName(name);
    }

    @Override
    public void setLore(ItemMeta meta, List<Component> lore) {
        meta.lore(lore);
    }

    @Override
    public Component getDisplayName(ItemMeta meta) {
        return meta.displayName();
    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        return meta.lore();
    }

    @Override
    public void setItemModel(ItemMeta meta, NamespacedKey itemModel) {
        try {
            meta.getClass().getMethod("setItemModel", NamespacedKey.class).invoke(meta, itemModel);
        } catch (Exception ignored) {
        }
    }
}

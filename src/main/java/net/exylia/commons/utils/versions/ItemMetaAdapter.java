package net.exylia.commons.utils.versions;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public interface ItemMetaAdapter {
     
    void setDisplayName(ItemMeta meta, Component name);

    void setLore(ItemMeta meta, List<Component> lore);

    Component getDisplayName(ItemMeta meta);

    List<Component> getLore(ItemMeta meta);
}

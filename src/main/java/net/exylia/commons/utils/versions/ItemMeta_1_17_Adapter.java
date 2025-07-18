package net.exylia.commons.utils.versions;

import net.exylia.commons.utils.OldColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemMeta_1_17_Adapter implements ItemMetaAdapter {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacySection();

    @Override
    public void setDisplayName(ItemMeta meta, Component name) {
        String miniMessageString = MiniMessage.miniMessage().serialize(name);
        meta.setDisplayName(OldColorUtils.parseOld(miniMessageString));
    }

    @Override
    public void setLore(ItemMeta meta, List<Component> lore) {
        List<String> legacyLore = new ArrayList<>();
        for (Component line : lore) {
            String miniMessageString = MiniMessage.miniMessage().serialize(line);
            legacyLore.add(OldColorUtils.parseOld(miniMessageString));
        }
        meta.setLore(legacyLore);
    }

    @Override
    public Component getDisplayName(ItemMeta meta) {
        if (!meta.hasDisplayName()) {
            return null;
        }

        String displayName = meta.getDisplayName();
        return LEGACY_SERIALIZER.deserialize(displayName);
    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        if (!meta.hasLore()) {
            return null;
        }

        List<String> legacyLore = meta.getLore();
        List<Component> componentLore = new ArrayList<>();

        for (String line : legacyLore) {
            componentLore.add(LEGACY_SERIALIZER.deserialize(line));
        }

        return componentLore;
    }
}
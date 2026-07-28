package net.exylia.commons.v2.ui.selector.impl.loot.menu;

import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.items.snapshot.ItemSnapshot;
import net.exylia.commons.v2.loot.model.LootEntry;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;

public final class LootEditMenu {

    private LootEditMenu() {}

    public static void open(Player player, LootEntry entry) {
        ItemData globalFiller = ItemData.builder()
                .rawMaterial("GRAY_STAINED_GLASS_PANE")
                .rawDisplayName(" ")
                .hideTooltip(true)
                .build();

        ItemData displayItem = ItemData.builder()
                .rawMaterial(resolveItemMaterial(entry))
                .rawDisplayName("{primary}&l" + entry.getDisplayName())
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}ID {letters_black}» {muted}" + entry.getId().substring(0, 8) + "..."
                ))
                .slotConfig(SlotConfig.single(4))
                .build();

        ItemData itemButton = ItemData.builder()
                .rawMaterial("ITEM_FRAME")
                .rawDisplayName("{info}&lITEM 🎁")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}The item given by this",
                        " {letters_black}▎ {letters}loot entry.",
                        "",
                        "{warning}➥ Click to change item"
                ))
                .slotConfig(SlotConfig.single(20))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:loot_set_item " + entry.getId())
                        .build()))
                .build();

        ItemData minButton = ItemData.builder()
                .rawMaterial("REPEATER")
                .rawDisplayName("{highlight}&lMIN AMOUNT")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Current {letters_black}» {highlight}" + entry.getMinAmount(),
                        "",
                        "{success}● {letters}Left Click {letters_black}» +1",
                        "{error}● {letters}Right Click {letters_black}» -1"
                ))
                .slotConfig(SlotConfig.single(21))
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.LEFT)
                                .action("commons:loot_adjust_min " + entry.getId() + " 1")
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:loot_adjust_min " + entry.getId() + " -1")
                                .build()
                ))
                .build();

        ItemData maxButton = ItemData.builder()
                .rawMaterial("REPEATER")
                .rawDisplayName("{highlight}&lMAX AMOUNT")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Current {letters_black}» {highlight}" + entry.getMaxAmount(),
                        "",
                        "{success}● {letters}Left Click {letters_black}» +1",
                        "{error}● {letters}Right Click {letters_black}» -1"
                ))
                .slotConfig(SlotConfig.single(22))
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.LEFT)
                                .action("commons:loot_adjust_max " + entry.getId() + " 1")
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:loot_adjust_max " + entry.getId() + " -1")
                                .build()
                ))
                .build();

        ItemData weightButton = ItemData.builder()
                .rawMaterial("SUNFLOWER")
                .rawDisplayName("{highlight}&lWEIGHT 🎲")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Probability weight of this",
                        " {letters_black}▎ {letters}entry relative to the others.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {highlight}" + formatWeight(entry.getWeight()),
                        "",
                        "{success}● {letters}Left Click {letters_black}» +5",
                        "{error}● {letters}Right Click {letters_black}» -5"
                ))
                .slotConfig(SlotConfig.single(23))
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.LEFT)
                                .action("commons:loot_adjust_weight " + entry.getId() + " 5")
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:loot_adjust_weight " + entry.getId() + " -5")
                                .build()
                ))
                .build();

        ItemData tierButton = ItemData.builder()
                .rawMaterial("NAME_TAG")
                .rawDisplayName("{secondary_light}&lTIER")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Optional rarity label for",
                        " {letters_black}▎ {letters}this loot entry.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + (entry.getTier() != null && !entry.getTier().isBlank() ? entry.getTier() : "None"),
                        "",
                        "{success}● {letters}Left Click {letters_black}» Change",
                        "{error}● {letters}Right Click {letters_black}» Clear"
                ))
                .slotConfig(SlotConfig.single(24))
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.LEFT)
                                .action("commons:loot_set_tier " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:loot_clear_tier " + entry.getId())
                                .build()
                ))
                .build();

        ItemData deleteButton = ItemData.builder()
                .rawMaterial("TNT")
                .rawDisplayName("{error}&lDELETE ENTRY")
                .rawLore(List.of(
                        " {letters_black}▎ {letters}Permanently remove this",
                        " {letters_black}▎ {letters}loot entry from the table.",
                        "",
                        "{warning}➥ Click to delete"
                ))
                .slotConfig(SlotConfig.single(31))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:loot_delete " + entry.getId())
                        .build()))
                .build();

        ItemData backButton = ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("{secondary}&l« BACK")
                .rawLore(List.of(
                        " {letters_black}▎ {letters}Return to the loot list.",
                        "",
                        "{warning}➥ Click to go back"
                ))
                .slotConfig(SlotConfig.single(36))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:loot_list")
                        .build()))
                .build();

        LinkedHashMap<String, ItemData> items = new LinkedHashMap<>();
        items.put("display", displayItem);
        items.put("item", itemButton);
        items.put("min", minButton);
        items.put("max", maxButton);
        items.put("weight", weightButton);
        items.put("tier", tierButton);
        items.put("delete", deleteButton);
        items.put("back", backButton);

        MenuData menuData = MenuData.builder()
                .title("{primary}&lEDIT LOOT ENTRY")
                .type(MenuType.SIMPLE)
                .size(45)
                .globalFiller(globalFiller)
                .items(items)
                .build();

        MenuAPI.open(player, menuData);
    }

    private static String formatWeight(double weight) {
        return weight == Math.floor(weight) ? String.valueOf((int) weight) : String.valueOf(weight);
    }

    private static String resolveItemMaterial(LootEntry entry) {
        if (entry.getItemSnapshot() == null) return "BARRIER";
        String snap = entry.getItemSnapshot();
        if (snap.startsWith("bytes:")) {
            try {
                return ItemSnapshot.from(snap).toItemStack().getType().name();
            } catch (Exception e) {
                return "BARRIER";
            }
        }
        if (snap.startsWith("item:")) {
            int mIdx = snap.indexOf("\"m\":\"");
            if (mIdx >= 0) {
                int start = mIdx + 5;
                int end = snap.indexOf('"', start);
                if (end > start) return snap.substring(start, end);
            }
        }
        if (snap.startsWith("urlhead:") || snap.startsWith("playerhead:") || snap.startsWith("basehead:")) {
            return "PLAYER_HEAD";
        }
        return snap.contains(":") ? "BARRIER" : snap.toUpperCase();
    }
}

package net.exylia.commons.v2.ui.selector.impl.loot.menu;

import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Type-select step ("Item" or "Command") shown before creating a new loot entry when the
 * session opted in via {@code LootEditorSelector.allowCommands(true)}.
 */
public final class LootTypeSelectMenu {

    private LootTypeSelectMenu() {}

    public static void open(Player player) {
        ItemData globalFiller = ItemData.builder()
                .rawMaterial("GRAY_STAINED_GLASS_PANE")
                .rawDisplayName(" ")
                .hideTooltip(true)
                .build();

        ItemData itemButton = ItemData.builder()
                .rawMaterial("CHEST")
                .rawDisplayName("{info}&lITEM ENTRY 🎁")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Gives a specific item",
                        " {letters_black}▎ {letters}when this entry is rolled.",
                        "",
                        "{warning}➥ Click to select"
                ))
                .slotConfig(SlotConfig.single(11))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:loot_add_type ITEM")
                        .build()))
                .build();

        ItemData commandButton = ItemData.builder()
                .rawMaterial("COMMAND_BLOCK")
                .rawDisplayName("{warning}&lCOMMAND ENTRY ⌨")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Executes a console command",
                        " {letters_black}▎ {letters}when this entry is rolled.",
                        " {letters_black}▎ {muted}Use %player% for the recipient name.",
                        "",
                        "{warning}➥ Click to select"
                ))
                .slotConfig(SlotConfig.single(15))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:loot_add_type COMMAND")
                        .build()))
                .build();

        ItemData backButton = ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("{secondary}&l« BACK")
                .rawLore(List.of(
                        " {letters_black}▎ {letters}Return to the loot table.",
                        "",
                        "{warning}➥ Click to go back"
                ))
                .slotConfig(SlotConfig.single(22))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:loot_list")
                        .build()))
                .build();

        LinkedHashMap<String, ItemData> items = new LinkedHashMap<>();
        items.put("item", itemButton);
        items.put("command", commandButton);
        items.put("back", backButton);

        MenuData menuData = MenuData.builder()
                .title("{primary}&lSELECT ENTRY TYPE")
                .type(MenuType.SIMPLE)
                .size(27)
                .globalFiller(globalFiller)
                .items(items)
                .clickSounds(List.of("UI_BUTTON_CLICK|1.0|1.5"))
                .build();

        MenuAPI.open(player, menuData);
    }
}

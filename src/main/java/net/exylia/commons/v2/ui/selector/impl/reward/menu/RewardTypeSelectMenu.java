package net.exylia.commons.v2.ui.selector.impl.reward.menu;

import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.selector.impl.reward.RewardEditorSession;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;

public final class RewardTypeSelectMenu {

    private RewardTypeSelectMenu() {}

    public static void open(Player player, RewardEditorSession session) {
        ItemData globalFiller = ItemData.builder()
                .rawMaterial("GRAY_STAINED_GLASS_PANE")
                .rawDisplayName(" ")
                .hideTooltip(true)
                .build();

        ItemData commandButton = ItemData.builder()
                .rawMaterial("COMMAND_BLOCK")
                .rawDisplayName("{warning}&lCOMMAND REWARD ⌨")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Executes a console command",
                        " {letters_black}▎ {letters}when this reward is given.",
                        "",
                        "{warning}➥ Click to select"
                ))
                .slotConfig(SlotConfig.single(11))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_add_type COMMAND")
                        .build()))
                .build();

        ItemData itemButton = ItemData.builder()
                .rawMaterial("CHEST")
                .rawDisplayName("{info}&lITEM REWARD 🎁")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Gives a specific item",
                        " {letters_black}▎ {letters}to the player.",
                        "",
                        "{warning}➥ Click to select"
                ))
                .slotConfig(SlotConfig.single(13))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_add_type ITEM")
                        .build()))
                .build();

        ItemData messageButton = ItemData.builder()
                .rawMaterial("PAPER")
                .rawDisplayName("{letters}&lMESSAGE REWARD ✉")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Sends a message to the",
                        " {letters_black}▎ {letters}player as a reward.",
                        "",
                        "{warning}➥ Click to select"
                ))
                .slotConfig(SlotConfig.single(15))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_add_type MESSAGE")
                        .build()))
                .build();

        ItemData backButton = ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("{secondary}&l« BACK")
                .rawLore(List.of(
                        " {letters_black}▎ {letters}Return to the reward list.",
                        "",
                        "{warning}➥ Click to go back"
                ))
                .slotConfig(SlotConfig.single(22))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_list")
                        .build()))
                .build();

        LinkedHashMap<String, ItemData> items = new LinkedHashMap<>();
        items.put("command", commandButton);
        items.put("item", itemButton);
        items.put("message", messageButton);
        items.put("back", backButton);

        MenuData menuData = MenuData.builder()
                .title("{primary}&lSELECT REWARD TYPE")
                .type(MenuType.SIMPLE)
                .size(27)
                .globalFiller(globalFiller)
                .items(items)
                .build();

        MenuAPI.open(player, menuData);
    }
}

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
                .build();

        ItemData commandButton = ItemData.builder()
                .rawMaterial("COMMAND_BLOCK")
                .rawDisplayName("&6&lCommand Reward")
                .rawLore(List.of(
                        "&7Execute a console command",
                        "&7when the reward is given.",
                        "",
                        "&eClick to select"
                ))
                .slotConfig(SlotConfig.single(11))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_add_type COMMAND")
                        .build()))
                .build();

        ItemData itemButton = ItemData.builder()
                .rawMaterial("CHEST")
                .rawDisplayName("&b&lItem Reward")
                .rawLore(List.of(
                        "&7Give a specific item",
                        "&7to the player.",
                        "",
                        "&eClick to select"
                ))
                .slotConfig(SlotConfig.single(13))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_add_type ITEM")
                        .build()))
                .build();

        ItemData messageButton = ItemData.builder()
                .rawMaterial("PAPER")
                .rawDisplayName("&d&lMessage Reward")
                .rawLore(List.of(
                        "&7Send a message to the",
                        "&7player as a reward.",
                        "",
                        "&eClick to select"
                ))
                .slotConfig(SlotConfig.single(15))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_add_type MESSAGE")
                        .build()))
                .build();

        ItemData backButton = ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("&7Back")
                .rawLore(List.of("&7Return to the reward list"))
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
                .title("&8Select Reward Type")
                .type(MenuType.SIMPLE)
                .size(27)
                .globalFiller(globalFiller)
                .items(items)
                .build();

        MenuAPI.open(player, menuData);
    }
}

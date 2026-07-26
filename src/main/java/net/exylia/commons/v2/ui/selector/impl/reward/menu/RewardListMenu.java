package net.exylia.commons.v2.ui.selector.impl.reward.menu;

import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.reward.model.RewardEntry;
import net.exylia.commons.v2.reward.model.RewardType;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.model.NavigationData;
import net.exylia.commons.v2.ui.selector.impl.reward.RewardClipboard;
import net.exylia.commons.v2.ui.selector.impl.reward.RewardEditorSession;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;

public final class RewardListMenu {

    private static final List<Integer> PAGINATION_SLOTS = buildPaginationSlots();

    private RewardListMenu() {}

    public static void open(Player player, RewardEditorSession session) {
        List<ItemData> rewardItems = buildRewardItems(session);

        ItemData globalFiller = ItemData.builder()
                .rawMaterial("GRAY_STAINED_GLASS_PANE")
                .rawDisplayName(" ")
                .hideTooltip(true)
                .slotConfig(SlotConfig.single(0))
                .build();

        ItemData paginationFiller = ItemData.builder()
                .rawMaterial("LIGHT_GRAY_STAINED_GLASS_PANE")
                .rawDisplayName(" ")
                .hideTooltip(true)
                .slotConfig(SlotConfig.single(0))
                .build();

        ItemData addButton = ItemData.builder()
                .rawMaterial("EMERALD")
                .rawDisplayName("<#8fffc1><bold>ADD REWARD")
                .rawLore(List.of("&7Click to add a new reward"))
                .slotConfig(SlotConfig.single(45))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_add")
                        .build()))
                .build();

        ItemData saveButton = ItemData.builder()
                .rawMaterial("LIME_DYE")
                .rawDisplayName("<#8fffc1><bold>SAVE")
                .glowing(true)
                .rawLore(List.of("&7Click to save all rewards"))
                .slotConfig(SlotConfig.single(52))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_save")
                        .build()))
                .build();

        ItemData cancelButton = ItemData.builder()
                .rawMaterial("RED_DYE")
                .rawDisplayName("<#a33b53><bold>CANCEL")
                .rawLore(List.of("&7Click to cancel without saving"))
                .slotConfig(SlotConfig.single(53))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_cancel")
                        .build()))
                .build();

        ItemData prevButton = ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("&7Previous Page")
                .slotConfig(SlotConfig.single(47))
                .build();

        ItemData nextButton = ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("&7Next Page")
                .slotConfig(SlotConfig.single(51))
                .build();

        MenuData.MenuDataBuilder builder = MenuData.builder()
                .title(session.getTitle())
                .type(MenuType.PAGINATION)
                .size(54)
                .globalFiller(globalFiller)
                .paginationFiller(paginationFiller)
                .paginationSlots(PAGINATION_SLOTS)
                .paginationItems(rewardItems)
                .paginationNavigation(NavigationData.builder()
                        .previousButton(prevButton)
                        .previousButtonSlot(47)
                        .nextButton(nextButton)
                        .nextButtonSlot(51)
                        .build());

        builder.items(new LinkedHashMap<>());
        MenuData menuData = builder.build();
        menuData.getItems().put("add", addButton);
        menuData.getItems().put("save", saveButton);
        menuData.getItems().put("cancel", cancelButton);

        if (RewardClipboard.has(player)) {
            ItemData pasteButton = ItemData.builder()
                    .rawMaterial("WRITABLE_BOOK")
                    .rawDisplayName("<#f7d77a><bold>PASTE REWARD")
                    .rawLore(List.of("&7Click to paste copied reward"))
                    .slotConfig(SlotConfig.single(46))
                    .actions(List.of(ClickAction.builder()
                            .clickType(ClickTypeGroup.ANY)
                            .action("commons:reward_paste")
                            .build()))
                    .build();
            menuData.getItems().put("paste", pasteButton);
        }

        MenuAPI.open(player, menuData);
    }

    private static List<ItemData> buildRewardItems(RewardEditorSession session) {
        List<ItemData> items = new ArrayList<>();
        for (RewardEntry entry : session.getRewards()) {
            items.add(buildRewardItem(entry));
        }
        return items;
    }

    private static ItemData buildRewardItem(RewardEntry entry) {
        String material = materialFor(entry.getType());
        String name = "<color:" + colorFor(entry.getType()) + ">" + formatType(entry.getType()) + " &7» &f" + entry.getValuePreview();

        List<String> lore = new ArrayList<>();
        lore.add("&8ID: &7" + entry.getId().substring(0, 8) + "...");
        lore.add("&7Chance: &f" + entry.getChance() + "%");
        if (entry.getPriority() != 0) {
            lore.add("&7Priority: &f" + entry.getPriority());
        }
        if (entry.getCondition() != null) {
            lore.add("&7Condition: &f" + entry.getCondition());
        }
        if (entry.getPermission() != null) {
            lore.add("&7Permission: &f" + entry.getPermission());
        }
        lore.add("");
        lore.add("&aLeft Click &7» Edit");
        lore.add("&cRight Click &7» Delete");
        lore.add("&eShift+Left &7» Copy");

        return ItemData.builder()
                .rawMaterial(material)
                .rawDisplayName(name)
                .rawLore(lore)
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.LEFT)
                                .action("commons:reward_edit " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:reward_delete " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.SHIFT_LEFT)
                                .action("commons:reward_copy " + entry.getId())
                                .build()
                ))
                .build();
    }

    private static String materialFor(RewardType type) {
        return switch (type) {
            case COMMAND -> "COMMAND_BLOCK";
            case ITEM -> "CHEST";
            case MESSAGE -> "PAPER";
        };
    }

    private static String formatType(RewardType type) {
        return switch (type) {
            case COMMAND -> "Command";
            case ITEM -> "Item";
            case MESSAGE -> "Message";
        };
    }

    private static String colorFor(RewardType type) {
        return switch (type) {
            case COMMAND -> "#f5a86c";
            case ITEM -> "#83d8ff";
            case MESSAGE -> "#d7b8ff";
        };
    }

    private static List<Integer> buildPaginationSlots() {
        return IntStream.rangeClosed(0, 35).boxed().toList();
    }
}

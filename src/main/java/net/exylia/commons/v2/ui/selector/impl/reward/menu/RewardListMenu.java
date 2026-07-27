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
                .rawDisplayName("{success}&lADD REWARD")
                .rawLore(List.of(
                        "{letters_black}▎ {letters}Create a new command, item",
                        "{letters_black}▎ {letters}or message reward.",
                        "",
                        "{warning}➥ Click to add"
                ))
                .slotConfig(SlotConfig.single(45))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_add")
                        .build()))
                .build();

        ItemData saveButton = ItemData.builder()
                .rawMaterial("LIME_DYE")
                .rawDisplayName("{success}&lSAVE CHANGES")
                .glowing(true)
                .rawLore(List.of(
                        "{letters_black}▎ {letters}Persist every reward configured",
                        "{letters_black}▎ {letters}in this list.",
                        "",
                        "{warning}➥ Click to save"
                ))
                .slotConfig(SlotConfig.single(52))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_save")
                        .build()))
                .build();

        ItemData cancelButton = ItemData.builder()
                .rawMaterial("RED_DYE")
                .rawDisplayName("{error}&lCANCEL")
                .rawLore(List.of(
                        "{letters_black}▎ {letters}Discard every unsaved change",
                        "{letters_black}▎ {letters}and close this menu.",
                        "",
                        "{warning}➥ Click to cancel"
                ))
                .slotConfig(SlotConfig.single(53))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_cancel")
                        .build()))
                .build();

        ItemData prevButton = ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("{secondary}&l« Previous Page")
                .slotConfig(SlotConfig.single(47))
                .build();

        ItemData nextButton = ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("{secondary}&lNext Page »")
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
                    .rawDisplayName("{highlight}&lPASTE REWARD")
                    .rawLore(List.of(
                            "{letters_black}▎ {letters}Add a copy of the reward",
                            "{letters_black}▎ {letters}currently in your clipboard.",
                            "",
                            "{warning}➥ Click to paste"
                    ))
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
        String color = colorFor(entry.getType());
        String name = "<color:" + color + ">&l" + entry.getDisplayName();

        List<String> lore = new ArrayList<>();
        lore.add("{secondary}Details:");
        lore.add(" {letters_black}▎ {letters}Type " + typeIcon(entry.getType()) + " {letters_black}» <color:" + color + ">" + formatType(entry.getType()));
        lore.add(" {letters_black}▎ {letters}Value {letters_black}» {info}" + entry.getValuePreview());
        lore.add(" {letters_black}▎ {letters}Chance 🎲 {letters_black}» {highlight}" + formatChance(entry.getChance()) + "%");
        if (entry.getPriority() != 0) {
            lore.add(" {letters_black}▎ {letters}Priority {letters_black}» {info}" + entry.getPriority());
        }
        if (entry.getCondition() != null) {
            lore.add(" {letters_black}▎ {letters}Condition {letters_black}» {info}" + entry.getCondition());
        }
        if (entry.getPermission() != null) {
            lore.add(" {letters_black}▎ {letters}Permission 🔒 {letters_black}» {info}" + entry.getPermission());
        }
        lore.add("");
        lore.add("{success}● {letters}Left Click {letters_black}» Edit");
        lore.add("{error}● {letters}Right Click {letters_black}» Delete");
        lore.add("{warning}● {letters}Shift + Left {letters_black}» Copy");

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

    private static String typeIcon(RewardType type) {
        return switch (type) {
            case COMMAND -> "⌨";
            case ITEM -> "🎁";
            case MESSAGE -> "✉";
        };
    }

    private static String colorFor(RewardType type) {
        return switch (type) {
            case COMMAND -> "#f5a86c";
            case ITEM -> "#83d8ff";
            case MESSAGE -> "#d7b8ff";
        };
    }

    private static String formatChance(double chance) {
        return chance == Math.floor(chance) ? String.valueOf((int) chance) : String.valueOf(chance);
    }

    private static List<Integer> buildPaginationSlots() {
        return IntStream.rangeClosed(0, 35).boxed().toList();
    }
}

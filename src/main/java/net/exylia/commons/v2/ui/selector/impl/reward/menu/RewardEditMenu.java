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
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;

public final class RewardEditMenu {

    private RewardEditMenu() {}

    public static void open(Player player, RewardEntry entry) {
        ItemData globalFiller = ItemData.builder()
                .rawMaterial("GRAY_STAINED_GLASS_PANE")
                .rawDisplayName(" ")
                .hideTooltip(true)
                .build();

        ItemData displayItem = buildDisplayItem(entry);

        ItemData valueButton = buildValueButton(entry);

        ItemData nameButton = ItemData.builder()
                .rawMaterial("NAME_TAG")
                .rawDisplayName("{highlight}&lDISPLAY NAME")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Shown in menus and reward",
                        " {letters_black}▎ {letters}broadcasts instead of the raw value.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + (entry.getName() != null && !entry.getName().isBlank() ? entry.getName() : "None"),
                        "",
                        "{success}● {letters}Left Click {letters_black}» Change",
                        "{error}● {letters}Right Click {letters_black}» Clear"
                ))
                .slotConfig(SlotConfig.single(19))
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.LEFT)
                                .action("commons:reward_set_name " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:reward_clear_name " + entry.getId())
                                .build()
                ))
                .build();

        ItemData chanceButton = ItemData.builder()
                .rawMaterial("SUNFLOWER")
                .rawDisplayName("{highlight}&lCHANCE 🎲")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Probability of receiving",
                        " {letters_black}▎ {letters}this reward when it is given.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {highlight}" + formatChance(entry.getChance()) + "%",
                        "",
                        "{warning}➥ Click to change"
                ))
                .slotConfig(SlotConfig.single(21))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_set_chance " + entry.getId())
                        .build()))
                .build();

        ItemData conditionButton = ItemData.builder()
                .rawMaterial("COMPARATOR")
                .rawDisplayName("{info}&lCONDITION")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Expression that must be true",
                        " {letters_black}▎ {letters}for this reward to be given.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + (entry.getCondition() != null ? entry.getCondition() : "None"),
                        "",
                        "{success}● {letters}Left Click {letters_black}» Change",
                        "{error}● {letters}Right Click {letters_black}» Clear"
                ))
                .slotConfig(SlotConfig.single(22))
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.LEFT)
                                .action("commons:reward_set_condition " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:reward_clear_condition " + entry.getId())
                                .build()
                ))
                .build();

        ItemData priorityButton = ItemData.builder()
                .rawMaterial("REPEATER")
                .rawDisplayName("{secondary_light}&lPRIORITY")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Execution order relative to",
                        " {letters_black}▎ {letters}other rewards. Higher goes first.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + entry.getPriority(),
                        "",
                        "{warning}➥ Click to change"
                ))
                .slotConfig(SlotConfig.single(23))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_set_priority " + entry.getId())
                        .build()))
                .build();

        ItemData permissionButton = ItemData.builder()
                .rawMaterial("NAME_TAG")
                .rawDisplayName("{success}&lPERMISSION 🔒")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Permission node required to",
                        " {letters_black}▎ {letters}receive this reward.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + (entry.getPermission() != null ? entry.getPermission() : "None"),
                        "",
                        "{success}● {letters}Left Click {letters_black}» Change",
                        "{error}● {letters}Right Click {letters_black}» Clear"
                ))
                .slotConfig(SlotConfig.single(24))
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.LEFT)
                                .action("commons:reward_set_permission " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:reward_clear_permission " + entry.getId())
                                .build()
                ))
                .build();

        ItemData amountButton = ItemData.builder()
                .rawMaterial("HOPPER")
                .rawDisplayName("{secondary_light}&lITEM AMOUNT")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}How many of this item are",
                        " {letters_black}▎ {letters}given at once. Only applies",
                        " {letters_black}▎ {letters}to item rewards.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {highlight}" + entry.getItemAmount(),
                        "",
                        "{warning}➥ Click to change"
                ))
                .slotConfig(SlotConfig.single(30))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_set_amount " + entry.getId())
                        .build()))
                .build();

        ItemData iconButton = ItemData.builder()
                .rawMaterial(entry.getResolvedIconMaterial())
                .rawDisplayName("{secondary_light}&lPREVIEW ICON 🖼")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Item shown to players in",
                        " {letters_black}▎ {letters}preview menus for this reward.",
                        " {letters_black}▎ {muted}Falls back to the item value or a",
                        " {letters_black}▎ {muted}type-based icon when not set.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + (entry.hasIcon() ? "Custom" : "Default"),
                        "",
                        "{success}● {letters}Left Click {letters_black}» Change",
                        "{error}● {letters}Right Click {letters_black}» Clear"
                ))
                .slotConfig(SlotConfig.single(31))
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.LEFT)
                                .action("commons:reward_set_icon " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:reward_clear_icon " + entry.getId())
                                .build()
                ))
                .build();

        ItemData deliveryMessageButton = ItemData.builder()
                .rawMaterial("WRITABLE_BOOK")
                .rawDisplayName("{letters}&lDELIVERY MESSAGE")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Message sent to the player",
                        " {letters_black}▎ {letters}right after this reward is given.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + (entry.getDeliveryMessage() != null ? entry.getDeliveryMessage() : "None"),
                        "",
                        "{success}● {letters}Left Click {letters_black}» Change",
                        "{error}● {letters}Right Click {letters_black}» Clear"
                ))
                .slotConfig(SlotConfig.single(25))
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.LEFT)
                                .action("commons:reward_set_delivery_msg " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:reward_clear_delivery_msg " + entry.getId())
                                .build()
                ))
                .build();

        ItemData deleteButton = ItemData.builder()
                .rawMaterial("TNT")
                .rawDisplayName("{error}&lDELETE REWARD")
                .rawLore(List.of(
                        " {letters_black}▎ {letters}Permanently remove this reward",
                        " {letters_black}▎ {letters}from the list.",
                        "",
                        "{warning}➥ Click to delete"
                ))
                .slotConfig(SlotConfig.single(44))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_delete " + entry.getId())
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
                .slotConfig(SlotConfig.single(36))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_list")
                        .build()))
                .build();

        LinkedHashMap<String, ItemData> items = new LinkedHashMap<>();
        items.put("display", displayItem);
        items.put("value", valueButton);
        items.put("name", nameButton);
        items.put("chance", chanceButton);
        items.put("condition", conditionButton);
        items.put("permission", permissionButton);
        items.put("priority", priorityButton);
        if (entry.getType() == RewardType.ITEM) {
            items.put("amount", amountButton);
        }
        items.put("icon", iconButton);
        items.put("delivery_msg", deliveryMessageButton);
        items.put("delete", deleteButton);
        items.put("back", backButton);

        MenuData menuData = MenuData.builder()
                .title("{primary}&lEDIT REWARD")
                .type(MenuType.SIMPLE)
                .size(45)
                .globalFiller(globalFiller)
                .items(items)
                .clickSounds(List.of("UI_BUTTON_CLICK|1.0|1.5"))
                .build();

        MenuAPI.open(player, menuData);
    }

    private static ItemData buildDisplayItem(RewardEntry entry) {
        String material = entry.getResolvedIconMaterial();

        String typeName = switch (entry.getType()) {
            case COMMAND -> "{warning}Command";
            case ITEM -> "{info}Item";
            case MESSAGE -> "{letters}Message";
        };

        return ItemData.builder()
                .rawMaterial(material)
                .rawDisplayName("{primary}&l" + entry.getDisplayName())
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}ID {letters_black}» {muted}" + entry.getId().substring(0, 8) + "...",
                        " {letters_black}▎ {letters}Type {letters_black}» " + typeName,
                        " {letters_black}▎ {letters}Value {letters_black}» {info}" + entry.getValuePreview()
                ))
                .slotConfig(SlotConfig.single(4))
                .build();
    }

    private static String formatChance(double chance) {
        return chance == Math.floor(chance) ? String.valueOf((int) chance) : String.valueOf(chance);
    }

    private static ItemData buildValueButton(RewardEntry entry) {
        return switch (entry.getType()) {
            case COMMAND -> ItemData.builder()
                    .rawMaterial("OAK_SIGN")
                    .rawDisplayName("{warning}&lCOMMAND VALUE ⌨")
                    .rawLore(List.of(
                            "{secondary}Details:",
                            " {letters_black}▎ {letters}Command executed when",
                            " {letters_black}▎ {letters}this reward is given.",
                            " {letters_black}▎ {muted}Use %player% for the player name.",
                            "",
                            " {letters_black}▎ {letters}Current {letters_black}» {info}" + (entry.getCommand() != null ? entry.getCommand() : "Not set"),
                            "",
                            "{warning}➥ Click to change"
                    ))
                    .slotConfig(SlotConfig.single(20))
                    .actions(List.of(ClickAction.builder()
                            .clickType(ClickTypeGroup.ANY)
                            .action("commons:reward_set_value " + entry.getId())
                            .build()))
                    .build();
            case ITEM -> ItemData.builder()
                    .rawMaterial("ITEM_FRAME")
                    .rawDisplayName("{info}&lITEM VALUE 🎁")
                    .rawLore(List.of(
                            "{secondary}Details:",
                            " {letters_black}▎ {letters}Item given to the player",
                            " {letters_black}▎ {letters}when this reward is given.",
                            "",
                            " {letters_black}▎ {letters}Current {letters_black}» {info}" + entry.getValuePreview(),
                            "",
                            "{warning}➥ Click to change item"
                    ))
                    .slotConfig(SlotConfig.single(20))
                    .actions(List.of(ClickAction.builder()
                            .clickType(ClickTypeGroup.ANY)
                            .action("commons:reward_set_value " + entry.getId())
                            .build()))
                    .build();
            case MESSAGE -> ItemData.builder()
                    .rawMaterial("WRITABLE_BOOK")
                    .rawDisplayName("{letters}&lMESSAGE VALUE ✉")
                    .rawLore(List.of(
                            "{secondary}Details:",
                            " {letters_black}▎ {letters}Message sent to the player",
                            " {letters_black}▎ {letters}when this reward is given.",
                            " {letters_black}▎ {muted}Supports color codes and placeholders.",
                            "",
                            " {letters_black}▎ {letters}Current {letters_black}» {info}" + (entry.getMessage() != null ? entry.getMessage() : "Not set"),
                            "",
                            "{warning}➥ Click to change"
                    ))
                    .slotConfig(SlotConfig.single(20))
                    .actions(List.of(ClickAction.builder()
                            .clickType(ClickTypeGroup.ANY)
                            .action("commons:reward_set_value " + entry.getId())
                            .build()))
                    .build();
        };
    }
}

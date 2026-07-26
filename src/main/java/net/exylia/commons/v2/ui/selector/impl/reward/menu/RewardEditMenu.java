package net.exylia.commons.v2.ui.selector.impl.reward.menu;

import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.items.snapshot.ItemSnapshot;
import net.exylia.commons.v2.reward.model.RewardEntry;
import net.exylia.commons.v2.reward.model.RewardType;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;

public final class RewardEditMenu {

    private RewardEditMenu() {}

    public static void open(Player player, RewardEntry entry) {
        ItemData globalFiller = ItemData.builder()
                .rawMaterial("GRAY_STAINED_GLASS_PANE")
                .rawDisplayName(" ")
                .build();

        ItemData displayItem = buildDisplayItem(entry);

        ItemData valueButton = buildValueButton(entry);

        ItemData chanceButton = ItemData.builder()
                .rawMaterial("SUNFLOWER")
                .rawDisplayName("&e&lChance")
                .rawLore(List.of(
                        "&7Current: &f" + entry.getChance() + "%",
                        "",
                        "&eClick to change"
                ))
                .slotConfig(SlotConfig.single(20))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_set_chance " + entry.getId())
                        .build()))
                .build();

        ItemData conditionButton = ItemData.builder()
                .rawMaterial("COMPARATOR")
                .rawDisplayName("&b&lCondition")
                .rawLore(List.of(
                        "&7Current: &f" + (entry.getCondition() != null ? entry.getCondition() : "none"),
                        "",
                        "&eClick to change",
                        "&cRight-Click to clear"
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
                .rawDisplayName("&d&lPriority")
                .rawLore(List.of(
                        "&7Current: &f" + entry.getPriority(),
                        "&8Higher = executed first",
                        "",
                        "&eClick to change"
                ))
                .slotConfig(SlotConfig.single(24))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_set_priority " + entry.getId())
                        .build()))
                .build();

        ItemData permissionButton = ItemData.builder()
                .rawMaterial("NAME_TAG")
                .rawDisplayName("&a&lPermission")
                .rawLore(List.of(
                        "&7Required to receive this reward.",
                        "&7Current: &f" + (entry.getPermission() != null ? entry.getPermission() : "none"),
                        "",
                        "&eClick to change",
                        "&cRight-Click to clear"
                ))
                .slotConfig(SlotConfig.single(34))
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

        ItemData deliveryMessageButton = ItemData.builder()
                .rawMaterial("WRITABLE_BOOK")
                .rawDisplayName("&f&lDelivery Message")
                .rawLore(List.of(
                        "&7Sent to player after reward.",
                        "&7Current: &f" + (entry.getDeliveryMessage() != null ? entry.getDeliveryMessage() : "none"),
                        "",
                        "&eClick to change",
                        "&cRight-Click to clear"
                ))
                .slotConfig(SlotConfig.single(30))
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
                .rawDisplayName("&c&lDelete Reward")
                .rawLore(List.of("&7Click to permanently delete this reward"))
                .slotConfig(SlotConfig.single(32))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_delete " + entry.getId())
                        .build()))
                .build();

        ItemData backButton = ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("&7Back")
                .rawLore(List.of("&7Return to the reward list"))
                .slotConfig(SlotConfig.single(36))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:reward_list")
                        .build()))
                .build();

        LinkedHashMap<String, ItemData> items = new LinkedHashMap<>();
        items.put("display", displayItem);
        items.put("value", valueButton);
        items.put("chance", chanceButton);
        items.put("condition", conditionButton);
        items.put("permission", permissionButton);
        items.put("priority", priorityButton);
        items.put("delivery_msg", deliveryMessageButton);
        items.put("delete", deleteButton);
        items.put("back", backButton);

        MenuData menuData = MenuData.builder()
                .title("&8Edit Reward")
                .type(MenuType.SIMPLE)
                .size(45)
                .globalFiller(globalFiller)
                .items(items)
                .build();

        MenuAPI.open(player, menuData);
    }

    private static ItemData buildDisplayItem(RewardEntry entry) {
        String material = switch (entry.getType()) {
            case COMMAND -> "COMMAND_BLOCK";
            case ITEM -> resolveItemMaterial(entry);
            case MESSAGE -> "PAPER";
        };

        String typeName = switch (entry.getType()) {
            case COMMAND -> "&6Command";
            case ITEM -> "&bItem";
            case MESSAGE -> "&dMessage";
        };

        return ItemData.builder()
                .rawMaterial(material)
                .rawDisplayName(typeName + " &7» &f" + entry.getValuePreview())
                .rawLore(List.of(
                        "&8ID: " + entry.getId(),
                        "&7Type: " + typeName
                ))
                .slotConfig(SlotConfig.single(13))
                .build();
    }

    private static String resolveItemMaterial(RewardEntry entry) {
        if (entry.getItemSnapshot() == null) return "CHEST";
        String snap = entry.getItemSnapshot();
        if (snap.startsWith("bytes:")) {
            try {
                ItemStack item = ItemSnapshot.from(snap).toItemStack();
                return item.getType().name();
            } catch (Exception e) {
                return "CHEST";
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
        return snap.contains(":") ? "CHEST" : snap.toUpperCase();
    }

    private static ItemData buildValueButton(RewardEntry entry) {
        return switch (entry.getType()) {
            case COMMAND -> ItemData.builder()
                    .rawMaterial("OAK_SIGN")
                    .rawDisplayName("&6&lCommand Value")
                    .rawLore(List.of(
                            "&7Current: &f" + (entry.getCommand() != null ? entry.getCommand() : "(not set)"),
                            "&8Use %player% for player name",
                            "",
                            "&eClick to change"
                    ))
                    .slotConfig(SlotConfig.single(11))
                    .actions(List.of(ClickAction.builder()
                            .clickType(ClickTypeGroup.ANY)
                            .action("commons:reward_set_value " + entry.getId())
                            .build()))
                    .build();
            case ITEM -> ItemData.builder()
                    .rawMaterial("ITEM_FRAME")
                    .rawDisplayName("&b&lItem Value")
                    .rawLore(List.of(
                            "&7Current: &f" + entry.getValuePreview(),
                            "",
                            "&eClick to change item"
                    ))
                    .slotConfig(SlotConfig.single(11))
                    .actions(List.of(ClickAction.builder()
                            .clickType(ClickTypeGroup.ANY)
                            .action("commons:reward_set_value " + entry.getId())
                            .build()))
                    .build();
            case MESSAGE -> ItemData.builder()
                    .rawMaterial("WRITABLE_BOOK")
                    .rawDisplayName("&d&lMessage Value")
                    .rawLore(List.of(
                            "&7Current: &f" + (entry.getMessage() != null ? entry.getMessage() : "(not set)"),
                            "&8Supports color codes and placeholders",
                            "",
                            "&eClick to change"
                    ))
                    .slotConfig(SlotConfig.single(11))
                    .actions(List.of(ClickAction.builder()
                            .clickType(ClickTypeGroup.ANY)
                            .action("commons:reward_set_value " + entry.getId())
                            .build()))
                    .build();
        };
    }
}

package net.exylia.commons.v2.ui.selector.impl.iconpicker.menu;

import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.selector.impl.iconpicker.IconPickerRegistry;
import net.exylia.commons.v2.ui.selector.impl.iconpicker.IconPickerSession;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;

public final class IconPickerMenu {

    public static final int CAPTURE_SLOT = 20;

    private IconPickerMenu() {}

    public static void open(Player player, IconPickerSession session) {
        IconPickerRegistry.getInstance().put(session);

        ItemData globalFiller = ItemData.builder()
                .rawMaterial("GRAY_STAINED_GLASS_PANE")
                .rawDisplayName(" ")
                .hideTooltip(true)
                .build();

        ItemData infoItem = ItemData.builder()
                .rawMaterial("ITEM_FRAME")
                .rawDisplayName("{highlight}&lPLACE YOUR ITEM")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Drag an item from your",
                        " {letters_black}▎ {letters}inventory into the empty",
                        " {letters_black}▎ {letters}slot to the right.",
                        "",
                        " {letters_black}▎ {muted}The item is captured instantly.",
                        " {letters_black}▎ {muted}Right-click the slot to clear it."
                ))
                .slotConfig(SlotConfig.single(19))
                .build();

        LinkedHashMap<String, ItemData> items = new LinkedHashMap<>();
        items.put("info", infoItem);

        ItemStack captured = session.getCapturedItem();
        if (captured != null) {
            ItemData capturedItemData = ItemData.builder()
                    .itemStack(captured)
                    .slotConfig(SlotConfig.single(CAPTURE_SLOT))
                    .actions(List.of(ClickAction.builder()
                            .clickType(ClickTypeGroup.RIGHT)
                            .action("commons:iconpicker_clear_slot")
                            .build()))
                    .build();
            items.put("captured", capturedItemData);
        }

        ItemData idButton = ItemData.builder()
                .rawMaterial("NAME_TAG")
                .rawDisplayName("{info}&lSET BY MATERIAL / ID ⌨")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Type a material name",
                        " {letters_black}▎ {letters}(e.g. DIAMOND_SWORD) or a",
                        " {letters_black}▎ {letters}custom item ID.",
                        "",
                        "{warning}➥ Click to type"
                ))
                .slotConfig(SlotConfig.single(29))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:iconpicker_set_id")
                        .build()))
                .build();
        items.put("id", idButton);

        ItemData headButton = ItemData.builder()
                .rawMaterial("PLAYER_HEAD")
                .rawDisplayName("{secondary_light}&lCUSTOM HEAD 🗿")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Use a base64 texture, a",
                        " {letters_black}▎ {letters}skin URL, or a player name.",
                        "",
                        "{warning}➥ Click to select"
                ))
                .slotConfig(SlotConfig.single(31))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:iconpicker_open_heads")
                        .build()))
                .build();
        items.put("head", headButton);

        ItemData confirmButton = ItemData.builder()
                .rawMaterial("LIME_DYE")
                .rawDisplayName("{success}&lCONFIRM")
                .glowing(true)
                .rawLore(List.of(
                        "{letters_black}▎ {letters}Use the item currently placed",
                        "{letters_black}▎ {letters}in the slot as the icon.",
                        "",
                        "{warning}➥ Click to confirm"
                ))
                .slotConfig(SlotConfig.single(33))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:iconpicker_confirm")
                        .build()))
                .build();
        items.put("confirm", confirmButton);

        ItemData cancelButton = ItemData.builder()
                .rawMaterial("RED_DYE")
                .rawDisplayName("{error}&lCANCEL")
                .rawLore(List.of(
                        "{letters_black}▎ {letters}Discard and go back.",
                        "",
                        "{warning}➥ Click to cancel"
                ))
                .slotConfig(SlotConfig.single(13))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:iconpicker_cancel")
                        .build()))
                .build();
        items.put("cancel", cancelButton);

        MenuData menuData = MenuData.builder()
                .title("{primary}&lICON PICKER")
                .type(MenuType.SIMPLE)
                .size(45)
                .globalFiller(globalFiller)
                .items(items)
                .clickSounds(List.of("UI_BUTTON_CLICK|1.0|1.5"))
                .build();

        menuData.addCaptureSlot(CAPTURE_SLOT, item -> {
            session.setCapturedItem(item);
            open(player, session);
        });

        MenuAPI.open(player, menuData);
    }
}

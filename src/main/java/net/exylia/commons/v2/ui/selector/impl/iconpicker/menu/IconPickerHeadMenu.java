package net.exylia.commons.v2.ui.selector.impl.iconpicker.menu;

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

public final class IconPickerHeadMenu {

    private IconPickerHeadMenu() {}

    public static void open(Player player) {
        ItemData globalFiller = ItemData.builder()
                .rawMaterial("GRAY_STAINED_GLASS_PANE")
                .rawDisplayName(" ")
                .hideTooltip(true)
                .build();

        ItemData base64Button = ItemData.builder()
                .rawMaterial("PLAYER_HEAD")
                .rawDisplayName("{info}&lBASE64 TEXTURE")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Use a raw base64-encoded",
                        " {letters_black}▎ {letters}skin texture value.",
                        "",
                        "{warning}➥ Click to select"
                ))
                .slotConfig(SlotConfig.single(11))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:iconpicker_head_input BASE64")
                        .build()))
                .build();

        ItemData urlButton = ItemData.builder()
                .rawMaterial("PLAYER_HEAD")
                .rawDisplayName("{success}&lSKIN URL 🔗")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Use a direct texture URL",
                        " {letters_black}▎ {letters}from textures.minecraft.net.",
                        "",
                        "{warning}➥ Click to select"
                ))
                .slotConfig(SlotConfig.single(13))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:iconpicker_head_input URL")
                        .build()))
                .build();

        ItemData playerButton = ItemData.builder()
                .rawMaterial("PLAYER_HEAD")
                .rawDisplayName("{highlight}&lPLAYER NAME 👤")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Use the skin of an",
                        " {letters_black}▎ {letters}existing player by name.",
                        "",
                        "{warning}➥ Click to select"
                ))
                .slotConfig(SlotConfig.single(15))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:iconpicker_head_input PLAYER")
                        .build()))
                .build();

        ItemData backButton = ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("{secondary}&l« BACK")
                .rawLore(List.of(
                        " {letters_black}▎ {letters}Return to the icon picker.",
                        "",
                        "{warning}➥ Click to go back"
                ))
                .slotConfig(SlotConfig.single(22))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:iconpicker_open")
                        .build()))
                .build();

        LinkedHashMap<String, ItemData> items = new LinkedHashMap<>();
        items.put("base64", base64Button);
        items.put("url", urlButton);
        items.put("player", playerButton);
        items.put("back", backButton);

        MenuData menuData = MenuData.builder()
                .title("{primary}&lCUSTOM HEAD SOURCE")
                .type(MenuType.SIMPLE)
                .size(27)
                .globalFiller(globalFiller)
                .items(items)
                .clickSounds(List.of("UI_BUTTON_CLICK|1.0|1.5"))
                .build();

        MenuAPI.open(player, menuData);
    }
}

package net.exylia.commons.v2.ui.selector.impl.namedcommand.menu;

import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.namedcommand.model.NamedCommandEntry;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.model.NavigationData;
import net.exylia.commons.v2.ui.selector.impl.namedcommand.NamedCommandClipboard;
import net.exylia.commons.v2.ui.selector.impl.namedcommand.NamedCommandEditorSession;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;

public final class NamedCommandListMenu {

    private static final List<Integer> PAGINATION_SLOTS = buildPaginationSlots();

    private NamedCommandListMenu() {}

    public static void open(Player player, NamedCommandEditorSession session) {
        List<ItemData> commandItems = buildCommandItems(session);

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
                .rawDisplayName("{success}&lADD COMMAND")
                .rawLore(List.of(
                        "{letters_black}▎ {letters}Create a new named command",
                        "{letters_black}▎ {letters}for this list.",
                        "",
                        "{warning}➥ Click to add"
                ))
                .slotConfig(SlotConfig.single(45))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:namedcmd_add")
                        .build()))
                .build();

        ItemData saveButton = ItemData.builder()
                .rawMaterial("LIME_DYE")
                .rawDisplayName("{success}&lSAVE CHANGES")
                .glowing(true)
                .rawLore(List.of(
                        "{letters_black}▎ {letters}Persist every command configured",
                        "{letters_black}▎ {letters}in this list.",
                        "",
                        "{warning}➥ Click to save"
                ))
                .slotConfig(SlotConfig.single(52))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:namedcmd_save")
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
                        .action("commons:namedcmd_cancel")
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
                .paginationItems(commandItems)
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

        if (NamedCommandClipboard.has(player)) {
            ItemData pasteButton = ItemData.builder()
                    .rawMaterial("WRITABLE_BOOK")
                    .rawDisplayName("{highlight}&lPASTE COMMAND")
                    .rawLore(List.of(
                            "{letters_black}▎ {letters}Add a copy of the command",
                            "{letters_black}▎ {letters}currently in your clipboard.",
                            "",
                            "{warning}➥ Click to paste"
                    ))
                    .slotConfig(SlotConfig.single(46))
                    .actions(List.of(ClickAction.builder()
                            .clickType(ClickTypeGroup.ANY)
                            .action("commons:namedcmd_paste")
                            .build()))
                    .build();
            menuData.getItems().put("paste", pasteButton);
        }

        MenuAPI.open(player, menuData);
    }

    private static List<ItemData> buildCommandItems(NamedCommandEditorSession session) {
        List<ItemData> items = new ArrayList<>();
        for (NamedCommandEntry entry : session.getCommands()) {
            items.add(buildCommandItem(entry));
        }
        return items;
    }

    private static ItemData buildCommandItem(NamedCommandEntry entry) {
        return ItemData.builder()
                .rawMaterial("COMMAND_BLOCK")
                .rawDisplayName("{warning}&l" + entry.getDisplayName())
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Command ⌨ {letters_black}» {info}" + (entry.getCommand() != null ? entry.getCommand() : "Not set"),
                        "",
                        "{success}● {letters}Left Click {letters_black}» Rename",
                        "{info}● {letters}Middle Click {letters_black}» Edit Command",
                        "{error}● {letters}Right Click {letters_black}» Delete",
                        "{warning}● {letters}Shift + Left {letters_black}» Copy"
                ))
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.LEFT)
                                .action("commons:namedcmd_edit_name " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.MIDDLE)
                                .action("commons:namedcmd_edit_command " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:namedcmd_delete " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.SHIFT_LEFT)
                                .action("commons:namedcmd_copy " + entry.getId())
                                .build()
                ))
                .build();
    }

    private static List<Integer> buildPaginationSlots() {
        return IntStream.rangeClosed(0, 35).boxed().toList();
    }
}

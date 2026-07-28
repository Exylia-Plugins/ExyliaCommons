package net.exylia.commons.v2.ui.selector.impl.loot.menu;

import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.items.snapshot.ItemSnapshot;
import net.exylia.commons.v2.loot.model.LootEntry;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.model.NavigationData;
import net.exylia.commons.v2.ui.selector.impl.loot.LootClipboard;
import net.exylia.commons.v2.ui.selector.impl.loot.LootEditorSession;
import net.exylia.commons.v2.ui.selector.impl.loot.LootListClipboard;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;

public final class LootListMenu {

    private static final List<Integer> PAGINATION_SLOTS = buildPaginationSlots();

    private LootListMenu() {}

    public static void open(Player player, LootEditorSession session) {
        List<ItemData> entryItems = buildEntryItems(session);

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
                .rawDisplayName("{success}&lADD ENTRY")
                .rawLore(List.of(
                        "{letters_black}▎ {letters}Add a new item to this",
                        "{letters_black}▎ {letters}loot table.",
                        "",
                        "{warning}➥ Click to add"
                ))
                .slotConfig(SlotConfig.single(45))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:loot_add")
                        .build()))
                .build();

        ItemData saveButton = ItemData.builder()
                .rawMaterial("LIME_DYE")
                .rawDisplayName("{success}&lSAVE CHANGES")
                .glowing(true)
                .rawLore(List.of(
                        "{letters_black}▎ {letters}Persist every entry configured",
                        "{letters_black}▎ {letters}in this loot table.",
                        "",
                        "{warning}➥ Click to save"
                ))
                .slotConfig(SlotConfig.single(52))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:loot_save")
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
                        .action("commons:loot_cancel")
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
                .paginationItems(entryItems)
                .clickSounds(List.of("UI_BUTTON_CLICK|1.0|1.5"))
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

        if (LootClipboard.has(player)) {
            ItemData pasteButton = ItemData.builder()
                    .rawMaterial("WRITABLE_BOOK")
                    .rawDisplayName("{highlight}&lPASTE ENTRY")
                    .rawLore(List.of(
                            "{letters_black}▎ {letters}Add a copy of the entry",
                            "{letters_black}▎ {letters}currently in your clipboard.",
                            "",
                            "{warning}➥ Click to paste"
                    ))
                    .slotConfig(SlotConfig.single(46))
                    .actions(List.of(ClickAction.builder()
                            .clickType(ClickTypeGroup.ANY)
                            .action("commons:loot_paste")
                            .build()))
                    .build();
            menuData.getItems().put("paste", pasteButton);
        }

        if (!session.getEntries().isEmpty()) {
            ItemData copyAllButton = ItemData.builder()
                    .rawMaterial("BOOKSHELF")
                    .rawDisplayName("{info}&lCOPY ALL ENTRIES")
                    .rawLore(List.of(
                            "{letters_black}▎ {letters}Copy every entry in this",
                            "{letters_black}▎ {letters}loot table (" + session.getEntries().size() + ") to your clipboard,",
                            "{letters_black}▎ {letters}ready to paste in another table.",
                            "",
                            "{warning}➥ Click to copy all"
                    ))
                    .slotConfig(SlotConfig.single(48))
                    .actions(List.of(ClickAction.builder()
                            .clickType(ClickTypeGroup.ANY)
                            .action("commons:loot_copy_all")
                            .build()))
                    .build();
            menuData.getItems().put("copy_all", copyAllButton);
        }

        if (LootListClipboard.has(player)) {
            ItemData pasteAllButton = ItemData.builder()
                    .rawMaterial("ENCHANTED_BOOK")
                    .glowing(true)
                    .rawDisplayName("{highlight}&lPASTE ALL ENTRIES")
                    .rawLore(List.of(
                            "{letters_black}▎ {letters}Append the " + LootListClipboard.size(player) + " entries stored",
                            "{letters_black}▎ {letters}in your table clipboard to this",
                            "{letters_black}▎ {letters}loot table.",
                            "",
                            "{warning}➥ Click to paste all"
                    ))
                    .slotConfig(SlotConfig.single(49))
                    .actions(List.of(ClickAction.builder()
                            .clickType(ClickTypeGroup.ANY)
                            .action("commons:loot_paste_all")
                            .build()))
                    .build();
            menuData.getItems().put("paste_all", pasteAllButton);
        }

        MenuAPI.open(player, menuData);
    }

    private static List<ItemData> buildEntryItems(LootEditorSession session) {
        List<ItemData> items = new ArrayList<>();
        for (LootEntry entry : session.getEntries()) {
            items.add(buildEntryItem(entry));
        }
        return items;
    }

    private static ItemData buildEntryItem(LootEntry entry) {
        String material = resolveItemMaterial(entry);

        List<String> lore = new ArrayList<>();
        lore.add("{secondary}Details:");
        lore.add(" {letters_black}▎ {letters}Amount {letters_black}» {info}" + entry.getMinAmount() + " {muted}— {info}" + entry.getMaxAmount());
        lore.add(" {letters_black}▎ {letters}Weight 🎲 {letters_black}» {highlight}" + formatWeight(entry.getWeight()));
        if (entry.getTier() != null && !entry.getTier().isBlank()) {
            lore.add(" {letters_black}▎ {letters}Tier {letters_black}» {info}" + entry.getTier());
        }
        lore.add("");
        lore.add("{success}● {letters}Left Click {letters_black}» Edit");
        lore.add("{error}● {letters}Right Click {letters_black}» Delete");
        lore.add("{warning}● {letters}Shift + Left {letters_black}» Copy");

        return ItemData.builder()
                .rawMaterial(material)
                .rawLore(lore)
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.LEFT)
                                .action("commons:loot_edit " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:loot_delete " + entry.getId())
                                .build(),
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.SHIFT_LEFT)
                                .action("commons:loot_copy " + entry.getId())
                                .build()
                ))
                .build();
    }

    private static String formatWeight(double weight) {
        return weight == Math.floor(weight) ? String.valueOf((int) weight) : String.valueOf(weight);
    }

    private static String resolveItemMaterial(LootEntry entry) {
        if (entry.getItemSnapshot() == null) return "BARRIER";
        String snap = entry.getItemSnapshot();
        if (snap.startsWith("bytes:")) {
            try {
                return ItemSnapshot.from(snap).toItemStack().getType().name();
            } catch (Exception e) {
                return "BARRIER";
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
        return snap.contains(":") ? "BARRIER" : snap.toUpperCase();
    }

    private static List<Integer> buildPaginationSlots() {
        return IntStream.rangeClosed(0, 35).boxed().toList();
    }
}

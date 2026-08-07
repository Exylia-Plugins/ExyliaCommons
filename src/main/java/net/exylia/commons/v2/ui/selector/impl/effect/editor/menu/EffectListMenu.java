package net.exylia.commons.v2.ui.selector.impl.effect.editor.menu;

import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.effect.model.EffectType;
import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.model.NavigationData;
import net.exylia.commons.v2.ui.selector.impl.effect.EffectClipboard;
import net.exylia.commons.v2.ui.selector.impl.effect.EffectEditorSession;
import net.exylia.commons.v2.ui.selector.impl.effect.EffectListClipboard;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;

public final class EffectListMenu {
    private static final List<Integer> PAGINATION_SLOTS = IntStream.rangeClosed(0, 35).boxed().toList();

    private EffectListMenu() {}

    public static void open(Player player, EffectEditorSession session) {
        List<ItemData> effectItems = new ArrayList<>();
        for (EffectEntry entry : session.getEntries()) effectItems.add(buildEntryItem(entry));

        ItemData filler = ItemData.builder().rawMaterial("GRAY_STAINED_GLASS_PANE").rawDisplayName(" ")
                .hideTooltip(true).slotConfig(SlotConfig.single(0)).build();
        ItemData pageFiller = ItemData.builder().rawMaterial("LIGHT_GRAY_STAINED_GLASS_PANE").rawDisplayName(" ")
                .hideTooltip(true).slotConfig(SlotConfig.single(0)).build();
        ItemData previous = ItemData.builder().rawMaterial("ARROW").rawDisplayName("{secondary}&l« PREVIOUS")
                .slotConfig(SlotConfig.single(47)).build();
        ItemData next = ItemData.builder().rawMaterial("ARROW").rawDisplayName("{secondary}&lNEXT »")
                .slotConfig(SlotConfig.single(51)).build();

        MenuData data = MenuData.builder()
                .title(session.getTitle())
                .type(MenuType.PAGINATION)
                .size(54)
                .globalFiller(filler)
                .paginationFiller(pageFiller)
                .paginationSlots(PAGINATION_SLOTS)
                .paginationItems(effectItems)
                .clickSounds(List.of("UI_BUTTON_CLICK|1.0|1.5"))
                .paginationNavigation(NavigationData.builder().previousButton(previous).previousButtonSlot(47)
                        .nextButton(next).nextButtonSlot(51).build())
                .items(new LinkedHashMap<>())
                .build();

        data.getItems().put("add", button("EMERALD", "{success}&lADD EFFECT", List.of(
                "{letters_black}▎ {letters}Create a new visual or text effect.", "", "{warning}➥ Click to add"), 45, "commons:effect_add"));
        data.getItems().put("save", button("LIME_DYE", "{success}&lSAVE CHANGES", List.of(
                "{letters_black}▎ {letters}Save this effect sequence.", "", "{warning}➥ Click to save"), 49, "commons:effect_save"));
        data.getItems().put("cancel", button("RED_DYE", "{error}&lCANCEL", List.of(
                "{letters_black}▎ {letters}Discard the current changes.", "", "{warning}➥ Click to cancel"), 53, "commons:effect_cancel"));

        if (EffectClipboard.has(player)) {
            data.getItems().put("paste", button("WRITABLE_BOOK", "{highlight}&lPASTE EFFECT", List.of(
                    "{letters_black}▎ {letters}Add the effect from your clipboard.", "", "{warning}➥ Click to paste"), 46, "commons:effect_paste"));
        }
        data.getItems().put("copy_all", button("BOOK", "{info}&lCOPY ALL", List.of(
                "{letters_black}▎ {letters}Copy this complete sequence.", "", "{warning}➥ Click to copy"), 48, "commons:effect_copy_all"));
        if (EffectListClipboard.has(player)) {
            data.getItems().put("paste_all", button("BOOKSHELF", "{highlight}&lPASTE ALL", List.of(
                    "{letters_black}▎ {letters}Add all effects from the clipboard.", "", "{warning}➥ Click to paste"), 50, "commons:effect_paste_all"));
        }
        MenuAPI.open(player, data);
    }

    private static ItemData button(String material, String name, List<String> lore, int slot, String action) {
        return ItemData.builder().rawMaterial(material).rawDisplayName(name).rawLore(lore)
                .slotConfig(SlotConfig.single(slot)).actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY).action(action).build())).build();
    }

    private static ItemData buildEntryItem(EffectEntry entry) {
        EffectType type = entry.getType() == null ? EffectType.MESSAGE : entry.getType();
        List<String> lore = new ArrayList<>();
        lore.add("{secondary}Details:");
        lore.add(" {letters_black}▎ {letters}Type {letters_black}» {info}" + type.name());
        lore.add(" {letters_black}▎ {letters}" + entry.summary());
        lore.add("");
        lore.add("{warning}➥ Click to edit");
        lore.add("{error}● {letters}Right Click {letters_black}» Delete");
        lore.add("{info}● {letters}Shift + Left {letters_black}» Copy");
        return ItemData.builder().rawMaterial(material(type)).rawDisplayName("{primary}&l" + entry.displayName())
                .rawLore(lore).slotConfig(SlotConfig.single(0))
                .actions(List.of(
                        ClickAction.builder().clickType(ClickTypeGroup.LEFT).action("commons:effect_edit " + entry.getId()).build(),
                        ClickAction.builder().clickType(ClickTypeGroup.RIGHT).action("commons:effect_delete " + entry.getId()).build(),
                        ClickAction.builder().clickType(ClickTypeGroup.SHIFT_LEFT).action("commons:effect_copy " + entry.getId()).build()
                )).build();
    }

    private static String material(EffectType type) {
        return switch (type) {
            case PARTICLE -> "BLAZE_POWDER";
            case SOUND -> "NOTE_BLOCK";
            case POTION -> "POTION";
            case FIREWORK -> "FIREWORK_ROCKET";
            case TITLE -> "NAME_TAG";
            case ACTIONBAR -> "OAK_SIGN";
            case MESSAGE -> "PAPER";
        };
    }
}

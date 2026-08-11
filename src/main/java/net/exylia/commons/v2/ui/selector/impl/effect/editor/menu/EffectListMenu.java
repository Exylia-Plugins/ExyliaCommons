package net.exylia.commons.v2.ui.selector.impl.effect.editor.menu;

import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.effect.model.EffectScope;
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

/** Paginated list of the effects being edited. */
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
                "{letters_black}▎ {letters}Create a new visual, audio",
                "{letters_black}▎ {letters}or text effect.",
                "",
                "{warning}➥ Click to add"), 45, "commons:effect_add"));

        data.getItems().put("save", button("LIME_DYE", "{success}&lSAVE CHANGES", List.of(
                "{letters_black}▎ {letters}Persist every effect configured",
                "{letters_black}▎ {letters}in this list.",
                "",
                "{warning}➥ Click to save"), 52, "commons:effect_save"));

        data.getItems().put("cancel", button("RED_DYE", "{error}&lCANCEL", List.of(
                "{letters_black}▎ {letters}Discard every unsaved change",
                "{letters_black}▎ {letters}and close this menu.",
                "",
                "{warning}➥ Click to cancel"), 53, "commons:effect_cancel"));

        if (EffectClipboard.has(player)) {
            data.getItems().put("paste", button("WRITABLE_BOOK", "{highlight}&lPASTE EFFECT", List.of(
                    "{letters_black}▎ {letters}Add a copy of the effect",
                    "{letters_black}▎ {letters}currently in your clipboard.",
                    "",
                    "{warning}➥ Click to paste"), 46, "commons:effect_paste"));
        }

        if (!session.getEntries().isEmpty()) {
            data.getItems().put("copy_all", button("BOOKSHELF", "{info}&lCOPY ALL EFFECTS", List.of(
                    "{letters_black}▎ {letters}Copy every effect in this",
                    "{letters_black}▎ {letters}list (" + session.getEntries().size() + ") to your clipboard.",
                    "",
                    "{warning}➥ Click to copy all"), 48, "commons:effect_copy_all"));
        }

        if (EffectListClipboard.has(player)) {
            data.getItems().put("paste_all", button("ENCHANTED_BOOK", "{highlight}&lPASTE ALL EFFECTS", List.of(
                    "{letters_black}▎ {letters}Append the " + EffectListClipboard.size(player) + " effect(s) stored",
                    "{letters_black}▎ {letters}in your list clipboard.",
                    "",
                    "{warning}➥ Click to paste all"), 49, "commons:effect_paste_all"));
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
        EffectScope scope = entry.getScope() == null ? EffectScope.PLAYER : entry.getScope();
        String color = colorFor(type);

        List<String> lore = new ArrayList<>();
        lore.add("{secondary}Details:");
        lore.add(" {letters_black}▎ {letters}Type " + iconFor(type) + " {letters_black}» <color:" + color + ">" + type.name());
        lore.add(" {letters_black}▎ {letters}Value {letters_black}» {info}" + entry.summary());
        lore.add(" {letters_black}▎ {letters}Chance 🎲 {letters_black}» {highlight}" + decimal(entry.getChance()) + "%");
        lore.add(" {letters_black}▎ {letters}Scope ◎ {letters_black}» {info}" + scope.name()
                + (scope == EffectScope.RADIUS ? " {letters_black}(" + decimal(entry.getRadius()) + "b)" : ""));
        if (entry.getDelayTicks() > 0) {
            lore.add(" {letters_black}▎ {letters}Delay ⏱ {letters_black}» {info}" + formatDelay(entry.getDelayTicks()));
        }
        if (entry.getPriority() != 0) {
            lore.add(" {letters_black}▎ {letters}Priority {letters_black}» {info}" + entry.getPriority());
        }
        if (entry.getCondition() != null && !entry.getCondition().isBlank()) {
            lore.add(" {letters_black}▎ {letters}Condition {letters_black}» {info}" + entry.getCondition());
        }
        if (entry.getPermission() != null && !entry.getPermission().isBlank()) {
            lore.add(" {letters_black}▎ {letters}Permission 🔒 {letters_black}» {info}" + entry.getPermission());
        }
        lore.add("");
        lore.add("{success}● {letters}Left Click {letters_black}» Edit");
        lore.add("{error}● {letters}Right Click {letters_black}» Delete");
        lore.add("{warning}● {letters}Shift + Left {letters_black}» Copy");

        return ItemData.builder()
                .rawMaterial(entry.resolvedIconMaterial())
                .rawDisplayName("<color:" + color + ">&l" + entry.displayName())
                .rawLore(lore)
                .actions(List.of(
                        ClickAction.builder().clickType(ClickTypeGroup.LEFT)
                                .action("commons:effect_edit " + entry.getId()).build(),
                        ClickAction.builder().clickType(ClickTypeGroup.RIGHT)
                                .action("commons:effect_delete " + entry.getId()).build(),
                        ClickAction.builder().clickType(ClickTypeGroup.SHIFT_LEFT)
                                .action("commons:effect_copy " + entry.getId()).build()))
                .build();
    }

    private static String iconFor(EffectType type) {
        return switch (type) {
            case PARTICLE -> "✦";
            case SOUND -> "♪";
            case POTION -> "⚗";
            case FIREWORK -> "✷";
            case TITLE -> "✉";
            case ACTIONBAR -> "▬";
            case MESSAGE -> "✎";
            case SEQUENCE -> "❈";
        };
    }

    private static String colorFor(EffectType type) {
        return switch (type) {
            case PARTICLE -> "#b48fd9";
            case SOUND -> "#83d8ff";
            case POTION -> "#8fffc1";
            case FIREWORK -> "#ff6b9d";
            case TITLE -> "#ffd700";
            case ACTIONBAR -> "#f5a86c";
            case MESSAGE -> "#e7cfff";
            case SEQUENCE -> "#aa76de";
        };
    }

    private static String decimal(double value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value);
    }

    private static String formatDelay(long ticks) {
        if (ticks % 20 == 0) return (ticks / 20) + "s";
        return ticks + " ticks";
    }
}

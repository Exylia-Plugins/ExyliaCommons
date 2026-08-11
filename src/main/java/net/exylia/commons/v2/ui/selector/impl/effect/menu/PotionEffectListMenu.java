package net.exylia.commons.v2.ui.selector.impl.effect.menu;

import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.model.NavigationData;
import net.exylia.commons.v2.ui.selector.impl.effect.PotionEffectColors;
import net.exylia.commons.v2.ui.selector.impl.effect.PotionEffectEditorSession;
import net.exylia.commons.v2.ui.selector.impl.effect.PotionEffectResult;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;

public final class PotionEffectListMenu {

    private static final List<Integer> PAGINATION_SLOTS = buildPaginationSlots();

    private PotionEffectListMenu() {}

    public static void open(Player player, PotionEffectEditorSession session) {
        List<ItemData> effectItems = buildEffectItems(session);

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
                .rawDisplayName("{success}&lADD EFFECT")
                .rawLore(List.of(
                        "{letters_black}▎ {letters}Add a new potion effect",
                        "{letters_black}▎ {letters}to this list.",
                        "",
                        "{warning}➥ Click to add"
                ))
                .slotConfig(SlotConfig.single(45))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:potion_add")
                        .build()))
                .build();

        ItemData saveButton = ItemData.builder()
                .rawMaterial("LIME_DYE")
                .rawDisplayName("{success}&lSAVE CHANGES")
                .glowing(true)
                .rawLore(List.of(
                        "{letters_black}▎ {letters}Persist every effect configured",
                        "{letters_black}▎ {letters}in this list.",
                        "",
                        "{warning}➥ Click to save"
                ))
                .slotConfig(SlotConfig.single(52))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:potion_save")
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
                        .action("commons:potion_cancel")
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
                .paginationItems(effectItems)
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

        MenuAPI.open(player, menuData);
    }

    private static List<ItemData> buildEffectItems(PotionEffectEditorSession session) {
        List<ItemData> items = new ArrayList<>();
        List<PotionEffectResult> effects = session.getEffects();
        for (int i = 0; i < effects.size(); i++) {
            items.add(buildEffectItem(effects.get(i), i));
        }
        return items;
    }

    private static ItemData buildEffectItem(PotionEffectResult result, int index) {
        String color = PotionEffectColors.colorFor(result.effectType());
        String name = "<color:" + color + ">&l" + result.formattedTypeName();

        List<String> lore = new ArrayList<>();
        lore.add("{secondary}Details:");
        lore.add(" {letters_black}▎ {letters}Level {letters_black}» {highlight}" + result.level());
        lore.add(" {letters_black}▎ {letters}Duration ⏱ {letters_black}» {info}" + formatDuration(result.durationSeconds()));
        lore.add("");
        lore.add("{error}● {letters}Right Click {letters_black}» Remove");

        return ItemData.builder()
                .rawMaterial("POTION")
                .rawDisplayName(name)
                .rawLore(lore)
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:potion_delete " + index)
                                .build()
                ))
                .build();
    }

    private static String formatDuration(int seconds) {
        if (seconds < 0) return "∞ Infinite";
        if (seconds == 0) return "Instant";
        if (seconds < 60) return seconds + "s";
        int mins = seconds / 60;
        int secs = seconds % 60;
        return secs > 0 ? mins + "m " + secs + "s" : mins + "m";
    }

    private static List<Integer> buildPaginationSlots() {
        return IntStream.rangeClosed(0, 35).boxed().toList();
    }
}

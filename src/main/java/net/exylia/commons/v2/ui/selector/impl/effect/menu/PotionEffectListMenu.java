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
                .rawDisplayName("<#8fffc1><bold>ADD EFFECT")
                .rawLore(List.of("&7Click to add a new potion effect"))
                .slotConfig(SlotConfig.single(45))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:effect_add")
                        .build()))
                .build();

        ItemData saveButton = ItemData.builder()
                .rawMaterial("LIME_DYE")
                .rawDisplayName("<#8fffc1><bold>SAVE")
                .glowing(true)
                .rawLore(List.of("&7Click to save all effects"))
                .slotConfig(SlotConfig.single(52))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:effect_save")
                        .build()))
                .build();

        ItemData cancelButton = ItemData.builder()
                .rawMaterial("RED_DYE")
                .rawDisplayName("<#a33b53><bold>CANCEL")
                .rawLore(List.of("&7Click to cancel without saving"))
                .slotConfig(SlotConfig.single(53))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:effect_cancel")
                        .build()))
                .build();

        ItemData prevButton = ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("&7Previous Page")
                .slotConfig(SlotConfig.single(47))
                .build();

        ItemData nextButton = ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("&7Next Page")
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
        String name = "<color:" + color + ">" + result.formattedTypeName();

        List<String> lore = new ArrayList<>();
        lore.add("&7Level: &f" + result.level());
        lore.add("&7Duration: &f" + formatDuration(result.durationSeconds()));
        lore.add("");
        lore.add("&cRight Click &7» Remove");

        return ItemData.builder()
                .rawMaterial("POTION")
                .rawDisplayName(name)
                .rawLore(lore)
                .actions(List.of(
                        ClickAction.builder()
                                .clickType(ClickTypeGroup.RIGHT)
                                .action("commons:effect_delete " + index)
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

package net.exylia.commons.v2.ui.selector.impl.effect.editor.menu;

import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.selector.impl.effect.EffectEditorSession;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;

/** Type picker shown when adding a new effect to the list. */
public final class EffectTypeSelectMenu {

    private EffectTypeSelectMenu() {}

    public static void open(Player player, EffectEditorSession session) {
        LinkedHashMap<String, ItemData> items = new LinkedHashMap<>();

        items.put("particle", option("BLAZE_POWDER", "{primary}&lPARTICLE ✦", 10, "PARTICLE", List.of(
                " {letters_black}▎ {letters}Spawns a particle at the",
                " {letters_black}▎ {letters}effect location.")));

        items.put("sound", option("NOTE_BLOCK", "{primary}&lSOUND ♪", 11, "SOUND", List.of(
                " {letters_black}▎ {letters}Plays a sound with custom",
                " {letters_black}▎ {letters}volume and pitch.")));

        items.put("potion", option("POTION", "{primary}&lPOTION ⚗", 12, "POTION", List.of(
                " {letters_black}▎ {letters}Applies a potion effect to",
                " {letters_black}▎ {letters}the receiving player.")));

        items.put("firework", option("FIREWORK_ROCKET", "{primary}&lFIREWORK ✷", 13, "FIREWORK", List.of(
                " {letters_black}▎ {letters}Launches a firework with",
                " {letters_black}▎ {letters}configurable colors and shape.")));

        items.put("title", option("NAME_TAG", "{primary}&lTITLE ✉", 14, "TITLE", List.of(
                " {letters_black}▎ {letters}Shows a title and subtitle",
                " {letters_black}▎ {letters}on screen.")));

        items.put("actionbar", option("OAK_SIGN", "{primary}&lACTIONBAR ▬", 15, "ACTIONBAR", List.of(
                " {letters_black}▎ {letters}Shows a short line above",
                " {letters_black}▎ {letters}the hotbar.")));

        items.put("message", option("PAPER", "{primary}&lMESSAGE ✎", 16, "MESSAGE", List.of(
                " {letters_black}▎ {letters}Sends a chat message to",
                " {letters_black}▎ {letters}the receiving player.")));

        items.put("sequence", option("END_CRYSTAL", "{primary}&lSEQUENCE ❈", 22, "SEQUENCE", List.of(
                " {letters_black}▎ {letters}A choreographed list of steps",
                " {letters_black}▎ {letters}with shapes and delays.",
                " {letters_black}▎ {muted}Uses the sequence engine tokens.")));

        items.put("back", ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("{secondary}&l« BACK")
                .rawLore(List.of(
                        " {letters_black}▎ {letters}Return to the effect list.",
                        "",
                        "{warning}➥ Click to go back"))
                .slotConfig(SlotConfig.single(31))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:effect_list")
                        .build()))
                .build());

        MenuAPI.open(player, MenuData.builder()
                .title("{primary}&lSELECT EFFECT TYPE")
                .type(MenuType.SIMPLE)
                .size(36)
                .globalFiller(ItemData.builder()
                        .rawMaterial("GRAY_STAINED_GLASS_PANE")
                        .rawDisplayName(" ")
                        .hideTooltip(true)
                        .build())
                .items(items)
                .clickSounds(List.of("UI_BUTTON_CLICK|1.0|1.5"))
                .build());
    }

    private static ItemData option(String material, String name, int slot, String type, List<String> description) {
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("{secondary}Details:");
        lore.addAll(description);
        lore.add("");
        lore.add("{warning}➥ Click to select");

        return ItemData.builder()
                .rawMaterial(material)
                .rawDisplayName(name)
                .rawLore(lore)
                .slotConfig(SlotConfig.single(slot))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:effect_add_type " + type)
                        .build()))
                .build();
    }
}

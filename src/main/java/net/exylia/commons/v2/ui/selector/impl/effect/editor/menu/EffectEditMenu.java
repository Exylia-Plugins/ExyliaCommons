package net.exylia.commons.v2.ui.selector.impl.effect.editor.menu;

import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.effect.model.EffectScope;
import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Per-effect editor. Row 2 holds the type-specific payload, row 3 the shared gating fields
 * (chance, condition, permission, priority, delay, scope).
 */
public final class EffectEditMenu {

    private EffectEditMenu() {}

    public static void open(Player player, EffectEntry entry) {
        LinkedHashMap<String, ItemData> items = new LinkedHashMap<>();

        items.put("display", display(entry));
        items.put("value", value(entry));

        int slot = 20;
        for (ItemData extra : typeSpecific(entry)) {
            items.put("extra_" + slot, reslot(extra, slot));
            slot++;
        }

        items.put("chance", button(entry, "SUNFLOWER", "{highlight}&lCHANCE 🎲", 28,
                List.of(
                        " {letters_black}▎ {letters}Probability that this effect",
                        " {letters_black}▎ {letters}plays when triggered.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {highlight}" + decimal(entry.getChance()) + "%"),
                "effect_set_chance", null));

        items.put("condition", button(entry, "COMPARATOR", "{info}&lCONDITION", 29,
                List.of(
                        " {letters_black}▎ {letters}Expression that must be true",
                        " {letters_black}▎ {letters}for this effect to play.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + orNone(entry.getCondition())),
                "effect_set_condition", "effect_clear_condition"));

        items.put("permission", button(entry, "NAME_TAG", "{success}&lPERMISSION 🔒", 30,
                List.of(
                        " {letters_black}▎ {letters}Permission node required by",
                        " {letters_black}▎ {letters}the receiving player.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + orNone(entry.getPermission())),
                "effect_set_permission", "effect_clear_permission"));

        items.put("priority", button(entry, "REPEATER", "{secondary_light}&lPRIORITY", 31,
                List.of(
                        " {letters_black}▎ {letters}Execution order relative to",
                        " {letters_black}▎ {letters}other effects. Higher goes first.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + entry.getPriority()),
                "effect_set_priority", null));

        items.put("delay", button(entry, "CLOCK", "{warning}&lDELAY ⏱", 32,
                List.of(
                        " {letters_black}▎ {letters}Ticks to wait before this",
                        " {letters_black}▎ {letters}effect plays. 20 ticks = 1s.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + formatDelay(entry.getDelayTicks())),
                "effect_set_delay", null));

        items.put("scope", scope(entry));

        items.put("name", button(entry, "OAK_HANGING_SIGN", "{highlight}&lDISPLAY NAME", 33,
                List.of(
                        " {letters_black}▎ {letters}Shown in editor menus instead",
                        " {letters_black}▎ {letters}of the raw value.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + orNone(entry.getName())),
                "effect_set_name", "effect_clear_name"));

        items.put("icon", button(entry, entry.resolvedIconMaterial(), "{secondary_light}&lPREVIEW ICON 🖼", 34,
                List.of(
                        " {letters_black}▎ {letters}Item shown for this effect",
                        " {letters_black}▎ {letters}in editor and preview menus.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + (entry.hasIcon() ? "Custom" : "Default")),
                "effect_set_icon", "effect_clear_icon"));

        items.put("preview", ItemData.builder()
                .rawMaterial("ENDER_EYE")
                .rawDisplayName("{info}&lPREVIEW ✦")
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}Play this effect on yourself,",
                        " {letters_black}▎ {letters}ignoring chance and conditions.",
                        "",
                        "{warning}➥ Click to preview"))
                .slotConfig(SlotConfig.single(40))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:effect_preview " + entry.getId())
                        .build()))
                .build());

        items.put("delete", ItemData.builder()
                .rawMaterial("TNT")
                .rawDisplayName("{error}&lDELETE EFFECT")
                .rawLore(List.of(
                        " {letters_black}▎ {letters}Permanently remove this effect",
                        " {letters_black}▎ {letters}from the list.",
                        "",
                        "{warning}➥ Click to delete"))
                .slotConfig(SlotConfig.single(44))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:effect_delete " + entry.getId())
                        .build()))
                .build());

        items.put("back", ItemData.builder()
                .rawMaterial("ARROW")
                .rawDisplayName("{secondary}&l« BACK")
                .rawLore(List.of(
                        " {letters_black}▎ {letters}Return to the effect list.",
                        "",
                        "{warning}➥ Click to go back"))
                .slotConfig(SlotConfig.single(36))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:effect_list")
                        .build()))
                .build());

        MenuAPI.open(player, MenuData.builder()
                .title("{primary}&lEDIT EFFECT")
                .type(MenuType.SIMPLE)
                .size(45)
                .globalFiller(ItemData.builder()
                        .rawMaterial("GRAY_STAINED_GLASS_PANE")
                        .rawDisplayName(" ")
                        .hideTooltip(true)
                        .build())
                .items(items)
                .clickSounds(List.of("UI_BUTTON_CLICK|1.0|1.5"))
                .build());
    }

    // ----------------------------------------------------------------- pieces

    private static ItemData display(EffectEntry entry) {
        return ItemData.builder()
                .rawMaterial(entry.resolvedIconMaterial())
                .rawDisplayName("{primary}&l" + entry.displayName())
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}ID {letters_black}» {muted}" + shortId(entry.getId()),
                        " {letters_black}▎ {letters}Type {letters_black}» {info}" + entry.getType().name(),
                        " {letters_black}▎ {letters}Value {letters_black}» {info}" + entry.summary()))
                .slotConfig(SlotConfig.single(4))
                .build();
    }

    private static ItemData value(EffectEntry entry) {
        String label = switch (entry.getType()) {
            case PARTICLE -> "{primary}&lPARTICLE ✦";
            case SOUND -> "{primary}&lSOUND ♪";
            case POTION -> "{primary}&lPOTION ⚗";
            case FIREWORK -> "{primary}&lFIREWORK SHAPE ✷";
            case TITLE -> "{primary}&lTITLE TEXT ✉";
            case ACTIONBAR -> "{primary}&lACTIONBAR TEXT ▬";
            case MESSAGE -> "{primary}&lMESSAGE TEXT ✎";
            case SEQUENCE -> "{primary}&lSEQUENCE STEPS ❈";
        };

        String current = switch (entry.getType()) {
            case PARTICLE -> orNone(entry.getParticle());
            case SOUND -> orNone(entry.getSound());
            case POTION -> orNone(entry.getPotion());
            case FIREWORK -> orNone(entry.getFireworkType());
            case TITLE -> orNone(entry.getTitle());
            case ACTIONBAR -> orNone(entry.getActionbar());
            case MESSAGE -> orNone(entry.getMessage());
            case SEQUENCE -> entry.getSequence().size() + " step(s)";
        };

        return ItemData.builder()
                .rawMaterial(entry.resolvedIconMaterial())
                .rawDisplayName(label)
                .rawLore(List.of(
                        "{secondary}Details:",
                        " {letters_black}▎ {letters}The payload played by this effect.",
                        "",
                        " {letters_black}▎ {letters}Current {letters_black}» {info}" + current,
                        "",
                        "{warning}➥ Click to change"))
                .slotConfig(SlotConfig.single(19))
                .actions(List.of(ClickAction.builder()
                        .clickType(ClickTypeGroup.ANY)
                        .action("commons:effect_set_value " + entry.getId())
                        .build()))
                .build();
    }

    private static ItemData scope(EffectEntry entry) {
        EffectScope current = entry.getScope() == null ? EffectScope.PLAYER : entry.getScope();

        List<String> lore = new ArrayList<>(List.of(
                "{secondary}Details:",
                " {letters_black}▎ {letters}Who receives this effect.",
                "",
                " {letters_black}▎ {letters}PLAYER {letters_black}» {muted}only the trigger player",
                " {letters_black}▎ {letters}NEARBY {letters_black}» {muted}players around them",
                " {letters_black}▎ {letters}LOCATION {letters_black}» {muted}everyone who can see it",
                " {letters_black}▎ {letters}RADIUS {letters_black}» {muted}within a set distance",
                " {letters_black}▎ {letters}GLOBAL {letters_black}» {muted}every online player",
                "",
                " {letters_black}▎ {letters}Current {letters_black}» {highlight}" + current.name()));

        if (current == EffectScope.RADIUS) {
            lore.add(" {letters_black}▎ {letters}Radius {letters_black}» {info}" + decimal(entry.getRadius()) + " blocks");
        }
        lore.add("");
        lore.add("{success}● {letters}Left Click {letters_black}» Cycle");
        if (current == EffectScope.RADIUS) {
            lore.add("{info}● {letters}Right Click {letters_black}» Set radius");
        }

        List<ClickAction> actions = new ArrayList<>();
        actions.add(ClickAction.builder()
                .clickType(ClickTypeGroup.LEFT)
                .action("commons:effect_cycle_scope " + entry.getId())
                .build());
        if (current == EffectScope.RADIUS) {
            actions.add(ClickAction.builder()
                    .clickType(ClickTypeGroup.RIGHT)
                    .action("commons:effect_set_radius " + entry.getId())
                    .build());
        }

        return ItemData.builder()
                .rawMaterial("SPYGLASS")
                .rawDisplayName("{info}&lSCOPE ◎")
                .rawLore(lore)
                .slotConfig(SlotConfig.single(25))
                .actions(actions)
                .build();
    }

    /** Extra buttons that only make sense for the entry's type. Slots are assigned by the caller. */
    private static List<ItemData> typeSpecific(EffectEntry entry) {
        return switch (entry.getType()) {
            case PARTICLE -> List.of(
                    button(entry, "HOPPER", "{secondary_light}&lCOUNT", 0,
                            List.of(
                                    " {letters_black}▎ {letters}How many particles spawn.",
                                    "",
                                    " {letters_black}▎ {letters}Current {letters_black}» {highlight}" + entry.getParticleCount()),
                            "effect_set_count", null),
                    button(entry, "PISTON", "{secondary_light}&lOFFSET", 0,
                            List.of(
                                    " {letters_black}▎ {letters}Random spread on each axis.",
                                    "",
                                    " {letters_black}▎ {letters}Current {letters_black}» {info}"
                                            + decimal(entry.getOffsetX()) + " "
                                            + decimal(entry.getOffsetY()) + " "
                                            + decimal(entry.getOffsetZ())),
                            "effect_set_offset", null),
                    button(entry, "RED_DYE", "{accent}&lCOLOR 🎨", 0,
                            List.of(
                                    " {letters_black}▎ {letters}Tint for DUST particles.",
                                    " {letters_black}▎ {muted}Required when using DUST.",
                                    "",
                                    " {letters_black}▎ {letters}Current {letters_black}» {info}" + orNone(entry.getParticleColor())),
                            "effect_set_color", null));

            case SOUND -> List.of(
                    button(entry, "BELL", "{secondary_light}&lVOLUME", 0,
                            List.of(
                                    " {letters_black}▎ {letters}How loud the sound is, and",
                                    " {letters_black}▎ {letters}how far it carries.",
                                    "",
                                    " {letters_black}▎ {letters}Current {letters_black}» {highlight}" + decimal(entry.getSoundVolume())),
                            "effect_set_volume", null),
                    button(entry, "NOTE_BLOCK", "{secondary_light}&lPITCH", 0,
                            List.of(
                                    " {letters_black}▎ {letters}Playback speed, 0.5 to 2.0.",
                                    "",
                                    " {letters_black}▎ {letters}Current {letters_black}» {highlight}" + decimal(entry.getSoundPitch())),
                            "effect_set_pitch", null));

            case POTION -> List.of(
                    button(entry, "GLOWSTONE_DUST", "{secondary_light}&lAMPLIFIER", 0,
                            List.of(
                                    " {letters_black}▎ {letters}Effect strength. 0 is level I.",
                                    "",
                                    " {letters_black}▎ {letters}Current {letters_black}» {highlight}" + (entry.getPotionAmplifier() + 1)),
                            "effect_set_amplifier", null),
                    button(entry, "CLOCK", "{secondary_light}&lDURATION ⏱", 0,
                            List.of(
                                    " {letters_black}▎ {letters}How long the potion lasts.",
                                    "",
                                    " {letters_black}▎ {letters}Current {letters_black}» {info}" + (entry.getPotionDurationTicks() / 20) + "s"),
                            "effect_set_duration", null));

            case TITLE -> List.of(
                    button(entry, "PAPER", "{secondary_light}&lSUBTITLE", 0,
                            List.of(
                                    " {letters_black}▎ {letters}Smaller line under the title.",
                                    "",
                                    " {letters_black}▎ {letters}Current {letters_black}» {info}" + orNone(entry.getSubtitle())),
                            "effect_set_subtitle", null),
                    button(entry, "CLOCK", "{secondary_light}&lTIMES ⏱", 0,
                            List.of(
                                    " {letters_black}▎ {letters}Fade in, stay and fade out,",
                                    " {letters_black}▎ {letters}measured in ticks.",
                                    "",
                                    " {letters_black}▎ {letters}Current {letters_black}» {info}"
                                            + entry.getTitleFadeIn() + " / " + entry.getTitleStay() + " / " + entry.getTitleFadeOut()),
                            "effect_set_times", null));

            case MESSAGE -> List.of(
                    button(entry, entry.isCentered() ? "LIME_DYE" : "GRAY_DYE",
                            "{secondary_light}&lCENTERED", 0,
                            List.of(
                                    " {letters_black}▎ {letters}Center the message in chat.",
                                    "",
                                    " {letters_black}▎ {letters}Current {letters_black}» "
                                            + (entry.isCentered() ? "{success}Enabled" : "{muted}Disabled")),
                            "effect_toggle_centered", null));

            case SEQUENCE -> List.of(
                    button(entry, "EMERALD", "{success}&lADD STEP", 0,
                            List.of(
                                    " {letters_black}▎ {letters}Append a sequence token,",
                                    " {letters_black}▎ {muted}e.g. [CIRCLE] FLAME;radius:1.2",
                                    "",
                                    " {letters_black}▎ {letters}Steps {letters_black}» {highlight}" + entry.getSequence().size()),
                            "effect_add_step", null),
                    button(entry, "BARRIER", "{error}&lCLEAR STEPS", 0,
                            List.of(
                                    " {letters_black}▎ {letters}Remove every step from",
                                    " {letters_black}▎ {letters}this sequence."),
                            "effect_clear_steps", null));

            case ACTIONBAR, FIREWORK -> List.of();
        };
    }

    // ---------------------------------------------------------------- helpers

    private static ItemData button(
            EffectEntry entry,
            String material,
            String name,
            int slot,
            List<String> details,
            String primaryAction,
            String secondaryAction
    ) {
        List<String> lore = new ArrayList<>();
        lore.add("{secondary}Details:");
        lore.addAll(details);
        lore.add("");
        if (secondaryAction != null) {
            lore.add("{success}● {letters}Left Click {letters_black}» Change");
            lore.add("{error}● {letters}Right Click {letters_black}» Clear");
        } else {
            lore.add("{warning}➥ Click to change");
        }

        List<ClickAction> actions = new ArrayList<>();
        actions.add(ClickAction.builder()
                .clickType(secondaryAction != null ? ClickTypeGroup.LEFT : ClickTypeGroup.ANY)
                .action("commons:" + primaryAction + " " + entry.getId())
                .build());
        if (secondaryAction != null) {
            actions.add(ClickAction.builder()
                    .clickType(ClickTypeGroup.RIGHT)
                    .action("commons:" + secondaryAction + " " + entry.getId())
                    .build());
        }

        return ItemData.builder()
                .rawMaterial(material)
                .rawDisplayName(name)
                .rawLore(lore)
                .slotConfig(SlotConfig.single(slot))
                .actions(actions)
                .build();
    }

    private static ItemData reslot(ItemData item, int slot) {
        return item.toBuilder().slotConfig(SlotConfig.single(slot)).build();
    }

    private static String orNone(String value) {
        return value != null && !value.isBlank() ? value : "None";
    }

    private static String shortId(String id) {
        return id != null && id.length() > 8 ? id.substring(0, 8) + "..." : String.valueOf(id);
    }

    private static String decimal(double value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value);
    }

    private static String formatDelay(long ticks) {
        if (ticks <= 0) return "Immediate";
        if (ticks % 20 == 0) return (ticks / 20) + "s";
        return ticks + " ticks";
    }
}

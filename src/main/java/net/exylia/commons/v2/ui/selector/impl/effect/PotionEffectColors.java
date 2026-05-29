package net.exylia.commons.v2.ui.selector.impl.effect;

import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public final class PotionEffectColors {

    private PotionEffectColors() {}

    private static final Map<String, String> COLORS = Map.ofEntries(
        Map.entry("minecraft:absorption",          "#2552A5"),
        Map.entry("minecraft:bad_luck",            "#C0A44D"),
        Map.entry("minecraft:bad_omen",            "#0B6138"),
        Map.entry("minecraft:blindness",           "#4A4A52"),
        Map.entry("minecraft:conduit_power",       "#1DC2D1"),
        Map.entry("minecraft:darkness",            "#59534A"),
        Map.entry("minecraft:dolphins_grace",      "#88A3BE"),
        Map.entry("minecraft:fire_resistance",     "#E49A3A"),
        Map.entry("minecraft:glowing",             "#94A061"),
        Map.entry("minecraft:haste",               "#D9C043"),
        Map.entry("minecraft:health_boost",        "#F87D23"),
        Map.entry("minecraft:hero_of_the_village", "#44FF44"),
        Map.entry("minecraft:hunger",              "#587653"),
        Map.entry("minecraft:infested",            "#909090"),
        Map.entry("minecraft:instant_damage",      "#8B2020"),
        Map.entry("minecraft:instant_health",      "#F82423"),
        Map.entry("minecraft:invisibility",        "#7F8392"),
        Map.entry("minecraft:jump_boost",          "#786297"),
        Map.entry("minecraft:levitation",          "#CEFFFF"),
        Map.entry("minecraft:luck",                "#339900"),
        Map.entry("minecraft:mining_fatigue",      "#8A7B30"),
        Map.entry("minecraft:nausea",              "#7A3A6A"),
        Map.entry("minecraft:night_vision",        "#1F1FA1"),
        Map.entry("minecraft:oozing",              "#95A11B"),
        Map.entry("minecraft:poison",              "#4E9331"),
        Map.entry("minecraft:raid_omen",           "#0B6138"),
        Map.entry("minecraft:regeneration",        "#CD5CAB"),
        Map.entry("minecraft:resistance",          "#99453A"),
        Map.entry("minecraft:saturation",          "#F82423"),
        Map.entry("minecraft:slow_falling",        "#C8CAA8"),
        Map.entry("minecraft:slowness",            "#5A6C81"),
        Map.entry("minecraft:speed",               "#7CAFC6"),
        Map.entry("minecraft:strength",            "#932423"),
        Map.entry("minecraft:trial_omen",          "#8D3F3F"),
        Map.entry("minecraft:water_breathing",     "#2E5299"),
        Map.entry("minecraft:weakness",            "#787878"),
        Map.entry("minecraft:weaving",             "#6A5880"),
        Map.entry("minecraft:wind_charged",        "#8EBFDF"),
        Map.entry("minecraft:wither",              "#6A5852")
    );

    public static String colorFor(PotionEffectType type) {
        return COLORS.getOrDefault(type.getKey().asString(), "#FFFFFF");
    }
}

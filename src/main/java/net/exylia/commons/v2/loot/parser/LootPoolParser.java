package net.exylia.commons.v2.loot.parser;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.items.snapshot.ItemSnapshot;
import net.exylia.commons.v2.loot.model.LootEntry;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses compact loot definitions written as {@code "MATERIAL MIN MAX WEIGHT [TIER]"} into
 * {@link LootEntry} instances.
 *
 * <p>The material token also accepts potion prefixes: {@code SPLASH:TYPE},
 * {@code LINGERING:TYPE}, {@code TIPPED:TYPE} and {@code POTION:TYPE}.
 */
public final class LootPoolParser {

    private static final Map<String, String> POTION_ALIASES = Map.of(
            "SPEED", "SWIFTNESS",
            "SWIFTNESS", "SPEED",
            "INSTANT_HEAL", "HEALING",
            "HEALING", "INSTANT_HEAL",
            "INSTANT_DAMAGE", "HARMING",
            "HARMING", "INSTANT_DAMAGE",
            "JUMP", "LEAPING",
            "LEAPING", "JUMP",
            "REGEN", "REGENERATION"
    );

    private LootPoolParser() {}

    public static List<LootEntry> parseAll(List<String> definitions) {
        if (definitions == null || definitions.isEmpty()) return List.of();
        List<LootEntry> entries = new ArrayList<>(definitions.size());
        for (String definition : definitions) {
            LootEntry entry = parse(definition);
            if (entry != null) entries.add(entry);
        }
        return entries;
    }

    public static LootEntry parse(String definition) {
        if (definition == null || definition.isBlank()) return null;

        String[] parts = definition.trim().split("\\s+", 5);
        if (parts.length < 4) {
            DebugAPI.logLibWarn("LootPoolParser: invalid entry \"" + definition + "\" (expected at least 4 tokens) — skipping");
            return null;
        }

        int min, max;
        double weight;
        try {
            min = Integer.parseInt(parts[1]);
            max = Integer.parseInt(parts[2]);
            weight = Double.parseDouble(parts[3]);
        } catch (NumberFormatException e) {
            DebugAPI.logLibWarn("LootPoolParser: non-numeric values in \"" + definition + "\" — skipping");
            return null;
        }

        if (min <= 0 || max < min || weight <= 0) {
            DebugAPI.logLibWarn("LootPoolParser: invalid numeric values in \"" + definition + "\" — skipping");
            return null;
        }

        ItemStack item = resolveItem(parts[0].toUpperCase(), definition);
        if (item == null) return null;

        return LootEntry.builder()
                .itemSnapshot(ItemSnapshot.from(item).serialize())
                .minAmount(min)
                .maxAmount(max)
                .weight(weight)
                .tier(parts.length == 5 ? parts[4].toUpperCase() : null)
                .build();
    }

    private static ItemStack resolveItem(String key, String definition) {
        int separator = key.indexOf(':');
        if (separator > 0) {
            String prefix = key.substring(0, separator);
            Material container = potionContainer(prefix);
            if (container == null) {
                DebugAPI.logLibWarn("LootPoolParser: unknown prefix \"" + prefix + "\" in \"" + definition + "\" — skipping");
                return null;
            }
            PotionType type = resolvePotionType(key.substring(separator + 1));
            if (type == null) {
                DebugAPI.logLibWarn("LootPoolParser: unknown potion type in \"" + definition + "\" — skipping");
                return null;
            }
            return buildPotion(container, type);
        }

        Material material = Material.matchMaterial(key);
        if (material == null || !material.isItem()) {
            DebugAPI.logLibWarn("LootPoolParser: unknown material \"" + key + "\" in \"" + definition + "\" — skipping");
            return null;
        }
        return new ItemStack(material);
    }

    private static Material potionContainer(String prefix) {
        return switch (prefix) {
            case "POTION" -> Material.POTION;
            case "SPLASH" -> Material.SPLASH_POTION;
            case "LINGERING" -> Material.LINGERING_POTION;
            case "TIPPED" -> Material.TIPPED_ARROW;
            default -> null;
        };
    }

    private static ItemStack buildPotion(Material container, PotionType type) {
        ItemStack item = new ItemStack(container);
        if (item.getItemMeta() instanceof PotionMeta meta) {
            meta.setBasePotionType(type);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static PotionType resolvePotionType(String name) {
        PotionType type = safePotionType(name);
        if (type != null) return type;

        String alias = POTION_ALIASES.get(name);
        return alias != null ? safePotionType(alias) : null;
    }

    private static PotionType safePotionType(String name) {
        try {
            return PotionType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

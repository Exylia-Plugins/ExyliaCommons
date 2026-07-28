package net.exylia.commons.v2.loot.util;

import net.exylia.commons.v2.loot.model.LootEntry;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LootPicker {

    private LootPicker() {}

    /**
     * Rolls each entry independently against its weight treated as a 0-100 percentage chance.
     * Returns every entry that "hit" its roll. If none hit, forces one random entry so the
     * caller never ends up with an empty result (matches legacy loot-chest behavior).
     */
    public static List<ItemStack> rollIndependent(List<LootEntry> entries) {
        return rollIndependent(entries, true);
    }

    /**
     * Rolls each entry independently against its weight treated as a 0-100 percentage chance.
     * When forceOneIfEmpty is false, an empty result is possible (matches legacy item-spawner
     * behavior, where a spawn tick can legitimately produce nothing).
     */
    public static List<ItemStack> rollIndependent(List<LootEntry> entries, boolean forceOneIfEmpty) {
        if (entries.isEmpty()) return new ArrayList<>();
        List<ItemStack> result = new ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (LootEntry entry : entries) {
            if (rng.nextDouble(100.0) < entry.getWeight()) {
                ItemStack item = entry.toItemStack();
                if (item != null) result.add(item);
            }
        }

        if (result.isEmpty() && forceOneIfEmpty) {
            LootEntry forced = entries.get(rng.nextInt(entries.size()));
            ItemStack item = forced.toItemStack();
            if (item != null) result.add(item);
        }

        Collections.shuffle(result);
        return result;
    }

    /**
     * Picks exactly one entry using cumulative weighted distribution (each entry's weight is
     * its relative share of the total). Returns null if the list is empty or every entry has
     * a resolvable item.
     */
    public static ItemStack pickOne(List<LootEntry> entries) {
        if (entries.isEmpty()) return null;
        double total = entries.stream().mapToDouble(LootEntry::getWeight).sum();
        if (total <= 0) return null;

        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cumulative = 0;
        for (LootEntry entry : entries) {
            cumulative += entry.getWeight();
            if (roll < cumulative) return entry.toItemStack();
        }
        return entries.get(entries.size() - 1).toItemStack();
    }
}

package net.exylia.commons.v2.loot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.exylia.commons.v2.items.snapshot.ItemSnapshot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LootEntry {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String itemSnapshot;

    @Builder.Default
    private int minAmount = 1;

    @Builder.Default
    private int maxAmount = 1;

    @Builder.Default
    private double weight = 50.0;

    private String tier;

    public static LootEntry of(ItemStack item) {
        return LootEntry.builder()
                .itemSnapshot(ItemSnapshot.from(item).serialize())
                .build();
    }

    public ItemStack toItemStack() {
        if (itemSnapshot == null) return null;
        ItemStack item = ItemSnapshot.from(itemSnapshot).toItemStack();
        item.setAmount(rollAmount());
        return item;
    }

    public int rollAmount() {
        if (minAmount >= maxAmount) return minAmount;
        return ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
    }

    public String getDisplayName() {
        return itemSnapshot != null ? ItemSnapshot.from(itemSnapshot).toDisplayLabel() : "(not set)";
    }

    public LootEntry copy() {
        return LootEntry.builder()
                .id(UUID.randomUUID().toString())
                .itemSnapshot(itemSnapshot)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .weight(weight)
                .tier(tier)
                .build();
    }
}

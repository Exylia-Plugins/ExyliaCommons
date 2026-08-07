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

    /** Defaults to ITEM for full backwards compatibility with every pre-existing loot table
     * (item-spawners, loot-chests, event configs) that never set this field. */
    @Builder.Default
    private LootEntryType type = LootEntryType.ITEM;

    /** Used when {@link #type} is {@link LootEntryType#ITEM}. */
    private String itemSnapshot;

    @Builder.Default
    private int minAmount = 1;

    @Builder.Default
    private int maxAmount = 1;

    /** Used when {@link #type} is {@link LootEntryType#COMMAND}. Executed as the console with
     * %player% (and any other registered placeholders) resolved against the recipient. */
    private String command;

    @Builder.Default
    private double weight = 50.0;

    private String tier;

    public static LootEntry of(ItemStack item) {
        return LootEntry.builder()
                .type(LootEntryType.ITEM)
                .itemSnapshot(ItemSnapshot.from(item).serialize())
                .build();
    }

    public static LootEntry ofCommand(String command) {
        return LootEntry.builder()
                .type(LootEntryType.COMMAND)
                .command(command)
                .build();
    }

    /** Defensively defaults to ITEM: entries persisted before command-entry support existed, or
     * deserialized via GSON's no-constructor path, may have a null type. */
    private LootEntryType safeType() {
        return type != null ? type : LootEntryType.ITEM;
    }

    public boolean isItem() {
        return safeType() == LootEntryType.ITEM;
    }

    public boolean isCommand() {
        return safeType() == LootEntryType.COMMAND;
    }

    public ItemStack toItemStack() {
        if (!isItem() || itemSnapshot == null) return null;
        ItemStack item = ItemSnapshot.from(itemSnapshot).toItemStack();
        item.setAmount(rollAmount());
        return item;
    }

    public int rollAmount() {
        if (minAmount >= maxAmount) return minAmount;
        return ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
    }

    public String getDisplayName() {
        return switch (safeType()) {
            case ITEM -> itemSnapshot != null ? ItemSnapshot.from(itemSnapshot).toDisplayLabel() : "(not set)";
            case COMMAND -> command != null && !command.isBlank() ? command : "(no command)";
        };
    }

    public String getResolvedIconMaterial() {
        return switch (safeType()) {
            case ITEM -> itemSnapshot != null ? ItemSnapshot.from(itemSnapshot).toItemStack().getType().name() : "BARRIER";
            case COMMAND -> "COMMAND_BLOCK";
        };
    }

    public LootEntry copy() {
        return LootEntry.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .itemSnapshot(itemSnapshot)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .command(command)
                .weight(weight)
                .tier(tier)
                .build();
    }
}

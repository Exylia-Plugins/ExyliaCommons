package net.exylia.commons.v2.reward.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.exylia.commons.v2.items.snapshot.ItemSnapshot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardEntry {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private RewardType type;

    private String command;
    private String itemSnapshot;
    private String message;

    @Builder.Default
    private double chance = 100.0;

    private String condition;
    private String permission;
    private String deliveryMessage;

    @Builder.Default
    private int priority = 0;

    public static RewardEntry ofCommand(String command) {
        return RewardEntry.builder()
                .type(RewardType.COMMAND)
                .command(command)
                .build();
    }

    public static RewardEntry ofMessage(String message) {
        return RewardEntry.builder()
                .type(RewardType.MESSAGE)
                .message(message)
                .build();
    }

    public static RewardEntry ofItem(ItemStack item) {
        return RewardEntry.builder()
                .type(RewardType.ITEM)
                .itemSnapshot(ItemSnapshot.from(item).serialize())
                .build();
    }

    public Reward toReward() {
        Object data = switch (type) {
            case COMMAND -> command;
            case MESSAGE -> message;
            case ITEM -> itemSnapshot != null ? ItemSnapshot.from(itemSnapshot) : null;
        };
        return Reward.builder()
                .id(id)
                .type(type)
                .data(data)
                .chance(chance)
                .condition(condition)
                .permission(permission)
                .message(deliveryMessage)
                .priority(priority)
                .build();
    }

    public String getValuePreview() {
        return switch (type) {
            case COMMAND -> command != null ? command : "(not set)";
            case MESSAGE -> message != null ? message : "(not set)";
            case ITEM -> {
                if (itemSnapshot == null) yield "(no item)";
                String snap = itemSnapshot;
                if (snap.startsWith("bytes:")) {
                    try {
                        ItemStack item = ItemSnapshot.from(snap).toItemStack();
                        yield item.getType().name().toLowerCase().replace('_', ' ');
                    } catch (Exception e) {
                        yield "item";
                    }
                }
                if (snap.startsWith("item:")) {
                    int mIdx = snap.indexOf("\"m\":\"");
                    if (mIdx >= 0) {
                        int start = mIdx + 5;
                        int end = snap.indexOf('"', start);
                        if (end > start) yield snap.substring(start, end).toLowerCase().replace('_', ' ');
                    }
                }
                if (snap.startsWith("urlhead:") || snap.startsWith("playerhead:") || snap.startsWith("basehead:")) {
                    yield "Custom Skull";
                }
                yield snap.toLowerCase().replace('_', ' ');
            }
        };
    }

    public RewardEntry copy() {
        return RewardEntry.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .command(command)
                .itemSnapshot(itemSnapshot)
                .message(message)
                .chance(chance)
                .condition(condition)
                .permission(permission)
                .deliveryMessage(deliveryMessage)
                .priority(priority)
                .build();
    }
}

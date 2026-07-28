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

    private String name;

    private RewardType type;

    private String command;
    private String itemSnapshot;
    private String message;
    private String icon;

    @Builder.Default
    private int itemAmount = 1;

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
            case ITEM -> buildItemStack();
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

    public String getDisplayName() {
        return name != null && !name.isBlank() ? name : getValuePreview();
    }

    private ItemStack buildItemStack() {
        if (itemSnapshot == null) return null;
        ItemStack item = ItemSnapshot.from(itemSnapshot).toItemStack();
        item.setAmount(Math.max(1, itemAmount));
        return item;
    }

    public boolean hasIcon() {
        return icon != null && !icon.isBlank();
    }

    public String getResolvedIconMaterial() {
        if (hasIcon()) {
            return resolveMaterialFromSnapshot(icon);
        }
        if (type == RewardType.ITEM && itemSnapshot != null) {
            return resolveMaterialFromSnapshot(itemSnapshot);
        }
        return switch (type) {
            case COMMAND -> "COMMAND_BLOCK";
            case ITEM -> "CHEST";
            case MESSAGE -> "PAPER";
        };
    }

    private static String resolveMaterialFromSnapshot(String snap) {
        if (snap == null) return "CHEST";
        if (snap.startsWith("bytes:")) {
            try {
                ItemStack item = ItemSnapshot.from(snap).toItemStack();
                return item.getType().name();
            } catch (Exception e) {
                return "CHEST";
            }
        }
        if (snap.startsWith("item:")) {
            int mIdx = snap.indexOf("\"m\":\"");
            if (mIdx >= 0) {
                int start = mIdx + 5;
                int end = snap.indexOf('"', start);
                if (end > start) return snap.substring(start, end);
            }
        }
        if (snap.startsWith("urlhead:") || snap.startsWith("playerhead:") || snap.startsWith("basehead:")) {
            return "PLAYER_HEAD";
        }
        return snap.contains(":") ? "CHEST" : snap.toUpperCase();
    }

    public String getValuePreview() {
        return switch (type) {
            case COMMAND -> command != null ? command : "(not set)";
            case MESSAGE -> message != null ? message : "(not set)";
            case ITEM -> {
                if (itemSnapshot == null) yield "(no item)";
                String snap = itemSnapshot;
                String amountPrefix = itemAmount > 1 ? itemAmount + "x " : "";
                if (snap.startsWith("bytes:")) {
                    try {
                        ItemStack item = ItemSnapshot.from(snap).toItemStack();
                        yield amountPrefix + item.getType().name().toLowerCase().replace('_', ' ');
                    } catch (Exception e) {
                        yield amountPrefix + "item";
                    }
                }
                if (snap.startsWith("item:")) {
                    int mIdx = snap.indexOf("\"m\":\"");
                    if (mIdx >= 0) {
                        int start = mIdx + 5;
                        int end = snap.indexOf('"', start);
                        if (end > start) yield amountPrefix + snap.substring(start, end).toLowerCase().replace('_', ' ');
                    }
                }
                if (snap.startsWith("urlhead:") || snap.startsWith("playerhead:") || snap.startsWith("basehead:")) {
                    yield amountPrefix + "Custom Skull";
                }
                yield amountPrefix + snap.toLowerCase().replace('_', ' ');
            }
        };
    }

    public RewardEntry copy() {
        return RewardEntry.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .type(type)
                .command(command)
                .itemSnapshot(itemSnapshot)
                .message(message)
                .icon(icon)
                .itemAmount(itemAmount)
                .chance(chance)
                .condition(condition)
                .permission(permission)
                .deliveryMessage(deliveryMessage)
                .priority(priority)
                .build();
    }
}

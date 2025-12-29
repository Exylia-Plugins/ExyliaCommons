package net.exylia.commons.v2.items.api;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.items.model.ItemData;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
@Builder(toBuilder = true)
public class ProcessedItem {

    private final ItemStack itemStack;
    private final Integer slot;
    private final List<Integer> slots;
    private final List<String> clickActions;
    private final List<String> rightClickActions;
    private final List<String> commands;
    private final ItemData rawItemData;
    private final boolean hasDynamicContent;

    public boolean needsRefresh() {
        return hasDynamicContent || rawItemData.isDynamicUpdate();
    }

    public boolean hasSlot() {
        return slot != null || (slots != null && !slots.isEmpty());
    }

    public boolean isSingleSlot() {
        return slot != null;
    }

    public boolean isMultipleSlots() {
        return slots != null && !slots.isEmpty();
    }

    public boolean hasActions() {
        return (clickActions != null && !clickActions.isEmpty()) ||
               (rightClickActions != null && !rightClickActions.isEmpty());
    }

    public boolean hasCommands() {
        return commands != null && !commands.isEmpty();
    }

    public ItemStack getItemStack() {
        return itemStack != null ? itemStack.clone() : null;
    }
}

package net.exylia.commons.v2.items.api;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickCommand;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder(toBuilder = true)
public class ProcessedItem {

    private final ItemStack itemStack;
    private final Integer slot;
    private final List<Integer> slots;
    private final List<ClickAction> actions;
    private final List<ClickCommand> commands;
    private final ItemData rawItemData;
    private final boolean hasDynamicContent;
    private final boolean requiresTarget;

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
        return actions != null && !actions.isEmpty();
    }

    public boolean hasCommands() {
        return commands != null && !commands.isEmpty();
    }

    public List<String> getActionsForClick(ClickType clickType) {
        if (actions == null || actions.isEmpty()) {
            return Collections.emptyList();
        }

        ClickTypeGroup group = ClickTypeGroup.fromBukkit(clickType);
        return actions.stream()
            .filter(action -> action.matchesClick(group))
            .map(ClickAction::getAction)
            .collect(Collectors.toList());
    }

    public List<String> getCommandsForClick(ClickType clickType) {
        if (commands == null || commands.isEmpty()) {
            return Collections.emptyList();
        }

        ClickTypeGroup group = ClickTypeGroup.fromBukkit(clickType);
        return commands.stream()
            .filter(command -> command.matchesClick(group))
            .map(ClickCommand::getCommand)
            .collect(Collectors.toList());
    }

    public ItemStack getItemStack() {
        return itemStack != null ? itemStack.clone() : null;
    }
}

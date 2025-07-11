package net.exylia.commons.wizard.selection;

import net.exylia.commons.selection.model.Selection;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Custom selection wizard handler interface
 * Implement this to create your own selection wizard logic
 */
public interface SelectionWizardHandler<T> {

    /**
     * Called when wizard starts
     * Send initial instructions to the player here
     *
     * @param player The player
     * @param selectionsNeeded Total selections needed
     */
    void onStart(Player player, int selectionsNeeded);

    /**
     * Process selection completion
     *
     * @param player The player
     * @param selection The completed selection
     * @param completedSelections All selections completed so far (including current)
     * @param remainingSelections How many selections are still needed
     * @return SelectionWizardResult indicating what to do next
     */
    SelectionWizardResult onSelectionComplete(Player player, Selection selection, List<Selection> completedSelections, int remainingSelections);
}
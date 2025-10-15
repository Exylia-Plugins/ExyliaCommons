package net.exylia.commons.wizard.selection;

import net.exylia.commons.selection.model.Selection;
import org.bukkit.entity.Player;

import java.util.List;

public interface SelectionWizardHandler<T> {

    void onStart(Player player, int selectionsNeeded);

    SelectionWizardResult onSelectionComplete(Player player, Selection selection, List<Selection> completedSelections, int remainingSelections);
}

package net.exylia.commons.v2.wizard.handler;

import net.exylia.commons.v2.region.selection.Selection;
import net.exylia.commons.v2.wizard.result.WizardResult;
import org.bukkit.entity.Player;

import java.util.List;

@FunctionalInterface
public interface SelectionHandler<T> {
    WizardResult<T> onSelection(Player player, Selection selection, List<Selection> allSelections, int remaining);

    default void onStart(Player player, int total) {}
}

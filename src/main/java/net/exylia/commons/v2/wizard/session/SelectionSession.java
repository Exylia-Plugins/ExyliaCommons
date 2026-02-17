package net.exylia.commons.v2.wizard.session;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.region.selection.Selection;
import net.exylia.commons.v2.wizard.config.WizardConfig;
import net.exylia.commons.v2.wizard.handler.SelectionHandler;
import net.exylia.commons.v2.wizard.model.WizardType;
import net.exylia.commons.v2.wizard.result.WizardResult;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class SelectionSession<T> extends WizardSession<T> {
    private final SelectionHandler<T> handler;
    private final int totalCount;
    private final List<Selection> completedSelections;
    @Setter
    private Selection currentSelection;
    private boolean awaitingConfirmation;
    @Setter
    private boolean justConfirmed;

    public SelectionSession(Player player, WizardConfig config, int count, SelectionHandler<T> handler) {
        super(player, config);
        this.handler = handler;
        this.totalCount = count;
        this.completedSelections = new ArrayList<>();
        this.awaitingConfirmation = false;
    }

    @Override
    public WizardType getType() {
        return WizardType.SELECTION;
    }

    @Override
    public void handleInteraction(PlayerInteractEvent event) {
        if (!player.isSneaking()) return;
        if (!awaitingConfirmation) return;

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.LEFT_CLICK_AIR) return;

        event.setCancelled(true);
        confirmSelection();
    }

    public void notifySelectionReady(Selection selection) {
        this.currentSelection = selection;
        this.awaitingConfirmation = true;
    }

    public void confirmSelection() {
        if (currentSelection == null || !currentSelection.isComplete()) return;

        completedSelections.add(currentSelection);
        awaitingConfirmation = false;
        justConfirmed = true;

        int remaining = totalCount - completedSelections.size();
        WizardResult<T> result = handler.onSelection(player, currentSelection, getSelections(), remaining);

        currentSelection = null;
        processResult(result);
    }

    private void processResult(WizardResult<T> result) {
        switch (result.getType()) {
            case COMPLETE -> complete(result.getValue());
            case CANCEL -> cancel();
            case CONTINUE -> {}
        }
    }

    public List<Selection> getSelections() {
        return Collections.unmodifiableList(completedSelections);
    }

    public int getRemaining() {
        return totalCount - completedSelections.size();
    }

    public int getCurrent() {
        return completedSelections.size() + 1;
    }

    public String generateSelectionId() {
        return "wizard_" + player.getUniqueId().toString().substring(0, 8) + "_" + getCurrent();
    }

    public boolean needsMoreSelections() {
        return completedSelections.size() < totalCount;
    }
}

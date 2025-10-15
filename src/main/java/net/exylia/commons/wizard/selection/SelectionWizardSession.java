package net.exylia.commons.wizard.selection;

import lombok.Getter;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
public class SelectionWizardSession {

    private final Player player;
    private final int totalSelections;
    private final SelectionWizardHandler<?> handler;
    private final CompletableFuture<Object> future;
    private final long startTime;

    private final List<Selection> completedSelections;
    private Selection currentSelection;  

    SelectionWizardSession(Player player, int totalSelections, SelectionWizardHandler<?> handler) {
        this.player = player;
        this.totalSelections = totalSelections;
        this.handler = handler;
        this.future = new CompletableFuture<>();
        this.startTime = System.currentTimeMillis();
        this.completedSelections = new ArrayList<>();
    }

    void setCurrentSelection(Selection selection) {
        this.currentSelection = selection;
    }

    Selection getCurrentSelection() {
        return currentSelection;
    }

    void addSelection(Selection selection) {
        completedSelections.add(selection);
    }

    int getRemainingSelections() {
        return totalSelections - completedSelections.size();
    }

    int getCurrentSelectionIndex() {
        return completedSelections.size() + 1;
    }

    boolean isComplete() {
        return completedSelections.size() >= totalSelections;
    }

    void complete(Object result) {
        if (!future.isDone()) {
            future.complete(result);
        }
    }

    void cancel() {
        if (!future.isDone()) {
            future.cancel(true);
        }
    }

    void completeExceptionally(Throwable throwable) {
        if (!future.isDone()) {
            future.completeExceptionally(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    <T> CompletableFuture<T> getFuture() {
        return (CompletableFuture<T>) future;
    }
}

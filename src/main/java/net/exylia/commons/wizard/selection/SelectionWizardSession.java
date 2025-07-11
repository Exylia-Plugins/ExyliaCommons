package net.exylia.commons.wizard.selection;

import lombok.Getter;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an active selection wizard session
 */
@Getter
public class SelectionWizardSession {

    private final Player player;
    private final int totalSelections;
    private final SelectionWizardHandler<?> handler;
    private final CompletableFuture<Object> future;
    private final long startTime;

    private final List<Selection> completedSelections;
    private Selection currentSelection; // Selection ready for confirmation

    SelectionWizardSession(Player player, int totalSelections, SelectionWizardHandler<?> handler) {
        this.player = player;
        this.totalSelections = totalSelections;
        this.handler = handler;
        this.future = new CompletableFuture<>();
        this.startTime = System.currentTimeMillis();
        this.completedSelections = new ArrayList<>();
    }

    /**
     * Set the current selection that's ready for confirmation
     */
    void setCurrentSelection(Selection selection) {
        this.currentSelection = selection;
    }

    /**
     * Get the current selection ready for confirmation
     */
    Selection getCurrentSelection() {
        return currentSelection;
    }

    /**
     * Add a completed selection
     */
    void addSelection(Selection selection) {
        completedSelections.add(selection);
    }

    /**
     * Get remaining selections needed
     */
    int getRemainingSelections() {
        return totalSelections - completedSelections.size();
    }

    /**
     * Get current selection index (1-based)
     */
    int getCurrentSelectionIndex() {
        return completedSelections.size() + 1;
    }

    /**
     * Check if wizard is complete
     */
    boolean isComplete() {
        return completedSelections.size() >= totalSelections;
    }

    /**
     * Complete the session with result
     */
    void complete(Object result) {
        if (!future.isDone()) {
            future.complete(result);
        }
    }

    /**
     * Cancel the session
     */
    void cancel() {
        if (!future.isDone()) {
            future.cancel(true);
        }
    }

    /**
     * Complete with exception
     */
    void completeExceptionally(Throwable throwable) {
        if (!future.isDone()) {
            future.completeExceptionally(throwable);
        }
    }

    /**
     * Get the future with correct type
     */
    @SuppressWarnings("unchecked")
    <T> CompletableFuture<T> getFuture() {
        return (CompletableFuture<T>) future;
    }
}
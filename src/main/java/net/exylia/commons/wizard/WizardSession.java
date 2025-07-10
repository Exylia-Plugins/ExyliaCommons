package net.exylia.commons.wizard;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an active wizard session
 */
@Getter
class WizardSession {

    private final Player player;
    private final int totalPositions;
    private final WizardHandler<?> handler;
    private final CompletableFuture<Object> future;
    private final long startTime;

    private final List<Location> selectedLocations;

    WizardSession(Player player, int totalPositions, WizardHandler<?> handler) {
        this.player = player;
        this.totalPositions = totalPositions;
        this.handler = handler;
        this.future = new CompletableFuture<>();
        this.startTime = System.currentTimeMillis();
        this.selectedLocations = new ArrayList<>();
    }

    /**
     * Add a selected location
     */
    void addLocation(Location location) {
        selectedLocations.add(location.clone());
    }

    /**
     * Get remaining positions needed
     */
    int getRemainingPositions() {
        return totalPositions - selectedLocations.size();
    }

    /**
     * Check if wizard is complete
     */
    boolean isComplete() {
        return selectedLocations.size() >= totalPositions;
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
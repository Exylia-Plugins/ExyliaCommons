package net.exylia.commons.wizard.location;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    void addLocation(Location location) {
        selectedLocations.add(location.clone());
    }

    int getRemainingPositions() {
        return totalPositions - selectedLocations.size();
    }

    boolean isComplete() {
        return selectedLocations.size() >= totalPositions;
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

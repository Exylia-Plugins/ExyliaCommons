package net.exylia.commons.v2.hologram.update;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.hologram.model.Hologram;
import net.exylia.commons.v2.placeholders.PlaceholdersV2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class PlaceholderUpdateTask {
    private final int batchSize;
    private final List<UpdateEntry> updateQueue = new ArrayList<>();

    public void addToQueue(Hologram hologram, int lineIndex) {
        synchronized (updateQueue) {
            updateQueue.add(new UpdateEntry(hologram, lineIndex));
        }
    }

    public CompletableFuture<Void> processBatch() {
        List<UpdateEntry> batch;

        synchronized (updateQueue) {
            if (updateQueue.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            int size = Math.min(batchSize, updateQueue.size());
            batch = new ArrayList<>(updateQueue.subList(0, size));
            updateQueue.subList(0, size).clear();
        }

        return CompletableFuture.runAsync(() -> {
            batch.forEach(entry -> {
                Hologram hologram = entry.hologram;
                if (hologram.isSpawned()) {
                    hologram.updateAsync();
                }
            });
        });
    }

    public void clear() {
        synchronized (updateQueue) {
            updateQueue.clear();
        }
    }

    public int getQueueSize() {
        synchronized (updateQueue) {
            return updateQueue.size();
        }
    }

    @Getter
    @RequiredArgsConstructor
    private static class UpdateEntry {
        private final Hologram hologram;
        private final int lineIndex;
    }
}

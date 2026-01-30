package net.exylia.commons.v2.ui.animation;

import net.exylia.commons.v2.items.api.ProcessedItem;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class AnimationExecutor {

    private AnimationExecutor() {}

    public static CompletableFuture<Void> execute(
            Inventory inventory,
            Map<Integer, ProcessedItem> items,
            AnimationType type,
            int speed,
            AtomicBoolean cancelFlag
    ) {
        if (type == AnimationType.NONE || inventory == null) {
            applyAllItems(inventory, items);
            return CompletableFuture.completedFuture(null);
        }

        MenuAnimation animation = AnimationRegistry.getOrDefault(type);
        int rows = inventory.getSize() / 9;
        List<List<Integer>> frames = animation.calculateFrames(rows, 9);

        if (frames.isEmpty() || frames.size() == 1) {
            applyAllItems(inventory, items);
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicInteger frameIndex = new AtomicInteger(0);
        AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();

        ScheduledTask task = Tasks.timer(() -> {
            if (cancelFlag.get() || inventory.getViewers().isEmpty()) {
                ScheduledTask currentTask = taskRef.get();
                if (currentTask != null) {
                    currentTask.cancel();
                }
                future.complete(null);
                return;
            }

            int currentFrame = frameIndex.getAndIncrement();

            if (currentFrame >= frames.size()) {
                ScheduledTask currentTask = taskRef.get();
                if (currentTask != null) {
                    currentTask.cancel();
                }
                future.complete(null);
                return;
            }

            List<Integer> slotsToReveal = frames.get(currentFrame);
            for (Integer slot : slotsToReveal) {
                ProcessedItem item = items.get(slot);
                if (item != null && slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, item.getItemStack());
                }
            }
        }, speed, speed);

        taskRef.set(task);
        return future;
    }

    public static CompletableFuture<Void> executeWithTransition(
            Inventory inventory,
            Map<Integer, ProcessedItem> oldItems,
            Map<Integer, ProcessedItem> newItems,
            AnimationType type,
            int speed,
            AtomicBoolean cancelFlag
    ) {
        if (type == AnimationType.NONE || inventory == null) {
            applyAllItems(inventory, newItems);
            return CompletableFuture.completedFuture(null);
        }

        MenuAnimation animation = AnimationRegistry.getOrDefault(type);
        int rows = inventory.getSize() / 9;
        List<List<Integer>> frames = animation.calculateFrames(rows, 9);

        if (frames.isEmpty() || frames.size() == 1) {
            applyAllItems(inventory, newItems);
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicInteger frameIndex = new AtomicInteger(0);
        AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();

        clearInventory(inventory);

        ScheduledTask task = Tasks.timer(() -> {
            if (cancelFlag.get() || inventory.getViewers().isEmpty()) {
                ScheduledTask currentTask = taskRef.get();
                if (currentTask != null) {
                    currentTask.cancel();
                }
                applyAllItems(inventory, newItems);
                future.complete(null);
                return;
            }

            int currentFrame = frameIndex.getAndIncrement();

            if (currentFrame >= frames.size()) {
                ScheduledTask currentTask = taskRef.get();
                if (currentTask != null) {
                    currentTask.cancel();
                }
                future.complete(null);
                return;
            }

            List<Integer> slotsToReveal = frames.get(currentFrame);
            for (Integer slot : slotsToReveal) {
                ProcessedItem item = newItems.get(slot);
                if (item != null && slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, item.getItemStack());
                }
            }
        }, speed, speed);

        taskRef.set(task);
        return future;
    }

    private static void applyAllItems(Inventory inventory, Map<Integer, ProcessedItem> items) {
        if (inventory == null) return;
        items.forEach((slot, item) -> {
            if (slot >= 0 && slot < inventory.getSize() && item != null) {
                inventory.setItem(slot, item.getItemStack());
            }
        });
    }

    private static void clearInventory(Inventory inventory) {
        if (inventory == null) return;
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }
    }
}

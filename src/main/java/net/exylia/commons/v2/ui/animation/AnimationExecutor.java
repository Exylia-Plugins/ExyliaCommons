package net.exylia.commons.v2.ui.animation;

import net.exylia.commons.v2.items.api.ProcessedItem;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class AnimationExecutor {

    private AnimationExecutor() {}

    public static CompletableFuture<Void> execute(
            Player player,
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

        List<List<Integer>> frames = resolveFrames(inventory, type);

        if (frames.isEmpty() || frames.size() == 1) {
            applyAllItems(inventory, items);
            return CompletableFuture.completedFuture(null);
        }

        return runFrames(player, inventory, items, frames, speed, cancelFlag, null);
    }

    public static CompletableFuture<Void> executeWithTransition(
            Player player,
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

        List<List<Integer>> frames = resolveFrames(inventory, type);

        if (frames.isEmpty() || frames.size() == 1) {
            applyAllItems(inventory, newItems);
            return CompletableFuture.completedFuture(null);
        }

        clearInventory(inventory);

        return runFrames(player, inventory, newItems, frames, speed, cancelFlag, newItems);
    }

    private static List<List<Integer>> resolveFrames(Inventory inventory, AnimationType type) {
        MenuAnimation animation = AnimationRegistry.getOrDefault(type);
        int rows = inventory.getSize() / 9;
        return animation.calculateFrames(rows, 9);
    }

    private static CompletableFuture<Void> runFrames(
            Player player,
            Inventory inventory,
            Map<Integer, ProcessedItem> items,
            List<List<Integer>> frames,
            int speed,
            AtomicBoolean cancelFlag,
            Map<Integer, ProcessedItem> itemsOnCancel
    ) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicInteger frameIndex = new AtomicInteger(0);
        AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();

        ScheduledTask task = Tasks.atTimer(player, () -> {
            if (cancelFlag.get() || inventory.getViewers().isEmpty()) {
                cancel(taskRef);
                if (itemsOnCancel != null) {
                    applyAllItems(inventory, itemsOnCancel);
                }
                future.complete(null);
                return;
            }

            int currentFrame = frameIndex.getAndIncrement();

            if (currentFrame >= frames.size()) {
                cancel(taskRef);
                future.complete(null);
                return;
            }

            for (Integer slot : frames.get(currentFrame)) {
                ProcessedItem item = items.get(slot);
                if (item != null && slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, item.getItemStack());
                }
            }
        }, speed, speed);

        taskRef.set(task);
        return future;
    }

    private static void cancel(AtomicReference<ScheduledTask> taskRef) {
        ScheduledTask task = taskRef.get();
        if (task != null) {
            task.cancel();
        }
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

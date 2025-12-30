package net.exylia.commons.v2.hologram.visibility;

import lombok.AllArgsConstructor;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.hologram.model.Hologram;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VisibilityBatcher {
    private final Queue<VisibilityAction> actionQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, Long> lastProcessed = new ConcurrentHashMap<>();
    private static final long BATCH_DELAY_MS = 50;

    public void queueShow(Player player, Hologram hologram) {
        String key = player.getUniqueId() + ":" + hologram.getId();
        Long last = lastProcessed.get(key);
        if (last != null && System.currentTimeMillis() - last < BATCH_DELAY_MS) {
            return;
        }
        actionQueue.offer(new VisibilityAction(player, hologram, ActionType.SHOW));
    }

    public void queueHide(Player player, Hologram hologram) {
        String key = player.getUniqueId() + ":" + hologram.getId();
        Long last = lastProcessed.get(key);
        if (last != null && System.currentTimeMillis() - last < BATCH_DELAY_MS) {
            return;
        }
        actionQueue.offer(new VisibilityAction(player, hologram, ActionType.HIDE));
    }

    public void processBatch() {
        if (actionQueue.isEmpty()) {
            return;
        }

        List<VisibilityAction> batch = new ArrayList<>();
        VisibilityAction action;
        while ((action = actionQueue.poll()) != null && batch.size() < 100) {
            batch.add(action);
        }

        if (!batch.isEmpty()) {
            Schedulers.sync(() -> {
                long now = System.currentTimeMillis();
                for (VisibilityAction va : batch) {
                    if (!va.player.isOnline()) {
                        continue;
                    }

                    String key = va.player.getUniqueId() + ":" + va.hologram.getId();
                    lastProcessed.put(key, now);

                    if (va.type == ActionType.SHOW) {
                        va.hologram.spawnForPlayer(va.player);
                    } else {
                        va.hologram.hideFrom(va.player);
                    }
                }
            });
        }
    }

    public void clear() {
        actionQueue.clear();
        lastProcessed.clear();
    }

    public int getQueueSize() {
        return actionQueue.size();
    }

    @AllArgsConstructor
    private static class VisibilityAction {
        private final Player player;
        private final Hologram hologram;
        private final ActionType type;
    }

    private enum ActionType {
        SHOW, HIDE
    }
}

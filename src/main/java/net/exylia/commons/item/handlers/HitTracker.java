package net.exylia.commons.item.handlers;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HitTracker {

    private static final Map<UUID, Map<UUID, PlayerHitData>> playerHitData = new HashMap<>();

    @Data
    @AllArgsConstructor
    private static class PlayerHitData {
        private List<Long> hitTimes;
        private String itemId;

        private PlayerHitData(String itemId) {
            this.hitTimes = new ArrayList<>();
            this.itemId = itemId;
        }
    }

    public static boolean recordHit(UUID attacker, UUID target, String itemId, int requiredHits, long periodTicks) {
        long currentTime = System.currentTimeMillis();
        long periodMillis = periodTicks * 50;

        playerHitData.computeIfAbsent(attacker, k -> new HashMap<>());
        Map<UUID, PlayerHitData> attackerData = playerHitData.get(attacker);

        PlayerHitData hitData = attackerData.get(target);
        if (hitData == null || !hitData.getItemId().equals(itemId)) {
            hitData = new PlayerHitData(itemId);
            attackerData.put(target, hitData);
        }

        List<Long> hitTimes = hitData.getHitTimes();
        hitTimes.removeIf(time -> currentTime - time > periodMillis);

        hitTimes.add(currentTime);

        if (hitTimes.size() >= requiredHits) {
            hitTimes.clear();
            return true;
        }

        return false;
    }

    public static void clearPlayerData(UUID player) {
        playerHitData.remove(player);

        playerHitData.values().forEach(targetMap -> targetMap.remove(player));
    }

    public static void clearAll() {
        playerHitData.clear();
    }

    public static int getHitCount(UUID attacker, UUID target, String itemId) {
        Map<UUID, PlayerHitData> attackerData = playerHitData.get(attacker);
        if (attackerData == null) return 0;

        PlayerHitData hitData = attackerData.get(target);
        if (hitData == null || !hitData.getItemId().equals(itemId)) return 0;

        return hitData.getHitTimes().size();
    }
}

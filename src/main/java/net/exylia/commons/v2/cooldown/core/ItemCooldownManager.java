package net.exylia.commons.v2.cooldown.core;

import net.exylia.commons.v2.clientapi.cooldown.api.CooldownAPI;
import net.exylia.commons.v2.clientapi.cooldown.model.CooldownDefinition;
import net.exylia.commons.v2.clientapi.cooldown.model.CooldownIcon;
import net.exylia.commons.v2.cooldown.model.ItemCooldown;
import net.exylia.commons.v2.formatter.api.FormatterAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemCooldownManager {

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, ItemCooldown>> cooldowns = new ConcurrentHashMap<>();

    public void set(Player player, String id, long durationMs) {
        set(player, id, durationMs, null);
    }

    public void set(Player player, String id, long durationMs, Material material) {
        UUID playerId = player.getUniqueId();

        ConcurrentHashMap<String, ItemCooldown> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        ItemCooldown existing = playerCooldowns.get(id);
        if (existing != null && existing.hasMaterial() && (material == null || existing.getMaterial() != material)) {
            player.setCooldown(existing.getMaterial(), 0);
        }

        ItemCooldown cooldown = new ItemCooldown(id, playerId, durationMs, material);
        playerCooldowns.put(id, cooldown);

        if (material != null) {
            int ticks = (int) Math.ceil((double) durationMs / 50.0);
            player.setCooldown(material, ticks);
        }

        if (CooldownAPI.isInitialized() && material != null) {
            CooldownAPI.display(player, CooldownDefinition.builder()
                    .name(id)
                    .duration(Duration.ofMillis(durationMs))
                    .icon(CooldownIcon.item(material.name()))
                    .build());
        }
    }

    public boolean isOnCooldown(Player player, String id) {
        return isOnCooldown(player.getUniqueId(), id);
    }

    public boolean isOnCooldown(UUID playerId, String id) {
        ConcurrentHashMap<String, ItemCooldown> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;

        ItemCooldown cooldown = playerCooldowns.get(id);
        if (cooldown == null) return false;

        if (cooldown.isExpired()) {
            playerCooldowns.remove(id);
            return false;
        }

        return true;
    }

    public long getRemainingMillis(Player player, String id) {
        return getRemainingMillis(player.getUniqueId(), id);
    }

    public long getRemainingMillis(UUID playerId, String id) {
        ConcurrentHashMap<String, ItemCooldown> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;

        ItemCooldown cooldown = playerCooldowns.get(id);
        if (cooldown == null) return 0;

        if (cooldown.isExpired()) {
            playerCooldowns.remove(id);
            return 0;
        }

        return cooldown.getRemainingMillis();
    }

    public String getFormattedRemaining(Player player, String id) {
        long remaining = getRemainingMillis(player.getUniqueId(), id);
        if (remaining <= 0) return null;
        return FormatterAPI.formatTime(remaining);
    }

    public ItemCooldown get(Player player, String id) {
        return get(player.getUniqueId(), id);
    }

    public ItemCooldown get(UUID playerId, String id) {
        ConcurrentHashMap<String, ItemCooldown> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return null;

        ItemCooldown cooldown = playerCooldowns.get(id);
        if (cooldown == null) return null;

        if (cooldown.isExpired()) {
            playerCooldowns.remove(id);
            return null;
        }

        return cooldown;
    }

    public Map<String, ItemCooldown> getActive(Player player) {
        return getActive(player.getUniqueId());
    }

    public Map<String, ItemCooldown> getActive(UUID playerId) {
        ConcurrentHashMap<String, ItemCooldown> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null || playerCooldowns.isEmpty()) return Collections.emptyMap();

        playerCooldowns.entrySet().removeIf(e -> e.getValue().isExpired());

        if (playerCooldowns.isEmpty()) return Collections.emptyMap();

        Map<String, ItemCooldown> result = new LinkedHashMap<>();
        for (Map.Entry<String, ItemCooldown> entry : playerCooldowns.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    public void remove(Player player, String id) {
        UUID playerId = player.getUniqueId();
        ConcurrentHashMap<String, ItemCooldown> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return;

        ItemCooldown cooldown = playerCooldowns.remove(id);
        if (cooldown == null) return;

        if (cooldown.hasMaterial()) {
            player.setCooldown(cooldown.getMaterial(), 0);
        }

        if (CooldownAPI.isInitialized()) {
            CooldownAPI.remove(player, id);
        }
    }

    public void removeAll(Player player) {
        UUID playerId = player.getUniqueId();
        ConcurrentHashMap<String, ItemCooldown> playerCooldowns = cooldowns.remove(playerId);
        if (playerCooldowns == null) return;

        for (ItemCooldown cooldown : playerCooldowns.values()) {
            if (cooldown.hasMaterial()) {
                player.setCooldown(cooldown.getMaterial(), 0);
            }
        }

        if (CooldownAPI.isInitialized()) {
            CooldownAPI.removeAll(player);
        }
    }

    public void cleanupPlayer(UUID playerId) {
        cooldowns.remove(playerId);
    }
}

package net.exylia.commons.v2.cooldown.core;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import net.exylia.commons.v2.cooldown.model.CooldownTrigger;
import net.exylia.commons.v2.cooldown.model.ItemCooldownDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemCooldownListener implements Listener {

    private final ItemCooldownManager cooldownManager;
    private final ItemCooldownRegistry registry;

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>>> regionUseCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<String>> playerRegionCache = new ConcurrentHashMap<>();

    private static Boolean worldGuardPresent = null;

    public ItemCooldownListener(ItemCooldownManager cooldownManager, ItemCooldownRegistry registry) {
        this.cooldownManager = cooldownManager;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractCheck(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null) return;

        Material material = event.getItem().getType();
        ItemCooldownDefinition def = registry.getByMaterial(material);
        if (def == null) return;

        CooldownTrigger trigger = def.getTrigger();
        if (trigger != CooldownTrigger.USE && trigger != CooldownTrigger.USE_ON_ELYTRA) return;

        Player player = event.getPlayer();
        if (trigger == CooldownTrigger.USE_ON_ELYTRA && !player.isGliding()) return;

        if (cooldownManager.isOnCooldown(player, def.getId())) {
            event.setCancelled(true);
            return;
        }

        if (def.hasAnyRegionMaxUses()) {
            for (String regionId : getPlayerRegionIds(player)) {
                if (def.hasRegionMaxUses(regionId)
                        && getRegionUseCount(player.getUniqueId(), regionId, def.getId()) >= def.getRegionMaxUses(regionId)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractApply(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null) return;

        Material material = event.getItem().getType();
        ItemCooldownDefinition def = registry.getByMaterial(material);
        if (def == null) return;

        CooldownTrigger trigger = def.getTrigger();
        if (trigger != CooldownTrigger.USE && trigger != CooldownTrigger.USE_ON_ELYTRA) return;

        Player player = event.getPlayer();
        if (trigger == CooldownTrigger.USE_ON_ELYTRA && !player.isGliding()) return;

        applyCooldown(player, def);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Material material = event.getItem().getType();
        ItemCooldownDefinition def = registry.getByMaterial(material);
        if (def == null || def.getTrigger() != CooldownTrigger.CONSUME) return;

        applyCooldown(event.getPlayer(), def);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunchCheck(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemCooldownDefinition def = registry.getByProjectile(event.getEntity().getClass());
        if (def == null || def.getTrigger() != CooldownTrigger.LAUNCH) return;

        if (cooldownManager.isOnCooldown(player, def.getId())) {
            event.setCancelled(true);
            return;
        }

        if (def.hasAnyRegionMaxUses()) {
            for (String regionId : getPlayerRegionIds(player)) {
                if (def.hasRegionMaxUses(regionId)
                        && getRegionUseCount(player.getUniqueId(), regionId, def.getId()) >= def.getRegionMaxUses(regionId)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemCooldownDefinition def = registry.getByProjectile(event.getEntity().getClass());
        if (def == null || def.getTrigger() != CooldownTrigger.LAUNCH) return;

        applyCooldown(player, def);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        for (ItemCooldownDefinition def : registry.getAll()) {
            if (def.getTrigger() != CooldownTrigger.RESURRECT) continue;
            if (cooldownManager.isOnCooldown(player, def.getId())) continue;
            applyCooldown(player, def);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onElytraBoostCheck(PlayerElytraBoostEvent event) {
        Player player = event.getPlayer();
        ItemCooldownDefinition def = registry.getByMaterial(event.getItemStack().getType());
        if (def == null || def.getTrigger() != CooldownTrigger.ELYTRA_BOOST) return;

        if (cooldownManager.isOnCooldown(player, def.getId())) {
            event.setCancelled(true);
            return;
        }

        if (def.hasAnyRegionMaxUses()) {
            for (String regionId : getPlayerRegionIds(player)) {
                if (def.hasRegionMaxUses(regionId)
                        && getRegionUseCount(player.getUniqueId(), regionId, def.getId()) >= def.getRegionMaxUses(regionId)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onElytraBoost(PlayerElytraBoostEvent event) {
        Player player = event.getPlayer();
        ItemCooldownDefinition def = registry.getByMaterial(event.getItemStack().getType());
        if (def == null || def.getTrigger() != CooldownTrigger.ELYTRA_BOOST) return;

        applyCooldown(player, def);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemCooldown(PlayerItemCooldownEvent event) {
        Player player = event.getPlayer();
        ItemCooldownDefinition def = registry.getByMaterial(event.getType());
        if (def == null) return;

        long remaining = cooldownManager.getRemainingMillis(player, def.getId());
        if (remaining <= 0) return;

        if ((long) event.getCooldown() * 50 < remaining) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> playerCounts = regionUseCounts.get(playerId);
        if (playerCounts == null || playerCounts.isEmpty()) return;

        Set<String> currentRegions = getPlayerRegionIds(player);
        Set<String> previousRegions = playerRegionCache.put(playerId, currentRegions);
        if (previousRegions == null) return;

        for (String regionId : previousRegions) {
            if (!currentRegions.contains(regionId)) {
                playerCounts.remove(regionId);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        regionUseCounts.remove(playerId);
        playerRegionCache.remove(playerId);
    }

    private void applyCooldown(Player player, ItemCooldownDefinition def) {
        long duration = getApplicableDuration(player, def);
        if (duration <= 0) return;

        if (def.hasAnyRegionMaxUses()) {
            Set<String> regionIds = getPlayerRegionIds(player);
            playerRegionCache.put(player.getUniqueId(), regionIds);
            for (String regionId : regionIds) {
                if (def.hasRegionMaxUses(regionId)) {
                    incrementRegionUseCount(player.getUniqueId(), regionId, def.getId());
                }
            }
        }

        cooldownManager.set(player, def.getId(), duration, def.getMaterial());
    }

    private long getApplicableDuration(Player player, ItemCooldownDefinition def) {
        String worldName = player.getWorld().getName();
        if (def.hasWorldDuration(worldName)) {
            return def.getWorldDurationMs(worldName);
        }

        if (def.hasAnyRegionDuration()) {
            for (String regionId : getPlayerRegionIds(player)) {
                if (def.hasRegionDuration(regionId)) {
                    return def.getRegionDurationMs(regionId);
                }
            }
        }

        return def.getDurationMs();
    }

    public int getRegionUseCount(UUID playerId, String regionId, String definitionId) {
        ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> playerCounts = regionUseCounts.get(playerId);
        if (playerCounts == null) return 0;
        ConcurrentHashMap<String, Integer> regionCounts = playerCounts.get(regionId);
        if (regionCounts == null) return 0;
        return regionCounts.getOrDefault(definitionId, 0);
    }

    private void incrementRegionUseCount(UUID playerId, String regionId, String definitionId) {
        regionUseCounts
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(regionId, k -> new ConcurrentHashMap<>())
                .merge(definitionId, 1, Integer::sum);
    }

    public Set<String> getPlayerRegionIds(Player player) {
        if (!isWorldGuardAvailable()) return Collections.emptySet();
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            Set<String> ids = new HashSet<>();
            for (ProtectedRegion region : query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()))) {
                ids.add(region.getId());
            }
            return ids;
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private static boolean isWorldGuardAvailable() {
        if (worldGuardPresent == null) {
            worldGuardPresent = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        }
        return worldGuardPresent;
    }
}

package net.exylia.commons.v2.region.listener;

import net.exylia.commons.v2.region.RegionManager;
import net.exylia.commons.v2.region.blocks.PlayerBlockTracker;
import net.exylia.commons.v2.region.blocks.TemporaryBlockManager;
import net.exylia.commons.v2.region.model.RegionFlag;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class RegionListener implements Listener {
    private final JavaPlugin plugin;
    private final RegionManager manager;

    public RegionListener(JavaPlugin plugin, RegionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        Player player = event.getPlayer();
        boolean allowed = manager.processPlayerMovement(player, from, to);

        if (!allowed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        Player player = event.getPlayer();
        boolean allowed = manager.processPlayerMovement(player, from, to);

        if (!allowed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        List<Region> regions = manager.getRegionsAt(location);
        if (regions.isEmpty()) {
            return;
        }
        Region region = regions.getFirst();
        if (!region.getFlagValue(RegionFlag.BREAK)) {
            if (region.getFlagValue(RegionFlag.BREAKABLE_BLOCKS_ONLY)
                    && region.isBreakableMaterial(event.getBlock().getType())) {
                event.setCancelled(false);
                return;
            }
            if (!player.hasPermission("exylia.region.bypass.break") && !region.isOwner(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        if (region.getFlagValue(RegionFlag.PLAYER_BUILD_ONLY)) {
            boolean isPlayerBlock = PlayerBlockTracker.getInstance().isPlayerPlacedBlock(region.getId(), location);
            if (!isPlayerBlock) {
                event.setCancelled(true);
                return;
            }
        }

        if (region.getFlagValue(RegionFlag.REGION_MEMBERS_ONLY)) {
            if (!region.isMember(player.getUniqueId()) && !player.hasPermission("exylia.region.bypass.members")) {
                event.setCancelled(true);
                return;
            }
        }

        PlayerBlockTracker.getInstance().removeBlock(region.getId(), location);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        List<Region> regions = manager.getRegionsAt(location);
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0);

        if (!region.getFlagValue(RegionFlag.BUILD)) {
            if (!player.hasPermission("exylia.region.bypass.build") && !region.isOwner(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        if (region.getFlagValue(RegionFlag.ALLOWED_BLOCKS_ONLY)) {
            Material material = event.getBlock().getType();
            if (!region.isMaterialAllowed(material)) {
                event.setCancelled(true);
                return;
            }
        }

        if (region.getFlagValue(RegionFlag.REGION_MEMBERS_ONLY)) {
            if (!region.isMember(player.getUniqueId()) && !player.hasPermission("exylia.region.bypass.members")) {
                event.setCancelled(true);
                return;
            }
        }

        if (region.getFlagValue(RegionFlag.PLAYER_BUILD_ONLY)) {
            PlayerBlockTracker.getInstance().trackBlock(region.getId(), player.getUniqueId(), location);
        }

        if (region.getFlagValue(RegionFlag.TEMPORARY_BLOCKS)) {
            int seconds = region.getTemporaryBlocksSeconds();
            boolean reGive = region.getFlagValue(RegionFlag.RE_GIVE_BLOCKS);
            TemporaryBlockManager.getInstance().addTemporaryBlock(location, player, seconds, reGive);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getLocation();

        List<Region> regions = manager.getRegionsAt(location);
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0);

        if (!region.getFlagValue(RegionFlag.INTERACT)) {
            if (!player.hasPermission("exylia.region.bypass.interact") && !region.isMember(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity().getType() != EntityType.PLAYER) {
            return;
        }

        if (event.getDamager().getType() != EntityType.PLAYER) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        List<Region> regions = manager.getRegionsAt(victim.getLocation());
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0);

        if (!region.getFlagValue(RegionFlag.PVP)) {
            if (!attacker.hasPermission("exylia.region.bypass.pvp")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();

        List<Region> regions = manager.getRegionsAt(location);
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0);

        if (!region.getFlagValue(RegionFlag.ITEM_DROP)) {
            if (!player.hasPermission("exylia.region.bypass.item_drop") && !region.isMember(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        Location location = event.getItem().getLocation();

        List<Region> regions = manager.getRegionsAt(location);
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0);

        if (!region.getFlagValue(RegionFlag.ITEM_PICKUP)) {
            if (!player.hasPermission("exylia.region.bypass.item_pickup") && !region.isMember(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();

        manager.processPlayerMovement(player, location, location);
    }
}

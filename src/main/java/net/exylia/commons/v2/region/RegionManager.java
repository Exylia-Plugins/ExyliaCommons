package net.exylia.commons.v2.region;
import net.exylia.commons.v2.debug.api.DebugAPI;

import lombok.Getter;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import net.exylia.commons.v2.region.blocks.PlayerBlockTracker;
import net.exylia.commons.v2.region.blocks.TemporaryBlockManager;
import net.exylia.commons.v2.region.cache.RegionCacheManager;
import net.exylia.commons.v2.region.schematic.SchematicManager;
import net.exylia.commons.v2.region.detection.MovementDetector;
import net.exylia.commons.v2.region.detection.PlayerTracker;
import net.exylia.commons.v2.region.detection.SpatialIndex;
import net.exylia.commons.v2.region.events.*;
import net.exylia.commons.v2.region.listener.RegionListener;
import net.exylia.commons.v2.region.model.RegionFlag;
import net.exylia.commons.v2.region.model.Region;
import net.exylia.commons.v2.region.selection.SelectionListener;
import net.exylia.commons.v2.region.selection.SelectionManager;
import net.exylia.commons.v2.region.visual.RegionSelector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;


public class RegionManager implements Listener {
    private static RegionManager instance;

    private final JavaPlugin plugin;
    private final Map<String, Region> regions;
    private final Map<UUID, Set<Region>> playerRegions;

    @Getter
    private final SpatialIndex spatialIndex;
    @Getter
    private final RegionCacheManager cacheManager;
    @Getter
    private final MovementDetector movementDetector;
    @Getter
    private final PlayerTracker playerTracker;

    private RegionListener regionListener;
    private ScheduledTask cleanupTask;
    private volatile boolean shuttingDown;

    private RegionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.regions = new ConcurrentHashMap<>();
        this.playerRegions = new ConcurrentHashMap<>();

        this.spatialIndex = new SpatialIndex();
        this.cacheManager = new RegionCacheManager(spatialIndex);
        this.playerTracker = new PlayerTracker();
        this.movementDetector = new MovementDetector(spatialIndex, playerTracker);

        PlayerBlockTracker.initialize(plugin);
        TemporaryBlockManager.initialize(plugin);
        SelectionManager.initialize(plugin);

        SchematicManager.initialize(plugin);

        this.regionListener = new RegionListener(plugin, this);
        SelectionListener selectionListener = new SelectionListener(plugin, SelectionManager.getInstance());
        this.shuttingDown = false;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getPluginManager().registerEvents(regionListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(selectionListener, plugin);

        startPeriodicTasks();

    }

    public static void initialize(JavaPlugin plugin) {
        synchronized (RegionManager.class) {
            if (instance != null && instance.plugin == plugin) {
                return;
            }

            if (instance != null) {
                instance.cleanup();
            }

            instance = new RegionManager(plugin);
        }
    }

    public static RegionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RegionManagerV2 has not been initialized");
        }
        return instance;
    }

    public boolean registerRegion(Region region) {
        if (!region.isValid()) {
            return false;
        }

        Region previousRegion = regions.put(region.getId(), region);
        if (previousRegion != null) {
            spatialIndex.removeRegion(previousRegion);
            cacheManager.invalidateRegion(previousRegion);
        }

        RegionCreateEvent createEvent = new RegionCreateEvent(region);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(createEvent);
        } else {
            Tasks.run(() -> Bukkit.getPluginManager().callEvent(createEvent));
        }

        spatialIndex.addRegion(region);
        cacheManager.invalidateRegion(region);

        DebugAPI.logLibDebug("Region registered: " + region.getInfo());
        return true;
    }

    public Region createRegion(String regionId, Location pos1, Location pos2) {
        net.exylia.commons.v2.region.selection.Selection selection = net.exylia.commons.v2.region.selection.Selection.of(pos1, pos2);
        Region region = new Region(regionId, selection);

        if (registerRegion(region)) {
            return region;
        }

        return null;
    }

    public boolean unregisterRegion(String regionId) {
        Region region = regions.remove(regionId);
        if (region == null) {
            return false;
        }

        RegionDeleteEvent deleteEvent = new RegionDeleteEvent(region);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(deleteEvent);
        } else {
            Tasks.run(() -> Bukkit.getPluginManager().callEvent(deleteEvent));
        }

        spatialIndex.removeRegion(region);
        cacheManager.invalidateRegion(region);
        PlayerBlockTracker.getInstance().clearRegionBlocks(region.getId());

        Set<Player> playersToRemove = new HashSet<>(region.getPlayersInside());
        for (Player player : playersToRemove) {
            handlePlayerExit(player, region);
        }

        DebugAPI.logLibDebug("Region unregistered: " + region.getId());
        return true;
    }

    public void unregisterAllRegions() {
        Collection<Region> allRegions = new ArrayList<>(regions.values());

        for (Region region : allRegions) {
            spatialIndex.removeRegion(region);
            PlayerBlockTracker.getInstance().clearRegionBlocks(region.getId());

            Set<Player> playersToRemove = new HashSet<>(region.getPlayersInside());
            for (Player player : playersToRemove) {
                handlePlayerExit(player, region);
            }
        }

        regions.clear();
        cacheManager.invalidateAll();
        DebugAPI.logLibInfo("All regions unregistered");
    }

    public Optional<Region> getRegion(String regionId) {
        return Optional.ofNullable(regions.get(regionId));
    }

    public Collection<Region> getAllRegions() {
        return new ArrayList<>(regions.values());
    }

    public List<Region> getRegionsAt(Location location) {
        return cacheManager.getRegionsAt(location);
    }

    public Optional<Region> getHighestPriorityRegionAt(Location location) {
        List<Region> regions = getRegionsAt(location);
        return regions.isEmpty() ? Optional.empty() : Optional.of(regions.get(0));
    }

    public List<Region> findRegions(Predicate<Region> filter) {
        return regions.values().stream()
                .filter(filter)
                .toList();
    }

    public Set<Region> getPlayerRegions(Player player) {
        return playerRegions.getOrDefault(player.getUniqueId(), Collections.emptySet());
    }

    public boolean isPlayerInRegion(Player player, Region region) {
        Set<Region> regions = playerRegions.get(player.getUniqueId());
        return regions != null && regions.contains(region);
    }

    public boolean isPlayerInAnyRegion(Player player) {
        Set<Region> regions = playerRegions.get(player.getUniqueId());
        return regions != null && !regions.isEmpty();
    }

    public boolean processPlayerMovement(Player player, Location from, Location to) {
        if (shuttingDown) {
            return true;
        }

        MovementDetector.MovementResult result = movementDetector.checkMovement(player, from, to);

        if (!result.isRequiresUpdate()) {
            return true;
        }

        List<Region> enterRegions = result.getEnterRegions();
        List<Region> exitRegions = result.getExitRegions();

        for (Region region : exitRegions) {
            if (!canPlayerExitRegion(player, region, from, to)) {
                DebugAPI.logLibDebug(String.format("Exit blocked for player %s from region %s", player.getName(), region.getId()));
                return false;
            }
        }

        for (Region region : enterRegions) {
            if (!canPlayerEnterRegion(player, region, from, to)) {
                DebugAPI.logLibDebug(String.format("Entry blocked for player %s to region %s", player.getName(), region.getId()));
                return false;
            }
        }

        for (Region region : exitRegions) {
            handlePlayerExit(player, region);
        }

        for (Region region : enterRegions) {
            handlePlayerEnter(player, region);
        }

        return true;
    }

    private boolean canPlayerEnterRegion(Player player, Region region, Location from, Location to) {
        RegionPreEnterEvent preEnterEvent = new RegionPreEnterEvent(player, region, from, to);
        Bukkit.getPluginManager().callEvent(preEnterEvent);

        if (preEnterEvent.isCancelled()) {
            if (preEnterEvent.getCancelMessage() != null) {
                player.sendMessage(preEnterEvent.getCancelMessage());
            }
            return false;
        }

        if (!region.getFlagValue(RegionFlag.ENTRY)) {
            if (!player.hasPermission("exylia.region.bypass.entry") &&
                    !region.isMember(player.getUniqueId())) {
                return false;
            }
        }

        if (region.getFlagValue(RegionFlag.REGION_MEMBERS_ONLY)) {
            return region.isMember(player.getUniqueId()) || player.hasPermission("exylia.region.bypass.members");
        }

        return true;
    }

    private boolean canPlayerExitRegion(Player player, Region region, Location from, Location to) {
        RegionPreExitEvent preExitEvent = new RegionPreExitEvent(player, region, from, to);
        Bukkit.getPluginManager().callEvent(preExitEvent);

        if (preExitEvent.isCancelled()) {
            if (preExitEvent.getCancelMessage() != null) {
                player.sendMessage(preExitEvent.getCancelMessage());
            }
            return false;
        }

        if (!region.getFlagValue(RegionFlag.EXIT)) {
            if (!player.hasPermission("exylia.region.bypass.exit") &&
                    !region.isOwner(player.getUniqueId())) {
                return false;
            }
        }

        return true;
    }

    private void handlePlayerEnter(Player player, Region region) {
        DebugAPI.logLibDebug(String.format("Player %s entering region %s", player.getName(), region.getId()));

        region.addPlayer(player);
        playerRegions.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(region);

        RegionEnterEvent enterEvent = new RegionEnterEvent(player, region);
        Bukkit.getPluginManager().callEvent(enterEvent);

        if (region.getOnEnter() != null) {
            try {
                region.getOnEnter().execute(player, region);
            } catch (Exception e) {
                DebugAPI.logLibDebug("Error executing onEnter callback: " + e.getMessage());
            }
        }
    }

    private void handlePlayerExit(Player player, Region region) {
        DebugAPI.logLibDebug(String.format("Player %s exiting region %s", player.getName(), region.getId()));

        region.removePlayer(player);
        Set<Region> regions = playerRegions.get(player.getUniqueId());
        if (regions != null) {
            regions.remove(region);
            if (regions.isEmpty()) {
                playerRegions.remove(player.getUniqueId());
            }
        }

        RegionExitEvent exitEvent = new RegionExitEvent(player, region);
        Bukkit.getPluginManager().callEvent(exitEvent);

        if (region.getOnExit() != null) {
            try {
                region.getOnExit().execute(player, region);
            } catch (Exception e) {
                DebugAPI.logLibDebug("Error executing onExit callback: " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (shuttingDown) {
            return;
        }

        Player player = event.getPlayer();
        cleanupPlayer(player);
    }

    public CompletableFuture<Boolean> restoreRegion(Region region) {
        return SchematicManager.getInstance().regenerateRegion(region);
    }

    public void cleanEntities(Region region) {
        World world = region.getWorld();
        if (world == null) return;

        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();
        BoundingBox box = new BoundingBox(
                min.getX(), min.getY(), min.getZ(),
                max.getX() + 1, max.getY() + 1, max.getZ() + 1
        );

        int[] count = {0};
        world.getNearbyEntities(box, e ->
                e instanceof EnderCrystal
                || e instanceof Minecart
                || e instanceof AbstractArrow
                || e instanceof Item
                || e instanceof ExperienceOrb
                || e instanceof Fireball
                || e instanceof Snowball
                || e instanceof Egg
                || e instanceof ThrownExpBottle
                || e instanceof Firework
        ).forEach(e -> { e.remove(); count[0]++; });

        DebugAPI.logLibInfo("[Restore] Removed " + count[0] + " entities in region '" + region.getId() + "'");
    }

    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();

        Set<Region> regions = playerRegions.remove(playerId);
        if (regions != null) {
            for (Region region : regions) {
                region.removePlayer(player);

                RegionExitEvent exitEvent = new RegionExitEvent(player, region);
                Bukkit.getPluginManager().callEvent(exitEvent);
            }
        }

        playerTracker.removePlayer(playerId);
        if (PlayerBlockTracker.isInitialized()) {
            PlayerBlockTracker.getInstance().clearPlayerBlocks(playerId);
        }
        cacheManager.removePlayerState(playerId);
        cacheManager.invalidatePlayerFlags(playerId);
    }

    private void startPeriodicTasks() {
        cleanupTask = Tasks.asyncTimer(() -> {
            for (Region region : regions.values()) {
                region.cleanupOfflinePlayers();
            }

            playerTracker.cleanupInactivePlayers();
            cacheManager.cleanup();
        }, 20L * 30, 20L * 30);
    }

    private void stopPeriodicTasks() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    public void cleanup() {
        shuttingDown = true;
        stopPeriodicTasks();

        for (Player player : Bukkit.getOnlinePlayers()) {
            cleanupPlayer(player);
        }

//        PlayerBlockTracker.getInstance().clearAll();
//        TemporaryBlockManager.getInstance().clearAll();
        SelectionManager.getInstance().cleanupAll();
        RegionSelector.getInstance().stopAllSessions();

        regions.clear();
        playerRegions.clear();
        playerTracker.clear();
        spatialIndex.clear();
        cacheManager.invalidateAll();

        synchronized (RegionManager.class) {
            if (instance == this) {
                instance = null;
            }
        }

        DebugAPI.logLibInfo("RegionManagerV2 cleaned up");
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_regions", regions.size());
        stats.put("total_players_tracked", playerRegions.size());
        stats.put("spatial_index", spatialIndex.getStats());
        stats.put("cache", cacheManager.getStats());
        stats.put("movement_detector", movementDetector.getStats());
        return stats;
    }

}

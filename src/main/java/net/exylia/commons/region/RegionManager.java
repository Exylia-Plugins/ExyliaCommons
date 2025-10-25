package net.exylia.commons.region;

import lombok.Getter;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.region.blocks.AllowedBlocksManager;
import net.exylia.commons.region.blocks.TemporaryBlocksManager;
import net.exylia.commons.region.cloning.RegionCloner;
import net.exylia.commons.region.events.*;
import net.exylia.commons.region.listener.RegionListener;
import net.exylia.commons.region.model.*;
import net.exylia.commons.region.optimization.RegionPositionTracker;
import net.exylia.commons.region.optimization.RegionSpatialIndex;
import net.exylia.commons.region.optimization.MovementOptimizer;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.exylia.commons.region.flags.FlagManager;
import net.exylia.commons.region.regeneration.RegionRegenerationManager;
import net.exylia.commons.region.blocks.PlayerBlockTracker;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public class RegionManager implements Listener {
    private static RegionManager instance;

    private final JavaPlugin plugin;
    private final Map<String, Region> regions;  
    private final Map<UUID, Set<Region>> playerRegions;  

    @Getter
    private final RegionSpatialIndex spatialIndex;  
    @Getter
    private final MovementOptimizer movementOptimizer;  

    @Getter
    private boolean asyncMovementChecking = true;
    @Getter
    private int movementCheckInterval = 1;  
    @Getter
    private final RegionPositionTracker positionTracker;  
    @Getter
    private boolean slowMovementDetection = true;
    @Getter
    private FlagManager flagManager;

    private RegionListener unifiedListener;

    private ScheduledTask cleanupTask;

    private RegionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.regions = new ConcurrentHashMap<>();
        this.playerRegions = new ConcurrentHashMap<>();

        this.spatialIndex = new RegionSpatialIndex();
        this.movementOptimizer = new MovementOptimizer(spatialIndex);
        this.positionTracker = new RegionPositionTracker(plugin, this);  

        FlagManager.initialize(plugin);
        this.flagManager = FlagManager.getInstance();

        RegionRegenerationManager.initialize(plugin);
        AllowedBlocksManager.initialize(plugin);
        TemporaryBlocksManager.initialize(plugin, this);
        PlayerBlockTracker.initialize(plugin);

        this.unifiedListener = new RegionListener(plugin, this);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        if (asyncMovementChecking) {
            startCleanupTask();
        }
        if (slowMovementDetection) {
            positionTracker.start();
        }

        scheduleDelayedFlagRepair(plugin);

        logInternalDebug("RegionManager inicializado con detección inmediata optimizada");
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new RegionManager(plugin);
        }
    }

    public static RegionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RegionManager no ha sido inicializado");
        }
        return instance;
    }

    public boolean registerRegion(Region region) {
        if (!region.isValid()) {
            return false;
        }

        RegionCreateEvent createEvent = new RegionCreateEvent(region);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(createEvent);
        } else {
            Schedulers.sync(() -> {
                Bukkit.getPluginManager().callEvent(createEvent);
            });
        }

        if (createEvent.isCancelled()) {
            return false;
        }

        regions.put(region.getId(), region);

        spatialIndex.addRegion(region);

        logInternalDebug("Región registrada con spatial index: " + region.getInfo());

        return true;
    }

    public Region createRegion(String regionId, Selection selection) {
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
        Bukkit.getPluginManager().callEvent(deleteEvent);

        spatialIndex.removeRegion(region);

        Set<Player> playersToRemove = new HashSet<>(region.getPlayersInside());
        for (Player player : playersToRemove) {
            handlePlayerExit(player, region);
        }

        logInternalDebug("Región eliminada del spatial index: " + region.getInfo());

        return true;
    }

    public void unregisterAllRegions() {
        Collection<Region> allRegions = new ArrayList<>(regions.values());

        for (Region region : allRegions) {
             
            spatialIndex.removeRegion(region);

            Set<Player> playersToRemove = new HashSet<>(region.getPlayersInside());
            for (Player player : playersToRemove) {
                handlePlayerExit(player, region);
            }
        }

        regions.clear();
        logInternalDebug("Todas las regiones han sido eliminadas y spatial index limpiado");
    }

    public Optional<Region> getRegion(String regionId) {
        return Optional.ofNullable(regions.get(regionId));
    }

    public Collection<Region> getRegions() {
        return new ArrayList<>(regions.values());
    }

    public Collection<Region> getAllRegions() {
        return getRegions();
    }

    public List<Region> getRegionsAt(Location location) {
        return spatialIndex.getRegionsAt(location);
    }

    public Optional<Region> getHighestPriorityRegionAt(Location location) {
        List<Region> regions = getRegionsAt(location);
        return regions.isEmpty() ? Optional.empty() : Optional.of(regions.get(0));
    }

    public List<Region> findRegions(Predicate<Region> filter) {
        return regions.values().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }
     
    public Optional<Region> findClosestRegion(Location location, Collection<Region> regions) {
        if (regions == null || regions.isEmpty()) {
            return Optional.empty();
        }

        Region closestRegion = null;
        double minDistance = Double.MAX_VALUE;

        for (Region region : regions) {
            double distance = calculateDistanceToRegion(location, region);

            if (distance < minDistance) {
                minDistance = distance;
                closestRegion = region;
            }
        }

        return Optional.ofNullable(closestRegion);
    }

    private double calculateDistanceToRegion(Location location, Region region) {
         
        if (region.contains(location)) {
            return 0.0;
        }

        Location minPoint = region.getMinimumPoint();
        Location maxPoint = region.getMaximumPoint();

        if (!location.getWorld().equals(minPoint.getWorld())) {
            return Double.MAX_VALUE;  
        }

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        double closestX = Math.max(minPoint.getX(), Math.min(x, maxPoint.getX()));
        double closestY = Math.max(minPoint.getY(), Math.min(y, maxPoint.getY()));
        double closestZ = Math.max(minPoint.getZ(), Math.min(z, maxPoint.getZ()));

        double dx = x - closestX;
        double dy = y - closestY;
        double dz = z - closestZ;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
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
         
        MovementOptimizer.MovementResult result = movementOptimizer.checkMovement(player, from, to);

        if (!result.requiresUpdate) {
            positionTracker.updatePlayerPosition(player, to);
            return true;
        }

        boolean movementAllowed = flagManager.canPlayerPerformActionAt(player, to, RegionFlag.ENTRY);
        if (!movementAllowed) {
            logInternalDebug(String.format(
                    "Movement blocked by flags for player %s to location %s",
                    player.getName(), to));
            return false;
        }

        Set<Region> currentRegions = result.currentRegions;
        Set<Region> previousRegions = result.previousRegions;

        Set<Region> enterRegions = result.getEnterRegions();
        for (Region region : enterRegions) {
            if (!canPlayerEnterRegion(player, region, from, to)) {
                logInternalDebug(String.format(
                        "Entry blocked for player %s to region %s",
                        player.getName(), region.getId()));
                return false;
            }
        }

        Set<Region> exitRegions = result.getExitRegions();
        for (Region region : exitRegions) {
            if (!canPlayerExitRegion(player, region, from, to)) {
                logInternalDebug(String.format(
                        "Exit blocked for player %s from region %s",
                        player.getName(), region.getId()));
                return false;
            }
        }

        if (!exitRegions.isEmpty() || !enterRegions.isEmpty()) {
            logInternalDebug(String.format(
                    "Player %s movement: entering %d regions, exiting %d regions",
                    player.getName(), enterRegions.size(), exitRegions.size()));
        }

        for (Region region : exitRegions) {
            handlePlayerExit(player, region);
        }

        for (Region region : enterRegions) {
            handlePlayerEnter(player, region);
        }

        Set<Region> stayRegions = new HashSet<>(previousRegions);
        stayRegions.retainAll(currentRegions);

        for (Region region : stayRegions) {
            handlePlayerMove(player, region, from, to);
        }
        positionTracker.updatePlayerPosition(player, to);
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
                    !region.isMember(player.getUniqueId()) &&
                    !region.isOwner(player.getUniqueId())) {
                return false;
            }
        }

        if (region.getMetadata("whitelist-only", Boolean.class) == Boolean.TRUE) {
            return region.isMember(player.getUniqueId()) || region.isOwner(player.getUniqueId());
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
        logInternalDebug(String.format(
                "Processing ENTER for player %s to region %s",
                player.getName(), region.getId()));

        region.addPlayer(player);
        playerRegions.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(region);

        flagManager.applyRegionEffects(player, region);

        RegionEnterEvent enterEvent = new RegionEnterEvent(player, region);
        Bukkit.getPluginManager().callEvent(enterEvent);

        if (region.getOnEnter() != null) {
            try {
                region.getOnEnter().execute(player, region);
            } catch (Exception e) {
                logInternalWarn("Error executing onEnter callback: " + e.getMessage());
            }
        }
    }

    private void handlePlayerExit(Player player, Region region) {
        logInternalDebug(String.format(
                "Processing EXIT for player %s from region %s",
                player.getName(), region.getId()));

        flagManager.removeRegionEffects(player, region);

        region.removePlayer(player);
        Set<Region> regions = playerRegions.get(player.getUniqueId());
        if (regions != null) {
            regions.remove(region);
        }

        RegionExitEvent exitEvent = new RegionExitEvent(player, region);
        Bukkit.getPluginManager().callEvent(exitEvent);

        logInternalDebug(String.format(
                "EXIT event fired for player %s from region %s with flags removed",
                player.getName(), region.getId()));

        if (region.getOnExit() != null) {
            try {
                region.getOnExit().execute(player, region);
            } catch (Exception e) {
                logInternalWarn("Error executing onExit callback: " + e.getMessage());
            }
        }
    }

    private void handlePlayerMove(Player player, Region region, Location from, Location to) {
         
        RegionMoveEvent moveEvent = new RegionMoveEvent(player, region, from, to);
        Bukkit.getPluginManager().callEvent(moveEvent);

        if (region.getOnMove() != null) {
            region.getOnMove().execute(player, region);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        cleanupPlayer(player);
    }

    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();

        movementOptimizer.cleanupPlayer(playerId);
        positionTracker.cleanupPlayer(playerId);

        Set<Region> regions = playerRegions.remove(playerId);
        if (regions != null) {
            for (Region region : regions) {
                 
                flagManager.removeRegionEffects(player, region);

                region.removePlayer(player);

                RegionExitEvent exitEvent = new RegionExitEvent(player, region);
                Bukkit.getPluginManager().callEvent(exitEvent);
            }
        }

        flagManager.cleanupPlayerState(player);
    }

    public boolean canPlayerPerformAction(Player player, RegionFlag flag) {
        return flagManager.canPlayerPerformAction(player, flag);
    }

    public boolean canPlayerPerformActionAt(Player player, Location location, RegionFlag flag) {
        return flagManager.canPlayerPerformActionAt(player, location, flag);
    }

    public boolean isPvpAllowed(Player attacker, Player target) {
        return flagManager.isPvpAllowed(attacker, target);
    }

    public boolean canPlayerPlaceMaterial(Player player, Location location, Material material) {
         
        if (!canPlayerPerformActionAt(player, location, RegionFlag.BUILD)) {
            return false;
        }

        List<Region> regions = getRegionsAt(location);
        if (regions.isEmpty()) {
            return true;  
        }

        Region region = regions.get(0);

        if (region.hasAllowedBlocksOnly()) {
            return region.isMaterialAllowed(material);
        }

        return true;  
    }

    public void refreshPlayerFlags(Player player) {
        Set<Region> currentRegions = playerRegions.get(player.getUniqueId());
        if (currentRegions != null) {
             
            for (Region region : currentRegions) {
                flagManager.removeRegionEffects(player, region);
            }

            List<Region> newRegions = getRegionsAt(player.getLocation());
            for (Region region : newRegions) {
                if (currentRegions.contains(region)) {
                    flagManager.applyRegionEffects(player, region);
                }
            }
        }

        flagManager.invalidatePlayerCache(player);
    }

    public void onRegionFlagChanged(Region region, RegionFlag flag) {
         
        flagManager.invalidateRegionFlagCache(region, flag);

        for (Player player : region.getPlayersInside()) {
             
            if (flag.affectsCombat() || flag.affectsMovement() || flag.affectsBuilding()) {
                refreshPlayerFlags(player);
            }
        }
    }

    public void setSlowMovementDetection(boolean enabled) {
        this.slowMovementDetection = enabled;

        if (enabled) {
            positionTracker.start();
            logInternalDebug("Slow movement detection ENABLED");
        } else {
            positionTracker.stop();
            logInternalDebug("Slow movement detection DISABLED");
        }
    }

    public void forcePositionCheck(Player player) {
        positionTracker.forceCheck(player);
    }

    public void forcePositionCheckAll() {
        positionTracker.forceCheckAll();
    }

    public RegionPositionTracker.PositionTrackerStats getPositionTrackerStats() {
        return positionTracker.getStats();
    }

    public void setAsyncMovementChecking(boolean enabled) {
        this.asyncMovementChecking = enabled;

        if (enabled && cleanupTask == null) {
            startCleanupTask();
        } else if (!enabled && cleanupTask != null) {
            stopCleanupTask();
        }
    }

    public void setMovementCheckInterval(int ticks) {
        this.movementCheckInterval = Math.max(1, ticks);
         
    }

    private void startCleanupTask() {
        if (cleanupTask != null) {
            return;
        }

        cleanupTask = Schedulers.asyncTimer(() -> {

            for (Player player : Bukkit.getOnlinePlayers()) {
                regions.values().forEach(Region::cleanupOfflinePlayers);
            }

            movementOptimizer.cleanupInactivePlayers();
        }, 20L * 30, 20L * 30);
    }

    private void stopCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    public RegionRegenerationManager.RegenerationStats getRegenerationStats() {
        return RegionRegenerationManager.getInstance().getStats();
    }

    public void clearRegenerationCache() {
        RegionRegenerationManager.getInstance().clearCache();
    }

    public void clearRegionPlayerBlocks(String regionId) {
        PlayerBlockTracker.getInstance().clearRegionBlocks(regionId);
    }

    public PlayerBlockTracker.BlockTrackerStats getBlockTrackerStats() {
        return PlayerBlockTracker.getInstance().getStats();
    }

    public CompletableFuture<Void> savePlayerBlocksAsync() {
        return PlayerBlockTracker.getInstance().saveDataAsync();
    }

    public TemporaryBlocksManager.TemporaryBlocksStats getTemporaryBlocksStats() {
        return TemporaryBlocksManager.getInstance().getStats();
    }

    public List<Region> getRegionsWithTemporaryBlocks() {
        return regions.values().stream()
                .filter(Region::hasTemporaryBlocks)
                .collect(Collectors.toList());
    }

    public Map<String, List<TemporaryBlocksManager.TemporaryBlock>> getActiveTemporaryBlocks() {
        Map<String, List<TemporaryBlocksManager.TemporaryBlock>> activeBlocks = new HashMap<>();

        for (Region region : getRegionsWithTemporaryBlocks()) {
            List<TemporaryBlocksManager.TemporaryBlock> regionBlocks = new ArrayList<>();

            Location min = region.getMinimumPoint();
            Location max = region.getMaximumPoint();

            for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                    for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                        Location loc = new Location(min.getWorld(), x, y, z);
                        TemporaryBlocksManager.TemporaryBlock tempBlock =
                                TemporaryBlocksManager.getInstance().getTemporaryBlock(loc);

                        if (tempBlock != null) {
                            regionBlocks.add(tempBlock);
                        }
                    }
                }
            }

            if (!regionBlocks.isEmpty()) {
                activeBlocks.put(region.getId(), regionBlocks);
            }
        }

        return activeBlocks;
    }

    public CompletableFuture<Integer> clearAllTemporaryBlocks() {
        return CompletableFuture.supplyAsync(() -> {
            int cleared = 0;
            Map<String, List<TemporaryBlocksManager.TemporaryBlock>> activeBlocks = getActiveTemporaryBlocks();

            for (Map.Entry<String, List<TemporaryBlocksManager.TemporaryBlock>> entry : activeBlocks.entrySet()) {
                for (TemporaryBlocksManager.TemporaryBlock block : entry.getValue()) {
                    TemporaryBlocksManager.getInstance().cancelBlockRemoval(block.getLocation());
                    block.getLocation().getBlock().setType(Material.AIR);
                    cleared++;
                }
            }

            logInternalDebug("Limpiados " + cleared + " bloques temporales activos");
            return cleared;
        });
    }

    public void cleanup() {
        stopCleanupTask();
        positionTracker.stop();

        for (Player player : Bukkit.getOnlinePlayers()) {
            cleanupPlayer(player);
        }

        TemporaryBlocksManager.getInstance().shutdown();
        RegionRegenerationManager.getInstance().shutdown();
        PlayerBlockTracker.getInstance().shutdown();

        regions.clear();
        playerRegions.clear();
        RegionCloner.getInstance().cleanup();

        logInternalDebug("RegionManager optimizado limpiado completamente");
    }

    private static class PendingMovement {
        final Player player;
        final Location from;
        final Location to;
        final long timestamp;

        PendingMovement(Player player, Location from, Location to, long timestamp) {
            this.player = player;
            this.from = from.clone();
            this.to = to.clone();
            this.timestamp = timestamp;
        }
    }

    public CompletableFuture<Region> cloneRegion(String sourceRegionId, Location targetCenter) {
        Optional<Region> sourceRegion = getRegion(sourceRegionId);
        if (sourceRegion.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return RegionCloner.getInstance().cloneRegion(sourceRegion.get(), targetCenter);
    }

    public CompletableFuture<Region> cloneRegion(String sourceRegionId, Location targetCenter, String customName) {
        Optional<Region> sourceRegion = getRegion(sourceRegionId);
        if (sourceRegion.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return RegionCloner.getInstance().cloneRegion(sourceRegion.get(), targetCenter, customName);
    }

    public CompletableFuture<Region> cloneRegion(String sourceRegionId, Location targetCenter, World targetWorld) {
        Optional<Region> sourceRegion = getRegion(sourceRegionId);
        if (sourceRegion.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return RegionCloner.getInstance().cloneRegion(sourceRegion.get(), targetCenter, targetWorld);
    }

    public CompletableFuture<Boolean> cloneMultipleRegions(String[] sourceRegionIds, Location[] targetCenters) {
        if (sourceRegionIds.length != targetCenters.length) {
            return CompletableFuture.completedFuture(false);
        }

        Region[] sourceRegions = new Region[sourceRegionIds.length];
        for (int i = 0; i < sourceRegionIds.length; i++) {
            Optional<Region> region = getRegion(sourceRegionIds[i]);
            if (region.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }
            sourceRegions[i] = region.get();
        }

        return RegionCloner.getInstance().cloneMultipleRegions(sourceRegions, targetCenters);
    }

    public boolean isRegionBeingCloned(String regionId) {
        return RegionCloner.getInstance().isCloneInProgress(regionId);
    }

    public int getActiveCloneOperationsCount() {
        return RegionCloner.getInstance().getActiveCloneCount();
    }

    public RegionCloner.CloneOperation getCloneOperation(String operationId) {
        return RegionCloner.getInstance().getCloneOperation(operationId);
    }

    public CompletableFuture<Boolean> copyRegionStructure(String sourceRegionId, Location targetCenter) {
        Optional<Region> sourceRegion = getRegion(sourceRegionId);
        if (sourceRegion.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        return RegionRegenerationManager.getInstance().copyRegionStructure(sourceRegion.get(), targetCenter);
    }

    public Collection<Region> findRegionsByPattern(String pattern) {
        return regions.values().stream()
                .filter(region -> region.getId().contains(pattern) ||
                        region.getDisplayName().contains(pattern))
                .collect(Collectors.toList());
    }

    public Collection<Region> getRegionsInWorld(World world) {
        return regions.values().stream()
                .filter(region -> region.getWorld().equals(world))
                .collect(Collectors.toList());
    }

    public Optional<Region> findCloneSource(String cloneRegionId) {
        if (!cloneRegionId.contains("_clone_")) {
            return Optional.empty();
        }

        String sourceId = cloneRegionId.substring(0, cloneRegionId.indexOf("_clone_"));
        return getRegion(sourceId);
    }

    public Collection<Region> findAllClonesOf(String sourceRegionId) {
        String clonePrefix = sourceRegionId + "_clone_";
        return regions.values().stream()
                .filter(region -> region.getId().startsWith(clonePrefix))
                .collect(Collectors.toList());
    }

    private void scheduleDelayedFlagRepair(JavaPlugin plugin) {
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            logInternalDebug("Starting automatic flag repair check...");
            net.exylia.commons.region.flags.FlagRepair.repairBuildingFlags();
        }, 40L);
    }
}

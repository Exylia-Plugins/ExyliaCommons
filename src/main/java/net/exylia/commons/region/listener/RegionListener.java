package net.exylia.commons.region.listener;

import lombok.Getter;
import net.exylia.commons.region.RegionManager;
import net.exylia.commons.region.blocks.AllowedBlocksManager;
import net.exylia.commons.region.blocks.PlayerBlockTracker;
import net.exylia.commons.region.blocks.TemporaryBlocksManager;
import net.exylia.commons.region.events.TemporaryBlockRemovedEvent;
import net.exylia.commons.region.flags.FlagManager;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

public class RegionListener implements Listener {

    private final JavaPlugin plugin;
    private final RegionManager regionManager;
    private final FlagManager flagManager;
    private final PlayerBlockTracker blockTracker;
    private final AllowedBlocksManager allowedBlocksManager;
    private final TemporaryBlocksManager temporaryBlocksManager;

    // ===== CACHE DE VALIDACIONES (mantener solo este cache) =====
    private final ConcurrentHashMap<String, CachedValidation> validationCache;
    private static final long VALIDATION_CACHE_EXPIRE = 1000; // REDUCIDO a 1 segundo para mejor respuesta

    // ===== ESTADÍSTICAS =====
    private volatile long totalEvents = 0;
    private volatile long cachedValidations = 0;
    private volatile long immediateMovements = 0;

    public RegionListener(JavaPlugin plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.flagManager = FlagManager.getInstance();
        this.blockTracker = PlayerBlockTracker.getInstance();
        this.allowedBlocksManager = AllowedBlocksManager.getInstance();
        this.temporaryBlocksManager = TemporaryBlocksManager.getInstance();

        // Solo cache de validaciones - SIN batching
        this.validationCache = new ConcurrentHashMap<>();

        // Registrar eventos
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Solo tarea de limpieza de cache
        startCacheCleanupTask();

        logInternalDebug("UnifiedRegionListener iniciado con procesamiento inmediato");
    }

    // ===== EVENTOS DE CONSTRUCCIÓN (sin cambios significativos) =====

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        totalEvents++;

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        Material material = event.getBlock().getType();

        // Cache de validación (reducido a 1 segundo)
        String cacheKey = getValidationCacheKey(player, location, "place", material.name());
        CachedValidation cached = validationCache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            cachedValidations++;
            if (!cached.allowed) {
                event.setCancelled(true);
                return;
            } else {
                executePostPlacementEffects(player, cached.region, location, material);
                if (cached.region != null) {
                    logBlockPlacementInfo(player, cached.region, location, material, "CACHED_PLACEMENT");
                }
                return;
            }
        }

        List<Region> regions = regionManager.getRegionsAt(location);
        if (regions.isEmpty()) {
            cacheValidation(cacheKey, true, null);
            return;
        }

        Region region = regions.get(0);
        ActionResult result = validateBlockPlacement(player, region, location, material);

        cacheValidation(cacheKey, result.isAllowed(), region);

        if (!result.isAllowed()) {
            event.setCancelled(true);
            logInternalDebug(String.format(
                    "Colocación de bloque denegada: %s intentó colocar %s en región %s - Razón: %s",
                    player.getName(), material.name(), region.getId(), result.getReason()
            ));
        } else {
            executePostPlacementEffects(player, region, location, material);
            logBlockPlacementInfo(player, region, location, material, "DIRECT_PLACEMENT");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        totalEvents++;

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        String cacheKey = getValidationCacheKey(player, location, "break", "");
        CachedValidation cached = validationCache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            cachedValidations++;
            if (!cached.allowed) {
                event.setCancelled(true);
                return;
            } else {
                executePostBreakEffects(player, cached.region, location);
                return;
            }
        }

        List<Region> regions = regionManager.getRegionsAt(location);
        if (regions.isEmpty()) {
            cacheValidation(cacheKey, true, null);
            return;
        }

        Region region = regions.get(0);
        ActionResult result = validateBlockBreaking(player, region, location);

        cacheValidation(cacheKey, result.isAllowed(), region);

        if (!result.isAllowed()) {
            event.setCancelled(true);
//            logInternalDebug(String.format(
//                    "Rotura de bloque denegada: %s intentó romper bloque en región %s - Razón: %s",
//                    player.getName(), region.getId(), result.getReason()
//            ));
        } else {
            executePostBreakEffects(player, region, location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTemporaryBlockRemoved(TemporaryBlockRemovedEvent event) {
        totalEvents++;

        Location location = event.getLocation();
        String regionId = event.getRegionId();

        // Si tenemos un regionId válido, notificar al PlayerBlockTracker
        if (regionId != null) {
            // Verificar si el bloque estaba siendo rastreado
            if (blockTracker.isPlayerPlacedBlock(regionId, location)) {
                blockTracker.removePlayerBlock(regionId, location);

                logInternalDebug(String.format(
                        "Bloque temporal removido del tracker: %s en región %s (vivió %d segundos)",
                        event.getMaterial().name(),
                        regionId,
                        event.getLifetimeSeconds()
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        totalEvents++;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Location location = block.getLocation();

        String cacheKey = getValidationCacheKey(player, location, "interact", block.getType().name());
        CachedValidation cached = validationCache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            cachedValidations++;
            if (!cached.allowed) {
                event.setCancelled(true);
                return;
            }
        } else {
            List<Region> regions = regionManager.getRegionsAt(location);
            if (regions.isEmpty()) {
                cacheValidation(cacheKey, true, null);
                return;
            }

            Region region = regions.get(0);
            ActionResult result = validateInteraction(player, region, location, block.getType());

            cacheValidation(cacheKey, result.isAllowed(), region);

            if (!result.isAllowed()) {
                event.setCancelled(true);
                logInternalDebug(String.format(
                        "Interacción denegada: %s intentó interactuar con %s en región %s - Razón: %s",
                        player.getName(), block.getType().name(), region.getId(), result.getReason()
                ));
            }
        }
    }

    // ===== EVENTOS DE COMBATE (sin cambios) =====

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        totalEvents++;

        String pvpCacheKey = getPvpCacheKey(attacker, target);
        CachedValidation cached = validationCache.get(pvpCacheKey);

        if (cached != null && !cached.isExpired()) {
            cachedValidations++;
            if (!cached.allowed) {
                event.setCancelled(true);
                return;
            }
        } else {
            ActionResult result = validatePvP(attacker, target);
            cacheValidation(pvpCacheKey, result.isAllowed(), null);

            if (!result.isAllowed()) {
                event.setCancelled(true);
                logInternalDebug(String.format(
                        "PvP denegado: %s intentó atacar a %s - Razón: %s",
                        attacker.getName(), target.getName(), result.getReason()
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        totalEvents++;

        List<Region> regions = regionManager.getRegionsAt(player.getLocation());
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0);
        ActionResult result = validateDamage(player, region, event.getCause());

        if (!result.isAllowed()) {
            event.setCancelled(true);
            logInternalDebug(String.format(
                    "Daño denegado: %s iba a recibir daño por %s en región %s",
                    player.getName(), event.getCause().name(), region.getId()
            ));
        }
    }

    // ===== EVENTOS DE MOVIMIENTO CORREGIDOS =====

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // CORRECCIÓN 1: Filtro menos agresivo - solo si es exactamente el mismo bloque
        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ() &&
                from.getWorld().equals(to.getWorld())) {
            return;
        }

        immediateMovements++;

        // CORRECCIÓN 2: PROCESAR INMEDIATAMENTE - Sin batching
        boolean movementAllowed = regionManager.processPlayerMovement(event.getPlayer(), from, to);

        if (!movementAllowed) {
            event.setCancelled(true);
            logInternalDebug(String.format(
                    "Movimiento cancelado inmediatamente para %s de %s a %s",
                    event.getPlayer().getName(),
                    String.format("(%d,%d,%d)", from.getBlockX(), from.getBlockY(), from.getBlockZ()),
                    String.format("(%d,%d,%d)", to.getBlockX(), to.getBlockY(), to.getBlockZ())
            ));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Los teleports siempre se procesan inmediatamente
        Location from = event.getFrom();
        Location to = event.getTo();

        boolean teleportAllowed = regionManager.processPlayerMovement(event.getPlayer(), from, to);

        if (!teleportAllowed) {
            event.setCancelled(true);
            logInternalDebug(String.format(
                    "Teleport cancelado para %s de %s a %s",
                    event.getPlayer().getName(), from, to
            ));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        totalEvents++;

        Location explosionLocation = event.getLocation();
        List<Region> regions = regionManager.getRegionsAt(explosionLocation);
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0);
        ActionResult result = validateExplosion(region, event.getEntityType());

        if (!result.isAllowed()) {
            event.setCancelled(true);
            logInternalDebug(String.format(
                    "Explosión denegada: %s en región %s - Razón: %s",
                    event.getEntityType().name(), region.getId(), result.getReason()
            ));
        } else {
            // Si BREAK está deshabilitado, solo limpiar la lista de bloques (mantener efectos de explosión)
            if (!region.getFlagValue(RegionFlag.BREAK)) {
                event.blockList().clear();
            } else if (region.getFlagValue(RegionFlag.PLAYER_BUILD_ONLY)) {
                filterExplosionBlocks(event, region);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onBlockExplode(BlockExplodeEvent event) {
        totalEvents++;

        Location explosionLocation = event.getBlock().getLocation();
        List<Region> regions = regionManager.getRegionsAt(explosionLocation);
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0);
        ActionResult result = validateExplosion(region, null);

        if (!result.isAllowed()) {
            event.setCancelled(true);
            logInternalDebug(String.format(
                    "Explosión de bloque denegada en región %s - Razón: %s",
                    region.getId(), result.getReason()
            ));
        } else {
            // Si BREAK está deshabilitado, solo limpiar la lista de bloques (mantener efectos de explosión)
            if (!region.getFlagValue(RegionFlag.BREAK)) {
                event.blockList().clear();
                logInternalDebug(String.format(
                        "Explosión de bloque sin romper bloques en región %s - BREAK deshabilitado",
                        region.getId()
                ));
            } else if (region.getFlagValue(RegionFlag.PLAYER_BUILD_ONLY)) {
                filterBlockExplosionBlocks(event, region);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Block block) {
            List<Region> regions = regionManager.getRegionsAt(block.getLocation());
            if (regions.isEmpty()) {
                return;
            }

            ActionResult result = validateChestAccess(player, regions.get(0), block.getLocation());

            if (!result.isAllowed()) {
                event.setCancelled(true);
                logInternalDebug(String.format(
                        "Acceso a inventario denegado: %s intentó abrir %s en región %s",
                        player.getName(), block.getType().name(), regions.get(0).getId()
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        totalEvents++;

        Player player = event.getPlayer();
        Location location = player.getLocation();

        String cacheKey = getValidationCacheKey(player, location, "item_drop", "");
        CachedValidation cached = validationCache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            cachedValidations++;
            if (!cached.allowed) {
                event.setCancelled(true);
                return;
            }
        } else {
            List<Region> regions = regionManager.getRegionsAt(location);
            if (regions.isEmpty()) {
                cacheValidation(cacheKey, true, null);
                return;
            }

            Region region = regions.get(0);
            ActionResult result = validateItemDrop(player, region, location);

            cacheValidation(cacheKey, result.isAllowed(), region);

            if (!result.isAllowed()) {
                event.setCancelled(true);
                logInternalDebug(String.format(
                        "Tirar item denegado: %s intentó tirar %s en región %s - Razón: %s",
                        player.getName(), event.getItemDrop().getItemStack().getType().name(),
                        region.getId(), result.getReason()
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        totalEvents++;

        Location location = event.getItem().getLocation();

        String cacheKey = getValidationCacheKey(player, location, "item_pickup", "");
        CachedValidation cached = validationCache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            cachedValidations++;
            if (!cached.allowed) {
                event.setCancelled(true);
                return;
            }
        } else {
            List<Region> regions = regionManager.getRegionsAt(location);
            if (regions.isEmpty()) {
                cacheValidation(cacheKey, true, null);
                return;
            }

            Region region = regions.get(0);
            ActionResult result = validateItemPickup(player, region, location);

            cacheValidation(cacheKey, result.isAllowed(), region);

            if (!result.isAllowed()) {
                event.setCancelled(true);
                logInternalDebug(String.format(
                        "Recoger item denegado: %s intentó recoger %s en región %s - Razón: %s",
                        player.getName(), event.getItem().getItemStack().getType().name(),
                        region.getId(), result.getReason()
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockForm(BlockFormEvent event) {
        totalEvents++;

        Location location = event.getBlock().getLocation();
        List<Region> regions = regionManager.getRegionsAt(location);

        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0);

        // Solo procesar si la región tiene tracking activo
        if (!region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
            return;
        }


            logInternalDebug(String.format(
                    "Bloque formado cerca de jugador: %s causó posible formación de %s en %s",
                    "N/A",
                    event.getNewState().getType().name(),
                    location
            ));

            // Registrar el bloque formado
            blockTracker.addPlayerBlock(region.getId(), location, event.getNewState().getType(),
                    null, "N/A");

            // Si la región tiene bloques temporales, programar remoción
            if (region.getFlagValue(RegionFlag.TEMPORARY_BLOCKS)) {
                temporaryBlocksManager.scheduleBlockRemoval(
                        region, location, event.getNewState().getType(), null
                );
            }
    }



    /**
     * Maneja la propagación de bloques (como fuego)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockSpread(BlockSpreadEvent event) {
        totalEvents++;

        Location location = event.getBlock().getLocation();
        List<Region> regions = regionManager.getRegionsAt(location);

        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0);

        // Solo procesar si la región tiene tracking activo
        if (!region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
            return;
        }

        // OPTIMIZADO: Solo verificar si el bloque fuente fue colocado por jugador
        Location sourceLocation = event.getSource().getLocation();

        // CACHE CHECK RÁPIDO: Solo verificar si ya sabemos que es de jugador
        if (blockTracker.isPlayerPlacedBlock(region.getId(), sourceLocation)) {
            // SIMPLIFICADO: Registrar propagación sin buscar jugador específico
            blockTracker.addPlayerBlock(region.getId(), location, event.getNewState().getType());

            logInternalDebug(String.format(
                    "Propagación registrada: %s se propagó de %s a %s",
                    event.getNewState().getType().name(),
                    sourceLocation,
                    location
            ));

            // Si la región tiene bloques temporales, programar remoción
            if (region.getFlagValue(RegionFlag.TEMPORARY_BLOCKS)) {
                // Sin jugador específico, usar tiempo estándar
                temporaryBlocksManager.scheduleBlockRemoval(
                        region, location, event.getNewState().getType(), null
                );
            }
        }
    }

    // ===== EVENTOS DE LIMPIEZA =====

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // Limpiar cache de validaciones del jugador
        String playerPrefix = playerId.toString();
        validationCache.entrySet().removeIf(entry -> entry.getKey().contains(playerPrefix));

        // Limpiar estado en los managers
        regionManager.cleanupPlayer(event.getPlayer());
        flagManager.cleanupPlayerState(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        // Misma lógica que quit
        onPlayerQuit(new PlayerQuitEvent(event.getPlayer(), ""));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();

        // Simular movimiento para detectar regiones al conectarse
        Location emptyLocation = new Location(location.getWorld(), 0, -1000, 0);

        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                regionManager.processPlayerMovement(player, emptyLocation, player.getLocation()), 1L);
    }

    // ===== MÉTODOS DE VALIDACIÓN (sin cambios) =====

    private ActionResult validateBlockPlacement(Player player, Region region, Location location, Material material) {
        if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.BUILD)) {
            return ActionResult.deny("build-denied", "No puedes construir en esta región");
        }

        if (region.getFlagValue(RegionFlag.RE_GIVE_BLOCKS)) {
            if (!region.getFlagValue(RegionFlag.TEMPORARY_BLOCKS)) {
                return ActionResult.deny("invalid-regive-config",
                        "RE_GIVE_BLOCKS requiere que TEMPORARY_BLOCKS esté activo");
            }
        }

        if (region.getFlagValue(RegionFlag.ALLOWED_BLOCKS_ONLY)) {
            if (!allowedBlocksManager.isMaterialAllowed(region, material)) {
                return ActionResult.deny("material-not-allowed",
                        "El material " + material.name() + " no está permitido en esta región");
            }
        }

        return ActionResult.allow();
    }

    private ActionResult validateBlockBreaking(Player player, Region region, Location location) {
        if (region.getFlagValue(RegionFlag.PLAYER_BUILD_ONLY)) {
            if (!blockTracker.isPlayerPlacedBlock(region.getId(), location)) {
                return ActionResult.deny("not-player-block",
                        "Solo puedes romper bloques que hayas colocado");
            }
            return ActionResult.allow();
        }

        if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.BREAK)) {
            return ActionResult.deny("break-denied", "No puedes romper bloques en esta región");
        }

        return ActionResult.allow();
    }

    private ActionResult validateInteraction(Player player, Region region, Location location, Material material) {
        if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.INTERACT)) {
            return ActionResult.deny("interact-denied", "No puedes interactuar en esta región");
        }

        if (isChestOrContainer(material)) {
            if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.CHEST_ACCESS)) {
                return ActionResult.deny("chest-access-denied", "No puedes acceder a contenedores");
            }
        } else if (isDoorOrGate(material)) {
            if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.USE_DOORS)) {
                return ActionResult.deny("door-access-denied", "No puedes usar puertas");
            }
        } else if (isButtonOrLever(material)) {
            if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.USE_BUTTONS)) {
                return ActionResult.deny("button-access-denied", "No puedes usar botones o palancas");
            }
        }

        return ActionResult.allow();
    }

    private ActionResult validatePvP(Player attacker, Player target) {
        if (flagManager.isPlayerInvincible(target)) {
            return ActionResult.deny("target-invincible", null);
        }

        if (!flagManager.isPvpAllowed(attacker, target)) {
            return ActionResult.deny("pvp-disabled", "PvP no está permitido aquí");
        }

        return ActionResult.allow();
    }

    private ActionResult validateDamage(Player player, Region region, EntityDamageEvent.DamageCause cause) {
        if (flagManager.isPlayerInvincible(player)) {
            return ActionResult.deny("invincible", null);
        }

        if (cause == EntityDamageEvent.DamageCause.FIRE ||
                cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                cause == EntityDamageEvent.DamageCause.LAVA) {
            if (!flagManager.canPlayerPerformAction(player, RegionFlag.FIRE_DAMAGE)) {
                return ActionResult.deny("fire-damage-disabled", null);
            }
        }

        return ActionResult.allow();
    }

    private ActionResult validateExplosion(Region region, EntityType entityType) {
        if (entityType == EntityType.PRIMED_TNT || entityType == EntityType.MINECART_TNT) {
            if (!region.getFlagValue(RegionFlag.TNT)) {
                return ActionResult.deny("tnt-disabled", null);
            }
        } else if (entityType == EntityType.CREEPER) {
            if (!region.getFlagValue(RegionFlag.CREEPER_EXPLOSION)) {
                return ActionResult.deny("creeper-explosion-disabled", null);
            }
        } else {
            if (!region.getFlagValue(RegionFlag.OTHER_EXPLOSION)) {
                return ActionResult.deny("other-explosion-disabled", null);
            }
        }

        return ActionResult.allow();
    }

    private ActionResult validateChestAccess(Player player, Region region, Location location) {
        if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.CHEST_ACCESS)) {
            return ActionResult.deny("chest-access-denied", "No puedes acceder a contenedores");
        }
        return ActionResult.allow();
    }

    private ActionResult validateItemDrop(Player player, Region region, Location location) {
        if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.ITEM_DROP)) {
            return ActionResult.deny("item-drop-denied", "No puedes tirar items en esta región");
        }
        return ActionResult.allow();
    }

    private ActionResult validateItemPickup(Player player, Region region, Location location) {
        if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.ITEM_PICKUP)) {
            return ActionResult.deny("item-pickup-denied", "No puedes recoger items en esta región");
        }
        return ActionResult.allow();
    }

    // ===== MÉTODOS DE EFECTOS POST-VALIDACIÓN =====

    private void executePostPlacementEffects(Player player, Region region, Location location, Material material) {
        if (region == null) return;

        if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
            blockTracker.addPlayerBlock(region.getId(), location, material, player);
        }

        if (region.getFlagValue(RegionFlag.TEMPORARY_BLOCKS)) {
            temporaryBlocksManager.scheduleBlockRemoval(region, location, material, player.getUniqueId());
        }
    }

    private void executePostBreakEffects(Player player, Region region, Location location) {
        if (region == null) return;

        if (temporaryBlocksManager.isTemporaryBlock(location)) {
            temporaryBlocksManager.cancelBlockRemoval(location);
        }

        if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
            blockTracker.removePlayerBlock(region.getId(), location);
        }
    }

    private void filterExplosionBlocks(EntityExplodeEvent event, Region region) {
        Iterator<Block> iterator = event.blockList().iterator();
        int originalCount = event.blockList().size();

        while (iterator.hasNext()) {
            Block block = iterator.next();
            Location blockLocation = block.getLocation();

            if (temporaryBlocksManager.isTemporaryBlock(blockLocation)) {
                temporaryBlocksManager.cancelBlockRemoval(blockLocation);
            }

            if (!blockTracker.isPlayerPlacedBlock(region.getId(), blockLocation)) {
                iterator.remove();
            } else {
                if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
                    blockTracker.removePlayerBlock(region.getId(), blockLocation);
                }
            }
        }

        logInternalDebug(String.format(
                "Explosión filtrada en región %s: %d->%d bloques pueden ser destruidos",
                region.getId(), originalCount, event.blockList().size()
        ));
    }

    private void filterBlockExplosionBlocks(BlockExplodeEvent event, Region region) {
        Iterator<Block> iterator = event.blockList().iterator();

        while (iterator.hasNext()) {
            Block block = iterator.next();
            Location blockLocation = block.getLocation();

            if (temporaryBlocksManager.isTemporaryBlock(blockLocation)) {
                temporaryBlocksManager.cancelBlockRemoval(blockLocation);
            }

            if (!blockTracker.isPlayerPlacedBlock(region.getId(), blockLocation)) {
                iterator.remove();
            } else {
                if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
                    blockTracker.removePlayerBlock(region.getId(), blockLocation);
                }
            }
        }
    }

    // ===== CACHE DE VALIDACIONES (solo cache ligero) =====

    private String getValidationCacheKey(Player player, Location location, String action, String extra) {
        return String.format("%s:%d:%d:%d:%s:%s",
                player.getUniqueId().toString(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                action,
                extra);
    }

    private String getPvpCacheKey(Player attacker, Player target) {
        UUID id1 = attacker.getUniqueId();
        UUID id2 = target.getUniqueId();

        if (id1.compareTo(id2) < 0) {
            return "pvp:" + id1 + ":" + id2;
        } else {
            return "pvp:" + id2 + ":" + id1;
        }
    }

    private void cacheValidation(String key, boolean allowed, Region region) {
        validationCache.put(key, new CachedValidation(allowed, region, System.currentTimeMillis()));

        // Limitar tamaño del cache (más conservador)
        if (validationCache.size() > 500) { // Reducido de 1000 a 500
            cleanupValidationCache();
        }
    }

    // ===== LIMPIEZA DE CACHE =====

    private void startCacheCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupValidationCache();
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L); // Cada segundo
    }

    private void cleanupValidationCache() {
        long currentTime = System.currentTimeMillis();
        validationCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue().timestamp > VALIDATION_CACHE_EXPIRE);
    }

    // ===== MÉTODOS AUXILIARES =====

    private boolean isChestOrContainer(Material material) {
        return material == Material.CHEST ||
                material == Material.TRAPPED_CHEST ||
                material == Material.ENDER_CHEST ||
                material == Material.BARREL ||
                material == Material.SHULKER_BOX ||
                material.name().contains("SHULKER_BOX");
    }

    private boolean isDoorOrGate(Material material) {
        return material.name().contains("DOOR") ||
                material.name().contains("GATE") ||
                material.name().contains("TRAPDOOR");
    }

    private boolean isButtonOrLever(Material material) {
        return material.name().contains("BUTTON") ||
                material == Material.LEVER ||
                material.name().contains("PRESSURE_PLATE");
    }

    // ===== ESTADÍSTICAS =====

    public ListenerStats getStats() {
        return new ListenerStats(
                totalEvents,
                cachedValidations,
                immediateMovements,
                0, // No more pending movements
                0, // No more pending actions
                validationCache.size()
        );
    }

    public void resetStats() {
        totalEvents = 0;
        cachedValidations = 0;
        immediateMovements = 0;
    }

    // ===== LIMPIEZA =====

    public void shutdown() {
        validationCache.clear();
    }

    // ===== CLASES AUXILIARES =====

    @Getter
    private static class ActionResult {
        private final boolean allowed;
        private final String reason;
        private final String message;

        private ActionResult(boolean allowed, String reason, String message) {
            this.allowed = allowed;
            this.reason = reason;
            this.message = message;
        }

        public static ActionResult allow() {
            return new ActionResult(true, null, null);
        }

        public static ActionResult deny(String reason, String message) {
            return new ActionResult(false, reason, message);
        }
    }

    private static class CachedValidation {
        final boolean allowed;
        final Region region;
        final long timestamp;

        CachedValidation(boolean allowed, Region region, long timestamp) {
            this.allowed = allowed;
            this.region = region;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > VALIDATION_CACHE_EXPIRE;
        }
    }

    public static class ListenerStats {
        private final long totalEvents;
        private final long cachedValidations;
        private final long immediateMovements;
        private final int pendingMovements;
        private final int pendingActions;
        private final int cacheSize;

        public ListenerStats(long totalEvents, long cachedValidations, long immediateMovements,
                             int pendingMovements, int pendingActions, int cacheSize) {
            this.totalEvents = totalEvents;
            this.cachedValidations = cachedValidations;
            this.immediateMovements = immediateMovements;
            this.pendingMovements = pendingMovements;
            this.pendingActions = pendingActions;
            this.cacheSize = cacheSize;
        }

        public long getTotalEvents() { return totalEvents; }
        public long getCachedValidations() { return cachedValidations; }
        public long getImmediateMovements() { return immediateMovements; }
        public int getPendingMovements() { return pendingMovements; }
        public int getPendingActions() { return pendingActions; }
        public int getCacheSize() { return cacheSize; }

        public double getCacheHitRatio() {
            return totalEvents > 0 ? (double) cachedValidations / totalEvents : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "ListenerStats{events=%d, cached=%d (%.1f%%), immediate_moves=%d, cache_size=%d}",
                    totalEvents, cachedValidations, getCacheHitRatio() * 100,
                    immediateMovements, cacheSize
            );
        }
    }

    private void logBlockPlacementInfo(Player player, Region region, Location location, Material material, String context) {
//        logInternalDebug(String.format(
//                "%s - Jugador: %s, Material: %s, Región: %s, Ubicación: (%d,%d,%d), Tracking: %s, Temporal: %s",
//                context,
//                player.getName(),
//                material.name(),
//                region.getId(),
//                location.getBlockX(),
//                location.getBlockY(),
//                location.getBlockZ(),
//                region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS) ? "SÍ" : "NO",
//                region.getFlagValue(RegionFlag.TEMPORARY_BLOCKS) ? "SÍ" : "NO"
//        ));
    }
}
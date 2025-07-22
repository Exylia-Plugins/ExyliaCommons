package net.exylia.commons.region.listener;

import lombok.Getter;
import net.exylia.commons.region.RegionManager;
import net.exylia.commons.region.blocks.AllowedBlocksManager;
import net.exylia.commons.region.blocks.PlayerBlockTracker;
import net.exylia.commons.region.blocks.TemporaryBlocksManager;
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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.List;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

/**
 * Listener unificado que maneja TODOS los eventos de región con lógica jerárquica
 * Usa prioridad NORMAL para no interferir con otros plugins
 */
public class UnifiedRegionListener implements Listener {

    private final JavaPlugin plugin;
    private final RegionManager regionManager;
    private final FlagManager flagManager;
    private final PlayerBlockTracker blockTracker;
    private final AllowedBlocksManager allowedBlocksManager;
    private final TemporaryBlocksManager temporaryBlocksManager;

    public UnifiedRegionListener(JavaPlugin plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.flagManager = FlagManager.getInstance();
        this.blockTracker = PlayerBlockTracker.getInstance();
        this.allowedBlocksManager = AllowedBlocksManager.getInstance();
        this.temporaryBlocksManager = TemporaryBlocksManager.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ===== EVENTOS DE CONSTRUCCIÓN =====

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        Material material = event.getBlock().getType();

        // Obtener regiones en la ubicación
        List<Region> regions = regionManager.getRegionsAt(location);
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        Region region = regions.get(0); // Mayor prioridad

        // === JERARQUÍA DE VALIDACIÓN ===
        ActionResult result = validateBlockPlacement(player, region, location, material);

        // Solo cancelar si no está permitido
        if (!result.isAllowed()) {
            event.setCancelled(true);

            logInternalDebug(debug(), String.format(
                    "Colocación de bloque denegada: %s intentó colocar %s en región %s - Razón: %s",
                    player.getName(), material.name(), region.getId(), result.getReason()
            ));
        } else {
            // Ejecutar efectos post-validación
            executePostPlacementEffects(player, region, location, material);

            logInternalDebug(debug(), String.format(
                    "Colocación de bloque permitida: %s colocó %s en región %s",
                    player.getName(), material.name(), region.getId()
            ));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        // Obtener regiones en la ubicación
        List<Region> regions = regionManager.getRegionsAt(location);
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        Region region = regions.get(0); // Mayor prioridad

        // === JERARQUÍA DE VALIDACIÓN ===
        ActionResult result = validateBlockBreaking(player, region, location);

        // Solo cancelar si no está permitido
        if (!result.isAllowed()) {
            event.setCancelled(true);

            logInternalDebug(debug(), String.format(
                    "Rotura de bloque denegada: %s intentó romper bloque en región %s - Razón: %s",
                    player.getName(), region.getId(), result.getReason()
            ));
        } else {
            // Ejecutar efectos post-validación
            executePostBreakEffects(player, region, location);

            logInternalDebug(debug(), String.format(
                    "Rotura de bloque permitida: %s rompió bloque en región %s",
                    player.getName(), region.getId()
            ));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Location location = block.getLocation();

        // Obtener regiones en la ubicación
        List<Region> regions = regionManager.getRegionsAt(location);
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        Region region = regions.get(0); // Mayor prioridad

        // === JERARQUÍA DE VALIDACIÓN ===
        ActionResult result = validateInteraction(player, region, location, block.getType());

        // Solo cancelar si no está permitido
        if (!result.isAllowed()) {
            event.setCancelled(true);

            logInternalDebug(debug(), String.format(
                    "Interacción denegada: %s intentó interactuar con %s en región %s - Razón: %s",
                    player.getName(), block.getType().name(), region.getId(), result.getReason()
            ));
        }
    }

    // ===== EVENTOS DE COMBATE =====

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        // Verificar si alguno de los jugadores está en una región
        List<Region> targetRegions = regionManager.getRegionsAt(target.getLocation());
        List<Region> attackerRegions = regionManager.getRegionsAt(attacker.getLocation());

        if (targetRegions.isEmpty() && attackerRegions.isEmpty()) {
            return; // Ninguno está en regiones, no interferir
        }

        // === JERARQUÍA DE VALIDACIÓN ===
        ActionResult result = validatePvP(attacker, target);

        // Solo cancelar si no está permitido
        if (!result.isAllowed()) {
            event.setCancelled(true);

            if (result.getMessage() != null) {
                logInternalDebug(debug(), String.format(
                        "PvP denegado: %s intentó atacar a %s - Razón: %s",
                        attacker.getName(), target.getName(), result.getReason()
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Solo actuar si el jugador está en una región
        List<Region> regions = regionManager.getRegionsAt(player.getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        Region region = regions.get(0); // Mayor prioridad

        // === JERARQUÍA DE VALIDACIÓN ===
        ActionResult result = validateDamage(player, region, event.getCause());

        // Solo cancelar si no está permitido
        if (!result.isAllowed()) {
            event.setCancelled(true);

            if (result.getMessage() != null) {
                logInternalDebug(debug(), String.format(
                        "Daño denegado: %s iba a recibir daño por %s en región %s",
                        player.getName(), event.getCause().name(), region.getId()
                ));
            }
        }
    }

    // ===== EVENTOS DE MOVIMIENTO =====

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // Optimización: Solo procesar si realmente se movió a un bloque diferente
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ() && from.getWorld().equals(to.getWorld())) {
            return;
        }

        // Usar el sistema existente del RegionManager
        boolean movementAllowed = regionManager.processPlayerMovement(event.getPlayer(), from, to);

        if (!movementAllowed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        boolean teleportAllowed = regionManager.processPlayerMovement(event.getPlayer(), from, to);

        if (!teleportAllowed) {
            event.setCancelled(true);
        }
    }

    // ===== EVENTOS DE EXPLOSIONES =====

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Location explosionLocation = event.getLocation();

        // Obtener regiones en la ubicación de la explosión
        List<Region> regions = regionManager.getRegionsAt(explosionLocation);
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0); // Mayor prioridad

        // === JERARQUÍA DE VALIDACIÓN ===
        ActionResult result = validateExplosion(region, event.getEntityType());

        if (!result.isAllowed()) {
            event.setCancelled(true);

            event.getEntityType();
            logInternalDebug(debug(), String.format(
                    "Explosión denegada: %s en región %s - Razón: %s",
                    event.getEntityType().name(),
                    region.getId(), result.getReason()
            ));
        } else {
            // Si la explosión está permitida, filtrar bloques según PLAYER_BUILD_ONLY
            if (region.getFlagValue(RegionFlag.PLAYER_BUILD_ONLY)) {
                filterExplosionBlocks(event, region);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Location explosionLocation = event.getBlock().getLocation();

        List<Region> regions = regionManager.getRegionsAt(explosionLocation);
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0);

        // Usar la misma lógica que EntityExplode
        ActionResult result = validateExplosion(region, null); // null para explosiones de bloque

        if (!result.isAllowed()) {
            event.setCancelled(true);

            logInternalDebug(debug(), String.format(
                    "Explosión de bloque denegada en región %s - Razón: %s",
                    region.getId(), result.getReason()
            ));
        } else {
            if (region.getFlagValue(RegionFlag.PLAYER_BUILD_ONLY)) {
                filterBlockExplosionBlocks(event, region);
            }
        }
    }

    // ===== EVENTOS ADICIONALES =====

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
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

                logInternalDebug(debug(), String.format(
                        "Acceso a inventario denegado: %s intentó abrir %s en región %s",
                        player.getName(), block.getType().name(), regions.get(0).getId()
                ));
            }
        }
    }

    // ===== EVENTOS DE LIMPIEZA =====

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        regionManager.cleanupPlayer(event.getPlayer());
        flagManager.cleanupPlayerState(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        regionManager.cleanupPlayer(event.getPlayer());
        flagManager.cleanupPlayerState(event.getPlayer());
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

    // ===== MÉTODOS DE VALIDACIÓN JERÁRQUICA =====

    /**
     * Valida la colocación de bloques con jerarquía de flags
     */
    private ActionResult validateBlockPlacement(Player player, Region region, Location location, Material material) {
        // 1. Verificar BUILD primero (flag padre)
        if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.BUILD)) {
            return ActionResult.deny("build-denied", "No puedes construir en esta región");
        }

        // 2. Si BUILD está permitido, verificar ALLOWED_BLOCKS_ONLY
        if (region.getFlagValue(RegionFlag.ALLOWED_BLOCKS_ONLY)) {
            if (!allowedBlocksManager.isMaterialAllowed(region, material)) {
                return ActionResult.deny("material-not-allowed",
                        "El material " + material.name() + " no está permitido en esta región");
            }
        }

        // 3. Si llega aquí, está permitido
        return ActionResult.allow();
    }

    /**
     * Valida la rotura de bloques con jerarquía de flags
     */
    private ActionResult validateBlockBreaking(Player player, Region region, Location location) {
        // 1. Verificar PLAYER_BUILD_ONLY primero (más restrictivo)
        if (region.getFlagValue(RegionFlag.PLAYER_BUILD_ONLY)) {
            if (!blockTracker.isPlayerPlacedBlock(region.getId(), location)) {
                return ActionResult.deny("not-player-block",
                        "Solo puedes romper bloques que hayas colocado");
            }
            // Si es un bloque de jugador, permitir independientemente de BREAK
            return ActionResult.allow();
        }

        // 2. Si PLAYER_BUILD_ONLY no está activo, verificar BREAK
        if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.BREAK)) {
            return ActionResult.deny("break-denied", "No puedes romper bloques en esta región");
        }

        return ActionResult.allow();
    }

    /**
     * Valida la interacción con bloques
     */
    private ActionResult validateInteraction(Player player, Region region, Location location, Material material) {
        // 1. Verificar INTERACT primero (flag padre)
        if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.INTERACT)) {
            return ActionResult.deny("interact-denied", "No puedes interactuar en esta región");
        }

        // 2. Verificaciones específicas solo si INTERACT está permitido
        if (isChestOrContainer(material)) {
            if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.CHEST_ACCESS)) {
                return ActionResult.deny("chest-access-denied", "No puedes acceder a contenedores");
            }
        }

        if (isDoorOrGate(material)) {
            if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.USE_DOORS)) {
                return ActionResult.deny("door-access-denied", "No puedes usar puertas");
            }
        }

        if (isButtonOrLever(material)) {
            if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.USE_BUTTONS)) {
                return ActionResult.deny("button-access-denied", "No puedes usar botones o palancas");
            }
        }

        return ActionResult.allow();
    }

    /**
     * Valida PvP entre jugadores
     */
    private ActionResult validatePvP(Player attacker, Player target) {
        // Verificar invencibilidad primero
        if (flagManager.isPlayerInvincible(target)) {
            return ActionResult.deny("target-invincible", null);
        }

        // Verificar PvP usando el sistema existente del FlagManager
        if (!flagManager.isPvpAllowed(attacker, target)) {
            return ActionResult.deny("pvp-disabled", "PvP no está permitido aquí");
        }

        return ActionResult.allow();
    }

    /**
     * Valida daño a jugadores
     */
    private ActionResult validateDamage(Player player, Region region, EntityDamageEvent.DamageCause cause) {
        // Verificar invencibilidad general
        if (flagManager.isPlayerInvincible(player)) {
            return ActionResult.deny("invincible", null);
        }

        // Verificar daño por fuego específico
        if (cause == EntityDamageEvent.DamageCause.FIRE ||
                cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                cause == EntityDamageEvent.DamageCause.LAVA) {
            if (!flagManager.canPlayerPerformAction(player, RegionFlag.FIRE_DAMAGE)) {
                return ActionResult.deny("fire-damage-disabled", null);
            }
        }

        return ActionResult.allow();
    }

    /**
     * Valida explosiones
     */
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

    /**
     * Valida acceso a cofres
     */
    private ActionResult validateChestAccess(Player player, Region region, Location location) {
        if (!flagManager.canPlayerPerformActionAt(player, location, RegionFlag.CHEST_ACCESS)) {
            return ActionResult.deny("chest-access-denied", "No puedes acceder a contenedores");
        }
        return ActionResult.allow();
    }

    // ===== MÉTODOS DE EFECTOS POST-VALIDACIÓN =====

    /**
     * Ejecuta efectos después de colocar un bloque exitosamente
     */
    private void executePostPlacementEffects(Player player, Region region, Location location, Material material) {
        // Rastreo de bloques de jugador
        if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
            blockTracker.addPlayerBlock(region.getId(), location, material);
        }

        // Bloques temporales
        if (region.getFlagValue(RegionFlag.TEMPORARY_BLOCKS)) {
            temporaryBlocksManager.scheduleBlockRemoval(region, location, material);
        }
    }

    /**
     * Ejecuta efectos después de romper un bloque exitosamente
     */
    private void executePostBreakEffects(Player player, Region region, Location location) {
        // Cancelar remoción temporal si existe
        if (temporaryBlocksManager.isTemporaryBlock(location)) {
            temporaryBlocksManager.cancelBlockRemoval(location);
        }

        // Remover del tracker si existe
        if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
            blockTracker.removePlayerBlock(region.getId(), location);
        }
    }

    /**
     * Filtra bloques en explosiones según PLAYER_BUILD_ONLY
     */
    private void filterExplosionBlocks(EntityExplodeEvent event, Region region) {
        Iterator<Block> iterator = event.blockList().iterator();
        int originalCount = event.blockList().size();

        while (iterator.hasNext()) {
            Block block = iterator.next();
            Location blockLocation = block.getLocation();

            // Cancelar remoción de bloques temporales
            if (temporaryBlocksManager.isTemporaryBlock(blockLocation)) {
                temporaryBlocksManager.cancelBlockRemoval(blockLocation);
            }

            // Verificar si el bloque fue colocado por un jugador
            if (!blockTracker.isPlayerPlacedBlock(region.getId(), blockLocation)) {
                iterator.remove();
            } else {
                // Remover del registro
                if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
                    blockTracker.removePlayerBlock(region.getId(), blockLocation);
                }
            }
        }

        logInternalDebug(debug(), String.format(
                "Explosión filtrada en región %s: %d->%d bloques pueden ser destruidos",
                region.getId(), originalCount, event.blockList().size()
        ));
    }

    /**
     * Filtra bloques en explosiones de bloque según PLAYER_BUILD_ONLY
     */
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

    // ===== CLASE AUXILIAR PARA RESULTADOS =====

    /**
     * Representa el resultado de una validación de acción
     */
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
}
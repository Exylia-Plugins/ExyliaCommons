package net.exylia.commons.region.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.exylia.commons.region.RegionManager;
import net.exylia.commons.region.flags.FlagManager;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/**
 * Listener que intercepta eventos para aplicar las restricciones de flags
 * Con prioridad alta para overridear otros plugins como WorldGuard
 * Solo actúa cuando hay regiones presentes
 */
public class FlagListener implements Listener {
    private final FlagManager flagManager;
    private final RegionManager regionManager;

    public FlagListener(FlagManager flagManager, RegionManager regionManager) {
        this.flagManager = flagManager;
        this.regionManager = regionManager;
    }

    // ===== EVENTOS DE CONSTRUCCIÓN =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Solo actuar si hay regiones en la ubicación
        List<Region> regions = regionManager.getRegionsAt(event.getBlock().getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        // Si nuestro sistema permite la acción, descancelar si fue cancelado por otro plugin
        if (flagManager.canPlayerPerformActionAt(player, event.getBlock().getLocation(), RegionFlag.BUILD)) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            // Si nuestro sistema no permite la acción, cancelar
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Solo actuar si hay regiones en la ubicación
        List<Region> regions = regionManager.getRegionsAt(event.getBlock().getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        // Si nuestro sistema permite la acción, descancelar si fue cancelado por otro plugin
        if (flagManager.canPlayerPerformActionAt(player, event.getBlock().getLocation(), RegionFlag.BREAK)) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            // Si nuestro sistema no permite la acción, cancelar
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        // Solo actuar si hay regiones en la ubicación
        List<Region> regions = regionManager.getRegionsAt(block.getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        Material material = block.getType();
        boolean shouldAllow = true;

        // Verificar interacción general
        if (!flagManager.canPlayerPerformActionAt(player, block.getLocation(), RegionFlag.INTERACT)) {
            shouldAllow = false;
        }

        // Verificaciones específicas solo si la interacción general está permitida
        if (shouldAllow) {
            if (isChestOrContainer(material)) {
                if (!flagManager.canPlayerPerformActionAt(player, block.getLocation(), RegionFlag.CHEST_ACCESS)) {
                    shouldAllow = false;
                }
            }

            if (shouldAllow && isDoorOrGate(material)) {
                if (!flagManager.canPlayerPerformActionAt(player, block.getLocation(), RegionFlag.USE_DOORS)) {
                    shouldAllow = false;
                }
            }

            if (shouldAllow && isButtonOrLever(material)) {
                if (!flagManager.canPlayerPerformActionAt(player, block.getLocation(), RegionFlag.USE_BUTTONS)) {
                    shouldAllow = false;
                }
            }
        }

        // Aplicar resultado final
        if (shouldAllow) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            event.setCancelled(true);
        }
    }

    // ===== EVENTOS DE COMBATE =====

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        // Verificar si alguno de los jugadores está en una región
        List<Region> targetRegions = regionManager.getRegionsAt(target.getLocation());
        List<Region> attackerRegions = regionManager.getRegionsAt(attacker.getLocation());

        if (targetRegions.isEmpty() && attackerRegions.isEmpty()) {
            return; // Ninguno está en regiones, no interferir
        }

        boolean shouldAllow = true;

        // Verificar invencibilidad
        if (flagManager.isPlayerInvincible(target)) {
            shouldAllow = false;
        }


        // Verificar PvP
        if (shouldAllow && !flagManager.isPvpAllowed(attacker, target)) {
            shouldAllow = false;
        }

        // Aplicar resultado
        if (shouldAllow) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Solo actuar si el jugador está en una región
        List<Region> regions = regionManager.getRegionsAt(player.getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        boolean shouldAllow = true;

        // Verificar invencibilidad general
        if (flagManager.isPlayerInvincible(player)) {
            shouldAllow = false;
        }

        // Verificar daño por fuego
        if (shouldAllow && (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                event.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            if (!flagManager.canPlayerPerformAction(player, RegionFlag.FIRE_DAMAGE)) {
                shouldAllow = false;
            }
        }

        // Aplicar resultado
        if (shouldAllow) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            event.setCancelled(true);
        }
    }

    // ===== EVENTOS DE ITEMS =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // Solo actuar si el jugador está en una región
        List<Region> regions = regionManager.getRegionsAt(player.getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        if (flagManager.canPlayerPerformAction(player, RegionFlag.ITEM_DROP)) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Solo actuar si hay regiones en la ubicación del item
        List<Region> regions = regionManager.getRegionsAt(event.getItem().getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        if (flagManager.canPlayerPerformActionAt(player, event.getItem().getLocation(), RegionFlag.ITEM_PICKUP)) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof Block block) {
            // Solo actuar si hay regiones en la ubicación del bloque
            List<Region> regions = regionManager.getRegionsAt(block.getLocation());
            if (regions.isEmpty()) {
                return; // No hay regiones, no interferir
            }

            if (flagManager.canPlayerPerformActionAt(player, block.getLocation(), RegionFlag.CHEST_ACCESS)) {
                if (event.isCancelled()) {
                    event.setCancelled(false);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    // ===== EVENTOS DE COMUNICACIÓN =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Solo actuar si el jugador está en una región
        List<Region> regions = regionManager.getRegionsAt(player.getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        if (flagManager.canPlayerPerformAction(player, RegionFlag.CHAT)) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Solo actuar si el jugador está en una región
        List<Region> regions = regionManager.getRegionsAt(player.getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        if (flagManager.canPlayerPerformAction(player, RegionFlag.COMMANDS)) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
                event.setCancelled(true);
        }
    }

    // ===== EVENTOS DE ENTIDADES =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;

        // Solo actuar si hay regiones en la ubicación del spawn
        List<Region> regions = regionManager.getRegionsAt(event.getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        EntityType type = event.getEntityType();
        boolean shouldAllow = true;

        if (isHostileMob(type)) {
            if (!flagManager.canPlayerPerformActionAt(null, event.getLocation(), RegionFlag.MOB_SPAWNING)) {
                shouldAllow = false;
            }
        } else if (isAnimal(type)) {
            if (!flagManager.canPlayerPerformActionAt(null, event.getLocation(), RegionFlag.ANIMAL_SPAWNING)) {
                shouldAllow = false;
            }
        }

        // Aplicar resultado
        if (shouldAllow) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            event.setCancelled(true);
        }
    }

    // ===== EVENTOS DE EXPLOSIONES =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Solo actuar si hay regiones en la ubicación de la explosión
        List<Region> regions = regionManager.getRegionsAt(event.getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        EntityType type = event.getEntityType();
        boolean shouldAllow = true;

        if (type == EntityType.PRIMED_TNT || type == EntityType.MINECART_TNT) {
            if (!flagManager.canPlayerPerformActionAt(null, event.getLocation(), RegionFlag.TNT)) {
                shouldAllow = false;
            }
        } else if (type == EntityType.CREEPER) {
            if (!flagManager.canPlayerPerformActionAt(null, event.getLocation(), RegionFlag.CREEPER_EXPLOSION)) {
                shouldAllow = false;
            }
        } else {
            if (!flagManager.canPlayerPerformActionAt(null, event.getLocation(), RegionFlag.OTHER_EXPLOSION)) {
                shouldAllow = false;
            }
        }

        // Aplicar resultado
        if (shouldAllow) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            event.setCancelled(true);
        }
    }

    // ===== EVENTOS DE FUEGO =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBurn(BlockBurnEvent event) {
        // Solo actuar si hay regiones en la ubicación
        List<Region> regions = regionManager.getRegionsAt(event.getBlock().getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        if (flagManager.canPlayerPerformActionAt(null, event.getBlock().getLocation(), RegionFlag.FIRE_SPREAD)) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockIgnite(BlockIgniteEvent event) {
        // Solo actuar si hay regiones en la ubicación
        List<Region> regions = regionManager.getRegionsAt(event.getBlock().getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        boolean shouldAllow = true;

        if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD) {
            if (!flagManager.canPlayerPerformActionAt(null, event.getBlock().getLocation(), RegionFlag.FIRE_SPREAD)) {
                shouldAllow = false;
            }
        } else if (event.getCause() == BlockIgniteEvent.IgniteCause.LAVA) {
            if (!flagManager.canPlayerPerformActionAt(null, event.getBlock().getLocation(), RegionFlag.LAVA_FIRE)) {
                shouldAllow = false;
            }
        }

        // Aplicar resultado
        if (shouldAllow) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            event.setCancelled(true);
        }
    }

    // ===== EVENTOS DE LÍQUIDOS =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockFromTo(BlockFromToEvent event) {
        // Solo actuar si hay regiones en la ubicación de destino
        List<Region> regions = regionManager.getRegionsAt(event.getToBlock().getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        Material material = event.getBlock().getType();
        boolean shouldAllow = true;

        if (material == Material.WATER || material == Material.WATER_BUCKET) {
            if (!flagManager.canPlayerPerformActionAt(null, event.getToBlock().getLocation(), RegionFlag.WATER_FLOW)) {
                shouldAllow = false;
            }
        } else if (material == Material.LAVA || material == Material.LAVA_BUCKET) {
            if (!flagManager.canPlayerPerformActionAt(null, event.getToBlock().getLocation(), RegionFlag.LAVA_FLOW)) {
                shouldAllow = false;
            }
        }

        // Aplicar resultado
        if (shouldAllow) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            event.setCancelled(true);
        }
    }

    // ===== EVENTOS DE MARCOS =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();

        // Solo actuar si hay regiones en la ubicación
        List<Region> regions = regionManager.getRegionsAt(event.getEntity().getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        if (player != null && flagManager.canPlayerPerformActionAt(player, event.getEntity().getLocation(), RegionFlag.ITEM_FRAME)) {
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHangingBreak(HangingBreakEvent event) {
        // Solo actuar si hay regiones en la ubicación
        List<Region> regions = regionManager.getRegionsAt(event.getEntity().getLocation());
        if (regions.isEmpty()) {
            return; // No hay regiones, no interferir
        }

        if (event instanceof HangingBreakByEntityEvent entityEvent) {
            if (entityEvent.getRemover() instanceof Player player) {
                if (flagManager.canPlayerPerformActionAt(player, event.getEntity().getLocation(), RegionFlag.ITEM_FRAME)) {
                    if (event.isCancelled()) {
                        event.setCancelled(false);
                    }
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    // ===== LIMPIEZA =====

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        flagManager.cleanupPlayerState(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        flagManager.cleanupPlayerState(event.getPlayer());
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

    private boolean isHostileMob(EntityType type) {
        return type == EntityType.ZOMBIE ||
                type == EntityType.SKELETON ||
                type == EntityType.CREEPER ||
                type == EntityType.SPIDER ||
                type == EntityType.ENDERMAN ||
                type == EntityType.WITCH ||
                type == EntityType.SLIME;
    }

    private boolean isAnimal(EntityType type) {
        return type == EntityType.COW ||
                type == EntityType.PIG ||
                type == EntityType.SHEEP ||
                type == EntityType.CHICKEN ||
                type == EntityType.HORSE ||
                type == EntityType.WOLF ||
                type == EntityType.CAT;
    }
}
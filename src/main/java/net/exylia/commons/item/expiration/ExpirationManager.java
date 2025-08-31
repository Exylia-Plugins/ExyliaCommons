package net.exylia.commons.item.expiration;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.item.ExpirationBehavior;
import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestor automático de expiración de items
 * Se encarga de verificar periódicamente los inventarios de los jugadores
 * y aplicar los comportamientos de expiración correspondientes
 */
public class ExpirationManager {

    @Getter
    private static ExpirationManager instance;
    private final JavaPlugin plugin;
    private BukkitTask schedulerTask;
    @Getter
    private boolean enabled = true;
    @Getter
    private long checkIntervalTicks = 600L;
    @Setter
    private boolean enableDebugMessages = false;
    
    @Setter
    private boolean updatePlaceholders = true;

    private ExpirationManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new ExpirationManager(plugin);
        }
    }

    public void start() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
        }

        if (!enabled) {
            return;
        }

        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkAllPlayersForExpiredItems, 
                checkIntervalTicks, checkIntervalTicks);
        
        if (enableDebugMessages) {
            DebugUtils.logInternalInfo("ExpirationManager iniciado con intervalo de " +
                (checkIntervalTicks / 20.0) + " segundos");
        }
    }

    /**
     * Detiene el sistema de monitoreo
     */
    public void stop() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
        
        if (enableDebugMessages) {
            DebugUtils.logInternalInfo("ExpirationManager detenido");
        }
    }

    /**
     * Verifica todos los jugadores conectados en busca de items expirados
     */
    private void checkAllPlayersForExpiredItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayerInventoryForExpiredItems(player);
        }
    }

    /**
     * Verifica el inventario de un jugador específico
     */
    public void checkPlayerInventoryForExpiredItems(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        List<ExpirationResult> results = new ArrayList<>();
        checkInventoryForExpiredItems(player.getInventory(), results);
        if (player.getEnderChest() != null) {
            checkInventoryForExpiredItems(player.getEnderChest(), results);
        }
        processExpirationResults(player, results);
    }

    /**
     * Verifica un inventario específico
     */
    private void checkInventoryForExpiredItems(Inventory inventory, List<ExpirationResult> results) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            if (!InteractiveItem.hasExpirationTime(item)) {
                continue;
            }

            // Actualizar placeholders si está habilitado
            if (updatePlaceholders) {
                ItemStack updatedItem = InteractiveItem.updateExpirationPlaceholders(item);
                if (updatedItem != item) {
                    inventory.setItem(slot, updatedItem);
                    item = updatedItem;
                }
            }

            if (!InteractiveItem.isItemStackExpired(item)) {
                continue;
            }

            ExpirationBehavior behavior = InteractiveItem.getItemStackExpirationBehavior(item);
            results.add(new ExpirationResult(inventory, slot, item, behavior));
        }
    }

    /**
     * Procesa los resultados de expiración
     */
    private void processExpirationResults(Player player, List<ExpirationResult> results) {
        if (results.isEmpty()) {
            return;
        }

        for (ExpirationResult result : results) {
            switch (result.behavior()) {
                case REMOVE:
                    result.inventory().setItem(result.slot(), null);
                    break;

                case DISABLE:
                    break;

                case TRANSFORM:
                    ItemStack rottenFood = new ItemStack(Material.ROTTEN_FLESH);
                    result.inventory().setItem(result.slot(), rottenFood);
                    break;

                case KEEP:
                default:
                    break;
            }
        }
    }

    /**
     * Verifica un item específico al intentar usarlo
     */
    public boolean canUseItem(ItemStack item, Player player) {
        if (!InteractiveItem.hasExpirationTime(item)) {
            return true;
        }

        if (!InteractiveItem.isItemStackExpired(item)) {
            return true;
        }

        ExpirationBehavior behavior = InteractiveItem.getItemStackExpirationBehavior(item);

        return switch (behavior) {
            case REMOVE -> false;
            case DISABLE -> {
                if (player != null) {
                }
                yield false;
            }
            case TRANSFORM -> {
                if (player != null) {
                }
                yield false;
            }
            default -> true;
        };
    }

    // Getters y Setters para configuración
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            start();
        } else {
            stop();
        }
    }

    public void setCheckIntervalTicks(long ticks) {
        this.checkIntervalTicks = Math.max(1L, ticks);
        if (enabled) {
            start(); // Reiniciar con nuevo intervalo
        }
    }

    public void setCheckIntervalSeconds(double seconds) {
        setCheckIntervalTicks((long) (seconds * 20));
    }

    public double getCheckIntervalSeconds() {
        return checkIntervalTicks / 20.0;
    }

    public boolean isDebugMessagesEnabled() {
        return enableDebugMessages;
    }
    
    public boolean isUpdatePlaceholders() {
        return updatePlaceholders;
    }

    /**
     * Actualiza solo los placeholders de expiración en el inventario de un jugador
     * sin verificar si están expirados
     */
    public void updatePlaceholdersForPlayer(Player player) {
        if (player == null || !player.isOnline() || !updatePlaceholders) {
            return;
        }

        updateInventoryPlaceholders(player.getInventory());
        
        if (player.getEnderChest() != null) {
            updateInventoryPlaceholders(player.getEnderChest());
        }
    }

    private void updateInventoryPlaceholders(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            if (!InteractiveItem.hasExpirationTime(item)) {
                continue;
            }

            ItemStack updatedItem = InteractiveItem.updateExpirationPlaceholders(item);
            if (updatedItem != item) {
                inventory.setItem(slot, updatedItem);
            }
        }
    }

        private record ExpirationResult(Inventory inventory, int slot, ItemStack item, ExpirationBehavior behavior) {
    }
}
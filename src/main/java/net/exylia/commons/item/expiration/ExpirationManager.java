package net.exylia.commons.item.expiration;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.item.ExpirationBehavior;
import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ExpirationManager {

    @Getter
    private static ExpirationManager instance;
    private final JavaPlugin plugin;
    private ScheduledTask schedulerTask;
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

        schedulerTask = Schedulers.syncTimer(this::checkAllPlayersForExpiredItems,
                checkIntervalTicks, checkIntervalTicks);
        
        if (enableDebugMessages) {
            DebugUtils.logInternalInfo("ExpirationManager iniciado con intervalo de " +
                (checkIntervalTicks / 20.0) + " segundos");
        }
    }

    public void stop() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
        
        if (enableDebugMessages) {
            DebugUtils.logInternalInfo("ExpirationManager detenido");
        }
    }

    private void checkAllPlayersForExpiredItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayerInventoryForExpiredItems(player);
        }
    }

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

    private void checkInventoryForExpiredItems(Inventory inventory, List<ExpirationResult> results) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            if (!InteractiveItem.hasExpirationTime(item)) {
                continue;
            }

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
            start();  
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

package net.exylia.commons.item;

import lombok.Getter;
import net.exylia.commons.actions.ActionSource;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.item.config.TriggerType;
import net.exylia.commons.item.cooldown.CooldownManager;
import net.exylia.commons.item.handlers.ItemInteractionHandler;
import net.exylia.commons.item.handlers.ItemInventoryHandler;
import net.exylia.commons.item.handlers.ItemRegionHandler;
import net.exylia.commons.item.registry.ItemRegistry;
import net.exylia.commons.item.registry.ItemRegistryImpl;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager principal del sistema de items - MODULARIZADO
 * MEJORADO: Soporte para nuevos eventos de trigger
 */
public class ItemManager implements Listener {

    @Getter
    private static JavaPlugin plugin;
    private static ItemRegistry registry;
    private static ItemInteractionHandler interactionHandler;
    private static NamespacedKey itemIdKey;
    private static boolean initialized = false;

    private static final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();
    private static final long DOUBLE_CLICK_PREVENTION_MS = 150;

    public static void initialize(JavaPlugin javaPlugin) {
        if (initialized) return;

        plugin = javaPlugin;
        registry = new ItemRegistryImpl();
        interactionHandler = new ItemInteractionHandler(plugin);
        itemIdKey = new NamespacedKey(plugin, "interactive_item_id");

        Bukkit.getPluginManager().registerEvents(new ItemManager(), plugin);
        CooldownManager.initialize(plugin);
        startClickTimeCleanupTask();

        initialized = true;

        if (WorldGuardUtils.isWorldGuardAvailable()) {
            DebugUtils.logInternalInfo("WorldGuard detectado - Soporte de regiones habilitado");
        }
    }

    public static void registerItemConfiguration(String id, ItemConfiguration config) {
        ensureInitialized();
        registry.registerItemConfiguration(id, config);
    }

    public static void registerItemConfigurations(ConfigurationSection configSection) {
        ensureInitialized();
        registry.registerItemConfigurations(configSection);
    }

    @Nullable
    public static ItemConfiguration getItemConfiguration(String id) {
        ensureInitialized();
        return registry.getItemConfiguration(id);
    }

    public static boolean hasItemConfiguration(String id) {
        ensureInitialized();
        return registry.hasItemConfiguration(id);
    }

    public static void unregisterItemConfiguration(String id) {
        ensureInitialized();
        registry.unregisterItemConfiguration(id);
    }

    public static void reloadItemConfiguration(String id, ItemConfiguration config) {
        ensureInitialized();
        registry.reloadItemConfiguration(id, config);
    }

    public static void reloadAllConfigurations(ConfigurationSection configSection) {
        ensureInitialized();
        registry.reloadAllConfigurations(configSection);
    }

    public static Map<String, ItemConfiguration> getAllConfigurations() {
        ensureInitialized();
        return registry.getAllConfigurations();
    }

    @Nullable
    public static InteractiveItem createItem(String id) {
        ensureInitialized();
        ItemConfiguration config = registry.getItemConfiguration(id);
        if (config == null) {
            DebugUtils.logInternalWarn("No configuration found for item ID: " + id);
            return null;
        }
        return new InteractiveItem(id, config);
    }

    @Nullable
    public static InteractiveItem createItem(String id, Player player) {
        ensureInitialized();
        ItemConfiguration config = registry.getItemConfiguration(id);
        if (config == null) {
            DebugUtils.logInternalWarn("No configuration found for item ID: " + id);
            return null;
        }
        return new InteractiveItem(id, config, player);
    }

    @Nullable
    public static ItemStack createItemStack(String id) {
        InteractiveItem item = createItem(id);
        return item != null ? prepareItem(item) : null;
    }

    @Nullable
    public static ItemStack createItemStack(String id, Player player) {
        InteractiveItem item = createItem(id, player);
        return item != null ? prepareItem(item) : null;
    }

    public static ItemStack prepareItem(InteractiveItem item) {
        ItemStack itemStack = item.getItemStack().clone();
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, item.getId());
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    @Nullable
    public static InteractiveItem getItemFromStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        String itemId = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (itemId == null) return null;

        ItemConfiguration config = registry.getItemConfiguration(itemId);
        if (config == null) {
            DebugUtils.logInternalWarn("Configuration not found for item ID: " + itemId + ". Item may be outdated.");
            return null;
        }

        return InteractiveItem.fromItemStack(itemStack);
    }

    public static boolean isInteractiveItem(ItemStack itemStack) {
        return getItemFromStack(itemStack) != null;
    }

    public static boolean canPlayerUseItemInCurrentRegion(Player player, ItemConfiguration config) {
        ensureInitialized();
        return ItemRegionHandler.canPlayerUseItemInCurrentRegion(player, config);
    }

    public static double getCooldownForPlayerRegion(Player player, ItemConfiguration config) {
        ensureInitialized();
        return ItemRegionHandler.getCooldownForPlayerRegion(player, config);
    }

    public static boolean canPlayerUseItem(Player player, String itemId) {
        ensureInitialized();
        return ItemInteractionHandler.canPlayerUseItem(player, itemId);
    }

    public static void setCooldown(Player player, String itemId, double seconds) {
        ensureInitialized();
        ItemInteractionHandler.setCooldown(player, itemId, seconds);
    }

    public static double getRemainingCooldown(Player player, String itemId) {
        ensureInitialized();
        return ItemInteractionHandler.getRemainingCooldown(player, itemId);
    }

    public static void removeCooldown(Player player, String itemId) {
        ensureInitialized();
        ItemInteractionHandler.removeCooldown(player, itemId);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        if (itemStack == null) return;

        InteractiveItem interactiveItem = getItemFromStack(itemStack);
        if (interactiveItem == null) return;

        if (!canPlayerClick(player.getUniqueId())) {
            return;
        }

        if (interactiveItem.shouldCancelEvent() &&
                interactiveItem.getConfiguration().getTriggerType() != TriggerType.AFTER_CONSUME) {
            event.setCancelled(true);
        }

        ItemClickInfo clickInfo = createItemClickInfo(event, player, itemStack);
        interactionHandler.processItemInteractionWithHand(player, itemStack, interactiveItem, clickInfo, event.getHand());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        InteractiveItem interactiveItem = getItemFromStack(clickedItem);
        if (interactiveItem == null) return;

        if (player.getGameMode() == GameMode.CREATIVE &&
                event.getClickedInventory() == player.getInventory()) {
            return;
        }

        if (ItemInventoryHandler.isMovementClick(event, interactiveItem.getConfiguration())) {
            return;
        }

        if (!canPlayerClick(player.getUniqueId())) {
            return;
        }

        if (interactiveItem.shouldCancelEvent() &&
                interactiveItem.getConfiguration().getTriggerType() != TriggerType.AFTER_CONSUME) {
            event.setCancelled(true);
        }

        ItemClickInfo clickInfo = createInventoryClickInfo(event, player, clickedItem);
        interactionHandler.processItemInteractionFromInventory(player, event, interactiveItem, clickInfo);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        InteractiveItem interactiveItem = getItemFromStack(itemStack);
        if (interactiveItem == null) return;

        if (interactiveItem.getConfiguration().getTriggerType() != TriggerType.AFTER_CONSUME) {
            return;
        }

        event.setCancelled(true); // Siempre cancelar para manejar usos manualmente

        if (!canPlayerClick(player.getUniqueId())) {
            return;
        }

        EquipmentSlot hand = player.getInventory().getItemInMainHand().equals(itemStack) ?
                EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;

        ItemClickInfo clickInfo = new ItemClickInfo(player,
                org.bukkit.event.inventory.ClickType.RIGHT,
                hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                itemStack,
                ActionSource.ITEM_USE);

        interactionHandler.processItemConsumptionEvent(player, itemStack, interactiveItem, clickInfo, hand);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player hitPlayer)) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        InteractiveItem interactiveItem = null;
        EquipmentSlot hand = null;

        if (isInteractiveItem(mainHand)) {
            interactiveItem = getItemFromStack(mainHand);
            hand = EquipmentSlot.HAND;
        } else if (isInteractiveItem(offHand)) {
            interactiveItem = getItemFromStack(offHand);
            hand = EquipmentSlot.OFF_HAND;
        }

        if (interactiveItem == null) return;

        if (!canPlayerClick(player.getUniqueId())) {
            return;
        }

        ItemClickInfo clickInfo = new ItemClickInfo(player,
                org.bukkit.event.inventory.ClickType.LEFT,
                hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                hand == EquipmentSlot.HAND ? mainHand : offHand,
                ActionSource.ITEM_CLICK);

        clickInfo.withData("hitPlayer", hitPlayer); // Añadir hitPlayer al contexto

        interactionHandler.processHitPlayer(player, hitPlayer, hand == EquipmentSlot.HAND ? mainHand : offHand,
                interactiveItem, clickInfo, hand);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        InteractiveItem interactiveItem = null;
        EquipmentSlot hand = null;

        if (isInteractiveItem(mainHand)) {
            interactiveItem = getItemFromStack(mainHand);
            hand = EquipmentSlot.HAND;
        } else if (isInteractiveItem(offHand)) {
            interactiveItem = getItemFromStack(offHand);
            hand = EquipmentSlot.OFF_HAND;
        }

        if (interactiveItem == null) return;

        if (interactiveItem.getConfiguration().getTriggerType() != TriggerType.ON_PROJECTILE_LAUNCH) {
            event.setCancelled(true); // Cancelar lanzamiento si no es el trigger correcto
            return;
        }

        if (!canPlayerClick(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        ItemClickInfo clickInfo = new ItemClickInfo(player,
                org.bukkit.event.inventory.ClickType.RIGHT,
                hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                hand == EquipmentSlot.HAND ? mainHand : offHand,
                ActionSource.ITEM_USE);

        interactionHandler.processProjectileLaunch(player, hand == EquipmentSlot.HAND ? mainHand : offHand,
                interactiveItem, clickInfo, hand);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        InteractiveItem interactiveItem = null;
        EquipmentSlot hand = null;

        if (isInteractiveItem(mainHand)) {
            interactiveItem = getItemFromStack(mainHand);
            hand = EquipmentSlot.HAND;
        } else if (isInteractiveItem(offHand)) {
            interactiveItem = getItemFromStack(offHand);
            hand = EquipmentSlot.OFF_HAND;
        }

        if (interactiveItem == null) return;

        if (interactiveItem.getConfiguration().getTriggerType() != TriggerType.ON_PROJECTILE_HIT) {
            return;
        }

        if (!canPlayerClick(player.getUniqueId())) {
            return;
        }

        Player hitPlayer = event.getHitEntity() instanceof Player ? (Player) event.getHitEntity() : null;

        ItemClickInfo clickInfo = new ItemClickInfo(player,
                org.bukkit.event.inventory.ClickType.RIGHT,
                hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                hand == EquipmentSlot.HAND ? mainHand : offHand,
                ActionSource.ITEM_USE);

        if (hitPlayer != null) {
            clickInfo.withData("hitPlayer", hitPlayer); // Añadir hitPlayer al contexto
        }

        interactionHandler.processProjectileHit(player, hitPlayer, hand == EquipmentSlot.HAND ? mainHand : offHand,
                interactiveItem, clickInfo, hand);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack draggedItem = event.getOldCursor();
        InteractiveItem interactiveItem = getItemFromStack(draggedItem);
        if (interactiveItem == null) return;

        ItemConfiguration config = interactiveItem.getConfiguration();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (!config.isAllowMovement()) {
            event.setCancelled(true);
            return;
        }

        boolean involvesPlayerInventory = event.getRawSlots().stream()
                .anyMatch(slot -> slot >= event.getView().getTopInventory().getSize());

        if (involvesPlayerInventory && !config.isAllowMovement()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        InteractiveItem interactiveItem = getItemFromStack(droppedItem);
        if (interactiveItem == null) return;

        ItemConfiguration config = interactiveItem.getConfiguration();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (!config.isAllowDrop()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();

        if (mainHand != null) {
            InteractiveItem mainInteractiveItem = getItemFromStack(mainHand);
            if (mainInteractiveItem != null) {
                ItemConfiguration config = mainInteractiveItem.getConfiguration();
                if (player.getGameMode() != GameMode.CREATIVE && !config.isAllowSwapToOffhand()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (offHand != null) {
            InteractiveItem offInteractiveItem = getItemFromStack(offHand);
            if (offInteractiveItem != null) {
                ItemConfiguration config = offInteractiveItem.getConfiguration();
                if (player.getGameMode() != GameMode.CREATIVE && !config.isAllowSwapToOffhand()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemStack = event.getItemInHand();
        InteractiveItem interactiveItem = getItemFromStack(itemStack);
        if (interactiveItem != null) {
            event.setCancelled(true);
        }
    }

    private static ItemClickInfo createItemClickInfo(PlayerInteractEvent event, Player player, ItemStack itemStack) {
        Action action = event.getAction();
        org.bukkit.event.inventory.ClickType clickType;

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            clickType = org.bukkit.event.inventory.ClickType.LEFT;
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            clickType = org.bukkit.event.inventory.ClickType.RIGHT;
        } else {
            clickType = org.bukkit.event.inventory.ClickType.UNKNOWN;
        }

        int slot = event.getHand() == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40;

        return new ItemClickInfo(player, clickType, slot, itemStack, ActionSource.ITEM_CLICK);
    }

    private static ItemClickInfo createInventoryClickInfo(InventoryClickEvent event, Player player, ItemStack itemStack) {
        return new ItemClickInfo(player, event.getClick(), event.getSlot(), itemStack, ActionSource.INVENTORY_CLICK);
    }

    private static boolean canPlayerClick(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastClickTime.get(playerId);

        if (lastTime != null && (currentTime - lastTime) < DOUBLE_CLICK_PREVENTION_MS) {
            return false;
        }

        lastClickTime.put(playerId, currentTime);
        return true;
    }

    private static void startClickTimeCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            lastClickTime.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > DOUBLE_CLICK_PREVENTION_MS);
        }, 20L * 60, 20L * 60);
    }

    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ItemManager has not been initialized. Call ItemManager.initialize(plugin) first.");
        }
    }
}
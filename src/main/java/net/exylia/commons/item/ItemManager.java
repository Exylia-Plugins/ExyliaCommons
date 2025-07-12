package net.exylia.commons.item;

import lombok.Getter;
import net.exylia.commons.actions.ActionSource;
import net.exylia.commons.item.config.ItemConfiguration;
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
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Manager principal del sistema de items - MODULARIZADO
 * MEJORADO: Sistema completo de restricciones para items de lobby/user
 */
public class ItemManager implements Listener {

    @Getter
    private static JavaPlugin plugin;
    private static ItemRegistry registry;
    private static ItemInteractionHandler interactionHandler;
    private static NamespacedKey itemIdKey;
    private static boolean initialized = false;

    // Control de doble clic
    private static final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();
    private static final long DOUBLE_CLICK_PREVENTION_MS = 150;

    /**
     * Inicializa el sistema de ítems modularizado
     */
    public static void initialize(JavaPlugin javaPlugin) {
        if (initialized) return;

        plugin = javaPlugin;
        registry = new ItemRegistryImpl();
        interactionHandler = new ItemInteractionHandler(plugin);
        itemIdKey = new NamespacedKey(plugin, "interactive_item_id");

        // Registrar eventos
        Bukkit.getPluginManager().registerEvents(new ItemManager(), plugin);

        // Inicializar sistema de cooldowns
        CooldownManager.initialize(plugin);

        // Iniciar tareas de limpieza
        startClickTimeCleanupTask();

        initialized = true;

        // Log del estado de WorldGuard
        if (WorldGuardUtils.isWorldGuardAvailable()) {
            DebugUtils.logInternalInfo("WorldGuard detectado - Soporte de regiones habilitado");
        } else {
            DebugUtils.logInternalInfo("WorldGuard no detectado - Funcionalidad de regiones deshabilitada");
        }

        DebugUtils.logInternalInfo("ItemManager modularizado inicializado correctamente");
    }

    // ===== MÉTODOS DELEGADOS AL REGISTRY =====

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

    // ===== MÉTODOS DE CREACIÓN DE ÍTEMS =====

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

    // ===== MÉTODOS DE GESTIÓN DE ÍTEMS =====

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

    // ===== MÉTODOS DELEGADOS A HANDLERS =====

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

    // ===== EVENTOS PRINCIPALES =====

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        if (itemStack == null) return;

        InteractiveItem interactiveItem = getItemFromStack(itemStack);
        if (interactiveItem == null) return;

        // Prevención de doble clic
        if (!canPlayerClick(player.getUniqueId())) {
            return;
        }

        // Cancelar evento si está configurado
        if (interactiveItem.shouldCancelEvent()) {
            event.setCancelled(true);
        }

        // Crear ItemClickInfo y procesar con la mano correcta
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

        // En modo creativo, NUNCA activar items desde inventario
        if (player.getGameMode() == GameMode.CREATIVE &&
                event.getClickedInventory() == player.getInventory()) {
            return;
        }

        // Verificar si es un clic de movimiento ANTES de verificar cooldown
        if (ItemInventoryHandler.isMovementClick(event, interactiveItem.getConfiguration())) {
            return;
        }

        if (!canPlayerClick(player.getUniqueId())) {
            return;
        }

        // Cancelar evento si está configurado (solo para clics de uso)
        if (interactiveItem.shouldCancelEvent()) {
            event.setCancelled(true);
        }

        ItemClickInfo clickInfo = createInventoryClickInfo(event, player, clickedItem);
        interactionHandler.processItemInteractionFromInventory(player, event, interactiveItem, clickInfo);
    }

    // ===== NUEVOS EVENTOS PARA RESTRICCIONES MEJORADAS =====

    /**
     * NUEVO: Maneja restricciones de arrastre más específicas
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack draggedItem = event.getOldCursor();
        InteractiveItem interactiveItem = getItemFromStack(draggedItem);
        if (interactiveItem == null) return;

        ItemConfiguration config = interactiveItem.getConfiguration();

        // En modo creativo, permitir arrastre siempre
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // Verificar si el item permite movimiento por arrastre
        if (!config.isAllowMovement()) {
            event.setCancelled(true);
            return;
        }

        // Si está arrastrando desde/hacia el inventario del jugador, aplicar restricciones
        boolean involvesPlayerInventory = event.getRawSlots().stream()
                .anyMatch(slot -> slot >= event.getView().getTopInventory().getSize());

        if (involvesPlayerInventory && !config.isAllowMovement()) {
            event.setCancelled(true);
        }
    }

    /**
     * NUEVO: Previene soltar items interactivos si no está permitido
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        InteractiveItem interactiveItem = getItemFromStack(droppedItem);
        if (interactiveItem == null) return;

        ItemConfiguration config = interactiveItem.getConfiguration();

        // En modo creativo, permitir siempre
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // Verificar si el item permite ser soltado
        if (!config.isAllowDrop()) {
            event.setCancelled(true);
        }
    }

    /**
     * NUEVO: Previene intercambio entre manos si no está permitido
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();

        // Verificar item en mano principal
        if (mainHand != null) {
            InteractiveItem mainInteractiveItem = getItemFromStack(mainHand);
            if (mainInteractiveItem != null) {
                ItemConfiguration config = mainInteractiveItem.getConfiguration();

                // En modo creativo, permitir siempre
                if (player.getGameMode() != GameMode.CREATIVE && !config.isAllowSwapToOffhand()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Verificar item en mano secundaria
        if (offHand != null) {
            InteractiveItem offInteractiveItem = getItemFromStack(offHand);
            if (offInteractiveItem != null) {
                ItemConfiguration config = offInteractiveItem.getConfiguration();

                // En modo creativo, permitir siempre
                if (player.getGameMode() != GameMode.CREATIVE && !config.isAllowSwapToOffhand()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // ===== EVENTOS EXISTENTES MEJORADOS =====

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        ItemStack itemInHand = event.getItemInHand();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean itemInHandInteractive = isInteractiveItem(itemInHand);
        boolean mainHandInteractive = isInteractiveItem(mainHand);
        boolean offHandInteractive = isInteractiveItem(offHand);

        if (itemInHandInteractive || mainHandInteractive || offHandInteractive) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();

        if (!(projectile.getShooter() instanceof Player player)) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean mainHandInteractive = isInteractiveItem(mainHand);
        boolean offHandInteractive = isInteractiveItem(offHand);

        if (mainHandInteractive || offHandInteractive) {
            event.setCancelled(true);
        }
    }

    // ===== MÉTODOS AUXILIARES PARA EVENTOS =====

    @NotNull
    private static ItemClickInfo createItemClickInfo(PlayerInteractEvent event, Player player, ItemStack itemStack) {
        ActionSource source = (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)
                ? ActionSource.ITEM_CLICK : ActionSource.ITEM_USE;

        org.bukkit.event.inventory.ClickType clickType =
                (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)
                        ? org.bukkit.event.inventory.ClickType.LEFT
                        : org.bukkit.event.inventory.ClickType.RIGHT;

        // Detectar en qué mano está el item correctamente
        int slot = event.getHand() == EquipmentSlot.OFF_HAND ? 40 : player.getInventory().getHeldItemSlot();

        return new ItemClickInfo(player, clickType, slot, itemStack, source);
    }

    @NotNull
    private static ItemClickInfo createInventoryClickInfo(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        return new ItemClickInfo(player, event.getClick(), event.getSlot(), clickedItem, ActionSource.ITEM_CLICK);
    }

    // ===== CONTROL DE DOBLE CLIC =====

    private static void startClickTimeCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, ItemManager::cleanupOldClickTimes, 1200L, 1200L);
    }

    private static void cleanupOldClickTimes() {
        long currentTime = System.currentTimeMillis();
        lastClickTime.entrySet().removeIf(entry -> currentTime - entry.getValue() > 60000);
    }

    private static boolean canPlayerClick(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        Long lastClick = lastClickTime.get(playerId);

        if (lastClick == null || currentTime - lastClick > DOUBLE_CLICK_PREVENTION_MS) {
            lastClickTime.put(playerId, currentTime);
            return true;
        }
        return false;
    }

    // ===== UTILIDADES =====

    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ItemManager not initialized. Call initialize() first.");
        }
    }

    /**
     * NUEVO: Obtiene información detallada sobre las restricciones de un item
     */
    public static String getItemRestrictionInfo(ItemStack itemStack) {
        InteractiveItem item = getItemFromStack(itemStack);
        if (item == null) {
            return "No es un item interactivo";
        }

        ItemConfiguration config = item.getConfiguration();
        StringBuilder info = new StringBuilder();

        info.append("=== RESTRICCIONES DEL ITEM ===\n");
        info.append("ID: ").append(item.getId()).append("\n");
        info.append("Permitir movimiento: ").append(config.isAllowMovement()).append("\n");
        info.append("Permitir shift+click: ").append(config.isAllowShiftClick()).append("\n");
        info.append("Permitir soltar: ").append(config.isAllowDrop()).append("\n");
        info.append("Permitir intercambio: ").append(config.isAllowSwapToOffhand()).append("\n");
        info.append("Permitir teclas numéricas: ").append(config.isAllowNumberKeys()).append("\n");

        return info.toString();
    }

    public static String getSystemStats() {
        ensureInitialized();

        if (registry instanceof ItemRegistryImpl registryImpl) {
            return registryImpl.getStats();
        }

        return "Registry stats not available";
    }

    public static void clearClickTimes() {
        lastClickTime.clear();
    }

    public static void shutdown() {
        if (!initialized) return;

        clearClickTimes();
        registry.clear();

        // Shutdown cooldown system
        CooldownManager.shutdown();

        initialized = false;
        DebugUtils.logInternalInfo("ItemManager modularizado desactivado");
    }

    // ===== GETTERS PARA ACCESO DIRECTO =====

    public static ItemRegistry getRegistry() {
        ensureInitialized();
        return registry;
    }

    public static ItemInteractionHandler getInteractionHandler() {
        ensureInitialized();
        return interactionHandler;
    }
}
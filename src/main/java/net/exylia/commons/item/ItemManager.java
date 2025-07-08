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
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
 * Delegado en handlers especializados y registry separado
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

    /**
     * Registra una configuración de ítem
     */
    public static void registerItemConfiguration(String id, ItemConfiguration config) {
        ensureInitialized();
        registry.registerItemConfiguration(id, config);
    }

    /**
     * Registra múltiples configuraciones desde ConfigurationSection
     */
    public static void registerItemConfigurations(ConfigurationSection configSection) {
        ensureInitialized();
        registry.registerItemConfigurations(configSection);
    }

    /**
     * Obtiene una configuración registrada
     */
    @Nullable
    public static ItemConfiguration getItemConfiguration(String id) {
        ensureInitialized();
        return registry.getItemConfiguration(id);
    }

    /**
     * Verifica si existe una configuración
     */
    public static boolean hasItemConfiguration(String id) {
        ensureInitialized();
        return registry.hasItemConfiguration(id);
    }

    /**
     * Remueve una configuración
     */
    public static void unregisterItemConfiguration(String id) {
        ensureInitialized();
        registry.unregisterItemConfiguration(id);
    }

    /**
     * Recarga una configuración específica
     */
    public static void reloadItemConfiguration(String id, ItemConfiguration config) {
        ensureInitialized();
        registry.reloadItemConfiguration(id, config);
    }

    /**
     * Recarga todas las configuraciones desde ConfigurationSection
     */
    public static void reloadAllConfigurations(ConfigurationSection configSection) {
        ensureInitialized();
        registry.reloadAllConfigurations(configSection);
    }

    /**
     * Obtiene todas las configuraciones registradas
     */
    public static Map<String, ItemConfiguration> getAllConfigurations() {
        ensureInitialized();
        return registry.getAllConfigurations();
    }

    // ===== MÉTODOS DE CREACIÓN DE ÍTEMS =====

    /**
     * Crea un InteractiveItem desde una configuración registrada
     */
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

    /**
     * Crea un InteractiveItem con placeholders
     */
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

    /**
     * Crea y prepara un ItemStack listo para usar
     */
    @Nullable
    public static ItemStack createItemStack(String id) {
        InteractiveItem item = createItem(id);
        return item != null ? prepareItem(item) : null;
    }

    /**
     * Crea y prepara un ItemStack con placeholders
     */
    @Nullable
    public static ItemStack createItemStack(String id, Player player) {
        InteractiveItem item = createItem(id, player);
        return item != null ? prepareItem(item) : null;
    }

    // ===== MÉTODOS DE GESTIÓN DE ÍTEMS =====

    /**
     * Prepara un ítem interactivo para ser usado
     * Solo marca el ItemStack con el ID en NBT
     */
    public static ItemStack prepareItem(InteractiveItem item) {
        ItemStack itemStack = item.getItemStack().clone();
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            // Solo guardar el ID en NBT
            meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, item.getId());
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    /**
     * Obtiene un ítem interactivo desde un ItemStack
     * Reconstruye desde configuración registrada + datos NBT
     */
    @Nullable
    public static InteractiveItem getItemFromStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        // Obtener ID desde NBT
        String itemId = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (itemId == null) return null;

        // Buscar configuración en memoria
        ItemConfiguration config = registry.getItemConfiguration(itemId);
        if (config == null) {
            DebugUtils.logInternalWarn("Configuration not found for item ID: " + itemId + ". Item may be outdated.");
            return null;
        }

        // Reconstruir InteractiveItem
        return InteractiveItem.fromItemStack(itemStack);
    }

    /**
     * Verifica si un ItemStack es un ítem interactivo
     */
    public static boolean isInteractiveItem(ItemStack itemStack) {
        return getItemFromStack(itemStack) != null;
    }

    // ===== MÉTODOS DELEGADOS A HANDLERS =====

    /**
     * Verifica si un jugador puede usar un ítem en su ubicación actual (regiones)
     */
    public static boolean canPlayerUseItemInCurrentRegion(Player player, ItemConfiguration config) {
        ensureInitialized();
        return ItemRegionHandler.canPlayerUseItemInCurrentRegion(player, config);
    }

    /**
     * Obtiene el cooldown apropiado para la ubicación actual del jugador
     */
    public static double getCooldownForPlayerRegion(Player player, ItemConfiguration config) {
        ensureInitialized();
        return ItemRegionHandler.getCooldownForPlayerRegion(player, config);
    }

    /**
     * Verifica si un jugador puede usar un ítem (considerando cooldown)
     */
    public static boolean canPlayerUseItem(Player player, String itemId) {
        ensureInitialized();
        return ItemInteractionHandler.canPlayerUseItem(player, itemId);
    }

    /**
     * Establece un cooldown para un jugador e ítem específico
     */
    public static void setCooldown(Player player, String itemId, double seconds) {
        ensureInitialized();
        ItemInteractionHandler.setCooldown(player, itemId, seconds);
    }

    /**
     * Obtiene el tiempo restante de cooldown
     */
    public static double getRemainingCooldown(Player player, String itemId) {
        ensureInitialized();
        return ItemInteractionHandler.getRemainingCooldown(player, itemId);
    }

    /**
     * Remueve el cooldown de un jugador para un ítem
     */
    public static void removeCooldown(Player player, String itemId) {
        ensureInitialized();
        ItemInteractionHandler.removeCooldown(player, itemId);
    }

    // ===== EVENTOS =====

    @EventHandler(priority = EventPriority.HIGH)
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        InteractiveItem interactiveItem = getItemFromStack(clickedItem);
        if (interactiveItem == null) return;

        // En modo creativo, NUNCA activar items desde inventario
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE &&
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

    /**
     * Maneja el arrastre de items interactivos en inventarios
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack draggedItem = event.getOldCursor();

        InteractiveItem interactiveItem = getItemFromStack(draggedItem);
        if (interactiveItem == null) return;

        // Verificar si el item permite movimiento por arrastre
        if (!interactiveItem.getConfiguration().isAllowMovement()) {
            event.setCancelled(true);
            return;
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
     * Obtiene estadísticas del sistema
     */
    public static String getSystemStats() {
        ensureInitialized();

        if (registry instanceof ItemRegistryImpl registryImpl) {
            return registryImpl.getStats();
        }

        return "Registry stats not available";
    }

    /**
     * Limpia todos los datos temporales
     */
    public static void clearClickTimes() {
        lastClickTime.clear();
    }

    /**
     * Apaga el sistema completo
     */
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

    /**
     * Obtiene el registry para operaciones avanzadas
     */
    public static ItemRegistry getRegistry() {
        ensureInitialized();
        return registry;
    }

    /**
     * Obtiene el handler de interacciones para operaciones avanzadas
     */
    public static ItemInteractionHandler getInteractionHandler() {
        ensureInitialized();
        return interactionHandler;
    }
}
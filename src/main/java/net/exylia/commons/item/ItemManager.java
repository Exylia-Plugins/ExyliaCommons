package net.exylia.commons.item;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.actions.ActionSource;
import net.exylia.commons.config.base.MessagesBase;
import net.exylia.commons.item.cooldown.CooldownManager;
import net.exylia.commons.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Sistema de gestión simplificado:
 * - Eliminados handlers personalizados de mensajes
 * - Mensajes definidos directamente en código
 * - Placeholders directos para usos y cooldown: %current_uses%, %max_uses%, %cooldown_formatted%, %cooldown_seconds%
 */
public class ItemManager implements Listener {
    @Getter
    private static JavaPlugin plugin;
    private static final Map<String, ItemConfiguration> itemConfigurations = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();
    private static boolean initialized = false;
    private static NamespacedKey itemIdKey;

    // Cooldown para prevenir doble clic
    private static final long DOUBLE_CLICK_PREVENTION_MS = 150;

    /**
     * Inicializa el sistema de ítems
     */
    public static void initialize(JavaPlugin javaPlugin) {
        if (initialized) return;
        plugin = javaPlugin;
        itemIdKey = new NamespacedKey(plugin, "interactive_item_id");
        Bukkit.getPluginManager().registerEvents(new ItemManager(), plugin);

        startClickTimeCleanupTask();
        initialized = true;

        // Log del estado de WorldGuard
        if (WorldGuardUtils.isWorldGuardAvailable()) {
            DebugUtils.logInternalInfo("WorldGuard detectado - Soporte de regiones habilitado");
        } else {
            DebugUtils.logInternalInfo("WorldGuard no detectado - Funcionalidad de regiones deshabilitada");
        }
    }

    // ===== REGISTRO DE CONFIGURACIONES =====

    /**
     * Registra una configuración de ítem
     * @param id ID único del ítem
     * @param config Configuración del ítem
     */
    public static void registerItemConfiguration(String id, ItemConfiguration config) {
        itemConfigurations.put(id.toLowerCase(), config);
    }

    /**
     * Registra múltiples configuraciones desde ConfigurationSection
     * @param configSection Sección con múltiples ítems
     */
    public static void registerItemConfigurations(ConfigurationSection configSection) {
        for (String itemId : configSection.getKeys(false)) {
            ConfigurationSection itemConfig = configSection.getConfigurationSection(itemId);
            if (itemConfig != null) {
                ItemConfiguration config = ItemConfiguration.fromConfig(itemConfig).build();
                registerItemConfiguration(itemId, config);
            }
        }
    }

    /**
     * Obtiene una configuración registrada
     * @param id ID del ítem
     * @return Configuración o null si no existe
     */
    @Nullable
    public static ItemConfiguration getItemConfiguration(String id) {
        return itemConfigurations.get(id.toLowerCase());
    }

    /**
     * Verifica si existe una configuración
     * @param id ID del ítem
     * @return true si existe
     */
    public static boolean hasItemConfiguration(String id) {
        return itemConfigurations.containsKey(id.toLowerCase());
    }

    /**
     * Remueve una configuración
     * @param id ID del ítem
     */
    public static void unregisterItemConfiguration(String id) {
        itemConfigurations.remove(id.toLowerCase());
    }

    /**
     * Recarga una configuración específica
     * @param id ID del ítem
     * @param config Nueva configuración
     */
    public static void reloadItemConfiguration(String id, ItemConfiguration config) {
        itemConfigurations.put(id.toLowerCase(), config);
    }

    /**
     * Recarga todas las configuraciones desde ConfigurationSection
     * @param configSection Sección con ítems
     */
    public static void reloadAllConfigurations(ConfigurationSection configSection) {
        itemConfigurations.clear();
        registerItemConfigurations(configSection);
    }

    /**
     * Obtiene todas las configuraciones registradas
     * @return Map de configuraciones
     */
    public static Map<String, ItemConfiguration> getAllConfigurations() {
        return new ConcurrentHashMap<>(itemConfigurations);
    }

    // ===== CREACIÓN DE ÍTEMS =====

    /**
     * Crea un InteractiveItem desde una configuración registrada
     * @param id ID de la configuración registrada
     * @return InteractiveItem o null si no existe la configuración
     */
    @Nullable
    public static InteractiveItem createItem(String id) {
        ItemConfiguration config = getItemConfiguration(id);
        if (config == null) {
            DebugUtils.logInternalWarn("No configuration found for item ID: " + id);
            return null;
        }
        return new InteractiveItem(id, config);
    }

    /**
     * Crea un InteractiveItem con placeholders
     * @param id ID de la configuración
     * @param player Jugador para placeholders
     * @return InteractiveItem o null si no existe la configuración
     */
    @Nullable
    public static InteractiveItem createItem(String id, Player player) {
        ItemConfiguration config = getItemConfiguration(id);
        if (config == null) {
            DebugUtils.logInternalWarn("No configuration found for item ID: " + id);
            return null;
        }
        return new InteractiveItem(id, config, player);
    }

    /**
     * Crea y prepara un ItemStack listo para usar
     * @param id ID de la configuración
     * @return ItemStack preparado o null si no existe la configuración
     */
    @Nullable
    public static ItemStack createItemStack(String id) {
        InteractiveItem item = createItem(id);
        return item != null ? prepareItem(item) : null;
    }

    /**
     * Crea y prepara un ItemStack con placeholders
     * @param id ID de la configuración
     * @param player Jugador para placeholders
     * @return ItemStack preparado o null si no existe la configuración
     */
    @Nullable
    public static ItemStack createItemStack(String id, Player player) {
        InteractiveItem item = createItem(id, player);
        return item != null ? prepareItem(item) : null;
    }

    // ===== MÉTODOS DE GESTIÓN =====

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
        ItemConfiguration config = getItemConfiguration(itemId);
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

    // ===== MÉTODOS DE REGIONES =====

    /**
     * Verifica si un jugador puede usar un ítem en su ubicación actual (considerando regiones)
     * @param player Jugador
     * @param config Configuración del ítem
     * @return true si puede usarlo en la región actual
     */
    public static boolean canPlayerUseItemInCurrentRegion(Player player, ItemConfiguration config) {
        if (!config.hasRegionConfiguration() || !WorldGuardUtils.isWorldGuardAvailable()) {
            return true; // Sin configuración de regiones o sin WorldGuard = permitir
        }

        List<String> playerRegions = WorldGuardUtils.getRegionsAtPlayer(player);

        // Si no está en ninguna región, depende del tipo de filtro
        if (playerRegions.isEmpty()) {
            return config.getRegionType() == RegionFilterType.BLACKLIST; // En blacklist se permite fuera de regiones
        }

        return config.canUseInAnyRegion(playerRegions);
    }

    /**
     * Obtiene el cooldown apropiado para la ubicación actual del jugador
     * @param player Jugador
     * @param config Configuración del ítem
     * @return Cooldown en segundos
     */
    public static int getCooldownForPlayerRegion(Player player, ItemConfiguration config) {
        if (!config.hasRegionCooldowns() || !WorldGuardUtils.isWorldGuardAvailable()) {
            return config.getCooldownSeconds(); // Sin configuración de regiones = cooldown por defecto
        }

        List<String> playerRegions = WorldGuardUtils.getRegionsAtPlayer(player);

        if (playerRegions.isEmpty()) {
            return config.getCooldownSeconds(); // Fuera de regiones = cooldown por defecto
        }

        return config.getHighestCooldownForRegions(playerRegions);
    }

    // ===== MÉTODOS DE COOLDOWN =====

    /**
     * Verifica si un jugador puede usar un ítem (considerando cooldown)
     * @param player Jugador
     * @param itemId ID del ítem
     * @return true si puede usarlo
     */
    public static boolean canPlayerUseItem(Player player, String itemId) {
        if (!CooldownManager.isInitialized()) {
            return true; // Si no hay sistema de cooldown, permitir uso
        }

        return !CooldownManager.getInstance().hasCooldown(player, itemId);
    }

    /**
     * Establece un cooldown para un jugador e ítem específico
     * @param player Jugador
     * @param itemId ID del ítem
     * @param seconds Segundos de cooldown
     */
    public static void setCooldown(Player player, String itemId, int seconds) {
        if (CooldownManager.isInitialized()) {
            CooldownManager.getInstance().setCooldown(player, itemId, seconds);
        }
    }

    /**
     * Obtiene el tiempo restante de cooldown
     * @param player Jugador
     * @param itemId ID del ítem
     * @return Segundos restantes
     */
    public static int getRemainingCooldown(Player player, String itemId) {
        if (!CooldownManager.isInitialized()) {
            return 0;
        }
        return CooldownManager.getInstance().getRemainingCooldown(player, itemId);
    }

    /**
     * Remueve el cooldown de un jugador para un ítem
     * @param player Jugador
     * @param itemId ID del ítem
     */
    public static void removeCooldown(Player player, String itemId) {
        if (CooldownManager.isInitialized()) {
            CooldownManager.getInstance().removeCooldown(player, itemId);
        }
    }

    // ===== MÉTODOS DE EFECTOS =====

    /**
     * Ejecuta los efectos visuales y sonoros del ítem
     * @param player Jugador que usó el ítem
     * @param location Ubicación donde ejecutar los efectos
     * @param config Configuración del ítem
     */
    private static void executeItemEffects(Player player, Location location, ItemConfiguration config) {
        if (config.hasSound()) {
            SoundUtils.playSound(player, config.getSoundOnUse());
        }

        if (config.hasParticles()) {
            ParticleUtils.spawnParticles(location, config.getParticlesOnUse());
        }

        if (config.hasFirework()) {
            if (config.isLaunchFireworkOnUse()) {
                FireworkUtils.launchRandomFirework(location.clone().add(0, 1, 0));
            } else if (config.getFireworkOnUse() != null && !config.getFireworkOnUse().trim().isEmpty()) {
                FireworkUtils.launchFirework(location.clone().add(0, 1, 0), config.getFireworkOnUse());
            }
        }
    }

    // ===== MÉTODOS DE MENSAJES SIMPLIFICADOS =====

    /**
     * Maneja el mensaje de cooldown con formato directo
     */
    private static void handleCooldownMessage(Player player, int remainingSeconds) {
        String formattedTime = formatCooldownTime(remainingSeconds);
        MessageUtils.sendMessageAsync(player, MessagesBase.get("items.in_cooldown", "%cooldown_formatted%", formattedTime, "%cooldown_seconds%", String.valueOf(remainingSeconds)));
    }

    /**
     * Maneja el mensaje de región denegada
     */
    private static void handleRegionDeniedMessage(Player player) {
        MessageUtils.sendMessageAsync(player, MessagesBase.get("items.region_denied"));
    }

    /**
     * Formatea el tiempo de cooldown en formato legible
     */
    private static String formatCooldownTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            int minutes = seconds / 60;
            int secs = seconds % 60;
            return minutes + "m" + (secs > 0 ? " " + secs + "s" : "");
        } else {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            return hours + "h" + (minutes > 0 ? " " + minutes + "m" : "");
        }
    }

    // ===== EVENTOS =====

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

        ItemConfiguration config = interactiveItem.getConfiguration();

        // Verificar si puede usar el item en la región actual
        if (!canPlayerUseItemInCurrentRegion(player, config)) {
            handleRegionDeniedMessage(player);
            event.setCancelled(true);
            return;
        }

        // Verificar cooldown antes que cualquier otra cosa
        if (config.hasCooldown()) {
            if (!canPlayerUseItem(player, interactiveItem.getId())) {
                int remainingSeconds = getRemainingCooldown(player, interactiveItem.getId());
                handleCooldownMessage(player, remainingSeconds);
                event.setCancelled(true);
                return;
            }
        }

        // Verificar si el ítem tiene usos restantes
        if (!interactiveItem.hasUsesRemaining()) {
            MessageUtils.sendMessageAsync(player, MessagesBase.get("items.no_uses_remaining"));
            event.setCancelled(true);
            return;
        }

        // Cancelar evento si está configurado
        if (interactiveItem.shouldCancelEvent()) {
            event.setCancelled(true);
        }

        // Crear ItemClickInfo y procesar con la mano correcta
        ItemClickInfo clickInfo = createItemClickInfo(event, player, itemStack);
        processItemInteractionWithHand(player, itemStack, interactiveItem, clickInfo, event.getHand());
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
            // En creativo, permitir movimiento normal sin activar
            return;
        }

        // Verificar si es un clic de movimiento ANTES de verificar cooldown
        if (isMovementClick(event, interactiveItem.getConfiguration())) {
            // Es un movimiento permitido, no activar el item
            return;
        }

        if (!canPlayerClick(player.getUniqueId())) {
            return;
        }

        ItemConfiguration config = interactiveItem.getConfiguration();

        // Verificar si puede usar el item en la región actual
        if (!canPlayerUseItemInCurrentRegion(player, config)) {
            handleRegionDeniedMessage(player);
            event.setCancelled(true);
            return;
        }

        // Verificar cooldown
        if (config.hasCooldown()) {
            if (!canPlayerUseItem(player, interactiveItem.getId())) {
                int remainingSeconds = getRemainingCooldown(player, interactiveItem.getId());
                handleCooldownMessage(player, remainingSeconds);
                event.setCancelled(true);
                return;
            }
        }

        // Verificar si el ítem tiene usos restantes
        if (!interactiveItem.hasUsesRemaining()) {
            MessageUtils.sendMessageAsync(player, MessagesBase.get("items.no_uses_remaining"));
            event.setCancelled(true);
            return;
        }

        // Cancelar evento si está configurado (solo para clics de uso)
        if (interactiveItem.shouldCancelEvent()) {
            event.setCancelled(true);
        }

        ItemClickInfo clickInfo = createInventoryClickInfo(event, player, clickedItem);
        processItemInteractionFromInventory(player, event, interactiveItem, clickInfo);
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

    /**
     * Determina si un clic en inventario es para mover el item o para usarlo
     */
    private boolean isMovementClick(InventoryClickEvent event, ItemConfiguration config) {
        Player player = (Player) event.getWhoClicked();
        ClickType click = event.getClick();

        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return true;
        }
        if (!config.isAllowMovement()) {
            return false;
        }
        switch (click) {
            case SHIFT_LEFT, SHIFT_RIGHT:
                return config.isAllowShiftClick();
            case NUMBER_KEY:
                return config.isAllowNumberKeys();
            case DROP, CONTROL_DROP:
                return config.isAllowDrop();
            case SWAP_OFFHAND:
                return config.isAllowSwapToOffhand();
            case DOUBLE_CLICK:
                return config.isAllowMovement();
            case MIDDLE:
                return config.isAllowMovement() &&
                        player.getGameMode() == org.bukkit.GameMode.CREATIVE;
        }
        if (click == ClickType.LEFT || click == ClickType.RIGHT) {
            return isPickupOrPlaceClick(event, config);
        }

        return false;
    }

    /**
     * Determina si un LEFT/RIGHT click es para recoger/colocar items
     */
    private boolean isPickupOrPlaceClick(InventoryClickEvent event, ItemConfiguration config) {
        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();

        if (cursor != null && !cursor.getType().isAir()) {
            return true;
        }

        if (clicked == null || clicked.getType().isAir()) {
            return true;
        }

        if (event.getClickedInventory() != event.getWhoClicked().getInventory()) {
            return true;
        }

        Player player = (Player) event.getWhoClicked();

        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return true;
        }

        return config.isAllowMovement();
    }

    // ===== MÉTODOS PRIVADOS DE PROCESAMIENTO =====

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

    /**
     * Procesa interacción con información de la mano usada
     */
    private void processItemInteractionWithHand(Player player, ItemStack itemStack, InteractiveItem interactiveItem,
                                                ItemClickInfo clickInfo, EquipmentSlot hand) {
        // PRIMERO: Ejecutar efectos visuales y sonoros
        executeItemEffects(player, player.getLocation(), interactiveItem.getConfiguration());

        boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);

        if (shouldConsumeUse(interactiveItem, actionExecuted)) {
            // Establecer cooldown específico de región si está configurado
            ItemConfiguration config = interactiveItem.getConfiguration();
            if (config.hasCooldown()) {
                int cooldownSeconds = getCooldownForPlayerRegion(player, config);
                setCooldown(player, interactiveItem.getId(), cooldownSeconds);
            }

            // Consumir un uso del ítem
            boolean hasUsesLeft = interactiveItem.consumeUse();

            if (!hasUsesLeft) {
                // Sin usos restantes - eliminar/reducir el ítem de la mano correcta
                removeOrReduceItemByEquipmentSlot(player, itemStack, hand);
                MessageUtils.sendMessageAsync(player, MessagesBase.get("items.consumed"));
                return;
            }

            // Solo actualizar si quedan usos, en la mano correcta
            updateItemByEquipmentSlot(player, itemStack, interactiveItem, hand);
        }

        // Consumir item completo si está configurado
        if (interactiveItem.shouldConsumeOnUse()) {
            removeOrReduceItemByEquipmentSlot(player, itemStack, hand);
        }
    }

    /**
     * Procesa interacción desde inventario
     */
    private void processItemInteractionFromInventory(Player player, InventoryClickEvent event, InteractiveItem interactiveItem, ItemClickInfo clickInfo) {
        // PRIMERO: Ejecutar efectos visuales y sonoros
        executeItemEffects(player, player.getLocation(), interactiveItem.getConfiguration());

        boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);

        if (shouldConsumeUse(interactiveItem, actionExecuted)) {
            // Establecer cooldown específico de región si está configurado
            ItemConfiguration config = interactiveItem.getConfiguration();
            if (config.hasCooldown()) {
                int cooldownSeconds = getCooldownForPlayerRegion(player, config);
                setCooldown(player, interactiveItem.getId(), cooldownSeconds);
            }

            // Consumir un uso del ítem
            boolean hasUsesLeft = interactiveItem.consumeUse();

            if (!hasUsesLeft) {
                // Sin usos restantes - eliminar/reducir el ítem INMEDIATAMENTE
                removeOrReduceItemFromInventory(event);
                MessageUtils.sendMessageAsync(player, MessagesBase.get("items.consumed"));
                return; // Salir inmediatamente después de eliminar
            }

            // Solo actualizar si quedan usos (en inventario)
            updateItemInInventory(event, interactiveItem);
        }

        // Consumir item completo si está configurado (después de procesar usos)
        if (interactiveItem.shouldConsumeOnUse()) {
            removeOrReduceItemFromInventory(event);
        }
    }

    private boolean executeItemActions(Player player, InteractiveItem interactiveItem, ItemClickInfo clickInfo) {
        boolean actionExecuted = false;

        if (interactiveItem.hasAction()) {
            actionExecuted = interactiveItem.executeAction(clickInfo);
        }

        if (!actionExecuted && !interactiveItem.getCommands().isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> interactiveItem.executeCommands(player));
        }

        if (interactiveItem.getClickHandler() != null) {
            interactiveItem.getClickHandler().accept(clickInfo);
        }

        return actionExecuted;
    }

    private boolean shouldConsumeUse(InteractiveItem interactiveItem, boolean actionExecuted) {
        return actionExecuted || !interactiveItem.getCommands().isEmpty() || interactiveItem.getClickHandler() != null;
    }

    // ===== MÉTODOS PARA MANEJO DE MANOS =====

    /**
     * Actualiza item en la mano específica usando EquipmentSlot
     */
    private void updateItemByEquipmentSlot(Player player, ItemStack itemStack, InteractiveItem interactiveItem, EquipmentSlot hand) {
        if (interactiveItem.hasLimitedUses()) {
            if (itemStack.getAmount() > 1 && interactiveItem.isStackable()) {
                itemStack.setAmount(itemStack.getAmount() - 1);
                ItemStack updatedStack = interactiveItem.getItemStack();
                updatedStack.setAmount(1);
                player.getInventory().addItem(updatedStack);
            } else {
                ItemStack updatedStack = interactiveItem.getItemStack();
                updatedStack.setAmount(itemStack.getAmount());

                // Usar métodos específicos para cada mano
                switch (hand) {
                    case HAND -> player.getInventory().setItemInMainHand(updatedStack);
                    case OFF_HAND -> player.getInventory().setItemInOffHand(updatedStack);
                }
            }
        }
    }

    /**
     * Remueve item de la mano específica usando EquipmentSlot
     */
    private void removeOrReduceItemByEquipmentSlot(Player player, ItemStack itemStack, EquipmentSlot hand) {
        if (itemStack.getAmount() > 1) {
            itemStack.setAmount(itemStack.getAmount() - 1);
        } else {
            // Usar métodos específicos para cada mano
            switch (hand) {
                case HAND -> player.getInventory().setItemInMainHand(null);
                case OFF_HAND -> player.getInventory().setItemInOffHand(null);
            }
        }
    }

    // ===== MÉTODOS PARA INVENTARIO =====

    private void removeOrReduceItemFromInventory(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null) return;

        if (currentItem.getAmount() > 1) {
            currentItem.setAmount(currentItem.getAmount() - 1);
            event.setCurrentItem(currentItem);
        } else {
            event.setCurrentItem(null);
        }
    }

    private void updateItemInInventory(InventoryClickEvent event, InteractiveItem interactiveItem) {
        if (interactiveItem.hasLimitedUses()) {
            ItemStack currentItem = event.getCurrentItem();
            if (currentItem == null) return;

            if (currentItem.getAmount() > 1 && interactiveItem.isStackable()) {
                currentItem.setAmount(currentItem.getAmount() - 1);
                event.setCurrentItem(currentItem);

                // Agregar el item actualizado al inventario si hay espacio
                ItemStack updatedStack = interactiveItem.getItemStack();
                updatedStack.setAmount(1);
                Player player = (Player) event.getWhoClicked();
                player.getInventory().addItem(updatedStack);
            } else {
                ItemStack updatedStack = interactiveItem.getItemStack();
                updatedStack.setAmount(currentItem.getAmount());
                event.setCurrentItem(updatedStack);
            }
        }
    }

    // ===== MÉTODOS DE LIMPIEZA =====

    public static void clearClickTimes() {
        lastClickTime.clear();
    }

    public static void shutdown() {
        clearClickTimes();
        itemConfigurations.clear();

        // Shutdown cooldown system
        CooldownManager.shutdown();

        initialized = false;
    }
}
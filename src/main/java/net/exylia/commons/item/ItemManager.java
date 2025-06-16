package net.exylia.commons.item;

import net.exylia.commons.actions.ActionSource;
import net.exylia.commons.item.cooldown.CooldownManager;
import net.exylia.commons.utils.SoundUtils;
import net.exylia.commons.utils.ParticleUtils;
import net.exylia.commons.utils.FireworkUtils;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Sistema de gestión optimizado:
 * - Registro de configuraciones en memoria (Map<String, ItemConfiguration>)
 * - Solo persiste ID y datos críticos en NBT
 * - Integración con sistema de cooldown persistente
 * - Integración con efectos de sonido, partículas y fuegos artificiales
 * - Mejor rendimiento y flexibilidad
 * - Soporte para items movibles en inventario
 */
public class ItemManager implements Listener {
    private static JavaPlugin plugin;
    private static final Map<String, ItemConfiguration> itemConfigurations = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();
    private static boolean initialized = false;
    private static NamespacedKey itemIdKey;

    // Callbacks para manejo de cooldown
    private static BiConsumer<Player, String> cooldownMessageHandler;

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

        // Inicializar sistema de cooldown
        CooldownManager.initialize(plugin);

        startClickTimeCleanupTask();
        initialized = true;
    }

    // ===== CONFIGURACIÓN DE CALLBACKS =====

    /**
     * Establece el manejador de mensajes de cooldown
     * @param handler Función que recibe (Player, mensaje) para mostrar al jugador
     */
    public static void setCooldownMessageHandler(BiConsumer<Player, String> handler) {
        cooldownMessageHandler = handler;
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
            Bukkit.getLogger().warning("No configuration found for item ID: " + id);
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
            Bukkit.getLogger().warning("No configuration found for item ID: " + id);
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
            Bukkit.getLogger().warning("Configuration not found for item ID: " + itemId + ". Item may be outdated.");
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
        // Ejecutar sonido
        if (config.hasSound()) {
            SoundUtils.playSound(player, config.getSoundOnUse());
        }

        // Ejecutar partículas
        if (config.hasParticles()) {
            ParticleUtils.spawnParticles(location, config.getParticlesOnUse());
        }

        // Ejecutar fuegos artificiales
        if (config.hasFirework()) {
            if (config.shouldLaunchFireworkOnUse()) {
                // Fuego artificial aleatorio
                FireworkUtils.launchRandomFirework(location.clone().add(0, 1, 0));
            } else if (config.getFireworkOnUse() != null && !config.getFireworkOnUse().trim().isEmpty()) {
                // Fuego artificial configurado
                FireworkUtils.launchFirework(location.clone().add(0, 1, 0), config.getFireworkOnUse());
            }
        }
    }

    // ===== UTILIDADES =====

    /**
     * Obtiene estadísticas del sistema
     */
    public static String getStats() {
        String cooldownStats = CooldownManager.isInitialized() ?
                CooldownManager.getInstance().getStats() : "Cooldown system disabled";

        int itemsWithEffects = (int) itemConfigurations.values().stream()
                .mapToLong(config -> config.hasEffects() ? 1 : 0)
                .sum();

        return String.format(
                "Registered configurations: %d, Items with effects: %d, Click times tracked: %d players, %s",
                itemConfigurations.size(),
                itemsWithEffects,
                lastClickTime.size(),
                cooldownStats
        );
    }

    /**
     * Valida que todas las configuraciones registradas sean válidas
     */
    public static void validateConfigurations() {
        itemConfigurations.forEach((id, config) -> {
            if (config.getMaterial() == null || config.getMaterial().isEmpty()) {
                Bukkit.getLogger().warning("Item configuration '" + id + "' has invalid material");
            }
            if (config.getMaxUses() == 0) {
                Bukkit.getLogger().warning("Item configuration '" + id + "' has 0 max uses (will be unusable)");
            }
            if (config.getCooldownSeconds() < 0) {
                Bukkit.getLogger().warning("Item configuration '" + id + "' has negative cooldown");
            }

            // Validar configuración de efectos
            if (config.hasSound()) {
                // Podrías agregar validación específica para sonidos aquí
                String sound = config.getSoundOnUse();
                if (sound != null && !sound.contains("|")) {
                    Bukkit.getLogger().info("Item '" + id + "' has simple sound format: " + sound);
                }
            }

            if (config.hasParticles()) {
                // Podrías agregar validación específica para partículas aquí
                String particles = config.getParticlesOnUse();
                if (particles != null && !particles.contains("|")) {
                    Bukkit.getLogger().info("Item '" + id + "' has simple particle format: " + particles);
                }
            }
        });
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

        // Verificar cooldown antes que cualquier otra cosa
        if (interactiveItem.getConfiguration().hasCooldown()) {
            if (!canPlayerUseItem(player, interactiveItem.getId())) {
                int remainingSeconds = getRemainingCooldown(player, interactiveItem.getId());
                handleCooldownMessage(player, interactiveItem, remainingSeconds);
                event.setCancelled(true);
                return;
            }
        }

        // Verificar si el ítem tiene usos restantes
        if (!interactiveItem.hasUsesRemaining()) {
            player.sendMessage("§cEste ítem ya no tiene usos restantes.");
            event.setCancelled(true);
            return;
        }

        // Cancelar evento si está configurado
        if (interactiveItem.shouldCancelEvent()) {
            event.setCancelled(true);
        }

        ItemClickInfo clickInfo = createItemClickInfo(event, player, itemStack);
        processItemInteraction(player, itemStack, interactiveItem, clickInfo);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        InteractiveItem interactiveItem = getItemFromStack(clickedItem);
        if (interactiveItem == null) return;

        if (isMovementClick(event, interactiveItem.getConfiguration())) {
            return;
        }

        if (!canPlayerClick(player.getUniqueId())) {
            return;
        }

        // Verificar cooldown
        if (interactiveItem.getConfiguration().hasCooldown()) {
            if (!canPlayerUseItem(player, interactiveItem.getId())) {
                int remainingSeconds = getRemainingCooldown(player, interactiveItem.getId());
                handleCooldownMessage(player, interactiveItem, remainingSeconds);
                event.setCancelled(true);
                return;
            }
        }

        // Verificar si el ítem tiene usos restantes
        if (!interactiveItem.hasUsesRemaining()) {
            player.sendMessage("§cEste ítem ya no tiene usos restantes.");
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
     * Este evento se dispara cuando el jugador arrastra un item a través de múltiples slots
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem == null) return;

        InteractiveItem interactiveItem = getItemFromStack(draggedItem);
        if (interactiveItem == null) return;

        // Verificar si el item permite movimiento por arrastre
        if (!interactiveItem.getConfiguration().allowsMovement()) {
            event.setCancelled(true);
            return;
        }
    }

    /**
     * Determina si un clic en inventario es para mover el item o para usarlo
     * @param event Evento de clic en inventario
     * @param config Configuración del item para verificar permisos
     * @return true si es un clic de movimiento, false si es de uso
     */
    private boolean isMovementClick(InventoryClickEvent event, ItemConfiguration config) {
        ClickType click = event.getClick();

        if (!config.allowsMovement()) {
            return false;
        }

        return switch (click) {
            case SHIFT_LEFT, SHIFT_RIGHT -> config.allowsShiftClick();
            case NUMBER_KEY -> config.allowsNumberKeys();
            case DROP, CONTROL_DROP -> config.allowsDrop();
            case SWAP_OFFHAND -> config.allowsSwapToOffhand();
            case LEFT, RIGHT -> config.allowsMovement() && isPickupOrPlaceClick(event);
            case MIDDLE -> config.allowsMovement() &&
                    event.getWhoClicked().getGameMode() == org.bukkit.GameMode.CREATIVE;
            case DOUBLE_CLICK -> config.allowsMovement();
            default -> false;
        };
    }

    /**
     * Determina si un LEFT/RIGHT click es para recoger/colocar items
     * @param event Evento de clic
     * @return true si es movimiento, false si es uso
     */
    private boolean isPickupOrPlaceClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();

        // Si el cursor tiene un item, probablemente está colocando/intercambiando
        if (cursor != null && !cursor.getType().isAir()) {
            return true;
        }

        // Si hace clic en un slot vacío, no es uso de item
        if (clicked == null || clicked.getType().isAir()) {
            return true;
        }

        // Si está en el inventario del jugador (no en una GUI personalizada),
        // LEFT/RIGHT normalmente son para recoger
        if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
            // Permitir el movimiento si está en el inventario principal
            return true;
        }

        // Si está en una GUI personalizada, LEFT/RIGHT probablemente son para usar
        return false;
    }

    /**
     * Maneja el mensaje de cooldown
     */
    private void handleCooldownMessage(Player player, InteractiveItem item, int remainingSeconds) {
        String message = item.getConfiguration().getCooldownMessage();
        if (message != null && !message.isEmpty()) {
            // Formatear tiempo
            String timeFormat = formatTime(remainingSeconds);
            message = message.replace("%time%", timeFormat)
                    .replace("%seconds%", String.valueOf(remainingSeconds));

            // Usar handler personalizado si existe
            if (cooldownMessageHandler != null) {
                cooldownMessageHandler.accept(player, message);
            } else {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Formatea el tiempo de cooldown en formato legible
     */
    private String formatTime(int seconds) {
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

    // ===== MÉTODOS PRIVADOS DE PROCESAMIENTO =====

    @NotNull
    private static ItemClickInfo createItemClickInfo(PlayerInteractEvent event, Player player, ItemStack itemStack) {
        ActionSource source = (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)
                ? ActionSource.ITEM_CLICK : ActionSource.ITEM_USE;

        org.bukkit.event.inventory.ClickType clickType =
                (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)
                        ? org.bukkit.event.inventory.ClickType.LEFT
                        : org.bukkit.event.inventory.ClickType.RIGHT;

        return new ItemClickInfo(player, clickType, player.getInventory().getHeldItemSlot(), itemStack, source);
    }

    @NotNull
    private static ItemClickInfo createInventoryClickInfo(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        return new ItemClickInfo(player, event.getClick(), event.getSlot(), clickedItem, ActionSource.ITEM_CLICK);
    }

    private void processItemInteraction(Player player, ItemStack itemStack, InteractiveItem interactiveItem, ItemClickInfo clickInfo) {
        // PRIMERO: Ejecutar efectos visuales y sonoros
        executeItemEffects(player, player.getLocation(), interactiveItem.getConfiguration());

        boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);

        if (shouldConsumeUse(interactiveItem, actionExecuted)) {
            // Establecer cooldown si está configurado
            if (interactiveItem.getConfiguration().hasCooldown()) {
                setCooldown(player, interactiveItem.getId(), interactiveItem.getConfiguration().getCooldownSeconds());
            }

            // Consumir un uso del ítem
            boolean hasUsesLeft = interactiveItem.consumeUse();

            if (!hasUsesLeft) {
                // Sin usos restantes - eliminar/reducir el ítem INMEDIATAMENTE
                removeOrReduceItemFromHand(player, itemStack);
                player.sendMessage("§7El ítem se ha agotado.");
                return; // Salir inmediatamente después de eliminar
            }

            // Solo actualizar si quedan usos
            updateItemInHand(player, itemStack, interactiveItem);
        }

        // Consumir item completo si está configurado (después de procesar usos)
        if (interactiveItem.shouldConsumeOnUse()) {
            removeOrReduceItemFromHand(player, itemStack);
        }
    }

    private void processItemInteractionFromInventory(Player player, InventoryClickEvent event, InteractiveItem interactiveItem, ItemClickInfo clickInfo) {
        // PRIMERO: Ejecutar efectos visuales y sonoros
        executeItemEffects(player, player.getLocation(), interactiveItem.getConfiguration());

        boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);

        if (shouldConsumeUse(interactiveItem, actionExecuted)) {
            // Establecer cooldown si está configurado
            if (interactiveItem.getConfiguration().hasCooldown()) {
                setCooldown(player, interactiveItem.getId(), interactiveItem.getConfiguration().getCooldownSeconds());
            }

            // Consumir un uso del ítem
            boolean hasUsesLeft = interactiveItem.consumeUse();

            if (!hasUsesLeft) {
                // Sin usos restantes - eliminar/reducir el ítem INMEDIATAMENTE
                removeOrReduceItemFromInventory(event);
                player.sendMessage("§7El ítem se ha agotado.");
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

    private void removeOrReduceItemFromHand(Player player, ItemStack itemStack) {
        if (itemStack.getAmount() > 1) {
            itemStack.setAmount(itemStack.getAmount() - 1);
        } else {
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), null);
        }
    }

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

    private void updateItemInHand(Player player, ItemStack itemStack, InteractiveItem interactiveItem) {
        if (interactiveItem.hasLimitedUses()) {
            if (itemStack.getAmount() > 1 && interactiveItem.isStackable()) {
                itemStack.setAmount(itemStack.getAmount() - 1);
                ItemStack updatedStack = interactiveItem.getItemStack();
                updatedStack.setAmount(1);
                player.getInventory().addItem(updatedStack);
            } else {
                ItemStack updatedStack = interactiveItem.getItemStack();
                updatedStack.setAmount(itemStack.getAmount());
                player.getInventory().setItem(player.getInventory().getHeldItemSlot(), updatedStack);
            }
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

    // ===== GETTERS =====

    public static JavaPlugin getPlugin() {
        return plugin;
    }

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
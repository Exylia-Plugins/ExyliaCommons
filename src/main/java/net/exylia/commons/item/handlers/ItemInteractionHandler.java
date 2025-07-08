package net.exylia.commons.item.handlers;

import net.exylia.commons.config.base.MainConfigBase;
import net.exylia.commons.config.base.MessagesBase;
import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.item.ItemClickInfo;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.item.cooldown.CooldownManager;
import net.exylia.commons.utils.MessageUtils;
import net.exylia.commons.utils.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import static net.exylia.commons.utils.TimeFormatter.timeFormatter;

/**
 * Manejador principal de interacciones con items
 */
public class ItemInteractionHandler {

    private final JavaPlugin plugin;

    public ItemInteractionHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Procesa interacción con información de la mano usada
     */
    public void processItemInteractionWithHand(Player player, ItemStack itemStack, InteractiveItem interactiveItem,
                                               ItemClickInfo clickInfo, EquipmentSlot hand) {

        // Validaciones previas
        if (!preValidateItemUsage(player, interactiveItem)) {
            return;
        }

        // PRIMERO: Ejecutar efectos visuales y sonoros
        ItemEffectsHandler.executeEffects(player, player.getLocation(), interactiveItem.getConfiguration());

        // Ejecutar acciones del item
        boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);

        // Procesar consumo de usos y cooldown
        if (shouldConsumeUse(interactiveItem, actionExecuted)) {
            processItemConsumption(player, itemStack, interactiveItem, hand);
        }

        // Consumir item completo si está configurado
        if (interactiveItem.shouldConsumeOnUse()) {
            ItemInventoryHandler.removeOrReduceItemByEquipmentSlot(player, itemStack, hand);
        }
    }

    /**
     * Procesa interacción desde inventario
     */
    public void processItemInteractionFromInventory(Player player, InventoryClickEvent event,
                                                    InteractiveItem interactiveItem, ItemClickInfo clickInfo) {

        // Validaciones previas
        if (!preValidateItemUsage(player, interactiveItem)) {
            return;
        }

        // PRIMERO: Ejecutar efectos visuales y sonoros
        ItemEffectsHandler.executeEffects(player, player.getLocation(), interactiveItem.getConfiguration());

        // Ejecutar acciones del item
        boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);

        // Procesar consumo de usos y cooldown
        if (shouldConsumeUse(interactiveItem, actionExecuted)) {
            processItemConsumptionFromInventory(event, interactiveItem);
        }

        // Consumir item completo si está configurado (después de procesar usos)
        if (interactiveItem.shouldConsumeOnUse()) {
            ItemInventoryHandler.removeOrReduceItemFromInventory(event);
        }
    }

    /**
     * Validaciones previas antes de usar un item
     * @param player Jugador
     * @param interactiveItem Item a validar
     * @return true si pasa todas las validaciones
     */
    private boolean preValidateItemUsage(Player player, InteractiveItem interactiveItem) {
        ItemConfiguration config = interactiveItem.getConfiguration();

        // Verificar región
        if (!ItemRegionHandler.canPlayerUseItemInCurrentRegion(player, config)) {
            handleRegionDeniedMessage(player);
            return false;
        }

        // Verificar cooldown
        if (config.hasCooldown()) {
            if (!canPlayerUseItem(player, interactiveItem.getId())) {
                double remainingSeconds = getRemainingCooldown(player, interactiveItem.getId());
                handleCooldownMessage(player, remainingSeconds);
                return false;
            }
        }

        // Verificar usos restantes
        if (!interactiveItem.hasUsesRemaining()) {
            MessageUtils.sendMessageAsync(player, MessagesBase.get("system.items.no_uses_remaining"));
            return false;
        }

        return true;
    }

    /**
     * Ejecuta las acciones del item
     * @param player Jugador
     * @param interactiveItem Item
     * @param clickInfo Información del clic
     * @return true si se ejecutó alguna acción
     */
    private boolean executeItemActions(Player player, InteractiveItem interactiveItem, ItemClickInfo clickInfo) {
        boolean actionExecuted = false;

        // Ejecutar acción principal
        if (interactiveItem.hasAction()) {
            actionExecuted = interactiveItem.executeAction(clickInfo);
        }

        // Ejecutar comandos si no se ejecutó acción o no son excluyentes
        if (!actionExecuted && !interactiveItem.getCommands().isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> interactiveItem.executeCommands(player));
        }

        // Ejecutar callback personalizado
        if (interactiveItem.getClickHandler() != null) {
            interactiveItem.getClickHandler().accept(clickInfo);
        }

        return actionExecuted;
    }

    /**
     * Determina si se debe consumir un uso del item
     */
    private boolean shouldConsumeUse(InteractiveItem interactiveItem, boolean actionExecuted) {
        return actionExecuted || !interactiveItem.getCommands().isEmpty() ||
                interactiveItem.getClickHandler() != null;
    }

    /**
     * Procesa el consumo del item desde mano
     */
    private void processItemConsumption(Player player, ItemStack itemStack,
                                        InteractiveItem interactiveItem, EquipmentSlot hand) {

        // Establecer cooldown específico de región
        setCooldownForRegion(player, interactiveItem);

        // Consumir un uso del ítem
        boolean hasUsesLeft = interactiveItem.consumeUse();

        if (!hasUsesLeft) {
            // Sin usos restantes - eliminar/reducir el ítem de la mano
            ItemInventoryHandler.removeOrReduceItemByEquipmentSlot(player, itemStack, hand);
            MessageUtils.sendMessageAsync(player, MessagesBase.get("system.items.consumed"));
            return;
        }

        // Solo actualizar si quedan usos
        ItemInventoryHandler.updateItemByEquipmentSlot(player, itemStack, interactiveItem, hand);
        interactiveItem.updatePlaceholders(player, hand);
    }

    /**
     * Procesa el consumo del item desde inventario
     */
    private void processItemConsumptionFromInventory(InventoryClickEvent event, InteractiveItem interactiveItem) {
        Player player = (Player) event.getWhoClicked();

        // Establecer cooldown específico de región
        setCooldownForRegion(player, interactiveItem);

        // Consumir un uso del ítem
        boolean hasUsesLeft = interactiveItem.consumeUse();

        if (!hasUsesLeft) {
            // Sin usos restantes - eliminar/reducir el ítem INMEDIATAMENTE
            ItemInventoryHandler.removeOrReduceItemFromInventory(event);
            MessageUtils.sendMessageAsync(player, MessagesBase.get("system.items.consumed"));
            return;
        }

        // Solo actualizar si quedan usos
        ItemInventoryHandler.updateItemInInventory(event, interactiveItem);
    }

    /**
     * Establece cooldown considerando la región del jugador
     */
    private void setCooldownForRegion(Player player, InteractiveItem interactiveItem) {
        ItemConfiguration config = interactiveItem.getConfiguration();
        if (config.hasCooldown()) {
            double cooldownSeconds = ItemRegionHandler.getCooldownForPlayerRegion(player, config);
            setCooldown(player, interactiveItem.getId(), cooldownSeconds);
        }
    }

    // ===== MÉTODOS DE COOLDOWN DELEGADOS =====

    /**
     * Verifica si un jugador puede usar un ítem (considerando cooldown)
     */
    public static boolean canPlayerUseItem(Player player, String itemId) {
        if (!CooldownManager.isInitialized()) {
            return true;
        }
        return !CooldownManager.getInstance().hasCooldown(player, itemId);
    }

    /**
     * Establece un cooldown para un jugador e ítem específico
     */
    public static void setCooldown(Player player, String itemId, double seconds) {
        if (CooldownManager.isInitialized()) {
            CooldownManager.getInstance().setCooldown(player, itemId, seconds);
        }
    }

    /**
     * Obtiene el tiempo restante de cooldown
     */
    public static double getRemainingCooldown(Player player, String itemId) {
        if (!CooldownManager.isInitialized()) {
            return 0.0;
        }
        return CooldownManager.getInstance().getRemainingCooldown(player, itemId);
    }

    /**
     * Remueve el cooldown de un jugador para un ítem
     */
    public static void removeCooldown(Player player, String itemId) {
        if (CooldownManager.isInitialized()) {
            CooldownManager.getInstance().removeCooldown(player, itemId);
        }
    }

    // ===== MÉTODOS DE MENSAJES =====

    /**
     * Maneja el mensaje de cooldown con formato de double
     */
    private void handleCooldownMessage(Player player, double remainingSeconds) {
        String formattedTime = timeFormatter.format(remainingSeconds);
        MessageUtils.sendMessageAsync(player, MessagesBase.get("system.items.in_cooldown",
                "%cooldown_formatted%", formattedTime,
                "%cooldown_seconds%", String.valueOf(remainingSeconds)));
    }

    /**
     * Maneja el mensaje de región denegada
     */
    private void handleRegionDeniedMessage(Player player) {
        MessageUtils.sendMessageAsync(player, MessagesBase.get("system.items.region_denied"));
    }
}
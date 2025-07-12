package net.exylia.commons.item.handlers;

import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Manejador de operaciones de inventario para items interactivos
 * MEJORADO: Validaciones más completas para restricciones
 */
public class ItemInventoryHandler {

    /**
     * Actualiza item en la mano específica usando EquipmentSlot
     */
    public static void updateItemByEquipmentSlot(Player player, ItemStack itemStack,
                                                 InteractiveItem interactiveItem, EquipmentSlot hand) {
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
    public static void removeOrReduceItemByEquipmentSlot(Player player, ItemStack itemStack, EquipmentSlot hand) {
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

    /**
     * Remueve o reduce item desde inventario
     */
    public static void removeOrReduceItemFromInventory(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null) return;

        if (currentItem.getAmount() > 1) {
            currentItem.setAmount(currentItem.getAmount() - 1);
            event.setCurrentItem(currentItem);
        } else {
            event.setCurrentItem(null);
        }
    }

    /**
     * Actualiza item en inventario
     */
    public static void updateItemInInventory(InventoryClickEvent event, InteractiveItem interactiveItem) {
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

    /**
     * MEJORADO: Verifica si un clic en inventario es para mover el item o para usarlo
     * Ahora con validaciones más específicas y logging
     */
    public static boolean isMovementClick(InventoryClickEvent event, ItemConfiguration config) {
        Player player = (Player) event.getWhoClicked();
        ClickType click = event.getClick();

        // El modo creativo siempre permite movimiento
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        // Si no permite movimiento básico, denegar la mayoría de operaciones
        if (!config.isAllowMovement()) {
            return false;
        }

        // Validar cada tipo de clic específicamente
        return switch (click) {
            case SHIFT_LEFT, SHIFT_RIGHT -> config.isAllowShiftClick();
            case NUMBER_KEY -> config.isAllowNumberKeys();
            case DROP, CONTROL_DROP -> config.isAllowDrop();
            case SWAP_OFFHAND -> config.isAllowSwapToOffhand();
            case DOUBLE_CLICK -> config.isAllowMovement();
            case MIDDLE -> config.isAllowMovement() && player.getGameMode() == GameMode.CREATIVE;
            case LEFT, RIGHT -> isPickupOrPlaceClick(event, config);
            case WINDOW_BORDER_LEFT, WINDOW_BORDER_RIGHT -> true;
            default -> config.isAllowMovement();
        };
    }

    /**
     * MEJORADO: Determina si un LEFT/RIGHT click es para recoger/colocar items
     * Ahora con más validaciones específicas
     */
    private static boolean isPickupOrPlaceClick(InventoryClickEvent event, ItemConfiguration config) {
        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();

        // Si tiene algo en el cursor, está intentando colocar
        if (cursor != null && !cursor.getType().isAir()) {
            return true;
        }

        // Si no hay nada clickeado, no hay restricción
        if (clicked == null || clicked.getType().isAir()) {
            return true;
        }

        // Si está clickeando en un inventario que no es el suyo, permitir
        if (event.getClickedInventory() != player.getInventory()) {
            return true;
        }

        // En modo creativo, siempre permitir
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        // Para items restringidos, verificar si permite movimiento
        boolean allowed = config.isAllowMovement();
        if (!allowed) {
        } else {
        }

        return allowed;
    }

    /**
     * NUEVO: Verifica si un item específico puede ser movido según sus restricciones
     */
    public static boolean canItemBeMoved(ItemStack itemStack, ClickType clickType, Player player) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return true;
        }

        // Si no es un item interactivo, permitir movimiento
        net.exylia.commons.item.InteractiveItem interactiveItem =
                net.exylia.commons.item.ItemManager.getItemFromStack(itemStack);

        if (interactiveItem == null) {
            return true;
        }

        ItemConfiguration config = interactiveItem.getConfiguration();

        // Modo creativo siempre permite
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        // Verificar según el tipo de clic
        return switch (clickType) {
            case SHIFT_LEFT, SHIFT_RIGHT -> config.isAllowShiftClick();
            case NUMBER_KEY -> config.isAllowNumberKeys();
            case DROP, CONTROL_DROP -> config.isAllowDrop();
            case SWAP_OFFHAND -> config.isAllowSwapToOffhand();
            case DOUBLE_CLICK -> config.isAllowMovement();
            case MIDDLE -> config.isAllowMovement() && player.getGameMode() == GameMode.CREATIVE;
            case LEFT, RIGHT -> config.isAllowMovement();
            default -> config.isAllowMovement();
        };
    }

    /**
     * NUEVO: Obtiene información detallada sobre por qué un movimiento fue denegado
     */
    public static String getMovementDenialReason(ItemStack itemStack, ClickType clickType, Player player) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return "No hay item";
        }

        net.exylia.commons.item.InteractiveItem interactiveItem =
                net.exylia.commons.item.ItemManager.getItemFromStack(itemStack);

        if (interactiveItem == null) {
            return "No es un item interactivo";
        }

        ItemConfiguration config = interactiveItem.getConfiguration();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return "Modo creativo permite todo";
        }

        return switch (clickType) {
            case SHIFT_LEFT, SHIFT_RIGHT ->
                    config.isAllowShiftClick() ? "Permitido" : "Shift+click deshabilitado";
            case NUMBER_KEY ->
                    config.isAllowNumberKeys() ? "Permitido" : "Teclas numéricas deshabilitadas";
            case DROP, CONTROL_DROP ->
                    config.isAllowDrop() ? "Permitido" : "Soltar item deshabilitado";
            case SWAP_OFFHAND ->
                    config.isAllowSwapToOffhand() ? "Permitido" : "Intercambio a mano secundaria deshabilitado";
            case DOUBLE_CLICK ->
                    config.isAllowMovement() ? "Permitido" : "Movimiento general deshabilitado";
            case MIDDLE ->
                    (config.isAllowMovement() && player.getGameMode() == GameMode.CREATIVE) ?
                            "Permitido" : "Click medio solo permitido en creativo con movimiento habilitado";
            case LEFT, RIGHT ->
                    config.isAllowMovement() ? "Permitido" : "Movimiento general deshabilitado";
            default ->
                    config.isAllowMovement() ? "Permitido" : "Tipo de click no permitido";
        };
    }

    /**
     * NUEVO: Valida si una operación de arrastre está permitida
     */
    public static boolean isDragOperationAllowed(ItemStack draggedItem, Player player,
                                                 java.util.Set<Integer> affectedSlots) {
        if (draggedItem == null || draggedItem.getType().isAir()) {
            return true;
        }

        net.exylia.commons.item.InteractiveItem interactiveItem =
                net.exylia.commons.item.ItemManager.getItemFromStack(draggedItem);

        if (interactiveItem == null) {
            return true;
        }

        ItemConfiguration config = interactiveItem.getConfiguration();

        // Modo creativo siempre permite
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        // Si no permite movimiento básico, denegar arrastre
        if (!config.isAllowMovement()) {
            return false;
        }

        return true;
    }
}
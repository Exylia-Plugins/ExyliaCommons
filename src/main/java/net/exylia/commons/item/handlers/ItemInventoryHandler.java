package net.exylia.commons.item.handlers;

import net.exylia.commons.item.InteractiveItem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Manejador de operaciones de inventario para items interactivos
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
     * Verifica si un clic en inventario es para mover el item o para usarlo
     */
    public static boolean isMovementClick(InventoryClickEvent event,
                                          net.exylia.commons.item.config.ItemConfiguration config) {
        Player player = (Player) event.getWhoClicked();
        org.bukkit.event.inventory.ClickType click = event.getClick();

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
        if (click == org.bukkit.event.inventory.ClickType.LEFT ||
                click == org.bukkit.event.inventory.ClickType.RIGHT) {
            return isPickupOrPlaceClick(event, config);
        }

        return false;
    }

    /**
     * Determina si un LEFT/RIGHT click es para recoger/colocar items
     */
    private static boolean isPickupOrPlaceClick(InventoryClickEvent event,
                                                net.exylia.commons.item.config.ItemConfiguration config) {
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
}
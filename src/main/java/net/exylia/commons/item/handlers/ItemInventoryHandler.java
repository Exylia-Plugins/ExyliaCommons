package net.exylia.commons.item.handlers;

import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ItemInventoryHandler {

    public static void updateItemByEquipmentSlot(Player player, ItemStack itemStack,
                                                 InteractiveItem interactiveItem, EquipmentSlot hand) {
        if (interactiveItem.hasLimitedUses()) {
            if (itemStack.getAmount() > 1 && interactiveItem.isStackable()) {

                ItemStack currentHandItem = (hand == EquipmentSlot.HAND) ?
                        player.getInventory().getItemInMainHand() :
                        player.getInventory().getItemInOffHand();

                if (currentHandItem.getAmount() > 1) {
                    int newAmount = currentHandItem.getAmount() - 1;
                    currentHandItem.setAmount(newAmount);

                    switch (hand) {
                        case HAND -> player.getInventory().setItemInMainHand(currentHandItem);
                        case OFF_HAND -> player.getInventory().setItemInOffHand(currentHandItem);
                    }
                }

                ItemStack updatedStack = interactiveItem.getItemStack();
                updatedStack.setAmount(1);
                player.getInventory().addItem(updatedStack);
            } else {
                ItemStack updatedStack = interactiveItem.getItemStack();
                updatedStack.setAmount(itemStack.getAmount());

                switch (hand) {
                    case HAND -> {
                        player.getInventory().setItemInMainHand(updatedStack);
                    }
                    case OFF_HAND -> {
                        player.getInventory().setItemInOffHand(updatedStack);
                    }
                }
            }
        }
    }

    public static void removeOrReduceItemByEquipmentSlot(Player player, ItemStack itemStack, EquipmentSlot hand) {
        ItemStack actualItem = hand == EquipmentSlot.HAND ?
                player.getInventory().getItemInMainHand() :
                player.getInventory().getItemInOffHand();

        if (actualItem.getType().isAir()) {
            return;
        }
        if (actualItem.getAmount() > 1) {
            actualItem.setAmount(actualItem.getAmount() - 1);
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(actualItem);
            } else {
                player.getInventory().setItemInOffHand(actualItem);
            }

        } else {
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            } else {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
        }

        player.updateInventory();
    }

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

    public static void updateItemInInventory(InventoryClickEvent event, InteractiveItem interactiveItem) {
        if (interactiveItem.hasLimitedUses()) {
            ItemStack currentItem = event.getCurrentItem();
            if (currentItem == null) return;

            if (currentItem.getAmount() > 1 && interactiveItem.isStackable()) {
                currentItem.setAmount(currentItem.getAmount() - 1);
                event.setCurrentItem(currentItem);

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

    public static boolean isMovementClick(InventoryClickEvent event, ItemConfiguration config) {
        Player player = (Player) event.getWhoClicked();
        ClickType click = event.getClick();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        if (!config.isAllowMovement()) {
            return false;
        }

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

    private static boolean isPickupOrPlaceClick(InventoryClickEvent event, ItemConfiguration config) {
        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();

        if (cursor != null && !cursor.getType().isAir()) {
            return true;
        }

        if (clicked == null || clicked.getType().isAir()) {
            return true;
        }

        if (event.getClickedInventory() != player.getInventory()) {
            return true;
        }

        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        boolean allowed = config.isAllowMovement();
        if (!allowed) {
        } else {
        }

        return allowed;
    }

    public static boolean canItemBeMoved(ItemStack itemStack, ClickType clickType, Player player) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return true;
        }

        net.exylia.commons.item.InteractiveItem interactiveItem =
                net.exylia.commons.item.ItemManager.getItemFromStack(itemStack);

        if (interactiveItem == null) {
            return true;
        }

        ItemConfiguration config = interactiveItem.getConfiguration();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

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

        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        if (!config.isAllowMovement()) {
            return false;
        }

        return true;
    }
}

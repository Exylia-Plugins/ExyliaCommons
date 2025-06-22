package net.exylia.commons.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Sistema de gestión de menús interactivos para plugins de Exylia
 * Ahora con soporte para menús editables y actualización automática de placeholders
 */
public class MenuManager implements Listener {
    private static JavaPlugin plugin;
    private static final Map<UUID, Menu> openMenus = new HashMap<>();
    private static final Map<UUID, PaginationMenu> openPaginationMenus = new HashMap<>();
    private static final Map<UUID, EditableMenu> openEditableMenus = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Inicializa el sistema de menús
     * @param javaPlugin El plugin principal
     */
    public static void initialize(JavaPlugin javaPlugin) {
        if (initialized) return;
        plugin = javaPlugin;
        Bukkit.getPluginManager().registerEvents(new MenuManager(), plugin);
        initialized = true;
    }

    /**
     * Maneja los clics en inventarios
     * @param event Evento de clic en inventario
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Menu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        // Si es un menú editable, manejar lógica especial
        EditableMenu editableMenu = openEditableMenus.get(player.getUniqueId());
        if (editableMenu != null) {
            handleEditableMenuClick(event, player, editableMenu);
            return;
        }

        // Lógica normal para menús no editables
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        MenuItem item = menu.getItem(event.getSlot());
        if (item != null) {
            MenuClickInfo clickInfo = new MenuClickInfo(player, event.getClick(), event.getSlot(), menu, item);

            // 1. Ejecutar acción personalizada si existe usando el sistema global (prioridad alta)
            boolean actionExecuted = false;
            if (item.hasAction()) {
                actionExecuted = item.executeAction(clickInfo);
            }

            // 2. Ejecutar comandos si hay definidos (solo si no se ejecutó una acción o si la acción falló)
            if (!actionExecuted && !item.getCommands().isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    item.executeCommands(player);
                    // Actualizar item después de ejecutar comandos si usa placeholders
                    scheduleItemPlaceholderUpdate(menu, event.getSlot(), item, player);
                });
            } else if (actionExecuted) {
                // Si se ejecutó una acción, programar actualización de placeholders
                scheduleItemPlaceholderUpdate(menu, event.getSlot(), item, player);
            }

            // 3. Ejecutar el handler de clic si está definido (siempre se ejecuta)
            if (item.getClickHandler() != null) {
                try {
                    item.getClickHandler().accept(clickInfo);
                    scheduleItemPlaceholderUpdate(menu, event.getSlot(), item, player);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Programa la actualización de placeholders de un item después de una acción
     */
    private void scheduleItemPlaceholderUpdate(Menu menu, int slot, MenuItem item, Player player) {
        if (item.usesPlaceholders()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateItemPlaceholders(menu, slot, item, player);
            }, 1L);
        }
    }

    /**
     * Actualiza los placeholders de un item específico
     */
    private void updateItemPlaceholders(Menu menu, int slot, MenuItem item, Player player) {
        try {
            if (menu.getViewer() != player || !player.isOnline()) {
                return;
            }

            MenuItem updatedItem = item.clone();
            updatedItem.updatePlaceholders(player);

            if (menu instanceof PaginationMenu paginationMenu) {
                paginationMenu.updateCurrentPageItemInPlace(slot, updatedItem);
            } else if (menu instanceof EditableMenu editableMenu) {
                if (!editableMenu.isSlotEditable(slot)) {
                    menu.updateItemInPlace(slot, updatedItem);
                }
            } else {
                menu.updateItemInPlace(slot, updatedItem);
            }

        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    /**
     * Maneja clics específicos en menús editables
     */
    private void handleEditableMenuClick(InventoryClickEvent event, Player player, EditableMenu editableMenu) {
        int slot = event.getSlot();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();
        Inventory playerInventory = player.getInventory();

        // Si el clic es en el inventario del menú
        if (clickedInventory == topInventory) {
            // Si el slot es editable
            if (editableMenu.isSlotEditable(slot)) {
                handleEditableSlotClick(event, player, editableMenu, slot);
                return;
            }

            // Si no es editable, comportamiento normal de menú
            event.setCancelled(true);
            MenuItem item = editableMenu.getItem(slot);
            if (item != null) {
                MenuClickInfo clickInfo = new MenuClickInfo(player, event.getClick(), slot, editableMenu, item);

                boolean actionExecuted = false;
                if (item.hasAction()) {
                    actionExecuted = item.executeAction(clickInfo);
                }

                if (!actionExecuted && !item.getCommands().isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        item.executeCommands(player);
                        // Actualizar item después de ejecutar comandos si usa placeholders
                        scheduleItemPlaceholderUpdate(editableMenu, slot, item, player);
                    });
                } else if (actionExecuted) {
                    // Si se ejecutó una acción, programar actualización de placeholders
                    scheduleItemPlaceholderUpdate(editableMenu, slot, item, player);
                }

                if (item.getClickHandler() != null) {
                    try {
                        item.getClickHandler().accept(clickInfo);
                        // Actualizar item después del click handler si usa placeholders
                        scheduleItemPlaceholderUpdate(editableMenu, slot, item, player);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // Si el clic es en el inventario del jugador
        else if (clickedInventory == playerInventory) {
            // Manejar shift+click desde el inventario del jugador
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                handlePlayerInventoryShiftClick(event, player, editableMenu);
            }
            // Para otros tipos de clic en el inventario del jugador, permitir interacción normal
        }
        // Cualquier otro caso, cancelar por seguridad
        else {
            event.setCancelled(true);
        }
    }

    /**
     * Actualiza todos los items con placeholders de un menú específico
     * Útil para actualizaciones masivas después de cambios importantes
     */
    public static void refreshMenuPlaceholders(Player player) {
        Menu menu = openMenus.get(player.getUniqueId());
        if (menu == null || menu.getViewer() != player) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                // Obtener todos los items que usan placeholders
                Map<Integer, MenuItem> itemsToUpdate = new HashMap<>();

                for (Map.Entry<Integer, MenuItem> entry : menu.getItems().entrySet()) {
                    MenuItem item = entry.getValue();
                    if (item != null && item.usesPlaceholders()) {
                        MenuItem updatedItem = item.clone();
                        updatedItem.updatePlaceholders(player);
                        itemsToUpdate.put(entry.getKey(), updatedItem);
                    }
                }

                // Actualizar todos los items de una vez
                if (!itemsToUpdate.isEmpty()) {
                    menu.updateItemsInPlace(itemsToUpdate);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1L);
    }

    /**
     * Actualiza un item específico con placeholders por su slot
     * Útil para actualizaciones específicas desde acciones externas
     */
    public static void refreshItemPlaceholders(Player player, int slot) {
        Menu menu = openMenus.get(player.getUniqueId());
        if (menu == null || menu.getViewer() != player) {
            return;
        }

        MenuItem item = menu.getItem(slot);
        if (item != null && item.usesPlaceholders()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    MenuItem updatedItem = item.clone();
                    updatedItem.updatePlaceholders(player);

                    if (menu instanceof PaginationMenu paginationMenu) {
                        paginationMenu.updateCurrentPageItemInPlace(slot, updatedItem);
                    } else {
                        menu.updateItemInPlace(slot, updatedItem);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 1L);
        }
    }

    /**
     * Maneja clics específicos en slots editables
     */
    private void handleEditableSlotClick(InventoryClickEvent event, Player player, EditableMenu editableMenu, int slot) {
        ClickType clickType = event.getClick();
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        switch (clickType) {
            case LEFT:
                handleLeftClick(event, editableMenu, slot, cursor, current);
                break;
            case RIGHT:
                handleRightClick(event, editableMenu, slot, cursor, current);
                break;
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                handleShiftClick(event, player, editableMenu, slot);
                break;
            case MIDDLE:
                handleMiddleClick(event, editableMenu, slot);
                break;
            default:
                event.setCancelled(true);
                break;
        }
    }

    /**
     * Maneja click izquierdo en slot editable
     */
    private void handleLeftClick(InventoryClickEvent event, EditableMenu editableMenu, int slot, ItemStack cursor, ItemStack current) {
        event.setCancelled(true);

        if (cursor != null && !cursor.getType().isAir()) {
            // Colocar item del cursor en el slot
            editableMenu.setEditableItem(slot, cursor);
            event.getWhoClicked().setItemOnCursor(current != null && !current.getType().isAir() ? current : null);
        } else if (current != null && !current.getType().isAir()) {
            // Tomar item del slot al cursor
            editableMenu.setEditableItem(slot, null);
            event.getWhoClicked().setItemOnCursor(current);
        }
    }

    /**
     * Maneja click derecho en slot editable
     */
    private void handleRightClick(InventoryClickEvent event, EditableMenu editableMenu, int slot, ItemStack cursor, ItemStack current) {
        event.setCancelled(true);

        if (cursor != null && !cursor.getType().isAir()) {
            // Colocar solo 1 item del cursor
            ItemStack singleItem = cursor.clone();
            singleItem.setAmount(1);
            editableMenu.setEditableItem(slot, singleItem);

            // Reducir cursor
            if (cursor.getAmount() > 1) {
                cursor.setAmount(cursor.getAmount() - 1);
            } else {
                event.getWhoClicked().setItemOnCursor(null);
            }
        } else if (current != null && !current.getType().isAir()) {
            // Tomar la mitad del stack
            int halfAmount = (int) Math.ceil(current.getAmount() / 2.0);
            ItemStack toTake = current.clone();
            toTake.setAmount(halfAmount);

            if (current.getAmount() > halfAmount) {
                ItemStack remaining = current.clone();
                remaining.setAmount(current.getAmount() - halfAmount);
                editableMenu.setEditableItem(slot, remaining);
            } else {
                editableMenu.setEditableItem(slot, null);
            }

            event.getWhoClicked().setItemOnCursor(toTake);
        }
    }

    /**
     * Maneja shift+click desde el inventario del jugador hacia el menú editable
     */
    private void handlePlayerInventoryShiftClick(InventoryClickEvent event, Player player, EditableMenu editableMenu) {
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir()) {
            return; // No hay item para mover
        }

        // Buscar el primer slot editable disponible
        int targetSlot = findFirstAvailableEditableSlot(editableMenu);

        if (targetSlot == -1) {
            // No hay slots editables disponibles
            return;
        }

        // Cancelar el evento para manejarlo manualmente
        event.setCancelled(true);

        // Mover el item al slot editable
        editableMenu.setEditableItem(targetSlot, clickedItem.clone());

        // Remover el item del inventario del jugador
        event.setCurrentItem(null);
    }

    /**
     * Maneja shift+click en slot editable (mover del menú al inventario del jugador)
     */
    private void handleShiftClick(InventoryClickEvent event, Player player, EditableMenu editableMenu, int slot) {
        event.setCancelled(true);

        ItemStack current = event.getCurrentItem();
        if (current != null && !current.getType().isAir()) {
            // Mover item al inventario del jugador
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(current);

            if (leftover.isEmpty()) {
                // Se movió completamente
                editableMenu.setEditableItem(slot, null);
            } else {
                // Quedó algo que no se pudo mover
                ItemStack remaining = leftover.values().iterator().next();
                editableMenu.setEditableItem(slot, remaining);
            }
        }
    }

    /**
     * Encuentra el primer slot editable disponible en el menú
     */
    private int findFirstAvailableEditableSlot(EditableMenu editableMenu) {
        for (int slot : editableMenu.getEditableSlots()) {
            if (!editableMenu.hasEditableItem(slot)) {
                return slot;
            }
        }
        return -1; // No hay slots disponibles
    }

    /**
     * Maneja click del medio en slot editable (clear slot)
     */
    private void handleMiddleClick(InventoryClickEvent event, EditableMenu editableMenu, int slot) {
        event.setCancelled(true);
        editableMenu.setEditableItem(slot, null);
    }

    /**
     * Evita arrastrar items en menús normales y maneja arrastrar en menús editables
     * @param event Evento de arrastrar en inventario
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!openMenus.containsKey(player.getUniqueId())) return;

        EditableMenu editableMenu = openEditableMenus.get(player.getUniqueId());
        if (editableMenu != null) {
            handleEditableMenuDrag(event, player, editableMenu);
        } else {
            // Menú normal - cancelar arrastrado
            event.setCancelled(true);
        }
    }

    /**
     * Maneja arrastrar items en menús editables
     */
    private void handleEditableMenuDrag(InventoryDragEvent event, Player player, EditableMenu editableMenu) {
        Set<Integer> affectedSlots = event.getRawSlots();
        Inventory topInventory = event.getView().getTopInventory();
        int topSize = topInventory.getSize();

        // Verificar si el arrastrado afecta slots del menú
        boolean affectsMenuSlots = affectedSlots.stream().anyMatch(slot -> slot < topSize);

        if (affectsMenuSlots) {
            // Verificar si todos los slots afectados del menú son editables
            boolean allEditableOrValid = affectedSlots.stream()
                    .filter(slot -> slot < topSize) // Solo slots del menú
                    .allMatch(editableMenu::isSlotEditable);

            if (!allEditableOrValid) {
                // Hay slots no editables afectados - cancelar
                event.setCancelled(true);
            } else {
                // Permitir el arrastrado y actualizar el estado del menú editable
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Sincronizar el estado después del arrastrado
                    for (int slot : affectedSlots) {
                        if (slot < topSize && editableMenu.isSlotEditable(slot)) {
                            ItemStack item = topInventory.getItem(slot);
                            if (item != null && !item.getType().isAir()) {
                                editableMenu.setEditableItem(slot, item);
                            } else {
                                editableMenu.setEditableItem(slot, null);
                            }
                        }
                    }
                }, 1L);
            }
        }
        // Si no afecta slots del menú, permitir la operación normal
    }

    /**
     * Maneja el cierre de inventarios
     * @param event Evento de cierre de inventario
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Menu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        menu.onClose(player);

        // Limpiar referencias
        PaginationMenu paginationMenu = openPaginationMenus.remove(player.getUniqueId());
        EditableMenu editableMenu = openEditableMenus.remove(player.getUniqueId());
        openMenus.remove(player.getUniqueId());

        if (menu.getCloseHandler() != null) {
            menu.getCloseHandler().accept(player);
        }

        if (menu.getReturnMenu() != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> menu.getReturnMenu().open(player), 1L);
        }
    }

    /**
     * Registra un menú abierto
     * @param player Jugador que abrió el menú
     * @param menu Menú abierto
     */
    static void registerOpenMenu(Player player, Menu menu) {
        openMenus.put(player.getUniqueId(), menu);

        if (menu instanceof EditableMenu) {
            openEditableMenus.put(player.getUniqueId(), (EditableMenu) menu);
        } else if (menu instanceof PaginationMenu) {
            openPaginationMenus.put(player.getUniqueId(), (PaginationMenu) menu);
        }
    }

    /**
     * Registra un menú paginado abierto
     * @param player Jugador que abrió el menú
     * @param paginationMenu Menú paginado abierto
     */
    static void registerOpenPaginationMenu(Player player, PaginationMenu paginationMenu) {
        openPaginationMenus.put(player.getUniqueId(), paginationMenu);
    }

    /**
     * Registra un menú editable abierto
     * @param player Jugador que abrió el menú
     * @param editableMenu Menú editable abierto
     */
    static void registerOpenEditableMenu(Player player, EditableMenu editableMenu) {
        openEditableMenus.put(player.getUniqueId(), editableMenu);
    }

    /**
     * Obtiene el plugin asociado
     * @return Plugin principal
     */
    public static JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Obtiene el menú editable abierto por un jugador
     * @param player Jugador
     * @return Menú editable o null si no tiene uno abierto
     */
    public static EditableMenu getOpenEditableMenu(Player player) {
        return openEditableMenus.get(player.getUniqueId());
    }

    /**
     * Verifica si un jugador tiene un menú editable abierto
     * @param player Jugador
     * @return true si tiene un menú editable abierto
     */
    public static boolean hasEditableMenuOpen(Player player) {
        return openEditableMenus.containsKey(player.getUniqueId());
    }

    /**
     * Obtiene el menú actualmente abierto por un jugador
     * @param player Jugador
     * @return Menú abierto o null si no tiene uno abierto
     */
    public static Menu getOpenMenu(Player player) {
        return openMenus.get(player.getUniqueId());
    }
}
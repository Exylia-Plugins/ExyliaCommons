package net.exylia.commons.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Menú que permite editar items arrastrándolos desde el inventario del jugador
 */
public class EditableMenu extends Menu {

    private final Set<Integer> editableSlots = new HashSet<>();
    private final Map<Integer, ItemStack> editableItems = new HashMap<>();

    // Callbacks para manejar cambios
    private BiConsumer<Integer, ItemStack> onItemPlaced;
    private BiConsumer<Integer, ItemStack> onItemRemoved;
    private Consumer<Map<Integer, ItemStack>> onItemsChanged;
    private Consumer<Player> externalCloseHandler;

    public EditableMenu(String title, int rows) {
        super(title, rows);
        super.setCloseHandler(this::onPlayerCloseMenu);
    }

    // ==================== CONFIGURACIÓN DE SLOTS EDITABLES ====================

    /**
     * Marca un slot como editable
     * @param slot Slot a marcar como editable
     * @return El mismo menú para encadenamiento
     */
    public EditableMenu addEditableSlot(int slot) {
        if (slot >= 0 && slot < size) {
            editableSlots.add(slot);
        }
        return this;
    }

    /**
     * Marca múltiples slots como editables
     * @param slots Slots a marcar como editables
     * @return El mismo menú para encadenamiento
     */
    public EditableMenu addEditableSlots(int... slots) {
        for (int slot : slots) {
            addEditableSlot(slot);
        }
        return this;
    }

    /**
     * Marca un rango de slots como editables
     * @param start Slot inicial (inclusivo)
     * @param end Slot final (inclusivo)
     * @return El mismo menú para encadenamiento
     */
    public EditableMenu addEditableSlotRange(int start, int end) {
        for (int i = start; i <= end && i < size; i++) {
            addEditableSlot(i);
        }
        return this;
    }

    /**
     * Elimina un slot de la lista de editables
     * @param slot Slot a eliminar
     * @return El mismo menú para encadenamiento
     */
    public EditableMenu removeEditableSlot(int slot) {
        editableSlots.remove(slot);
        editableItems.remove(slot);
        return this;
    }

    /**
     * Limpia todos los slots editables
     * @return El mismo menú para encadenamiento
     */
    public EditableMenu clearEditableSlots() {
        editableSlots.clear();
        editableItems.clear();
        return this;
    }

    // ==================== CONFIGURACIÓN DE CALLBACKS ====================

    /**
     * Establece el callback para cuando se coloca un item
     * @param callback Función que recibe (slot, item)
     * @return El mismo menú para encadenamiento
     */
    public EditableMenu setOnItemPlaced(BiConsumer<Integer, ItemStack> callback) {
        this.onItemPlaced = callback;
        return this;
    }

    /**
     * Establece el callback para cuando se remueve un item
     * @param callback Función que recibe (slot, item removido)
     * @return El mismo menú para encadenamiento
     */
    public EditableMenu setOnItemRemoved(BiConsumer<Integer, ItemStack> callback) {
        this.onItemRemoved = callback;
        return this;
    }

    /**
     * Establece el callback para cuando cambian los items (se ejecuta después de cualquier cambio)
     * @param callback Función que recibe el mapa completo de items editables
     * @return El mismo menú para encadenamiento
     */
    public EditableMenu setOnItemsChanged(Consumer<Map<Integer, ItemStack>> callback) {
        this.onItemsChanged = callback;
        return this;
    }

    // ==================== GESTIÓN DE ITEMS EDITABLES ====================

    /**
     * Establece un item en un slot editable
     * @param slot Slot donde colocar el item
     * @param item Item a colocar (null para remover)
     * @return true si se pudo establecer, false si el slot no es editable
     */
    public boolean setEditableItem(int slot, ItemStack item) {
        if (!editableSlots.contains(slot)) {
            return false;
        }

        ItemStack oldItem = editableItems.get(slot);

        if (item == null || item.getType().isAir()) {
            editableItems.remove(slot);
            // Colocar filler si existe
            if (globalFiller != null) {
                MenuItem filler = globalFiller.clone();
                if (viewer != null && filler.usesPlaceholders()) {
                    filler.updatePlaceholders(viewer);
                }
                super.items.put(slot, filler);
            } else {
                super.items.remove(slot);
            }

            if (onItemRemoved != null && oldItem != null) {
                onItemRemoved.accept(slot, oldItem);
            }
        } else {
            editableItems.put(slot, item.clone());

            // Crear MenuItem desde el ItemStack
            MenuItem menuItem = new MenuItem(item.clone());
            menuItem.hideAllAttributes();
            super.items.put(slot, menuItem);

            if (onItemPlaced != null) {
                onItemPlaced.accept(slot, item.clone());
            }
        }

        // Actualizar inventario si está abierto
        if (inventory != null && viewer != null) {
            MenuItem displayItem = super.items.get(slot);
            inventory.setItem(slot, displayItem != null ? displayItem.getItemStack() : null);
        }

        // Notificar cambios generales
        if (onItemsChanged != null) {
            onItemsChanged.accept(new HashMap<>(editableItems));
        }

        return true;
    }

    /**
     * Obtiene el item en un slot editable
     * @param slot Slot a consultar
     * @return Item en el slot o null si está vacío o no es editable
     */
    public ItemStack getEditableItem(int slot) {
        ItemStack item = editableItems.get(slot);
        return item != null ? item.clone() : null;
    }

    /**
     * Obtiene todos los items editables
     * @return Mapa de slot -> item (copia defensiva)
     */
    public Map<Integer, ItemStack> getEditableItems() {
        Map<Integer, ItemStack> result = new HashMap<>();
        editableItems.forEach((slot, item) -> result.put(slot, item.clone()));
        return result;
    }

    /**
     * Establece múltiples items editables de una vez
     * @param items Mapa de slot -> item
     */
    public void setEditableItems(Map<Integer, ItemStack> items) {
        // Limpiar items editables actuales
        for (int slot : editableSlots) {
            setEditableItem(slot, null);
        }

        // Establecer nuevos items
        items.forEach(this::setEditableItem);
    }

    /**
     * Limpia todos los items editables
     */
    public void clearEditableItems() {
        for (int slot : new HashSet<>(editableItems.keySet())) {
            setEditableItem(slot, null);
        }
    }

    // ==================== VERIFICACIONES ====================

    /**
     * Verifica si un slot es editable
     * @param slot Slot a verificar
     * @return true si el slot es editable
     */
    public boolean isSlotEditable(int slot) {
        return editableSlots.contains(slot);
    }

    /**
     * Verifica si un slot editable tiene un item
     * @param slot Slot a verificar
     * @return true si tiene un item
     */
    public boolean hasEditableItem(int slot) {
        return editableItems.containsKey(slot);
    }

    /**
     * Obtiene la cantidad total de items editables colocados
     * @return Número de items editables
     */
    public int getEditableItemCount() {
        return editableItems.size();
    }

    /**
     * Obtiene todos los slots editables
     * @return Set de slots editables (copia defensiva)
     */
    public Set<Integer> getEditableSlots() {
        return new HashSet<>(editableSlots);
    }

    // ==================== OVERRIDE PARA APERTURA ====================

    @Override
    public void open(Player player) {
        // Aplicar items editables actuales antes de abrir
        for (Map.Entry<Integer, ItemStack> entry : editableItems.entrySet()) {
            MenuItem menuItem = new MenuItem(entry.getValue().clone());
            menuItem.hideAllAttributes();
            super.items.put(entry.getKey(), menuItem);
        }

        super.open(player);
    }

    @Override
    protected void applyFillers(Player player) {
        if (globalFiller != null) {
            for (int i = 0; i < size; i++) {
                if (!editableSlots.contains(i) && !items.containsKey(i)) {
                    MenuItem filler = globalFiller.clone();
                    if (filler.usesPlaceholders()) {
                        filler.updatePlaceholders(player);
                    }
                    items.put(i, filler);
                }
            }
        }

        if (borderFiller != null) {
            applyBorderExcludingEditableSlots(player);
        }
    }

    private void applyBorderExcludingEditableSlots(Player player) {
        int rows = size / 9;

        for (int i = 0; i < 9; i++) {
            setBorderItemIfNotEditable(i, player);
            setBorderItemIfNotEditable(size - 9 + i, player);
        }

        for (int i = 1; i < rows - 1; i++) {
            setBorderItemIfNotEditable(i * 9, player);
            setBorderItemIfNotEditable(i * 9 + 8, player);
        }
    }

    private void setBorderItemIfNotEditable(int slot, Player player) {
        if (!editableSlots.contains(slot)) {
            MenuItem border = borderFiller.clone();
            if (border.usesPlaceholders()) {
                border.updatePlaceholders(player);
            }
            items.put(slot, border);
        }
    }

    // ==================== LIMPIEZA ====================

    private void onPlayerCloseMenu(Player player) {
        if (externalCloseHandler != null) {
            externalCloseHandler.accept(player);
        }
    }

    // ==================== OVERRIDE MÉTODOS DE ENCADENAMIENTO ====================

    @Override
    public EditableMenu setCloseHandler(Consumer<Player> closeHandler) {
        this.externalCloseHandler = closeHandler;
        super.setCloseHandler(this::onPlayerCloseMenu);
        return this;
    }

    @Override
    public EditableMenu setReturnMenu(Menu returnMenu) {
        super.setReturnMenu(returnMenu);
        return this;
    }

    @Override
    public EditableMenu setGlobalFiller(MenuItem filler) {
        super.setGlobalFiller(filler);
        return this;
    }

    @Override
    public EditableMenu setBorderFiller(MenuItem borderItem) {
        super.setBorderFiller(borderItem);
        return this;
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Convierte el menú editable a un array de ItemStack estándar de Minecraft (36 slots)
     * Útil para aplicar directamente al inventario de un jugador
     * @return Array de 36 ItemStacks
     */
    public ItemStack[] toPlayerInventoryArray() {
        ItemStack[] result = new ItemStack[36];

        for (Map.Entry<Integer, ItemStack> entry : editableItems.entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < 36) {
                result[slot] = entry.getValue().clone();
            }
        }

        return result;
    }

    /**
     * Carga items desde un array de ItemStack estándar
     * @param items Array de ItemStacks (típicamente de un inventario de jugador)
     */
    public void loadFromPlayerInventoryArray(ItemStack[] items) {
        clearEditableItems();

        for (int i = 0; i < items.length && i < 36; i++) {
            ItemStack item = items[i];
            if (item != null && !item.getType().isAir() && editableSlots.contains(i)) {
                setEditableItem(i, item);
            }
        }
    }
}
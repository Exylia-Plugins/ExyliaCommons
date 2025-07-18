package net.exylia.commons.utils.versions;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;

/**
 * Interfaz para la creación de inventarios compatible con diferentes versiones
 */
public interface InventoryAdapter {
    /**
     * Crea un inventario con un componente Adventure como título
     * @param size Tamaño del inventario
     * @param title Título del inventario como componente Adventure
     * @return Inventario creado
     */
    Inventory createInventory(int size, Component title);
}
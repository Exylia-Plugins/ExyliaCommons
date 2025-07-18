package net.exylia.commons.utils.versions;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Interfaz para la manipulación de metadatos de ítems que sea compatible con diferentes versiones
 */
public interface ItemMetaAdapter {
    /**
     * Establece el nombre de visualización de un ItemMeta usando un componente Adventure
     * @param meta ItemMeta a modificar
     * @param name Componente de Adventure para el nombre
     */
    void setDisplayName(ItemMeta meta, Component name);

    /**
     * Establece el lore de un ItemMeta usando componentes Adventure
     * @param meta ItemMeta a modificar
     * @param lore Lista de componentes Adventure para el lore
     */
    void setLore(ItemMeta meta, List<Component> lore);

    /**
     * Obtiene el nombre de visualización de un ItemMeta como componente Adventure
     * @param meta ItemMeta del cual obtener el nombre
     * @return Componente Adventure del nombre o null si no tiene
     */
    Component getDisplayName(ItemMeta meta);

    /**
     * Obtiene el lore de un ItemMeta como lista de componentes Adventure
     * @param meta ItemMeta del cual obtener el lore
     * @return Lista de componentes Adventure del lore o null si no tiene
     */
    List<Component> getLore(ItemMeta meta);
}
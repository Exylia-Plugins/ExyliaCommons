package net.exylia.commons.item;

import lombok.Getter;
import net.exylia.commons.actions.ActionSource;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase para manejar información de clics en items
 * ACTUALIZADO: Soporte para datos adicionales en el contexto
 */
@Getter
public class ItemClickInfo {

    private final Player player;
    private final ClickType clickType;
    private final int slot;
    private final ItemStack itemStack;
    private final ActionSource source;
    private final Map<String, Object> data;

    public ItemClickInfo(Player player, ClickType clickType, int slot, ItemStack itemStack, ActionSource source) {
        this.player = player;
        this.clickType = clickType;
        this.slot = slot;
        this.itemStack = itemStack;
        this.source = source;
        this.data = new HashMap<>();
    }

    /**
     * Añade un dato al contexto de la interacción
     */
    public ItemClickInfo withData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * Obtiene un dato del contexto
     */
    public Object getData(String key) {
        return data.get(key);
    }

    /**
     * Verifica si existe un dato en el contexto
     */
    public boolean hasData(String key) {
        return data.containsKey(key);
    }

    /**
     * Obtiene todos los datos del contexto
     */
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }
}
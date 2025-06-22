package net.exylia.commons.actions;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.HashMap;
import java.util.Map;

/**
 * Contexto de una acción que proporciona información sobre dónde y cómo se ejecutó
 */
public class ActionContext {
    private final Player player;
    private final ActionSource source;
    private final Map<String, Object> data;

    public ActionContext(Player player, ActionSource source) {
        this.player = player;
        this.source = source;
        this.data = new HashMap<>();
    }

    /**
     * Obtiene el jugador que ejecutó la acción
     * @return Jugador
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Obtiene la fuente de la acción
     * @return Fuente de la acción
     */
    public ActionSource getSource() {
        return source;
    }

    /**
     * Añade datos adicionales al contexto
     * @param key Clave del dato
     * @param value Valor del dato
     * @return El mismo contexto (para encadenamiento)
     */
    public ActionContext withData(String key, Object value) {
        data.put(key, value);
        return this;
    }

    /**
     * Obtiene un dato del contexto
     * @param key Clave del dato
     * @param type Tipo esperado del dato
     * @param <T> Tipo del dato
     * @return Dato o null si no existe o no es del tipo esperado
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Verifica si existe un dato en el contexto
     * @param key Clave del dato
     * @return true si existe el dato
     */
    public boolean hasData(String key) {
        return data.containsKey(key);
    }

    /**
     * Obtiene el tipo de clic si la acción proviene de un clic
     * @return Tipo de clic o null si no aplica
     */
    public ClickType getClickType() {
        return getData("clickType", ClickType.class);
    }

    /**
     * Obtiene el slot si la acción proviene de un inventario
     * @return Slot o -1 si no aplica
     */
    public int getSlot() {
        Integer slot = getData("slot", Integer.class);
        return slot != null ? slot : -1;
    }

    public boolean isLeftClick() {
        ClickType clickType = getClickType();
        return clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT;
    }

    public boolean isRightClick() {
        ClickType clickType = getClickType();
        return clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT;
    }

    public boolean isShiftClick() {
        ClickType clickType = getClickType();
        return clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT;
    }
}
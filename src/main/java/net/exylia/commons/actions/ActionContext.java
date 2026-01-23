package net.exylia.commons.actions;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.HashMap;
import java.util.Map;

@Deprecated
public class ActionContext {
    private final Player player;
    private final ActionSource source;
    private final Map<String, Object> data;

    public ActionContext(Player player, ActionSource source) {
        this.player = player;
        this.source = source;
        this.data = new HashMap<>();
    }

    public Player getPlayer() {
        return player;
    }

    public ActionSource getSource() {
        return source;
    }

    public ActionContext withData(String key, Object value) {
        data.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public boolean hasData(String key) {
        return data.containsKey(key);
    }

    public ClickType getClickType() {
        return getData("clickType", ClickType.class);
    }

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

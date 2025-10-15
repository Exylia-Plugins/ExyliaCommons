package net.exylia.commons.item;

import lombok.Getter;
import net.exylia.commons.actions.ActionSource;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ItemClickInfo {

    private final Player player;
    private final ClickType clickType;
    private final int slot;
    private final ItemStack itemStack;
    private final ActionSource source;
    private final Location location;
    private final Map<String, Object> data;

    public ItemClickInfo(Player player, ClickType clickType, int slot, ItemStack itemStack, ActionSource source) {
        this(player, clickType, slot, itemStack, source, null);
    }

    public ItemClickInfo(Player player, ClickType clickType, int slot, ItemStack itemStack, ActionSource source, Location location) {
        this.player = player;
        this.clickType = clickType;
        this.slot = slot;
        this.itemStack = itemStack;
        this.source = source;
        this.location = location;
        this.data = new HashMap<>();
    }

    public ItemClickInfo withData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public Object getData(String key) {
        return data.get(key);
    }

    public boolean hasData(String key) {
        return data.containsKey(key);
    }

    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }
}

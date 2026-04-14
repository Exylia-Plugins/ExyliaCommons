package net.exylia.commons.v2.compat;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class InventoryViewCompat {

    private static final Map<String, Method> methodCache = new ConcurrentHashMap<>();

    private InventoryViewCompat() {}

    public static Inventory getTopInventory(Player player) {
        try {
            Object view = player.getOpenInventory();
            if (view == null) return null;
            Method method = methodCache.computeIfAbsent(view.getClass().getName() + "#getTopInventory", k -> {
                try {
                    return view.getClass().getMethod("getTopInventory");
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });
            if (method == null) return null;
            return (Inventory) method.invoke(view);
        } catch (Exception e) {
            return null;
        }
    }

    public static InventoryType getViewType(InventoryClickEvent event) {
        try {
            Object view = event.getView();
            Method method = methodCache.computeIfAbsent(view.getClass().getName() + "#getType", k -> {
                try {
                    return view.getClass().getMethod("getType");
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });
            if (method == null) return null;
            return (InventoryType) method.invoke(view);
        } catch (Exception e) {
            return null;
        }
    }
}

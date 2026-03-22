package net.exylia.commons.v2.compat;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class InventoryViewCompat {

    private static final Map<Class<?>, Method> methodCache = new ConcurrentHashMap<>();

    private InventoryViewCompat() {}

    public static Inventory getTopInventory(Player player) {
        try {
            Object view = player.getOpenInventory();
            if (view == null) return null;
            Method method = methodCache.computeIfAbsent(view.getClass(), cls -> {
                try {
                    return cls.getMethod("getTopInventory");
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
}

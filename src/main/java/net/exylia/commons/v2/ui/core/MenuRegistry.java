package net.exylia.commons.v2.ui.core;

import net.exylia.commons.v2.ui.menu.MenuBase;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MenuRegistry {

    private final Map<UUID, MenuBase> activeMenus;

    public MenuRegistry() {
        this.activeMenus = new ConcurrentHashMap<>();
    }

    public void register(UUID playerId, MenuBase menu) {
        MenuBase existingMenu = activeMenus.get(playerId);
        if (existingMenu != null) {
            existingMenu.close();
        }

        activeMenus.put(playerId, menu);
    }

    public Optional<MenuBase> get(UUID playerId) {
        return Optional.ofNullable(activeMenus.get(playerId));
    }

    public void unregister(UUID playerId) {
        activeMenus.remove(playerId);
    }

    public void closeAll() {
        List<MenuBase> menus = new ArrayList<>(activeMenus.values());
        menus.forEach(MenuBase::close);
        activeMenus.clear();
    }

    public boolean hasActiveMenu(UUID playerId) {
        return activeMenus.containsKey(playerId);
    }

    public int getActiveMenuCount() {
        return activeMenus.size();
    }

    public Collection<MenuBase> getAllMenus() {
        return Collections.unmodifiableCollection(activeMenus.values());
    }
}

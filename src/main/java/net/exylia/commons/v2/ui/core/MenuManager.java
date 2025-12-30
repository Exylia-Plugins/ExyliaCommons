package net.exylia.commons.v2.ui.core;

import lombok.Getter;
import net.exylia.commons.v2.ui.event.MenuClickHandler;
import net.exylia.commons.v2.ui.event.MenuCloseHandler;
import net.exylia.commons.v2.ui.menu.MenuBase;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.navigation.NavigationManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter
public class MenuManager {

    private static MenuManager instance;
    private static boolean initialized = false;

    private JavaPlugin plugin;
    private final MenuRegistry registry;
    private final MenuFactory factory;
    private final NavigationManager navigationManager;

    private MenuClickHandler clickHandler;
    private MenuCloseHandler closeHandler;

    private MenuManager() {
        this.registry = new MenuRegistry();
        this.factory = new MenuFactory();
        this.navigationManager = new NavigationManager();
    }

    public static synchronized void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new MenuManager();
            instance.plugin = plugin;
            instance.registerListeners();
            instance.registerActions();
            initialized = true;
        }
    }

    public static MenuManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MenuManager not initialized. Call MenuManager.initialize(plugin) first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    private void registerListeners() {
        this.clickHandler = new MenuClickHandler();
        this.closeHandler = new MenuCloseHandler();

        plugin.getServer().getPluginManager().registerEvents(clickHandler, plugin);
        plugin.getServer().getPluginManager().registerEvents(closeHandler, plugin);
    }

    private void registerActions() {
        MenuActionRegistrar.registerMenuActions(plugin);
    }

    public CompletableFuture<Void> openMenuAsync(Player player, MenuData menuData) {
        Optional<MenuBase> currentMenu = registry.get(player.getUniqueId());
        currentMenu.ifPresent(menu -> navigationManager.push(player, menu.getMenuData()));

        return CompletableFuture.supplyAsync(() -> {
            MenuBase menu = factory.create(player, menuData);
            registry.register(player.getUniqueId(), menu);
            return menu;
        }).thenCompose(MenuBase::openAsync);
    }

    public void openMenu(Player player, MenuData menuData) {
        openMenuAsync(player, menuData).join();
    }

    public void closeMenu(Player player) {
        Optional<MenuBase> menu = registry.get(player.getUniqueId());
        menu.ifPresent(m -> {
            m.close();
            registry.unregister(player.getUniqueId());
        });
    }

    public boolean navigateBack(Player player) {
        Optional<MenuData> previousMenuData = navigationManager.pop(player);

        if (previousMenuData.isPresent()) {
            closeMenu(player);
            openMenu(player, previousMenuData.get());
            return true;
        }

        return false;
    }

    public void clearHistory(Player player) {
        navigationManager.clear(player);
    }

    public Optional<MenuBase> getActiveMenu(Player player) {
        return registry.get(player.getUniqueId());
    }

    public void shutdown() {
        registry.closeAll();
        navigationManager.clearAll();
    }
}

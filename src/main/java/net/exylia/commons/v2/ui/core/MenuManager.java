package net.exylia.commons.v2.ui.core;

import lombok.Getter;
import net.exylia.commons.v2.ui.event.MenuClickHandler;
import net.exylia.commons.v2.ui.event.MenuCloseHandler;
import net.exylia.commons.v2.ui.menu.MenuBase;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.navigation.NavigationManager;
import net.exylia.commons.v2.ui.packet.ContainerIdTracker;
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

        registerPacketListener();
    }

    private void registerPacketListener() {
        try {
            Class<?> packetEventsClass = Class.forName("com.github.retrooper.packetevents.PacketEvents");
            Class<?> listenerClass = Class.forName("net.exylia.commons.v2.ui.packet.InventoryPacketListener");

            Object packetEventsApi = packetEventsClass.getMethod("getAPI").invoke(null);
            Object eventManager = packetEventsApi.getClass().getMethod("getEventManager").invoke(packetEventsApi);

            Object listener = listenerClass.getDeclaredConstructor().newInstance();
            eventManager.getClass().getMethod("registerListener", Class.forName("com.github.retrooper.packetevents.event.PacketListenerCommon"))
                    .invoke(eventManager, listener);
        } catch (Exception ignored) {
        }
    }

    private void registerActions() {
        MenuActionRegistrar.registerMenuActions(plugin);
    }

    public CompletableFuture<Void> openMenuAsync(Player player, MenuData menuData) {
        Optional<MenuBase> currentMenu = registry.get(player.getUniqueId());
        currentMenu.ifPresent(menu -> {
            navigationManager.push(player, menu.getMenuData());
            menu.prepareTransition();
        });

        return CompletableFuture.supplyAsync(() -> {
            MenuBase menu = factory.create(player, menuData);
            registry.register(player.getUniqueId(), menu);
            return menu;
        }).thenCompose(MenuBase::openAsync);
    }

    public void openMenu(Player player, MenuData menuData) {
        Optional<MenuBase> currentMenu = registry.get(player.getUniqueId());
        currentMenu.ifPresent(menu -> {
            navigationManager.push(player, menu.getMenuData());
            menu.prepareTransition();
        });

        MenuBase menu = factory.create(player, menuData);
        registry.register(player.getUniqueId(), menu);
        menu.open();
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
            Optional<MenuBase> currentMenu = registry.get(player.getUniqueId());
            currentMenu.ifPresent(MenuBase::prepareTransition);

            MenuBase menu = factory.create(player, previousMenuData.get());
            registry.register(player.getUniqueId(), menu);
            menu.open();
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

    public boolean refreshActiveMenu(Player player) {
        Optional<MenuBase> menu = registry.get(player.getUniqueId());
        if (menu.isPresent() && menu.get().isOpen()) {
            if (menu.get() instanceof net.exylia.commons.v2.ui.menu.MultiPaginationMenu multiMenu) {
                multiMenu.refresh();
                return true;
            }
        }
        return false;
    }

    public void shutdown() {
        registry.closeAll();
        navigationManager.clearAll();
        ContainerIdTracker.clear();
    }
}

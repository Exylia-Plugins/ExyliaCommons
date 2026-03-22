package net.exylia.commons.v2.ui.core;

import lombok.Getter;
import net.exylia.commons.v2.command.api.CommandAPI;
import net.exylia.commons.v2.skull.api.SkullAPI;
import net.exylia.commons.v2.ui.event.MenuClickHandler;
import net.exylia.commons.v2.ui.event.MenuCloseHandler;
import net.exylia.commons.v2.ui.menu.ItemInputMenu;
import net.exylia.commons.v2.ui.menu.MenuBase;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.navigation.NavigationManager;
import net.exylia.commons.v2.ui.packet.ContainerIdTracker;
import net.exylia.commons.v2.ui.packet.InventoryPacketListener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Getter
public class MenuManager {

    private static MenuManager instance;
    @Getter
    private static boolean initialized = false;

    private JavaPlugin plugin;
    private final MenuRegistry registry;
    private final MenuFactory factory;
    private final NavigationManager navigationManager;
    private final java.util.Set<java.util.UUID> openingPlayers;

    private MenuClickHandler clickHandler;
    private MenuCloseHandler closeHandler;

    private MenuManager() {
        this.registry = new MenuRegistry();
        this.factory = new MenuFactory();
        this.navigationManager = new NavigationManager();
        this.openingPlayers = ConcurrentHashMap.newKeySet();
    }

    public static synchronized void initialize(JavaPlugin plugin) {
        if (instance == null) {
            if (!CommandAPI.isInitialized()) {
                CommandAPI.initialize(plugin);
            }
            if (!SkullAPI.isInitialized()) {
                SkullAPI.initialize(plugin);
            }
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

            Object packetEventsApi = packetEventsClass.getMethod("getAPI").invoke(null);
            Object eventManager = packetEventsApi.getClass().getMethod("getEventManager").invoke(packetEventsApi);

            InventoryPacketListener listener = new InventoryPacketListener();

            java.lang.reflect.Method registerMethod = null;
            for (java.lang.reflect.Method m : eventManager.getClass().getMethods()) {
                if ("registerListener".equals(m.getName()) && m.getParameterCount() == 1) {
                    if (m.getParameterTypes()[0].isAssignableFrom(listener.getClass())) {
                        registerMethod = m;
                        break;
                    }
                }
            }

            if (registerMethod != null) {
                registerMethod.invoke(eventManager, listener);
                net.exylia.commons.v2.debug.api.DebugAPI.logLibDebug(
                        net.exylia.commons.v2.debug.core.DebugCategory.UI,
                        "PacketEvents InventoryPacketListener registered successfully"
                );
            } else {
                net.exylia.commons.v2.debug.api.DebugAPI.logLibWarn(
                        net.exylia.commons.v2.debug.core.DebugCategory.UI,
                        "Could not find registerListener method on PacketEvents EventManager — title updates will not work"
                );
            }
        } catch (Exception e) {
            net.exylia.commons.v2.debug.api.DebugAPI.logLibWarn(
                    net.exylia.commons.v2.debug.core.DebugCategory.UI,
                    "Failed to register PacketEvents listener: " + e.getMessage()
            );
        }
    }

    private void registerActions() {
        MenuActionRegistrar.registerMenuActions(plugin);
    }

    public CompletableFuture<Void> openMenuAsync(Player player, MenuData menuData) {
        java.util.UUID playerId = player.getUniqueId();
        if (!openingPlayers.add(playerId)) {
            return CompletableFuture.completedFuture(null);
        }

        Optional<MenuBase> currentMenu = registry.get(player.getUniqueId());
        currentMenu.ifPresent(menu -> {
            navigationManager.push(player, menu.getMenuData());
            menu.prepareTransition();
        });

        registry.unregister(player.getUniqueId());

        try {
            MenuBase menu = factory.create(player, menuData);
            registry.register(player.getUniqueId(), menu);
            return menu.openAsync().whenComplete((unused, throwable) -> {
                if (throwable != null || !menu.isOpen()) {
                    registry.unregister(player.getUniqueId());
                }
                openingPlayers.remove(playerId);
            });
        } catch (Exception e) {
            openingPlayers.remove(playerId);
            return CompletableFuture.failedFuture(e);
        }
    }

    public void openMenu(Player player, MenuData menuData) {
        openMenuAsync(player, menuData);
    }

    public ItemInputMenu openItemInputMenu(Player player, MenuData menuData, Consumer<Map<Integer, ItemStack>> onClose) {
        java.util.UUID playerId = player.getUniqueId();
        if (!openingPlayers.add(playerId)) {
            Optional<MenuBase> current = registry.get(playerId);
            if (current.isPresent() && current.get() instanceof ItemInputMenu inputMenu) {
                return inputMenu;
            }
            return null;
        }

        Optional<MenuBase> currentMenu = registry.get(player.getUniqueId());
        currentMenu.ifPresent(menu -> {
            navigationManager.push(player, menu.getMenuData());
            menu.prepareTransition();
        });

        registry.unregister(player.getUniqueId());

        ItemInputMenu menu = new ItemInputMenu(player, menuData, onClose);
        registry.register(player.getUniqueId(), menu);
        menu.openAsync().whenComplete((unused, throwable) -> {
            if (throwable != null || !menu.isOpen()) {
                registry.unregister(player.getUniqueId());
            }
            openingPlayers.remove(playerId);
        });
        return menu;
    }

    public void closeMenu(Player player) {
        Optional<MenuBase> menu = registry.get(player.getUniqueId());
        menu.ifPresent(m -> {
            m.close();
            registry.unregister(player.getUniqueId());
            openingPlayers.remove(player.getUniqueId());
        });
    }

    public boolean navigateBack(Player player) {
        java.util.UUID playerId = player.getUniqueId();
        if (!openingPlayers.add(playerId)) {
            return false;
        }

        Optional<MenuData> previousMenuData = navigationManager.pop(player);

        if (previousMenuData.isPresent()) {
            Optional<MenuBase> currentMenu = registry.get(player.getUniqueId());
            currentMenu.ifPresent(MenuBase::prepareTransition);

            registry.unregister(player.getUniqueId());

            MenuBase menu = factory.create(player, previousMenuData.get());
            registry.register(player.getUniqueId(), menu);
            menu.openAsync().whenComplete((unused, throwable) -> {
                if (throwable != null || !menu.isOpen()) {
                    registry.unregister(player.getUniqueId());
                }
                openingPlayers.remove(playerId);
            });
            return true;
        }

        openingPlayers.remove(playerId);

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
        openingPlayers.clear();
    }
}

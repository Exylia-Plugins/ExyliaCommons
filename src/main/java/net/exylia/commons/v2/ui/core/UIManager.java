package net.exylia.commons.v2.ui.core;

import lombok.Getter;
import net.exylia.commons.v2.action.core.ActionManager;
import net.exylia.commons.v2.reload.api.ReloadContext;
import net.exylia.commons.v2.reload.api.ReloadableSystem;
import net.exylia.commons.v2.reload.core.ReloadPriority;
import net.exylia.commons.v2.reload.stats.SystemReloadMetrics;
import net.exylia.commons.v2.ui.cache.MenuCacheManager;
import net.exylia.commons.v2.ui.cache.UICacheStats;
import net.exylia.commons.v2.ui.config.UIConfigLoader;
import net.exylia.commons.v2.ui.exception.MenuException;
import net.exylia.commons.v2.ui.listener.*;
import net.exylia.commons.v2.ui.model.MenuContext;
import net.exylia.commons.v2.ui.model.MenuV2;
import net.exylia.commons.v2.ui.navigation.NavigationManager;
import net.exylia.commons.v2.ui.sound.MenuSoundManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

@Getter
public class UIManager implements ReloadableSystem {
    private static volatile UIManager instance;
    private static final Object LOCK = new Object();

    private final JavaPlugin plugin;
    private final MenuRegistry registry;
    private final MenuFactory factory;
    private final MenuExecutor executor;
    private final MenuCacheManager cacheManager;
    private final NavigationManager navigationManager;
    private final MenuSoundManager soundManager;
    private final UIConfigLoader configLoader;
    private final UIActions uiActions;

    private boolean initialized = false;

    private UIManager(JavaPlugin plugin) {
        this.plugin = plugin;

        this.cacheManager = new MenuCacheManager();
        this.registry = new MenuRegistry();
        this.navigationManager = new NavigationManager(cacheManager);
        this.factory = new MenuFactory(cacheManager);
        this.executor = new MenuExecutor(registry, cacheManager, navigationManager);
        this.soundManager = new MenuSoundManager();
        this.configLoader = new UIConfigLoader(plugin, cacheManager);
        this.uiActions = new UIActions(this);

        registerActions();
        registerListeners();

        this.initialized = true;
    }

    private void registerActions() {
        if (!net.exylia.commons.v2.action.core.ActionManager.isInitialized()) {
            plugin.getLogger().warning("ActionManager not initialized, UI actions not registered");
            return;
        }
        uiActions.registerAll();
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new InventoryDragListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new InventoryCloseListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), plugin);
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new UIManager(plugin);
                }
            }
        }
    }

    public static UIManager getInstance() {
        if (instance == null) {
            throw new MenuException("UIManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null && instance.initialized;
    }

    public CompletableFuture<String> registerMenuAsync(MenuV2 menu) {
        return CompletableFuture.supplyAsync(() -> {
            registry.registerTemplate(menu);
            return menu.getId();
        });
    }

    public void registerMenu(MenuV2 menu) {
        registry.registerTemplate(menu);
    }

    public CompletableFuture<Void> openMenuAsync(Player player, String menuId, MenuContext context) {
        return CompletableFuture.supplyAsync(() -> {
                    return registry.getTemplate(menuId)
                            .orElseThrow(() -> new MenuException("Menu not found: " + menuId));
                })
                .thenCompose(template -> {
                    MenuV2 instance = factory.createInstance(template, context);
                    return executor.openAsync(player, instance, context);
                });
    }

    public void openMenu(Player player, String menuId, MenuContext context) {
        try {
            openMenuAsync(player, menuId, context).join();
        } catch (Exception e) {
            throw new MenuException("Failed to open menu: " + menuId, e);
        }
    }

    public CompletableFuture<Void> closeMenuAsync(Player player) {
        return executor.closeAsync(player);
    }

    public void closeMenu(Player player) {
        executor.close(player);
    }

    @Override
    public String getName() {
        return "UI-V2";
    }

    @Override
    public ReloadPriority getPriority() {
        return ReloadPriority.NORMAL;
    }

    @Override
    public CompletableFuture<SystemReloadMetrics> reload(ReloadContext context) {
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                configLoader.reloadAll();

                cacheManager.invalidateAll();

                registry.getAllActiveMenus().forEach(menu -> {
                    if (menu.getState() == net.exylia.commons.v2.ui.model.MenuState.OPEN) {
                        menu.refresh();
                    }
                });

                long duration = System.currentTimeMillis() - startTime;

                return SystemReloadMetrics.success("UI-V2", duration);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;

                return SystemReloadMetrics.failure("UI-V2", duration, e);
            }
        });
    }

    public UICacheStats getCacheStats() {
        return cacheManager.getStats();
    }

    public void shutdown() {
        registry.getAllActiveMenus().forEach(MenuV2::close);

        cacheManager.invalidateAll();
        registry.clear();

        initialized = false;
    }
}

package net.exylia.commons.v2.ui.core;

import lombok.Getter;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.ui.cache.MenuCacheManager;
import net.exylia.commons.v2.ui.event.MenuClickEvent;
import net.exylia.commons.v2.ui.exception.InvalidMenuStateException;
import net.exylia.commons.v2.ui.exception.MenuException;
import net.exylia.commons.v2.ui.model.MenuContext;
import net.exylia.commons.v2.ui.model.MenuState;
import net.exylia.commons.v2.ui.model.MenuV2;
import net.exylia.commons.v2.ui.navigation.NavigationManager;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

@Getter
public class MenuExecutor {
    private final MenuRegistry registry;
    private final MenuCacheManager cacheManager;
    private final NavigationManager navigationManager;

    public MenuExecutor(MenuRegistry registry, MenuCacheManager cacheManager, NavigationManager navigationManager) {
        this.registry = registry;
        this.cacheManager = cacheManager;
        this.navigationManager = navigationManager;
    }

    public CompletableFuture<Void> openAsync(Player player, MenuV2 menu, MenuContext context) {
        if (player == null || menu == null) {
            return CompletableFuture.failedFuture(
                new MenuException("Player and menu cannot be null")
            );
        }

        if (!player.isOnline()) {
            return CompletableFuture.failedFuture(
                new MenuException("Player is not online")
            );
        }

        final MenuContext finalContext = context != null ? context : MenuContext.create(player);
        finalContext.withPlayer(player);

        return CompletableFuture.runAsync(() -> {
            try {
                menu.setContext(finalContext);
                menu.setState(MenuState.OPENING);

                registry.registerActiveMenu(player, menu.getId(), menu);

                if (finalContext.getParentMenu() != null) {
                    navigationManager.pushMenu(player, finalContext.getParentMenu());
                }

                Schedulers.sync(() -> {
                    try {
                        menu.open(player, finalContext);
                        menu.setState(MenuState.OPEN);
                    } catch (Exception e) {
                        menu.setState(MenuState.CLOSED);
                        registry.unregisterActiveMenu(player, menu.getId());
                        throw new MenuException("Failed to open menu", e);
                    }
                });

            } catch (Exception e) {
                throw new MenuException("Error during menu opening", e);
            }
        });
    }

    public void open(Player player, MenuV2 menu, MenuContext context) {
        try {
            openAsync(player, menu, context).join();
        } catch (Exception e) {
            throw new MenuException("Failed to open menu", e);
        }
    }

    public CompletableFuture<Void> closeAsync(Player player) {
        if (player == null) {
            return CompletableFuture.failedFuture(
                new MenuException("Player cannot be null")
            );
        }

        return CompletableFuture.runAsync(() -> {
            registry.getCurrentMenu(player).ifPresent(menu -> {
                try {
                    menu.setState(MenuState.CLOSING);

                    Schedulers.sync(() -> {
                        try {
                            menu.close();
                            menu.setState(MenuState.CLOSED);
                            registry.unregisterActiveMenu(player, menu.getId());
                        } catch (Exception e) {
                            menu.setState(MenuState.OPEN);
                            throw new MenuException("Failed to close menu", e);
                        }
                    });

                } catch (Exception e) {
                    throw new MenuException("Error during menu closing", e);
                }
            });
        });
    }

    public void close(Player player) {
        try {
            closeAsync(player).join();
        } catch (Exception e) {
            throw new MenuException("Failed to close menu", e);
        }
    }

    public CompletableFuture<Void> refreshAsync(Player player) {
        if (player == null) {
            return CompletableFuture.failedFuture(
                new MenuException("Player cannot be null")
            );
        }

        return CompletableFuture.runAsync(() -> {
            registry.getCurrentMenu(player).ifPresent(menu -> {
                if (menu.getState() != MenuState.OPEN) {
                    throw new InvalidMenuStateException(
                        menu.getState(), MenuState.OPEN
                    );
                }

                Schedulers.sync(menu::refresh);
            });
        });
    }

    public void refresh(Player player) {
        try {
            refreshAsync(player).join();
        } catch (Exception e) {
            throw new MenuException("Failed to refresh menu", e);
        }
    }

    public void handleClick(MenuClickEvent event) {
        if (event == null || event.getMenu() == null) {
            return;
        }

        MenuV2 menu = event.getMenu();

        if (menu.getState() != MenuState.OPEN) {
            return;
        }

        menu.handleClick(event);
    }
}

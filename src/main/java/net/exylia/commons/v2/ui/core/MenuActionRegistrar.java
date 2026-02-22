package net.exylia.commons.v2.ui.core;

import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.menu.MenuBase;
import net.exylia.commons.v2.ui.menu.PaginationMenu;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class MenuActionRegistrar {

    private static final long PAGE_NAVIGATION_COOLDOWN_MILLIS = 150L;

    public static void registerMenuActions(JavaPlugin plugin) {
        ActionAPI.register(createCloseAction(plugin));
        ActionAPI.register(createBackAction(plugin));
        ActionAPI.register(createNextPageAction(plugin));
        ActionAPI.register(createPreviousPageAction(plugin));
    }

    private static Action createCloseAction(JavaPlugin plugin) {
        return ActionAPI.create("close", plugin)
                .description("Close the current menu")
                .handler((ctx, args) -> MenuAPI.close(ctx.getPlayer()))
                .build();
    }

    private static Action createBackAction(JavaPlugin plugin) {
        return ActionAPI.create("back", plugin)
                .description("Navigate to previous menu")
                .handler((ctx, args) -> MenuAPI.navigateBack(ctx.getPlayer()))
                .build();
    }

    private static Action createNextPageAction(JavaPlugin plugin) {
        return ActionAPI.create("next_page", plugin)
                .description("Go to next page in paginated menu")
                .cooldown(PAGE_NAVIGATION_COOLDOWN_MILLIS, TimeUnit.MILLISECONDS)
                .handler((ctx, args) -> {
                    Optional<MenuBase> menuOpt = MenuAPI.getActiveMenu(ctx.getPlayer());
                    if (menuOpt.isPresent()) {
                        MenuBase menu = menuOpt.get();
                        if (menu instanceof PaginationMenu paginationMenu) {
                            paginationMenu.nextPage();
                        }
                    }
                })
                .build();
    }

    private static Action createPreviousPageAction(JavaPlugin plugin) {
        return ActionAPI.create("previous_page", plugin)
                .description("Go to previous page in paginated menu")
                .cooldown(PAGE_NAVIGATION_COOLDOWN_MILLIS, TimeUnit.MILLISECONDS)
                .handler((ctx, args) -> {
                    Optional<MenuBase> menuOpt = MenuAPI.getActiveMenu(ctx.getPlayer());
                    if (menuOpt.isPresent()) {
                        MenuBase menu = menuOpt.get();
                        if (menu instanceof PaginationMenu paginationMenu) {
                            paginationMenu.previousPage();
                        }
                    }
                })
                .build();
    }
}

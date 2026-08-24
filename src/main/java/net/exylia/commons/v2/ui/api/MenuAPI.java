package net.exylia.commons.v2.ui.api;

import net.exylia.commons.v2.ui.config.MenuParser;
import net.exylia.commons.v2.ui.core.MenuManager;
import net.exylia.commons.v2.ui.menu.ItemInputMenu;
import net.exylia.commons.v2.ui.menu.MenuBase;
import net.exylia.commons.v2.ui.model.MenuData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class MenuAPI {

    private MenuAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        MenuManager.initialize(plugin);
    }

    public static boolean isInitialized() {
        return MenuManager.isInitialized();
    }

    public static CompletableFuture<Void> openAsync(Player player, ConfigurationSection config) {
        MenuData menuData = parse(config);
        return MenuManager.getInstance().openMenuAsync(player, menuData);
    }

    public static void open(Player player, ConfigurationSection config) {
        openAsync(player, config);
    }

    public static void open(Player player, MenuData menuData) {
        MenuManager.getInstance().openMenu(player, menuData);
    }

    public static CompletableFuture<Void> openAsync(Player player, MenuData menuData) {
        return MenuManager.getInstance().openMenuAsync(player, menuData);
    }

    public static ItemInputMenu openItemInput(Player player, ConfigurationSection config, Consumer<Map<Integer, ItemStack>> onClose) {
        MenuData menuData = parse(config);
        return MenuManager.getInstance().openItemInputMenu(player, menuData, onClose);
    }

    public static ItemInputMenu openItemInput(Player player, MenuData menuData, Consumer<Map<Integer, ItemStack>> onClose) {
        return MenuManager.getInstance().openItemInputMenu(player, menuData, onClose);
    }

    public static Optional<MenuBase> getActiveMenu(Player player) {
        return MenuManager.getInstance().getActiveMenu(player);
    }

    public static boolean refresh(Player player) {
        return MenuManager.getInstance().refreshActiveMenu(player);
    }

    public static void close(Player player) {
        MenuManager.getInstance().closeMenu(player);
    }

    public static void closeAll() {
        MenuManager.getInstance().shutdown();
    }

    public static boolean navigateBack(Player player) {
        return MenuManager.getInstance().navigateBack(player);
    }

    public static void clearHistory(Player player) {
        MenuManager.getInstance().clearHistory(player);
    }

    public static MenuData parse(ConfigurationSection config) {
        return MenuParser.parse(config);
    }

    public static CompletableFuture<MenuData> parseAsync(ConfigurationSection config) {
        return CompletableFuture.supplyAsync(() -> MenuParser.parse(config));
    }

    public static void shutdown() {
        MenuManager.getInstance().shutdown();
    }

    public static MenuManager getManager() {
        return MenuManager.getInstance();
    }
}

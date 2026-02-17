package net.exylia.commons.v2.wizard.api;

import net.exylia.commons.v2.wizard.config.WizardConfig;
import net.exylia.commons.v2.wizard.core.WizardManager;
import net.exylia.commons.v2.wizard.handler.InteractionHandler;
import net.exylia.commons.v2.wizard.handler.LocationHandler;
import net.exylia.commons.v2.wizard.handler.SelectionHandler;
import net.exylia.commons.v2.wizard.session.WizardSession;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class WizardAPI {
    private WizardAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void init(JavaPlugin plugin) {
        WizardManager.getInstance().init(plugin);
    }

    public static void shutdown() {
        WizardManager.getInstance().shutdown();
    }

    public static <T> CompletableFuture<T> interaction(Player player, InteractionHandler<T> handler) {
        return interaction(player, WizardConfig.defaults(), handler);
    }

    public static <T> CompletableFuture<T> interaction(Player player, WizardConfig config, InteractionHandler<T> handler) {
        return WizardManager.getInstance().startInteraction(player, config, handler);
    }

    public static <T> CompletableFuture<T> location(Player player, int count, LocationHandler<T> handler) {
        return location(player, count, defaultLocationConfig(), handler);
    }

    public static <T> CompletableFuture<T> location(Player player, int count, WizardConfig config, LocationHandler<T> handler) {
        return WizardManager.getInstance().startLocation(player, count, config, handler);
    }

    public static <T> CompletableFuture<T> selection(Player player, int count, SelectionHandler<T> handler) {
        return selection(player, count, defaultSelectionConfig(), handler, false);
    }

    public static <T> CompletableFuture<T> selection(Player player, int count, boolean giveWand, SelectionHandler<T> handler) {
        return selection(player, count, defaultSelectionConfig(), handler, giveWand);
    }

    public static <T> CompletableFuture<T> selection(Player player, int count, WizardConfig config, SelectionHandler<T> handler, boolean giveWand) {
        return WizardManager.getInstance().startSelection(player, count, config, handler, giveWand);
    }

    public static boolean cancel(Player player) {
        return WizardManager.getInstance().cancel(player);
    }

    public static boolean hasActive(Player player) {
        return WizardManager.getInstance().hasActive(player);
    }

    public static Optional<WizardSession<?>> getSession(Player player) {
        return WizardManager.getInstance().getSession(player);
    }

    public static void updateDisplay(Player player, WizardConfig config) {
        WizardManager.getInstance().updateDisplay(player, config);
    }

    private static WizardConfig defaultLocationConfig() {
        return WizardConfig.builder()
                .titleText("{warning}⚡ Position {current}/{total}")
                .subtitleText("{info}Use SHIFT + LEFT CLICK")
                .actionBarText("{warning}Remaining: {remaining}")
                .build();
    }

    private static WizardConfig defaultSelectionConfig() {
        return WizardConfig.builder()
                .titleText("{warning}⚡ Select Area {current}/{total}")
                .subtitleText("{info}Use wand to select")
                .actionBarText("{warning}Selecting area {current}/{total}")
                .build();
    }
}

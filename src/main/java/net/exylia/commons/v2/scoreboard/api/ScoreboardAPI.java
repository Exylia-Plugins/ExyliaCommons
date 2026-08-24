package net.exylia.commons.v2.scoreboard.api;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.builder.ScoreboardBuilder;
import net.exylia.commons.v2.scoreboard.config.ScoreboardLoader;
import net.exylia.commons.v2.scoreboard.core.ScoreboardManager;
import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ScoreboardAPI {

    private ScoreboardAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(Plugin plugin) {
        ScoreboardManager.initialize(plugin);
    }

    public static ScoreboardBuilder builder() {
        return ScoreboardBuilder.create();
    }

    public static Scoreboard load(ConfigurationSection section) {
        return ScoreboardLoader.load(section);
    }

    /**
     * @return true si la version del servidor tiene packet adapter. Si es false
     * los scoreboards no se muestran, pero ninguna llamada falla.
     */
    public static boolean isSupported() {
        return ScoreboardManager.getInstance().isSupported();
    }

    public static CompletableFuture<String> show(Player player, Scoreboard scoreboard) {
        return show(player, scoreboard, null);
    }

    /**
     * Muestra un scoreboard. El registro es inmediato; el envio de packets lo
     * hace scoreboard-library en su propia tarea asincrona.
     * <p>
     * Devuelve un future ya completado: la firma se mantiene por compatibilidad
     * con el API anterior, que si era asincrona.
     */
    public static CompletableFuture<String> show(Player player, Scoreboard scoreboard, PlaceholderContext context) {
        return CompletableFuture.completedFuture(
                ScoreboardManager.getInstance().showScoreboard(player, scoreboard, context));
    }

    public static boolean hide(Player player) {
        return ScoreboardManager.getInstance().hideScoreboard(player);
    }

    public static boolean has(Player player) {
        return ScoreboardManager.getInstance().hasScoreboard(player);
    }

    public static Optional<ScoreboardInstance> get(Player player) {
        return ScoreboardManager.getInstance().getScoreboard(player);
    }

    public static void updateContext(Player player, PlaceholderContext context) {
        ScoreboardManager.getInstance().updateContext(player, context);
    }

    public static void forceUpdate(Player player) {
        ScoreboardManager.getInstance().forceUpdate(player);
    }

    public static void hideAll() {
        ScoreboardManager.getInstance().hideAll();
    }

    public static void shutdown() {
        ScoreboardManager.getInstance().shutdown();
    }

    public static int getActiveCount() {
        return ScoreboardManager.getInstance().getActiveCount();
    }

    public static ScoreboardStats getStats() {
        return ScoreboardManager.getInstance().getStats();
    }
}

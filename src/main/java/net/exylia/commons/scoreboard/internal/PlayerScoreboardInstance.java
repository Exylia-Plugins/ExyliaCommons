package net.exylia.commons.scoreboard.internal;

import lombok.Getter;
import net.exylia.commons.config.components.ScoreboardConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.scoreboard.internal.ScoreboardRenderer.RenderedScoreboard;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Instancia de scoreboard para un jugador específico
 * Maneja el estado interno y la lógica de actualización
 */
public class PlayerScoreboardInstance {

    private final Plugin plugin;
    @Getter
    private final Player player;
    @Getter
    private final ScoreboardConfig config;
    private final ScoreboardRenderer renderer;

    private ExyliaContext context;
    private RenderedScoreboard rendered;

    @Getter
    private boolean visible = false;
    private long lastUpdate = 0;

    public PlayerScoreboardInstance(Plugin plugin, Player player, ScoreboardConfig config,
                                    ExyliaContext context, ScoreboardRenderer renderer) {
        this.plugin = plugin;
        this.player = player;
        this.config = config;
        this.context = context.copy(); // Copia defensiva
        this.renderer = renderer;
    }

    /**
     * Muestra el scoreboard al jugador
     */
    public void show() {
        if (visible || !player.isOnline()) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (visible || !player.isOnline()) return;

            rendered = renderer.createScoreboard(player, config, context);
            player.setScoreboard(rendered.scoreboard);
            visible = true;

            update();
        });
    }

    /**
     * Oculta el scoreboard del jugador
     */
    public void hide() {
        if (!visible) return;

        visible = false;

        if (player.isOnline()) {
            // Restaurar scoreboard principal
            player.setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
        }

        // Limpiar recursos
        if (rendered != null) {
            rendered.cleanup();
            rendered = null;
        }
    }

    /**
     * Actualiza el contenido del scoreboard
     */
    public void update() {
        if (!visible || !player.isOnline() || rendered == null) {
            hide();
            return;
        }

        try {
            renderer.updateScoreboard(rendered, player, config, context);
            lastUpdate = System.currentTimeMillis();
        } catch (Exception e) {
            plugin.getLogger().warning("Error actualizando scoreboard de " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Verifica si el scoreboard debe actualizarse según su configuración
     */
    public boolean shouldUpdate() {
        if (!visible || config.getUpdateInterval() <= 0) {
            return false;
        }

        long interval = config.getUpdateInterval() * 50L; // Convertir ticks a ms
        return (System.currentTimeMillis() - lastUpdate) >= interval;
    }

    /**
     * Actualiza el contexto completo
     */
    public void updateContext(ExyliaContext newContext) {
        this.context = newContext.copy();
    }

    /**
     * Añade objetos al contexto existente
     */
    public void addToContext(Object... objects) {
        this.context.addAll(objects);
    }

    // ==================== GETTERS ====================

    public ExyliaContext getContext() { return context.copy(); }
}
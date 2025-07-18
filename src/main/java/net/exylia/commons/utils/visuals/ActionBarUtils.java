package net.exylia.commons.utils.visuals;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.config.components.ActionBarConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.*;

public class ActionBarUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, ActionBarInstance>> playerActionBars = new ConcurrentHashMap<>();

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
    }

    // ==================== API SIMPLE ====================

    /**
     * Envía un action bar usando configuración
     */
    public static String sendActionBar(Player player, ActionBarConfig config, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        String actionBarId = generateActionBarId();

        ExyliaContext enrichedContext = context.copy()
                .withPlayer(player)
                .withCurrentTime()
                .put("actionbar_id", actionBarId);

        ActionBarInstance instance = new ActionBarInstance(actionBarId, config, enrichedContext);

        BukkitTask task = executeActionBar(player, instance);
        if (task != null) {
            instance.setTask(task);
        }
        storeActionBarInstance(player, actionBarId, instance);

        return actionBarId;
    }

    /**
     * Envía un action bar sin contexto
     */
    public static String sendActionBar(Player player, ActionBarConfig config) {
        return sendActionBar(player, config, ExyliaContext.create());
    }

    /**
     * Envía un action bar con ID personalizado
     */
    public static String sendActionBar(Player player, String actionBarId, ActionBarConfig config, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        if (hasActionBar(player, actionBarId)) {
            cancelActionBar(player, actionBarId);
        }

        ExyliaContext enrichedContext = context.copy()
                .withPlayer(player)
                .withCurrentTime()
                .put("actionbar_id", actionBarId);

        ActionBarInstance instance = new ActionBarInstance(actionBarId, config, enrichedContext);

        BukkitTask task = executeActionBar(player, instance);
        if (task != null) {
            instance.setTask(task);
        }
        storeActionBarInstance(player, actionBarId, instance);

        return actionBarId;
    }

    // ==================== EJECUCIÓN ====================

    private static BukkitTask executeActionBar(Player player, ActionBarInstance instance) {
        ActionBarConfig config = instance.getConfig();

        // Para action bars permanentes, usar un task que se actualice periódicamente
        return new BukkitRunnable() {
            private long updateCount = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    removeActionBarInstance(player, instance.getId());
                    cancel();
                    return;
                }

                ExyliaContext currentContext = instance.getContext().copy()
                        .put("update_count", updateCount)
                        .withCurrentTime();

                String processedText = processPlaceholders(config.getText(), player, currentContext);
                MessageUtils.sendActionBar(player, processedText);

                updateCount++;
            }
        }.runTaskTimer(plugin, 0L, config.getUpdateInterval());
    }

    // ==================== GESTIÓN DE INSTANCIAS ====================

    @Getter
    public static class ActionBarInstance {
        private final String id;
        private final ActionBarConfig config;
        private ExyliaContext context;
        @Setter
        private BukkitTask task;
        private final long createdAt;

        ActionBarInstance(String id, ActionBarConfig config, ExyliaContext context) {
            this.id = id;
            this.config = config;
            this.context = context;
            this.createdAt = System.currentTimeMillis();
        }

        public void updateContext(ExyliaContext newContext) {
            this.context = newContext != null ? newContext : ExyliaContext.create();
        }

        public boolean isActive() {
            return task != null && !task.isCancelled();
        }

        public long getAge() {
            return System.currentTimeMillis() - createdAt;
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private static String processPlaceholders(String text, Player player, ExyliaContext context) {
        if (text == null || text.isEmpty()) return "";
        return PlaceholderSystemManager.getInstance().process(text, player, context.getAllObjects());
    }

    private static String generateActionBarId() {
        return "actionbar_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private static void storeActionBarInstance(Player player, String id, ActionBarInstance instance) {
        playerActionBars.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(id, instance);
    }

    private static ActionBarInstance getActionBarInstance(Player player, String id) {
        Map<String, ActionBarInstance> playerData = playerActionBars.get(player.getUniqueId());
        return playerData != null ? playerData.get(id) : null;
    }

    private static void removeActionBarInstance(Player player, String id) {
        Map<String, ActionBarInstance> playerData = playerActionBars.get(player.getUniqueId());
        if (playerData != null) {
            ActionBarInstance removed = playerData.remove(id);
            if (removed != null && removed.getTask() != null && !removed.getTask().isCancelled()) {
                removed.getTask().cancel();
            }
            if (playerData.isEmpty()) {
                playerActionBars.remove(player.getUniqueId());
            }
        }
    }

    // ==================== API DE GESTIÓN ====================

    /**
     * Cancela un action bar específico
     */
    public static boolean cancelActionBar(Player player, String actionBarId) {
        ActionBarInstance instance = getActionBarInstance(player, actionBarId);
        if (instance == null) return false;

        if (instance.getTask() != null && !instance.getTask().isCancelled()) {
            instance.getTask().cancel();
        }

        // Limpiar action bar enviando uno vacío
        MessageUtils.sendActionBar(player, "");

        removeActionBarInstance(player, actionBarId);
        return true;
    }

    /**
     * Cancela todos los action bars de un jugador
     */
    public static int cancelAllActionBars(Player player) {
        Map<String, ActionBarInstance> playerData = playerActionBars.get(player.getUniqueId());
        if (playerData == null || playerData.isEmpty()) return 0;

        int count = 0;
        Set<String> actionBarIds = new HashSet<>(playerData.keySet());
        for (String actionBarId : actionBarIds) {
            if (cancelActionBar(player, actionBarId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Verifica si un jugador tiene un action bar específico activo
     */
    public static boolean hasActionBar(Player player, String actionBarId) {
        return getActionBarInstance(player, actionBarId) != null;
    }

    /**
     * Obtiene todos los action bars activos de un jugador
     */
    public static Set<String> getActiveActionBars(Player player) {
        Map<String, ActionBarInstance> playerData = playerActionBars.get(player.getUniqueId());
        return playerData != null ? new HashSet<>(playerData.keySet()) : new HashSet<>();
    }

    /**
     * Actualiza el contexto de un action bar
     */
    public static boolean updateActionBar(Player player, String actionBarId, ExyliaContext newContext) {
        ActionBarInstance instance = getActionBarInstance(player, actionBarId);
        if (instance == null) return false;

        instance.updateContext(newContext);
        return true;
    }

    /**
     * Limpia todos los datos al cerrar el plugin
     */
    public static void cleanup() {
        for (UUID playerId : new HashSet<>(playerActionBars.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                cancelAllActionBars(player);
            }
        }
        playerActionBars.clear();
    }
}
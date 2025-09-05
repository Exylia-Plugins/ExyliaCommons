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
import java.util.stream.Collectors;

import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

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

    // ==================== API ACTIONBAR CON COUNTDOWN ====================

    /**
     * Envía un action bar con countdown automático
     * @param player El jugador
     * @param actionBarId ID del action bar
     * @param config Configuración del action bar (debe contener %time% en el texto)
     * @param durationTicks Duración del countdown en ticks
     * @param context Contexto adicional
     * @return ID del action bar creado
     */
    public static String sendCountdownActionBar(Player player, String actionBarId, ActionBarConfig config, long durationTicks, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        // Cancelar action bar existente si existe
        if (hasActionBar(player, actionBarId)) {
            cancelActionBar(player, actionBarId);
        }

        ExyliaContext enrichedContext = context.copy()
                .withPlayer(player)
                .withCurrentTime()
                .put("actionbar_id", actionBarId)
                .put("countdown_duration", durationTicks);

        // Crear configuración especial para countdown
        ActionBarConfig countdownConfig = new ActionBarConfig(
                config.getText(),
                config.getUpdateInterval()
        );

        CountdownActionBarInstance instance = new CountdownActionBarInstance(actionBarId, countdownConfig, enrichedContext, durationTicks);

        BukkitTask task = executeCountdownActionBar(player, instance);
        if (task != null) {
            instance.setTask(task);
        }
        storeActionBarInstance(player, actionBarId, instance);

        return actionBarId;
    }

    /**
     * Envía un action bar con countdown automático (versión simplificada)
     */
    public static String sendCountdownActionBar(Player player, ActionBarConfig config, long durationTicks) {
        return sendCountdownActionBar(player, generateActionBarId(), config, durationTicks, ExyliaContext.create());
    }

    /**
     * Envía un action bar con countdown usando duración en segundos
     */
    public static String sendCountdownActionBar(Player player, String actionBarId, ActionBarConfig config, int durationSeconds, ExyliaContext context) {
        return sendCountdownActionBar(player, actionBarId, config, durationSeconds * 20L, context);
    }

    /**
     * Envía un action bar con countdown usando duración en segundos (versión simplificada)
     */
    public static String sendCountdownActionBar(Player player, ActionBarConfig config, int durationSeconds) {
        return sendCountdownActionBar(player, generateActionBarId(), config, durationSeconds * 20L, ExyliaContext.create());
    }

    // ==================== EJECUCIÓN COUNTDOWN ====================

    private static BukkitTask executeCountdownActionBar(Player player, CountdownActionBarInstance instance) {
        ActionBarConfig config = instance.getConfig();

        return new BukkitRunnable() {
            private long ticksRemaining = instance.getDurationTicks();
            private long updateCount = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    removeActionBarInstance(player, instance.getId());
                    cancel();
                    return;
                }

                // Calcular tiempo restante
                long secondsRemaining = (ticksRemaining + 19) / 20; // Redondear hacia arriba

                // Actualizar contexto con información del countdown
                ExyliaContext currentContext = instance.getContext().copy()
                        .put("time", secondsRemaining)
                        .put("ticks_remaining", ticksRemaining)
                        .put("update_count", updateCount)
                        .put("countdown_active", true)
                        .withCurrentTime();

                // Procesar placeholders
                String processedText = processPlaceholders(config.getText(), player, currentContext);

                // Enviar action bar
                MessageUtils.sendActionBar(player, processedText);

                // Actualizar contadores
                ticksRemaining--;
                updateCount++;

                // Verificar si terminó el countdown
                if (ticksRemaining < 0) {
                    // Ejecutar callback si existe
                    if (instance.getOnComplete() != null) {
                        try {
                            instance.getOnComplete().run();
                        } catch (Exception e) {
                            logInternalWarn("Error ejecutando callback de countdown: " + e.getMessage());
                        }
                    }

                    removeActionBarInstance(player, instance.getId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Ejecutar cada tick para precisión
    }

    // ==================== CLASE COUNTDOWN ACTIONBAR INSTANCE ====================

    @Getter
    public static class CountdownActionBarInstance extends ActionBarInstance {
        private final long durationTicks;
        @Setter
        private Runnable onComplete;
        @Setter
        private Runnable onCancel;

        CountdownActionBarInstance(String id, ActionBarConfig config, ExyliaContext context, long durationTicks) {
            super(id, config, context);
            this.durationTicks = durationTicks;
        }

        public int getDurationSeconds() {
            return (int) ((durationTicks + 19) / 20);
        }

        @Override
        public boolean isPermanent() {
            return false; // Los countdown nunca son permanentes
        }

        public CountdownActionBarInstance onComplete(Runnable callback) {
            this.onComplete = callback;
            return this;
        }

        public CountdownActionBarInstance onCancel(Runnable callback) {
            this.onCancel = callback;
            return this;
        }
    }

    // ==================== API EXTENDIDA PARA COUNTDOWN ====================

    /**
     * Crea un action bar de countdown con callback
     */
    public static String sendCountdownActionBar(Player player, String actionBarId, ActionBarConfig config, int durationSeconds,
                                                ExyliaContext context, Runnable onComplete) {
        String id = sendCountdownActionBar(player, actionBarId, config, durationSeconds, context);

        ActionBarInstance instance = getActionBarInstance(player, id);
        if (instance instanceof CountdownActionBarInstance) {
            ((CountdownActionBarInstance) instance).onComplete(onComplete);
        }

        return id;
    }

    /**
     * Crea un action bar de countdown con callbacks de completado y cancelación
     */
    public static String sendCountdownActionBar(Player player, String actionBarId, ActionBarConfig config, int durationSeconds,
                                                ExyliaContext context, Runnable onComplete, Runnable onCancel) {
        String id = sendCountdownActionBar(player, actionBarId, config, durationSeconds, context);

        ActionBarInstance instance = getActionBarInstance(player, id);
        if (instance instanceof CountdownActionBarInstance) {
            CountdownActionBarInstance countdownInstance = (CountdownActionBarInstance) instance;
            countdownInstance.onComplete(onComplete);
            countdownInstance.onCancel(onCancel);
        }

        return id;
    }

    /**
     * Obtiene el tiempo restante de un countdown en segundos
     */
    public static int getCountdownTimeRemaining(Player player, String actionBarId) {
        ActionBarInstance instance = getActionBarInstance(player, actionBarId);
        if (instance instanceof CountdownActionBarInstance) {
            ExyliaContext context = instance.getContext();
            Object timeRemaining = context.get("time");
            if (timeRemaining instanceof Number) {
                return ((Number) timeRemaining).intValue();
            }
        }
        return -1;
    }

    /**
     * Verifica si un action bar es de tipo countdown
     */
    public static boolean isCountdownActionBar(Player player, String actionBarId) {
        ActionBarInstance instance = getActionBarInstance(player, actionBarId);
        return instance instanceof CountdownActionBarInstance;
    }

    /**
     * Obtiene todos los action bars de countdown activos de un jugador
     */
    public static Set<String> getCountdownActionBars(Player player) {
        Map<String, ActionBarInstance> playerData = playerActionBars.get(player.getUniqueId());
        if (playerData == null) return new HashSet<>();

        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof CountdownActionBarInstance)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    // ==================== EJECUCIÓN ====================

    private static BukkitTask executeActionBar(Player player, ActionBarInstance instance) {
        ActionBarConfig config = instance.getConfig();

        if (config.isPermanent()) {
            // Action bar permanente que se actualiza periódicamente
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
                            .put("permanent_active", true)
                            .withCurrentTime();

                    String processedText = processPlaceholders(config.getText(), player, currentContext);
                    MessageUtils.sendActionBar(player, processedText);

                    updateCount++;
                }
            }.runTaskTimer(plugin, 0L, config.getUpdateInterval());
        } else {
            // Action bar normal (una vez)
            String processedText = processPlaceholders(config.getText(), player, instance.getContext());
            MessageUtils.sendActionBar(player, processedText);

            // Auto-remover después de un tiempo (action bars desaparecen automáticamente)
            return Bukkit.getScheduler().runTaskLater(plugin, () -> {
                removeActionBarInstance(player, instance.getId());
            }, 60L); // 3 segundos
        }
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

        public boolean isPermanent() {
            return config.isPermanent();
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

        // Ejecutar callback de cancelación si es CountdownActionBarInstance
        if (instance instanceof CountdownActionBarInstance countdownInstance) {
            if (countdownInstance.getOnCancel() != null) {
                try {
                    countdownInstance.getOnCancel().run();
                } catch (Exception e) {
                    logInternalWarn("Error ejecutando callback de cancelación: " + e.getMessage());
                }
            }
        }

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
     * Obtiene todos los action bars permanentes activos de un jugador
     */
    public static Set<String> getPermanentActionBars(Player player) {
        Map<String, ActionBarInstance> playerData = playerActionBars.get(player.getUniqueId());
        if (playerData == null) return new HashSet<>();

        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue().isPermanent())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Verifica si un jugador tiene action bars permanentes activos
     */
    public static boolean hasPermanentActionBars(Player player) {
        return !getPermanentActionBars(player).isEmpty();
    }

    /**
     * Cancela todos los action bars permanentes de un jugador
     */
    public static int cancelAllPermanentActionBars(Player player) {
        Set<String> permanentActionBars = getPermanentActionBars(player);
        int count = 0;
        for (String actionBarId : permanentActionBars) {
            if (cancelActionBar(player, actionBarId)) {
                count++;
            }
        }
        return count;
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
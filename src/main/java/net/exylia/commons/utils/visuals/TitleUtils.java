package net.exylia.commons.utils.visuals;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.config.components.TitleConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TitleUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, TitleInstance>> playerTitles = new ConcurrentHashMap<>();

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
    }

    // ==================== API SIMPLE ====================

    /**
     * Envía un título usando configuración
     */
    public static String sendTitle(Player player, TitleConfig config, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        String titleId = generateTitleId();

        // Crear contexto enriquecido
        ExyliaContext enrichedContext = context.copy()
                .withPlayer(player)
                .withCurrentTime()
                .put("title_id", titleId);

        // Crear instancia
        TitleInstance instance = new TitleInstance(titleId, config, enrichedContext);

        // Ejecutar título
        BukkitTask task = executeTitle(player, instance);
        if (task != null) {
            instance.setTask(task);
        }
        storeTitleInstance(player, titleId, instance);

        return titleId;
    }

    /**
     * Envía un título sin contexto
     */
    public static String sendTitle(Player player, TitleConfig config) {
        return sendTitle(player, config, ExyliaContext.create());
    }

    /**
     * Envía un título con ID personalizado
     */
    public static String sendTitle(Player player, String titleId, TitleConfig config, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        // Cancelar título existente si existe
        if (hasTitle(player, titleId)) {
            cancelTitle(player, titleId);
        }

        ExyliaContext enrichedContext = context.copy()
                .withPlayer(player)
                .withCurrentTime()
                .put("title_id", titleId);

        TitleInstance instance = new TitleInstance(titleId, config, enrichedContext);

        BukkitTask task = executeTitle(player, instance);
        if (task != null) {
            instance.setTask(task);
        }
        storeTitleInstance(player, titleId, instance);

        return titleId;
    }

    // Agregar estos métodos a la clase TitleUtils existente

// ==================== API TÍTULOS CON COUNTDOWN ====================

    /**
     * Envía un título con countdown automático
     * @param player El jugador
     * @param titleId ID del título
     * @param config Configuración del título (debe contener %time% en title o subtitle)
     * @param durationTicks Duración del countdown en ticks
     * @param context Contexto adicional
     * @return ID del título creado
     */
    public static String sendCountdownTitle(Player player, String titleId, TitleConfig config, long durationTicks, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        // Cancelar título existente si existe
        if (hasTitle(player, titleId)) {
            cancelTitle(player, titleId);
        }

        ExyliaContext enrichedContext = context.copy()
                .withPlayer(player)
                .withCurrentTime()
                .put("title_id", titleId)
                .put("countdown_duration", durationTicks);

        // Crear configuración especial para countdown
        TitleConfig countdownConfig = new TitleConfig(
                config.getTitle(),
                config.getSubtitle(),
                config.getFadeIn(),
                config.getStay(),
                config.getFadeOut()
        );

        CountdownTitleInstance instance = new CountdownTitleInstance(titleId, countdownConfig, enrichedContext, durationTicks);

        BukkitTask task = executeCountdownTitle(player, instance);
        if (task != null) {
            instance.setTask(task);
        }
        storeTitleInstance(player, titleId, instance);

        return titleId;
    }

    /**
     * Envía un título con countdown automático (versión simplificada)
     * @param player El jugador
     * @param config Configuración del título
     * @param durationTicks Duración del countdown en ticks
     * @return ID del título creado
     */
    public static String sendCountdownTitle(Player player, TitleConfig config, long durationTicks) {
        return sendCountdownTitle(player, generateTitleId(), config, durationTicks, ExyliaContext.create());
    }

    /**
     * Envía un título con countdown usando duración en segundos
     * @param player El jugador
     * @param titleId ID del título
     * @param config Configuración del título
     * @param durationSeconds Duración en segundos
     * @param context Contexto adicional
     * @return ID del título creado
     */
    public static String sendCountdownTitle(Player player, String titleId, TitleConfig config, int durationSeconds, ExyliaContext context) {
        return sendCountdownTitle(player, titleId, config, durationSeconds * 20L, context);
    }

    /**
     * Envía un título con countdown usando duración en segundos (versión simplificada)
     */
    public static String sendCountdownTitle(Player player, TitleConfig config, int durationSeconds) {
        return sendCountdownTitle(player, generateTitleId(), config, durationSeconds * 20L, ExyliaContext.create());
    }

// ==================== EJECUCIÓN COUNTDOWN ====================

    private static BukkitTask executeCountdownTitle(Player player, CountdownTitleInstance instance) {
        TitleConfig config = instance.getConfig();

        return new BukkitRunnable() {
            private long ticksRemaining = instance.getDurationTicks();
            private long updateCount = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    removeTitleInstance(player, instance.getId());
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
                String processedTitle = processPlaceholders(config.getTitle(), player, currentContext);
                String processedSubtitle = processPlaceholders(config.getSubtitle(), player, currentContext);

                // Enviar título
                MessageUtils.sendTitle(player, processedTitle, processedSubtitle,
                        config.getFadeIn(), config.getStay(), config.getFadeOut());

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
                            // Log error pero continuar
                            plugin.getLogger().warning("Error ejecutando callback de countdown: " + e.getMessage());
                        }
                    }

                    removeTitleInstance(player, instance.getId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Ejecutar cada tick para precisión
    }

// ==================== CLASE COUNTDOWN TITLE INSTANCE ====================

    @Getter
    public static class CountdownTitleInstance extends TitleInstance {
        private final long durationTicks;
        @Setter
        private Runnable onComplete;
        @Setter
        private Runnable onCancel;

        CountdownTitleInstance(String id, TitleConfig config, ExyliaContext context, long durationTicks) {
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

        public CountdownTitleInstance onComplete(Runnable callback) {
            this.onComplete = callback;
            return this;
        }

        public CountdownTitleInstance onCancel(Runnable callback) {
            this.onCancel = callback;
            return this;
        }
    }

// ==================== API EXTENDIDA PARA COUNTDOWN ====================

    /**
     * Crea un título de countdown con callback
     */
    public static String sendCountdownTitle(Player player, String titleId, TitleConfig config, int durationSeconds,
                                            ExyliaContext context, Runnable onComplete) {
        String id = sendCountdownTitle(player, titleId, config, durationSeconds, context);

        TitleInstance instance = getTitleInstance(player, id);
        if (instance instanceof CountdownTitleInstance) {
            ((CountdownTitleInstance) instance).onComplete(onComplete);
        }

        return id;
    }

    /**
     * Crea un título de countdown con callbacks de completado y cancelación
     */
    public static String sendCountdownTitle(Player player, String titleId, TitleConfig config, int durationSeconds,
                                            ExyliaContext context, Runnable onComplete, Runnable onCancel) {
        String id = sendCountdownTitle(player, titleId, config, durationSeconds, context);

        TitleInstance instance = getTitleInstance(player, id);
        if (instance instanceof CountdownTitleInstance) {
            CountdownTitleInstance countdownInstance = (CountdownTitleInstance) instance;
            countdownInstance.onComplete(onComplete);
            countdownInstance.onCancel(onCancel);
        }

        return id;
    }

    /**
     * Obtiene el tiempo restante de un countdown en segundos
     */
    public static int getCountdownTimeRemaining(Player player, String titleId) {
        TitleInstance instance = getTitleInstance(player, titleId);
        if (instance instanceof CountdownTitleInstance) {
            ExyliaContext context = instance.getContext();
            Object timeRemaining = context.get("time");
            if (timeRemaining instanceof Number) {
                return ((Number) timeRemaining).intValue();
            }
        }
        return -1;
    }

    /**
     * Verifica si un título es de tipo countdown
     */
    public static boolean isCountdownTitle(Player player, String titleId) {
        TitleInstance instance = getTitleInstance(player, titleId);
        return instance instanceof CountdownTitleInstance;
    }

    /**
     * Obtiene todos los títulos de countdown activos de un jugador
     */
    public static Set<String> getCountdownTitles(Player player) {
        Map<String, TitleInstance> playerData = playerTitles.get(player.getUniqueId());
        if (playerData == null) return new HashSet<>();

        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof CountdownTitleInstance)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    // ==================== EJECUCIÓN ====================

    private static BukkitTask executeTitle(Player player, TitleInstance instance) {
        TitleConfig config = instance.getConfig();

        if (config.isPermanent()) {
            // Título permanente que se actualiza periódicamente
            return new BukkitRunnable() {
                private long updateCount = 0;

                @Override
                public void run() {
                    if (!player.isOnline()) {
                        removeTitleInstance(player, instance.getId());
                        cancel();
                        return;
                    }

                    ExyliaContext currentContext = instance.getContext().copy()
                            .put("update_count", updateCount)
                            .put("permanent_active", true)
                            .withCurrentTime();

                    String processedTitle = processPlaceholders(config.getTitle(), player, currentContext);
                    String processedSubtitle = processPlaceholders(config.getSubtitle(), player, currentContext);

                    MessageUtils.sendTitle(player, processedTitle, processedSubtitle,
                            config.getFadeIn(), config.getStay(), config.getFadeOut());

                    updateCount++;
                }
            }.runTaskTimer(plugin, 0L, config.getRefreshInterval());
        } else {
            // Título normal (una vez)
            String processedTitle = processPlaceholders(config.getTitle(), player, instance.getContext());
            String processedSubtitle = processPlaceholders(config.getSubtitle(), player, instance.getContext());

            MessageUtils.sendTitle(player, processedTitle, processedSubtitle,
                    config.getFadeIn(), config.getStay(), config.getFadeOut());

            // Auto-remover después del tiempo total
            long totalTime = config.getFadeIn() + config.getStay() + config.getFadeOut() + 10;
            return Bukkit.getScheduler().runTaskLater(plugin, () -> {
                removeTitleInstance(player, instance.getId());
            }, totalTime);
        }
    }

    // ==================== GESTIÓN DE INSTANCIAS ====================

    @Getter
    public static class TitleInstance {
        private final String id;
        private final TitleConfig config;
        private ExyliaContext context;
        @Setter
        private BukkitTask task;
        private final long createdAt;

        TitleInstance(String id, TitleConfig config, ExyliaContext context) {
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

    private static String generateTitleId() {
        return "title_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private static void storeTitleInstance(Player player, String id, TitleInstance instance) {
        playerTitles.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(id, instance);
    }

    private static TitleInstance getTitleInstance(Player player, String id) {
        Map<String, TitleInstance> playerData = playerTitles.get(player.getUniqueId());
        return playerData != null ? playerData.get(id) : null;
    }

    private static void removeTitleInstance(Player player, String id) {
        Map<String, TitleInstance> playerData = playerTitles.get(player.getUniqueId());
        if (playerData != null) {
            TitleInstance removed = playerData.remove(id);
            if (removed != null && removed.getTask() != null && !removed.getTask().isCancelled()) {
                removed.getTask().cancel();
            }
            if (playerData.isEmpty()) {
                playerTitles.remove(player.getUniqueId());
            }
        }
    }

    // ==================== API DE GESTIÓN ====================

    /**
     * Cancela un título específico
     */
    public static boolean cancelTitle(Player player, String titleId) {
        TitleInstance instance = getTitleInstance(player, titleId);
        if (instance == null) return false;

        // Ejecutar callback de cancelación si es CountdownTitleInstance
        if (instance instanceof CountdownTitleInstance countdownInstance) {
            if (countdownInstance.getOnCancel() != null) {
                try {
                    countdownInstance.getOnCancel().run();
                } catch (Exception e) {
                    plugin.getLogger().warning("Error ejecutando callback de cancelación: " + e.getMessage());
                }
            }
        }

        if (instance.getTask() != null && !instance.getTask().isCancelled()) {
            instance.getTask().cancel();
        }

        // Limpiar título enviando uno vacío
        MessageUtils.sendTitle(player, "", "", 0, 1, 0);

        removeTitleInstance(player, titleId);
        return true;
    }

    /**
     * Cancela todos los títulos de un jugador
     */
    public static int cancelAllTitles(Player player) {
        Map<String, TitleInstance> playerData = playerTitles.get(player.getUniqueId());
        if (playerData == null || playerData.isEmpty()) return 0;

        int count = 0;
        Set<String> titleIds = new HashSet<>(playerData.keySet());
        for (String titleId : titleIds) {
            if (cancelTitle(player, titleId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Verifica si un jugador tiene un título específico activo
     */
    public static boolean hasTitle(Player player, String titleId) {
        return getTitleInstance(player, titleId) != null;
    }

    /**
     * Obtiene todos los títulos activos de un jugador
     */
    public static Set<String> getActiveTitles(Player player) {
        Map<String, TitleInstance> playerData = playerTitles.get(player.getUniqueId());
        return playerData != null ? new HashSet<>(playerData.keySet()) : new HashSet<>();
    }

    /**
     * Actualiza el contexto de un título
     */
    public static boolean updateTitle(Player player, String titleId, ExyliaContext newContext) {
        TitleInstance instance = getTitleInstance(player, titleId);
        if (instance == null) return false;

        instance.updateContext(newContext);
        return true;
    }

    /**
     * Obtiene todos los títulos permanentes activos de un jugador
     */
    public static Set<String> getPermanentTitles(Player player) {
        Map<String, TitleInstance> playerData = playerTitles.get(player.getUniqueId());
        if (playerData == null) return new HashSet<>();

        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue().isPermanent())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Verifica si un jugador tiene títulos permanentes activos
     */
    public static boolean hasPermanentTitles(Player player) {
        return !getPermanentTitles(player).isEmpty();
    }

    /**
     * Cancela todos los títulos permanentes de un jugador
     */
    public static int cancelAllPermanentTitles(Player player) {
        Set<String> permanentTitles = getPermanentTitles(player);
        int count = 0;
        for (String titleId : permanentTitles) {
            if (cancelTitle(player, titleId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Limpia todos los títulos y recursos
     */
    public static void cleanup() {
        for (UUID playerId : new HashSet<>(playerTitles.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                cancelAllTitles(player);
            }
        }
        playerTitles.clear();
    }
}
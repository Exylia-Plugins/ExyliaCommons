package net.exylia.commons.utils.visuals;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.config.components.BossBarConfig;
import net.exylia.commons.utils.ColorUtils;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.TimeFormatter.timeFormatter;

public class BossbarUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, BossBarInstance>> playerBossBars = new ConcurrentHashMap<>();

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
    }

    // ==================== API SIMPLE ====================

    /**
     * Envía una boss bar usando configuración
     */
    public static String sendBossBar(Player player, BossBarConfig config, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        String bossBarId = generateBossBarId();

        ExyliaContext enrichedContext = context.copy()
                .withPlayer(player)
                .withCurrentTime()
                .put("bossbar_id", bossBarId);

        BossBarInstance instance = new BossBarInstance(bossBarId, config, enrichedContext);

        BukkitTask task = executeBossBar(player, instance);
        if (task != null) {
            instance.setTask(task);
        }
        storeBossBarInstance(player, bossBarId, instance);

        return bossBarId;
    }

    /**
     * Envía una boss bar sin contexto
     */
    public static String sendBossBar(Player player, BossBarConfig config) {
        return sendBossBar(player, config, ExyliaContext.create());
    }

    /**
     * Envía una boss bar con ID personalizado
     */
    public static String sendBossBar(Player player, String bossBarId, BossBarConfig config, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        if (hasBossBar(player, bossBarId)) {
            cancelBossBar(player, bossBarId);
        }

        ExyliaContext enrichedContext = context.copy()
                .withPlayer(player)
                .withCurrentTime()
                .put("bossbar_id", bossBarId);

        BossBarInstance instance = new BossBarInstance(bossBarId, config, enrichedContext);

        BukkitTask task = executeBossBar(player, instance);
        if (task != null) {
            instance.setTask(task);
        }
        storeBossBarInstance(player, bossBarId, instance);

        return bossBarId;
    }

    // ==================== API BOSSBAR CON COUNTDOWN ====================

    /**
     * Envía una boss bar con countdown automático
     * @param player El jugador
     * @param bossBarId ID de la boss bar
     * @param config Configuración de la boss bar (puede contener %time% y %progress% como placeholders)
     * @param durationTicks Duración del countdown en ticks
     * @param context Contexto adicional
     * @return ID de la boss bar creada
     */
    public static String sendCountdownBossBar(Player player, String bossBarId, BossBarConfig config, long durationTicks, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        // Cancelar boss bar existente si existe
        if (hasBossBar(player, bossBarId)) {
            cancelBossBar(player, bossBarId);
        }

        ExyliaContext enrichedContext = context.copy()
                .withPlayer(player)
                .withCurrentTime()
                .put("bossbar_id", bossBarId)
                .put("countdown_duration", durationTicks);

        CountdownBossBarInstance instance = new CountdownBossBarInstance(bossBarId, config, enrichedContext, durationTicks);

        BukkitTask task = executeCountdownBossBar(player, instance);
        if (task != null) {
            instance.setTask(task);
        }
        storeBossBarInstance(player, bossBarId, instance);

        return bossBarId;
    }

    /**
     * Envía una boss bar con countdown automático (versión simplificada)
     */
    public static String sendCountdownBossBar(Player player, BossBarConfig config, long durationTicks) {
        return sendCountdownBossBar(player, generateBossBarId(), config, durationTicks, ExyliaContext.create());
    }

    /**
     * Envía una boss bar con countdown usando duración en segundos
     */
    public static String sendCountdownBossBar(Player player, String bossBarId, BossBarConfig config, int durationSeconds, ExyliaContext context) {
        return sendCountdownBossBar(player, bossBarId, config, durationSeconds * 20L, context);
    }

    /**
     * Envía una boss bar con countdown usando duración en segundos (versión simplificada)
     */
    public static String sendCountdownBossBar(Player player, BossBarConfig config, int durationSeconds) {
        return sendCountdownBossBar(player, generateBossBarId(), config, durationSeconds * 20L, ExyliaContext.create());
    }

    // ==================== EJECUCIÓN COUNTDOWN ====================

    private static BukkitTask executeCountdownBossBar(Player player, CountdownBossBarInstance instance) {
        BossBarConfig config = instance.getConfig();

        // Crear boss bar inicial
        String processedText = processPlaceholders(config.getText(), player, instance.getContext());
        BossBar bossBar = MessageUtils.createBossBar(processedText, parseColor(config.getColor()), parseOverlay(config.getStyle()));
        bossBar.progress(1.0f); // Empezar con progreso completo
        instance.setBossBar(bossBar);
        MessageUtils.showPlayerBossBar(player, bossBar);

        return new BukkitRunnable() {
            private long ticksRemaining = instance.getDurationTicks();
            private final long totalTicks = instance.getDurationTicks();
            private long updateCount = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    removeBossBarInstance(player, instance.getId());
                    cancel();
                    return;
                }

                // Calcular tiempo restante y progreso
                long secondsRemaining = (ticksRemaining + 19) / 20; // Redondear hacia arriba
                double progress = totalTicks > 0 ? (double) ticksRemaining / totalTicks : 0.0;

                // Actualizar contexto con información del countdown
                ExyliaContext currentContext = instance.getContext().copy()
                        .put("time", secondsRemaining)
                        .put("time_formatted", timeFormatter.format(secondsRemaining))
                        .put("ticks_remaining", ticksRemaining)
                        .put("progress", progress)
                        .put("update_count", updateCount)
                        .put("countdown_active", true)
                        .withCurrentTime();

                // Procesar placeholders
                String processedText = processPlaceholders(config.getText(), player, currentContext);

                // Actualizar boss bar
                BossBar bossBar = instance.getBossBar();
                if (bossBar != null) {
                    bossBar.name(ColorUtils.parse(processedText));
                    bossBar.progress((float) Math.max(0.0, Math.min(1.0, progress)));
                }

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
                            plugin.getLogger().warning("Error ejecutando callback de countdown: " + e.getMessage());
                        }
                    }

                    removeBossBarInstance(player, instance.getId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Ejecutar cada tick para precisión
    }

    // ==================== CLASE COUNTDOWN BOSSBAR INSTANCE ====================

    @Getter
    public static class CountdownBossBarInstance extends BossBarInstance {
        private final long durationTicks;
        @Setter
        private Runnable onComplete;
        @Setter
        private Runnable onCancel;

        CountdownBossBarInstance(String id, BossBarConfig config, ExyliaContext context, long durationTicks) {
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

        public CountdownBossBarInstance onComplete(Runnable callback) {
            this.onComplete = callback;
            return this;
        }

        public CountdownBossBarInstance onCancel(Runnable callback) {
            this.onCancel = callback;
            return this;
        }
    }

    // ==================== API EXTENDIDA PARA COUNTDOWN ====================

    /**
     * Crea una boss bar de countdown con callback
     */
    public static String sendCountdownBossBar(Player player, String bossBarId, BossBarConfig config, int durationSeconds,
                                              ExyliaContext context, Runnable onComplete) {
        String id = sendCountdownBossBar(player, bossBarId, config, durationSeconds, context);

        BossBarInstance instance = getBossBarInstance(player, id);
        if (instance instanceof CountdownBossBarInstance) {
            ((CountdownBossBarInstance) instance).onComplete(onComplete);
        }

        return id;
    }

    /**
     * Crea una boss bar de countdown con callbacks de completado y cancelación
     */
    public static String sendCountdownBossBar(Player player, String bossBarId, BossBarConfig config, int durationSeconds,
                                              ExyliaContext context, Runnable onComplete, Runnable onCancel) {
        String id = sendCountdownBossBar(player, bossBarId, config, durationSeconds, context);

        BossBarInstance instance = getBossBarInstance(player, id);
        if (instance instanceof CountdownBossBarInstance) {
            CountdownBossBarInstance countdownInstance = (CountdownBossBarInstance) instance;
            countdownInstance.onComplete(onComplete);
            countdownInstance.onCancel(onCancel);
        }

        return id;
    }

    /**
     * Obtiene el tiempo restante de un countdown en segundos
     */
    public static int getCountdownTimeRemaining(Player player, String bossBarId) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        if (instance instanceof CountdownBossBarInstance) {
            ExyliaContext context = instance.getContext();
            Object timeRemaining = context.get("time");
            if (timeRemaining instanceof Number) {
                return ((Number) timeRemaining).intValue();
            }
        }
        return -1;
    }

    /**
     * Obtiene el progreso actual de un countdown (0.0 - 1.0)
     */
    public static double getCountdownProgress(Player player, String bossBarId) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        if (instance instanceof CountdownBossBarInstance) {
            ExyliaContext context = instance.getContext();
            Object progress = context.get("progress");
            if (progress instanceof Number) {
                return ((Number) progress).doubleValue();
            }
        }
        return -1.0;
    }

    /**
     * Verifica si una boss bar es de tipo countdown
     */
    public static boolean isCountdownBossBar(Player player, String bossBarId) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        return instance instanceof CountdownBossBarInstance;
    }

    /**
     * Obtiene todas las boss bars de countdown activas de un jugador
     */
    public static Set<String> getCountdownBossBars(Player player) {
        Map<String, BossBarInstance> playerData = playerBossBars.get(player.getUniqueId());
        if (playerData == null) return new HashSet<>();

        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof CountdownBossBarInstance)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    // ==================== EJECUCIÓN ====================

    private static BukkitTask executeBossBar(Player player, BossBarInstance instance) {
        BossBarConfig config = instance.getConfig();

        if (config.isPermanent()) {
            // Boss bar permanente que se actualiza periódicamente
            String processedText = processPlaceholders(config.getText(), player, instance.getContext());
            BossBar bossBar = MessageUtils.createBossBar(processedText, parseColor(config.getColor()), parseOverlay(config.getStyle()));
            bossBar.progress((float) config.getProgress());
            instance.setBossBar(bossBar);
            MessageUtils.showPlayerBossBar(player, bossBar);

            return new BukkitRunnable() {
                private long updateCount = 0;

                @Override
                public void run() {
                    if (!player.isOnline()) {
                        removeBossBarInstance(player, instance.getId());
                        cancel();
                        return;
                    }

                    ExyliaContext currentContext = instance.getContext().copy()
                            .put("update_count", updateCount)
                            .put("permanent_active", true)
                            .withCurrentTime();

                    String processedText = processPlaceholders(config.getText(), player, currentContext);
                    BossBar bossBar = instance.getBossBar();
                    if (bossBar != null) {
                        bossBar.name(ColorUtils.parse(processedText));
                    }

                    updateCount++;
                }
            }.runTaskTimer(plugin, 0L, config.getUpdateInterval());
        } else {
            // Boss bar estática (no se actualiza)
            String processedText = processPlaceholders(config.getText(), player, instance.getContext());
            BossBar bossBar = MessageUtils.createBossBar(processedText, parseColor(config.getColor()), parseOverlay(config.getStyle()));
            bossBar.progress((float) config.getProgress());
            instance.setBossBar(bossBar);
            MessageUtils.showPlayerBossBar(player, bossBar);

            return null; // No necesita task para boss bars estáticas
        }
    }

    // ==================== GESTIÓN DE INSTANCIAS ====================

    @Getter
    public static class BossBarInstance {
        private final String id;
        private final BossBarConfig config;
        private ExyliaContext context;
        @Setter
        private BukkitTask task;
        @Setter
        private BossBar bossBar;
        private final long createdAt;

        BossBarInstance(String id, BossBarConfig config, ExyliaContext context) {
            this.id = id;
            this.config = config;
            this.context = context;
            this.createdAt = System.currentTimeMillis();
        }

        public void updateContext(ExyliaContext newContext) {
            this.context = newContext != null ? newContext : ExyliaContext.create();
        }

        public boolean isActive() {
            return bossBar != null;
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

    private static String generateBossBarId() {
        return "bossbar_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private static BossBar.Color parseColor(String colorStr) {
        try {
            return BossBar.Color.valueOf(colorStr.toUpperCase());
        } catch (Exception e) {
            return BossBar.Color.BLUE;
        }
    }

    private static BossBar.Overlay parseOverlay(String overlayStr) {
        try {
            return BossBar.Overlay.valueOf(overlayStr.toUpperCase());
        } catch (Exception e) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    private static void storeBossBarInstance(Player player, String id, BossBarInstance instance) {
        playerBossBars.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(id, instance);
    }

    private static BossBarInstance getBossBarInstance(Player player, String id) {
        Map<String, BossBarInstance> playerData = playerBossBars.get(player.getUniqueId());
        return playerData != null ? playerData.get(id) : null;
    }

    private static void removeBossBarInstance(Player player, String id) {
        Map<String, BossBarInstance> playerData = playerBossBars.get(player.getUniqueId());
        if (playerData != null) {
            BossBarInstance removed = playerData.remove(id);
            if (removed != null) {
                if (removed.getTask() != null && !removed.getTask().isCancelled()) {
                    removed.getTask().cancel();
                }
                if (removed.getBossBar() != null) {
                    MessageUtils.hidePlayerBossBar(player, removed.getBossBar());
                }
            }
            if (playerData.isEmpty()) {
                playerBossBars.remove(player.getUniqueId());
            }
        }
    }

    // ==================== API DE GESTIÓN ====================

    /**
     * Cancela una boss bar específica
     */
    public static boolean cancelBossBar(Player player, String bossBarId) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        if (instance == null) return false;

        // Ejecutar callback de cancelación si es CountdownBossBarInstance
        if (instance instanceof CountdownBossBarInstance countdownInstance) {
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

        if (instance.getBossBar() != null) {
            MessageUtils.hidePlayerBossBar(player, instance.getBossBar());
        }

        removeBossBarInstance(player, bossBarId);
        return true;
    }

    /**
     * Cancela todas las boss bars de un jugador
     */
    public static int cancelAllBossBars(Player player) {
        Map<String, BossBarInstance> playerData = playerBossBars.get(player.getUniqueId());
        if (playerData == null || playerData.isEmpty()) return 0;

        int count = 0;
        Set<String> bossBarIds = new HashSet<>(playerData.keySet());
        for (String bossBarId : bossBarIds) {
            if (cancelBossBar(player, bossBarId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Verifica si un jugador tiene una boss bar específica activa
     */
    public static boolean hasBossBar(Player player, String bossBarId) {
        return getBossBarInstance(player, bossBarId) != null;
    }

    /**
     * Obtiene todas las boss bars activas de un jugador
     */
    public static Set<String> getActiveBossBars(Player player) {
        Map<String, BossBarInstance> playerData = playerBossBars.get(player.getUniqueId());
        return playerData != null ? new HashSet<>(playerData.keySet()) : new HashSet<>();
    }

    /**
     * Actualiza el contexto de una boss bar
     */
    public static boolean updateBossBar(Player player, String bossBarId, ExyliaContext newContext) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        if (instance == null) return false;

        instance.updateContext(newContext);

        // Actualizar texto si la boss bar está activa
        if (instance.getBossBar() != null) {
            String newText = processPlaceholders(instance.getConfig().getText(), player, newContext);
            instance.getBossBar().name(ColorUtils.parse(newText));
        }

        return true;
    }

    /**
     * Actualiza el progreso de una boss bar
     */
    public static boolean updateBossBarProgress(Player player, String bossBarId, double newProgress) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        if (instance == null || instance.getBossBar() == null) return false;

        instance.getBossBar().progress((float) Math.max(0.0, Math.min(1.0, newProgress)));
        return true;
    }

    /**
     * Actualiza el color de una boss bar
     */
    public static boolean updateBossBarColor(Player player, String bossBarId, String newColor) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        if (instance == null || instance.getBossBar() == null) return false;

        instance.getBossBar().color(parseColor(newColor));
        return true;
    }

    /**
     * Obtiene todas las boss bars permanentes activas de un jugador
     */
    public static Set<String> getPermanentBossBars(Player player) {
        Map<String, BossBarInstance> playerData = playerBossBars.get(player.getUniqueId());
        if (playerData == null) return new HashSet<>();

        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue().isPermanent())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Verifica si un jugador tiene boss bars permanentes activos
     */
    public static boolean hasPermanentBossBars(Player player) {
        return !getPermanentBossBars(player).isEmpty();
    }

    /**
     * Cancela todas las boss bars permanentes de un jugador
     */
    public static int cancelAllPermanentBossBars(Player player) {
        Set<String> permanentBossBars = getPermanentBossBars(player);
        int count = 0;
        for (String bossBarId : permanentBossBars) {
            if (cancelBossBar(player, bossBarId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Limpia todos los datos al cerrar el plugin
     */
    public static void cleanup() {
        for (UUID playerId : new HashSet<>(playerBossBars.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                cancelAllBossBars(player);
            }
        }
        playerBossBars.clear();
    }
}
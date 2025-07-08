package net.exylia.commons.utils;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.config.components.BossBarConfig;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BossbarUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, BossBarInstance>> playerBossBars = new ConcurrentHashMap<>();
    private static final Map<String, BossBarTemplate> bossBarTemplates = new ConcurrentHashMap<>();
    private static final Map<BossBarEvent, List<Consumer<BossBarEventData>>> eventCallbacks = new ConcurrentHashMap<>();
    private static final Executor asyncExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "BossbarUtils-Async");
        t.setDaemon(true);
        return t;
    });

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
        initializeEventCallbacks();
    }

    // ==================== API MODERNA ====================

    /**
     * Crea un builder para boss bars modernos
     */
    public static BossBarBuilder create() {
        return new BossBarBuilder();
    }

    /**
     * Crea una boss bar desde configuración
     */
    public static BossBarBuilder fromConfig(BossBarConfig config) {
        return new BossBarBuilder().loadFromConfig(config);
    }

    /**
     * Crea y envía una boss bar simple con contexto
     */
    public static CompletableFuture<String> sendBossBar(Player player, String text, BossBar.Color color, BossBar.Overlay overlay, ExyliaContext context) {
        return create()
                .text(text)
                .color(color)
                .overlay(overlay)
                .context(context)
                .sendAsync(player);
    }

    /**
     * Crea y envía una boss bar simple sin contexto
     */
    public static CompletableFuture<String> sendBossBar(Player player, String text, BossBar.Color color, BossBar.Overlay overlay) {
        return sendBossBar(player, text, color, overlay, ExyliaContext.create());
    }

    /**
     * Crea y envía una boss bar con ID específico
     */
    public static CompletableFuture<String> sendBossBar(Player player, String bossBarId, String text, BossBar.Color color, BossBar.Overlay overlay, ExyliaContext context) {
        return create()
                .id(bossBarId)
                .text(text)
                .color(color)
                .overlay(overlay)
                .context(context)
                .sendAsync(player);
    }

    /**
     * Registra una plantilla de boss bar reutilizable
     */
    public static void registerTemplate(String templateId, BossBarTemplate template) {
        bossBarTemplates.put(templateId, template);
    }

    /**
     * Crea una boss bar desde una plantilla
     */
    public static CompletableFuture<String> createFromTemplate(Player player, String templateId, ExyliaContext context) {
        BossBarTemplate template = bossBarTemplates.get(templateId);
        if (template == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Template no encontrada: " + templateId));
        }

        return template.applyAsync(player, context);
    }

    /**
     * Actualiza una boss bar existente con nuevo contexto
     */
    public static CompletableFuture<Boolean> updateBossBar(Player player, String bossBarId, ExyliaContext newContext) {
        return CompletableFuture.supplyAsync(() -> {
            BossBarInstance instance = getBossBarInstance(player, bossBarId);
            if (instance == null) return false;

            instance.updateContext(newContext);
            return true;
        }, asyncExecutor);
    }

    // ==================== BUILDER PATTERN MODERNIZADO ====================

    public static class BossBarBuilder {
        private String text = "";
        private ExyliaContext context = ExyliaContext.create();
        private BossBarType type = BossBarType.STATIC;
        private BossBar.Color color = BossBar.Color.BLUE;
        private BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;
        private float progress = 1.0f;

        // Configuraciones específicas por tipo
        private double duration = 10.0;
        private long updateInterval = 5L;
        private boolean decreasing = true;
        private boolean autoHide = true;
        private double countdownTime = 10.0;
        private String countdownFormat = "%time%";
        private BossBarAnimation animation = BossBarAnimation.NONE;
        private long animationSpeed = 10;
        private double animationDuration = 10.0;
        private boolean loop = false;
        private long refreshInterval = 60L;

        // Progress específico
        private double progressCurrent = 0.0;
        private double progressMax = 100.0;

        // Callbacks modernos
        private Consumer<BossBarEventData> onStart;
        private Consumer<BossBarEventData> onUpdate;
        private Consumer<BossBarEventData> onComplete;
        private Consumer<BossBarEventData> onCancel;

        // Configuración asíncrona
        private boolean asyncMode = true;

        // ID personalizado
        private String customId = null;

        // ===== CONFIGURACIÓN BÁSICA =====

        public BossBarBuilder text(String text) {
            this.text = text != null ? text : "";
            return this;
        }

        public BossBarBuilder context(ExyliaContext context) {
            this.context = context != null ? context : ExyliaContext.create();
            return this;
        }

        public BossBarBuilder addToContext(Object... objects) {
            this.context.addAll(objects);
            return this;
        }

        public BossBarBuilder color(BossBar.Color color) {
            this.color = color != null ? color : BossBar.Color.BLUE;
            return this;
        }

        public BossBarBuilder overlay(BossBar.Overlay overlay) {
            this.overlay = overlay != null ? overlay : BossBar.Overlay.PROGRESS;
            return this;
        }

        public BossBarBuilder progress(float progress) {
            this.progress = Math.max(0.0f, Math.min(1.0f, progress));
            return this;
        }

        // ===== TIPOS DE BOSS BAR =====

        public BossBarBuilder staticBar() {
            this.type = BossBarType.STATIC;
            return this;
        }

        public BossBarBuilder timed(double duration, long updateInterval, boolean decreasing, boolean autoHide) {
            this.type = BossBarType.TIMED;
            this.duration = Math.max(0.1, duration);
            this.updateInterval = Math.max(1, updateInterval);
            this.decreasing = decreasing;
            this.autoHide = autoHide;
            return this;
        }

        public BossBarBuilder timed(double duration) {
            return timed(duration, 20L, true, true);
        }

        public BossBarBuilder countdown(double timeInSeconds, String format) {
            this.type = BossBarType.COUNTDOWN;
            this.countdownTime = Math.max(0.1, timeInSeconds);
            this.countdownFormat = format != null ? format : "%time%";
            return this;
        }

        public BossBarBuilder countdown(double timeInSeconds) {
            return countdown(timeInSeconds, "%time%");
        }

        public BossBarBuilder animated(BossBarAnimation animation, long speed, double duration, boolean loop) {
            this.type = BossBarType.ANIMATED;
            this.animation = animation != null ? animation : BossBarAnimation.NONE;
            this.animationSpeed = Math.max(1, speed);
            this.animationDuration = Math.max(0.1, duration);
            this.loop = loop;
            return this;
        }

        public BossBarBuilder animated(BossBarAnimation animation) {
            return animated(animation, 10, 10.0, false);
        }

        public BossBarBuilder permanent(long refreshInterval) {
            this.type = BossBarType.PERMANENT;
            this.refreshInterval = Math.max(1, refreshInterval);
            return this;
        }

        public BossBarBuilder permanent() {
            return permanent(60L);
        }

        public BossBarBuilder progressBar(double current, double max) {
            this.type = BossBarType.PROGRESS;
            this.progressCurrent = Math.max(0, current);
            this.progressMax = Math.max(1, max);
            this.progress = (float) Math.min(1.0, this.progressCurrent / this.progressMax);
            return this;
        }

        // ===== CALLBACKS =====

        public BossBarBuilder onStart(Consumer<BossBarEventData> callback) {
            this.onStart = callback;
            return this;
        }

        public BossBarBuilder onUpdate(Consumer<BossBarEventData> callback) {
            this.onUpdate = callback;
            return this;
        }

        public BossBarBuilder onComplete(Consumer<BossBarEventData> callback) {
            this.onComplete = callback;
            return this;
        }

        public BossBarBuilder onCancel(Consumer<BossBarEventData> callback) {
            this.onCancel = callback;
            return this;
        }

        // ===== CONFIGURACIÓN AVANZADA =====

        public BossBarBuilder async(boolean async) {
            this.asyncMode = async;
            return this;
        }

        public BossBarBuilder sync() {
            return async(false);
        }

        public BossBarBuilder async() {
            return async(true);
        }

        public BossBarBuilder id(String customId) {
            this.customId = customId;
            return this;
        }

        // ===== CARGAR DESDE CONFIGURACIÓN =====

        public BossBarBuilder loadFromConfig(BossBarConfig config) {
            if (config == null || !config.isEnabled()) {
                return this;
            }

            this.text = config.getText();
            this.color = config.getColor();
            this.overlay = config.getOverlay();
            this.progress = config.getProgress();
            this.updateInterval = config.getUpdateInterval();

            switch (config.getType()) {
                case TIMED ->
                        timed(config.getDuration(), config.getUpdateInterval(), config.isDecreasing(), config.isAutoHide());
                case COUNTDOWN -> countdown(config.getCountdownTime(), config.getCountdownFormat());
                case ANIMATED ->
                        animated(config.getAnimation(), config.getAnimationSpeed(), config.getDuration(), config.isLoop());
                case PERMANENT -> permanent(config.getRefreshInterval());
                case PROGRESS -> progressBar(config.getProgressCurrent(), config.getProgressMax());
                default -> staticBar();
            }

            return this;
        }

        // ===== MÉTODOS DE ENVÍO =====

        public CompletableFuture<String> sendAsync(Player player) {
            if (player == null || !player.isOnline()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Jugador inválido"));
            }

            return CompletableFuture.supplyAsync(() -> {
                String bossBarId = customId != null ? customId : generateBossBarId();

                // Verificar si ya existe una boss bar con este ID y cancelarla
                if (customId != null && hasBossBar(player, customId)) {
                    cancelBossBar(player, customId);
                }

                // Crear contexto enriquecido
                ExyliaContext enrichedContext = enrichContext(context, player);

                // Crear instancia de la boss bar
                BossBarInstance instance = new BossBarInstance(
                        bossBarId, type, text, enrichedContext, color, overlay, progress,
                        onStart, onUpdate, onComplete, onCancel
                );

                // Ejecutar según el tipo (en el hilo principal)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    BukkitTask task = executeBossBar(player, instance);
                    instance.setTask(task);
                    storeBossBarInstance(player, bossBarId, instance);
                    fireEvent(BossBarEvent.START, new BossBarEventData(player, bossBarId, instance, null));
                });

                return bossBarId;
            }, asyncMode ? asyncExecutor : Runnable::run);
        }

        public String send(Player player) {
            if (player == null || !player.isOnline()) {
                throw new IllegalArgumentException("Jugador inválido");
            }

            String bossBarId = customId != null ? customId : generateBossBarId();

            if (customId != null && hasBossBar(player, customId)) {
                cancelBossBar(player, customId);
            }

            ExyliaContext enrichedContext = enrichContext(context, player);

            BossBarInstance instance = new BossBarInstance(
                    bossBarId, type, text, enrichedContext, color, overlay, progress,
                    onStart, onUpdate, onComplete, onCancel
            );

            BukkitTask task = executeBossBar(player, instance);
            instance.setTask(task);
            storeBossBarInstance(player, bossBarId, instance);

            fireEvent(BossBarEvent.START, new BossBarEventData(player, bossBarId, instance, null));

            return bossBarId;
        }

        public BossBarConfiguration build() {
            return new BossBarConfiguration(this);
        }

        // ===== MÉTODOS AUXILIARES =====

        private ExyliaContext enrichContext(ExyliaContext originalContext, Player player) {
            String bossBarId = customId != null ? customId : generateBossBarId();
            return originalContext.copy()
                    .withPlayer(player)
                    .withCurrentTime()
                    .put("bossbar_id", bossBarId)
                    .put("server_name", plugin.getServer().getName());
        }

        private BukkitTask executeBossBar(Player player, BossBarInstance instance) {
            return switch (type) {
                case STATIC -> executeStatic(player, instance);
                case TIMED -> executeTimed(player, instance, duration, updateInterval, decreasing, autoHide);
                case COUNTDOWN -> executeCountdown(player, instance, countdownTime, countdownFormat);
                case ANIMATED -> executeAnimated(player, instance, animation, animationSpeed, animationDuration, loop);
                case PERMANENT -> executePermanent(player, instance, refreshInterval);
                case PROGRESS -> executeProgress(player, instance, progressCurrent, progressMax);
            };
        }
    }

    // ==================== EJECUCIÓN DE TIPOS DE BOSS BAR ====================

    private static BukkitTask executeStatic(Player player, BossBarInstance instance) {
        String processedText = processPlaceholders(instance.getOriginalText(), player, instance.getContext());

        BossBar bossBar = MessageUtils.createBossBar(processedText, instance.getColor(), instance.getOverlay());
        bossBar.progress(instance.getProgress());
        instance.setBossBar(bossBar);

        MessageUtils.showPlayerBossBar(player, bossBar);

        // Boss bar estática no se auto-remueve, permanece hasta ser cancelada manualmente
        return null;
    }

    private static BukkitTask executeTimed(Player player, BossBarInstance instance, double duration,
                                           long updateInterval, boolean decreasing, boolean autoHide) {
        String processedText = processPlaceholders(instance.getOriginalText(), player, instance.getContext());

        BossBar bossBar = MessageUtils.createBossBar(processedText, instance.getColor(), instance.getOverlay());
        bossBar.progress(decreasing ? 1.0f : 0.0f);
        instance.setBossBar(bossBar);

        MessageUtils.showPlayerBossBar(player, bossBar);

        long totalTicks = (long) (duration * 20);

        return new BukkitRunnable() {
            private long ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelBossBar(player, instance.getId());
                    return;
                }

                ticksElapsed += updateInterval;
                double progress = Math.min(1.0, (double) ticksElapsed / totalTicks);

                ExyliaContext currentContext = instance.getContext().copy()
                        .put("time_elapsed", ticksElapsed / 20.0)
                        .put("time_remaining", (totalTicks - ticksElapsed) / 20.0)
                        .put("time_percentage", progress * 100)
                        .put("time_formatted", TimeFormatter.quickDigital((totalTicks - ticksElapsed) / 20.0))
                        .put("time_progress", progress);

                String currentText = processPlaceholders(instance.getOriginalText(), player, currentContext);
                bossBar.name(ColorUtils.parse(currentText));

                if (decreasing) {
                    bossBar.progress((float) (1.0 - progress));
                } else {
                    bossBar.progress((float) progress);
                }

                fireEvent(BossBarEvent.UPDATE, new BossBarEventData(player, instance.getId(), instance,
                        Map.of("progress", progress, "timeElapsed", ticksElapsed / 20.0)));

                if (progress >= 1.0) {
                    if (autoHide) {
                        cancelBossBar(player, instance.getId());
                    } else {
                        fireEvent(BossBarEvent.COMPLETE, new BossBarEventData(player, instance.getId(), instance, null));
                        this.cancel();
                        removeBossBarInstance(player, instance.getId());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, updateInterval);
    }

    private static BukkitTask executeCountdown(Player player, BossBarInstance instance, double timeInSeconds, String format) {
        BossBar bossBar = MessageUtils.createBossBar("", instance.getColor(), instance.getOverlay());
        bossBar.progress(1.0f);
        instance.setBossBar(bossBar);

        MessageUtils.showPlayerBossBar(player, bossBar);

        return new BukkitRunnable() {
            private double timeLeft = timeInSeconds;

            @Override
            public void run() {
                if (!player.isOnline() || timeLeft <= 0) {
                    if (timeLeft <= 0 && player.isOnline()) {
                        ExyliaContext finalContext = instance.getContext().copy()
                                .put("time", "0")
                                .put("time_seconds", 0)
                                .put("time_raw", 0.0)
                                .put("finished", true)
                                .put("countdown_completed", true);

                        String finalText = processPlaceholders(instance.getOriginalText(), player, finalContext);
                        bossBar.name(ColorUtils.parse(finalText));
                        bossBar.progress(0.0f);
                    }
                    fireEvent(BossBarEvent.COMPLETE, new BossBarEventData(player, instance.getId(), instance,
                            Map.of("timeLeft", timeLeft, "completed", timeLeft <= 0)));
                    removeBossBarInstance(player, instance.getId());
                    cancel();
                    return;
                }

                ExyliaContext currentContext = instance.getContext().copy()
                        .put("time", formatTime(timeLeft))
                        .put("time_seconds", (int) timeLeft)
                        .put("time_raw", timeLeft)
                        .put("time_percentage", (timeLeft / timeInSeconds) * 100)
                        .put("time_progress", 1.0 - (timeLeft / timeInSeconds));

                String text = processPlaceholders(instance.getOriginalText(), player, currentContext);
                bossBar.name(ColorUtils.parse(text));
                bossBar.progress((float) (timeLeft / timeInSeconds));

                fireEvent(BossBarEvent.UPDATE, new BossBarEventData(player, instance.getId(), instance,
                        Map.of("timeLeft", timeLeft, "timePercentage", (timeLeft / timeInSeconds) * 100)));

                timeLeft -= 1.0;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private static BukkitTask executeAnimated(Player player, BossBarInstance instance, BossBarAnimation animation,
                                              long speed, double duration, boolean loop) {
        BossBar bossBar = MessageUtils.createBossBar("", instance.getColor(), instance.getOverlay());
        bossBar.progress(0.0f);
        instance.setBossBar(bossBar);

        MessageUtils.showPlayerBossBar(player, bossBar);

        return new BukkitRunnable() {
            private int step = 0;
            private long ticksElapsed = 0;
            private final long maxTicks = (long) (duration * 20);

            @Override
            public void run() {
                if (!player.isOnline() || (!loop && ticksElapsed >= maxTicks)) {
                    fireEvent(BossBarEvent.COMPLETE, new BossBarEventData(player, instance.getId(), instance, null));
                    removeBossBarInstance(player, instance.getId());
                    cancel();
                    return;
                }

                AnimationData animData = applyAnimation(instance.getOriginalText(), animation, step);

                ExyliaContext animContext = instance.getContext().copy()
                        .put("animation_step", step)
                        .put("animation_progress", (double) ticksElapsed / maxTicks)
                        .put("animation_percentage", ((double) ticksElapsed / maxTicks) * 100)
                        .put("animation_cycle", step / 10);

                String text = processPlaceholders(animData.text, player, animContext);
                bossBar.name(ColorUtils.parse(text));
                bossBar.progress(animData.progress);
                bossBar.color(animData.color != null ? animData.color : instance.getColor());

                fireEvent(BossBarEvent.UPDATE, new BossBarEventData(player, instance.getId(), instance,
                        Map.of("step", step, "progress", (double) ticksElapsed / maxTicks)));

                step++;
                ticksElapsed += speed;

                if (loop && ticksElapsed >= maxTicks) {
                    step = 0;
                    ticksElapsed = 0;
                }
            }
        }.runTaskTimer(plugin, 0L, speed);
    }

    private static BukkitTask executePermanent(Player player, BossBarInstance instance, long refreshInterval) {
        String processedText = processPlaceholders(instance.getOriginalText(), player, instance.getContext());

        BossBar bossBar = MessageUtils.createBossBar(processedText, instance.getColor(), instance.getOverlay());
        bossBar.progress(instance.getProgress());
        instance.setBossBar(bossBar);

        MessageUtils.showPlayerBossBar(player, bossBar);

        return new BukkitRunnable() {
            private long updateCount = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    fireEvent(BossBarEvent.COMPLETE, new BossBarEventData(player, instance.getId(), instance, null));
                    removeBossBarInstance(player, instance.getId());
                    cancel();
                    return;
                }

                ExyliaContext currentContext = instance.getContext().copy()
                        .put("update_count", updateCount)
                        .put("permanent_active", true)
                        .withCurrentTime();

                String text = processPlaceholders(instance.getOriginalText(), player, currentContext);
                bossBar.name(ColorUtils.parse(text));

                fireEvent(BossBarEvent.UPDATE, new BossBarEventData(player, instance.getId(), instance,
                        Map.of("updateCount", updateCount)));

                updateCount++;
            }
        }.runTaskTimer(plugin, 0L, refreshInterval);
    }

    private static BukkitTask executeProgress(Player player, BossBarInstance instance, double current, double max) {
        ProgressData progress = new ProgressData(current, max);

        ExyliaContext progressContext = instance.getContext().copy().add(progress);
        String text = processPlaceholders(instance.getOriginalText(), player, progressContext);

        BossBar bossBar = MessageUtils.createBossBar(text, instance.getColor(), instance.getOverlay());
        bossBar.progress((float) (current / max));
        instance.setBossBar(bossBar);

        MessageUtils.showPlayerBossBar(player, bossBar);

        // Progress bar estática, no se auto-remueve
        return null;
    }

    // ==================== SISTEMA DE EVENTOS ====================

    public enum BossBarEvent {
        START, UPDATE, COMPLETE, CANCEL
    }

    public static class BossBarEventData {
        @Getter
        private final Player player;
        @Getter
        private final String bossBarId;
        @Getter
        private final BossBarInstance instance;
        private final Map<String, Object> data;
        @Getter
        private final long timestamp;

        public BossBarEventData(Player player, String bossBarId, BossBarInstance instance, Map<String, Object> data) {
            this.player = player;
            this.bossBarId = bossBarId;
            this.instance = instance;
            this.data = data != null ? new HashMap<>(data) : new HashMap<>();
            this.timestamp = System.currentTimeMillis();
        }

        public Map<String, Object> getData() {
            return new HashMap<>(data);
        }

        @SuppressWarnings("unchecked")
        public <T> T getData(String key, Class<T> type) {
            Object value = data.get(key);
            return type.isInstance(value) ? (T) value : null;
        }

        public <T> T getData(String key, Class<T> type, T defaultValue) {
            T value = getData(key, type);
            return value != null ? value : defaultValue;
        }
    }

    public static void addEventListener(BossBarEvent event, Consumer<BossBarEventData> callback) {
        eventCallbacks.computeIfAbsent(event, k -> new ArrayList<>()).add(callback);
    }

    public static void removeEventListener(BossBarEvent event, Consumer<BossBarEventData> callback) {
        List<Consumer<BossBarEventData>> callbacks = eventCallbacks.get(event);
        if (callbacks != null) {
            callbacks.remove(callback);
        }
    }

    private static void fireEvent(BossBarEvent event, BossBarEventData data) {
        List<Consumer<BossBarEventData>> callbacks = eventCallbacks.get(event);
        if (callbacks != null) {
            for (Consumer<BossBarEventData> callback : new ArrayList<>(callbacks)) {
                try {
                    callback.accept(data);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error en callback global de evento de boss bar: " + e.getMessage());
                }
            }
        }

        Consumer<BossBarEventData> instanceCallback = switch (event) {
            case START -> data.getInstance().getOnStart();
            case UPDATE -> data.getInstance().getOnUpdate();
            case COMPLETE -> data.getInstance().getOnComplete();
            case CANCEL -> data.getInstance().getOnCancel();
        };

        if (instanceCallback != null) {
            try {
                instanceCallback.accept(data);
            } catch (Exception e) {
                plugin.getLogger().warning("Error en callback específico de boss bar: " + e.getMessage());
            }
        }
    }

    // ==================== CLASES DE SOPORTE ====================

    public static class ProgressData {
        public final double current;
        public final double max;

        public ProgressData(double current, double max) {
            this.current = Math.max(0, current);
            this.max = Math.max(1, max);
        }

        public double getPercentage() {
            return Math.min(100, Math.max(0, (current / max) * 100));
        }

        public boolean isComplete() {
            return current >= max;
        }

        public float getProgress() {
            return (float) Math.min(1.0, Math.max(0.0, current / max));
        }
    }

    public static class AnimationData {
        public final String text;
        public final float progress;
        public final BossBar.Color color;

        public AnimationData(String text, float progress, BossBar.Color color) {
            this.text = text;
            this.progress = Math.max(0.0f, Math.min(1.0f, progress));
            this.color = color;
        }

        public AnimationData(String text, float progress) {
            this(text, progress, null);
        }
    }

    public enum BossBarAnimation {
        NONE, WAVE, PULSE, RAINBOW, LOADING, HEARTBEAT, BREATHE
    }

    @FunctionalInterface
    public interface BossBarTemplate {
        CompletableFuture<String> applyAsync(Player player, ExyliaContext context);

        default String apply(Player player, ExyliaContext context) {
            try {
                return applyAsync(player, context).get();
            } catch (Exception e) {
                throw new RuntimeException("Error aplicando plantilla de boss bar", e);
            }
        }
    }

    public static class BossBarConfiguration {
        private final BossBarBuilder builder;

        BossBarConfiguration(BossBarBuilder builder) {
            this.builder = builder;
        }

        public CompletableFuture<String> applyAsync(Player player, ExyliaContext context) {
            return new BossBarBuilder()
                    .text(builder.text)
                    .color(builder.color)
                    .overlay(builder.overlay)
                    .progress(builder.progress)
                    .context(context.merge(builder.context))
                    .async(builder.asyncMode)
                    .sendAsync(player);
        }

        public String apply(Player player, ExyliaContext context) {
            return new BossBarBuilder()
                    .text(builder.text)
                    .color(builder.color)
                    .overlay(builder.overlay)
                    .progress(builder.progress)
                    .context(context.merge(builder.context))
                    .sync()
                    .send(player);
        }
    }

    @Getter
    public static class BossBarInstance {
        private final String id;
        private final BossBarType type;
        @Getter
        private final String originalText;
        private ExyliaContext context;
        private final BossBar.Color color;
        private final BossBar.Overlay overlay;
        private final float progress;
        @Setter
        private BukkitTask task;
        @Setter
        private BossBar bossBar;
        private final long createdAt;

        private final Consumer<BossBarEventData> onStart;
        private final Consumer<BossBarEventData> onUpdate;
        private final Consumer<BossBarEventData> onComplete;
        private final Consumer<BossBarEventData> onCancel;

        BossBarInstance(String id, BossBarType type, String originalText, ExyliaContext context,
                        BossBar.Color color, BossBar.Overlay overlay, float progress,
                        Consumer<BossBarEventData> onStart, Consumer<BossBarEventData> onUpdate,
                        Consumer<BossBarEventData> onComplete, Consumer<BossBarEventData> onCancel) {
            this.id = id;
            this.type = type;
            this.originalText = originalText;
            this.context = context;
            this.color = color;
            this.overlay = overlay;
            this.progress = progress;
            this.onStart = onStart;
            this.onUpdate = onUpdate;
            this.onComplete = onComplete;
            this.onCancel = onCancel;
            this.createdAt = System.currentTimeMillis();
        }

        public long getAge() {
            return System.currentTimeMillis() - createdAt;
        }

        public void updateContext(ExyliaContext newContext) {
            this.context = newContext != null ? newContext : ExyliaContext.create();
        }

        public boolean isActive() {
            return task != null && !task.isCancelled();
        }

        public boolean isPermanent() {
            return type == BossBarType.PERMANENT;
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private static void initializeEventCallbacks() {
        for (BossBarEvent event : BossBarEvent.values()) {
            eventCallbacks.put(event, new ArrayList<>());
        }
    }

    private static String processPlaceholders(String text, Player player, ExyliaContext context) {
        if (text == null || text.isEmpty()) return "";
        return PlaceholderSystemManager.getInstance().process(text, player, context.getAllObjects());
    }

    private static String generateBossBarId() {
        return "bossbar_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000, 9999);
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

    private static AnimationData applyAnimation(String baseText, BossBarAnimation animation, int step) {
        return switch (animation) {
            case WAVE -> {
                float progress = (float) (0.5 + 0.5 * Math.sin(step * 0.3));
                yield new AnimationData(baseText, progress);
            }
            case PULSE -> {
                float progress = (float) Math.abs(Math.sin(step * 0.2));
                yield new AnimationData(baseText, progress);
            }
            case RAINBOW -> {
                BossBar.Color[] colors = {BossBar.Color.RED, BossBar.Color.YELLOW, BossBar.Color.GREEN,
                        BossBar.Color.BLUE, BossBar.Color.PURPLE, BossBar.Color.PINK};
                BossBar.Color color = colors[step % colors.length];
                yield new AnimationData(baseText, 1.0f, color);
            }
            case LOADING -> {
                float progress = (step % 40) / 40.0f;
                yield new AnimationData(baseText, progress);
            }
            case HEARTBEAT -> {
                float progress = step % 4 < 2 ? 1.0f : 0.3f;
                yield new AnimationData(baseText, progress);
            }
            case BREATHE -> {
                float progress = (float) (0.3 + 0.7 * (Math.sin(step * 0.1) + 1) / 2);
                yield new AnimationData(baseText, progress);
            }
            default -> new AnimationData(baseText, 1.0f);
        };
    }

    private static String formatTime(double seconds) {
        if (seconds < 60) {
            return String.format("%.0f", seconds);
        } else if (seconds < 3600) {
            int minutes = (int) (seconds / 60);
            int secs = (int) (seconds % 60);
            return String.format("%d:%02d", minutes, secs);
        } else {
            int hours = (int) (seconds / 3600);
            int minutes = (int) ((seconds % 3600) / 60);
            return String.format("%d:%02d:00", hours, minutes);
        }
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

    // ==================== API DE GESTIÓN ====================

    /**
     * Cancela una boss bar específica de forma asíncrona
     */
    public static CompletableFuture<Boolean> cancelBossBarAsync(Player player, String bossBarId) {
        return CompletableFuture.supplyAsync(() -> cancelBossBar(player, bossBarId), asyncExecutor);
    }

    /**
     * Cancela una boss bar específica
     */
    public static boolean cancelBossBar(Player player, String bossBarId) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        if (instance == null) return false;

        if (instance.getTask() != null && !instance.getTask().isCancelled()) {
            instance.getTask().cancel();
        }

        if (instance.getBossBar() != null) {
            MessageUtils.hidePlayerBossBar(player, instance.getBossBar());
        }

        fireEvent(BossBarEvent.CANCEL, new BossBarEventData(player, bossBarId, instance, null));
        removeBossBarInstance(player, bossBarId);

        return true;
    }

    /**
     * Cancela todas las boss bars de un jugador de forma asíncrona
     */
    public static CompletableFuture<Integer> cancelAllBossBarsAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> cancelAllBossBars(player), asyncExecutor);
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
     * Obtiene todas las boss bars activas de un jugador
     */
    public static Set<String> getActiveBossBars(Player player) {
        Map<String, BossBarInstance> playerData = playerBossBars.get(player.getUniqueId());
        return playerData != null ? new HashSet<>(playerData.keySet()) : new HashSet<>();
    }

    /**
     * Obtiene información detallada de boss bars activas
     */
    public static List<BossBarInfo> getBossBarInfos(Player player) {
        Map<String, BossBarInstance> playerData = playerBossBars.get(player.getUniqueId());
        if (playerData == null) return new ArrayList<>();

        return playerData.entrySet().stream()
                .map(entry -> new BossBarInfo(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Información de una boss bar activa
     */
    @Getter
    public static class BossBarInfo {
        private final String id;
        private final BossBarType type;
        private final long createdAt;
        private final boolean isActive;
        private final boolean isPermanent;
        private final BossBar.Color color;
        private final BossBar.Overlay overlay;

        BossBarInfo(String id, BossBarInstance instance) {
            this.id = id;
            this.type = instance.getType();
            this.createdAt = instance.getCreatedAt();
            this.isActive = instance.isActive();
            this.isPermanent = instance.isPermanent();
            this.color = instance.getColor();
            this.overlay = instance.getOverlay();
        }

        public long getAge() {
            return System.currentTimeMillis() - createdAt;
        }
    }

    /**
     * Obtiene todas las boss bars permanentes activas de un jugador
     */
    public static Set<String> getPermanentBossBars(Player player) {
        return getBossBarInfos(player).stream()
                .filter(BossBarInfo::isPermanent)
                .map(BossBarInfo::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Verifica si un jugador tiene una boss bar específica activa
     */
    public static boolean hasBossBar(Player player, String bossBarId) {
        return getBossBarInstance(player, bossBarId) != null;
    }

    /**
     * Verifica si un jugador tiene boss bars permanentes activas
     */
    public static boolean hasPermanentBossBars(Player player) {
        return !getPermanentBossBars(player).isEmpty();
    }

    /**
     * Actualiza el progreso de una boss bar
     */
    public static CompletableFuture<Boolean> updateBossBarProgress(Player player, String bossBarId, float newProgress) {
        return CompletableFuture.supplyAsync(() -> {
            BossBarInstance instance = getBossBarInstance(player, bossBarId);
            if (instance == null || instance.getBossBar() == null) return false;

            instance.getBossBar().progress(Math.max(0.0f, Math.min(1.0f, newProgress)));
            return true;
        }, asyncExecutor);
    }

    /**
     * Actualiza el texto de una boss bar
     */
    public static CompletableFuture<Boolean> updateBossBarText(Player player, String bossBarId, String newText) {
        return CompletableFuture.supplyAsync(() -> {
            BossBarInstance instance = getBossBarInstance(player, bossBarId);
            if (instance == null || instance.getBossBar() == null) return false;

            String processedText = processPlaceholders(newText, player, instance.getContext());
            instance.getBossBar().name(ColorUtils.parse(processedText));
            return true;
        }, asyncExecutor);
    }

    /**
     * Actualiza el color de una boss bar
     */
    public static CompletableFuture<Boolean> updateBossBarColor(Player player, String bossBarId, BossBar.Color newColor) {
        return CompletableFuture.supplyAsync(() -> {
            BossBarInstance instance = getBossBarInstance(player, bossBarId);
            if (instance == null || instance.getBossBar() == null) return false;

            instance.getBossBar().color(newColor);
            return true;
        }, asyncExecutor);
    }

    /**
     * Cancela todas las boss bars permanentes de un jugador
     */
    public static CompletableFuture<Integer> cancelAllPermanentBossBarsAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> permanentBossBars = getPermanentBossBars(player);
            int count = 0;
            for (String bossBarId : permanentBossBars) {
                if (cancelBossBar(player, bossBarId)) {
                    count++;
                }
            }
            return count;
        }, asyncExecutor);
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
        bossBarTemplates.clear();
        eventCallbacks.clear();
    }

    // ==================== MÉTODOS DE CONVENIENCIA MODERNOS ====================

    /**
     * Crea una boss bar de progreso moderno con ExyliaContext
     */
    public static CompletableFuture<String> createProgressBossBar(Player player, String textTemplate,
                                                                  double current, double max, BossBar.Color color, BossBar.Overlay overlay) {
        ProgressData progress = new ProgressData(current, max);
        ExyliaContext context = ExyliaContext.create().add(progress);

        return create()
                .text(textTemplate)
                .progressBar(current, max)
                .color(color)
                .overlay(overlay)
                .context(context)
                .sendAsync(player);
    }

    /**
     * Crea una boss bar de progreso con configuración por defecto
     */
    public static CompletableFuture<String> createProgressBossBar(Player player, String textTemplate,
                                                                  double current, double max) {
        return createProgressBossBar(player, textTemplate, current, max, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
    }

    /**
     * Actualiza una boss bar de progreso
     */
    public static CompletableFuture<Boolean> updateProgressBossBar(Player player, String bossBarId, double newCurrent, double max) {
        return CompletableFuture.supplyAsync(() -> {
            BossBarInstance instance = getBossBarInstance(player, bossBarId);
            if (instance == null) return false;

            ProgressData newProgress = new ProgressData(newCurrent, max);
            ExyliaContext newContext = instance.getContext().copy().add(newProgress);

            instance.updateContext(newContext);

            if (instance.getBossBar() != null) {
                instance.getBossBar().progress(newProgress.getProgress());
                String processedText = processPlaceholders(instance.getOriginalText(),
                        Bukkit.getPlayer(playerBossBars.entrySet().stream()
                                .filter(entry -> entry.getValue().containsValue(instance))
                                .map(Map.Entry::getKey)
                                .findFirst().orElse(null)), newContext);
                instance.getBossBar().name(ColorUtils.parse(processedText));
            }

            return true;
        }, asyncExecutor);
    }

    @Getter
    public static class CounterData {
        @Setter
        private long value;
        private final String name;

        public CounterData(String name, long initialValue) {
            this.name = name;
            this.value = initialValue;
        }

        public CounterData(String name) {
            this(name, 0);
        }

        public void increment() {
            value++;
        }

        public void decrement() {
            value--;
        }

        public void add(long amount) {
            value += amount;
        }
    }

    // ==================== ENUMS ====================

    private enum BossBarType {
        STATIC, TIMED, COUNTDOWN, ANIMATED, PERMANENT, PROGRESS
    }
}
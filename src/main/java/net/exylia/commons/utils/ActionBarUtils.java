package net.exylia.commons.utils;

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
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ActionBarUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, ActionBarInstance>> playerActionBars = new ConcurrentHashMap<>();
    private static final Map<String, ActionBarTemplate> actionBarTemplates = new ConcurrentHashMap<>();
    private static final Map<ActionBarEvent, List<Consumer<ActionBarEventData>>> eventCallbacks = new ConcurrentHashMap<>();
    private static final Executor asyncExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ActionBarUtils-Async");
        t.setDaemon(true);
        return t;
    });

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
        initializeEventCallbacks();
    }

    // ==================== API MODERNA ====================

    /**
     * Crea un builder para action bars modernos
     */
    public static ActionBarBuilder create() {
        return new ActionBarBuilder();
    }

    /**
     * Crea un action bar desde configuración
     */
    public static ActionBarBuilder fromConfig(ActionBarConfig config) {
        return new ActionBarBuilder().loadFromConfig(config);
    }

    /**
     * Crea y envía un action bar simple con contexto
     */
    public static CompletableFuture<String> sendActionBar(Player player, String text, ExyliaContext context) {
        return create()
                .text(text)
                .context(context)
                .sendAsync(player);
    }

    /**
     * Crea y envía un action bar simple sin contexto
     */
    public static CompletableFuture<String> sendActionBar(Player player, String text) {
        return sendActionBar(player, text, ExyliaContext.create());
    }

    /**
     * Crea y envía un action bar con ID específico
     */
    public static CompletableFuture<String> sendActionBar(Player player, String actionBarId, String text, ExyliaContext context) {
        return create()
                .id(actionBarId)
                .text(text)
                .context(context)
                .sendAsync(player);
    }

    /**
     * Registra una plantilla de action bar reutilizable
     */
    public static void registerTemplate(String templateId, ActionBarTemplate template) {
        actionBarTemplates.put(templateId, template);
    }

    /**
     * Crea un action bar desde una plantilla
     */
    public static CompletableFuture<String> createFromTemplate(Player player, String templateId, ExyliaContext context) {
        ActionBarTemplate template = actionBarTemplates.get(templateId);
        if (template == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Template no encontrada: " + templateId));
        }

        return template.applyAsync(player, context);
    }

    /**
     * Actualiza un action bar existente con nuevo contexto
     */
    public static CompletableFuture<Boolean> updateActionBar(Player player, String actionBarId, ExyliaContext newContext) {
        return CompletableFuture.supplyAsync(() -> {
            ActionBarInstance instance = getActionBarInstance(player, actionBarId);
            if (instance == null) return false;

            instance.updateContext(newContext);
            return true;
        }, asyncExecutor);
    }

    // ==================== BUILDER PATTERN MODERNIZADO ====================

    public static class ActionBarBuilder {
        private String text = "";
        private ExyliaContext context = ExyliaContext.create();
        private ActionBarType type = ActionBarType.SINGLE;

        // Configuraciones específicas por tipo
        private double duration = 5.0;
        private long updateInterval = 20L;
        private double countdownTime = 10.0;
        private String countdownFormat = "%time%";
        private ActionBarAnimation animation = ActionBarAnimation.NONE;
        private long animationSpeed = 10;
        private double animationDuration = 10.0;
        private boolean loop = false;
        private long refreshInterval = 60L;

        // Progress bar específico
        private double progressCurrent = 0.0;
        private double progressMax = 100.0;
        private int progressBarLength = 20;
        private char progressFilledChar = '█';
        private char progressEmptyChar = '░';

        // Callbacks modernos
        private Consumer<ActionBarEventData> onStart;
        private Consumer<ActionBarEventData> onUpdate;
        private Consumer<ActionBarEventData> onComplete;
        private Consumer<ActionBarEventData> onCancel;

        // Configuración asíncrona
        private boolean asyncMode = true;

        // ID personalizado
        private String customId = null;

        // ===== CONFIGURACIÓN BÁSICA =====

        public ActionBarBuilder text(String text) {
            this.text = text != null ? text : "";
            return this;
        }

        public ActionBarBuilder context(ExyliaContext context) {
            this.context = context != null ? context : ExyliaContext.create();
            return this;
        }

        public ActionBarBuilder addToContext(Object... objects) {
            this.context.addAll(objects);
            return this;
        }

        // ===== TIPOS DE ACTION BAR =====

        public ActionBarBuilder single() {
            this.type = ActionBarType.SINGLE;
            return this;
        }

        public ActionBarBuilder timed(double duration, long updateInterval) {
            this.type = ActionBarType.TIMED;
            this.duration = Math.max(0.1, duration);
            this.updateInterval = Math.max(1, updateInterval);
            return this;
        }

        public ActionBarBuilder timed(double duration) {
            return timed(duration, 20L);
        }

        public ActionBarBuilder countdown(double timeInSeconds, String format) {
            this.type = ActionBarType.COUNTDOWN;
            this.countdownTime = Math.max(0.1, timeInSeconds);
            this.countdownFormat = format != null ? format : "%time%";
            return this;
        }

        public ActionBarBuilder countdown(double timeInSeconds) {
            return countdown(timeInSeconds, "%time%");
        }

        public ActionBarBuilder animated(ActionBarAnimation animation, long speed, double duration, boolean loop) {
            this.type = ActionBarType.ANIMATED;
            this.animation = animation != null ? animation : ActionBarAnimation.NONE;
            this.animationSpeed = Math.max(1, speed);
            this.animationDuration = Math.max(0.1, duration);
            this.loop = loop;
            return this;
        }

        public ActionBarBuilder animated(ActionBarAnimation animation) {
            return animated(animation, 10, 10.0, false);
        }

        public ActionBarBuilder permanent(long refreshInterval) {
            this.type = ActionBarType.PERMANENT;
            this.refreshInterval = Math.max(1, refreshInterval);
            return this;
        }

        public ActionBarBuilder permanent() {
            return permanent(60L);
        }

        public ActionBarBuilder progress(double current, double max, int barLength, char filledChar, char emptyChar) {
            this.type = ActionBarType.PROGRESS;
            this.progressCurrent = Math.max(0, current);
            this.progressMax = Math.max(1, max);
            this.progressBarLength = Math.max(1, Math.min(50, barLength));
            this.progressFilledChar = filledChar;
            this.progressEmptyChar = emptyChar;
            return this;
        }

        public ActionBarBuilder progress(double current, double max) {
            return progress(current, max, 20, '█', '░');
        }

        // ===== CALLBACKS =====

        public ActionBarBuilder onStart(Consumer<ActionBarEventData> callback) {
            this.onStart = callback;
            return this;
        }

        public ActionBarBuilder onUpdate(Consumer<ActionBarEventData> callback) {
            this.onUpdate = callback;
            return this;
        }

        public ActionBarBuilder onComplete(Consumer<ActionBarEventData> callback) {
            this.onComplete = callback;
            return this;
        }

        public ActionBarBuilder onCancel(Consumer<ActionBarEventData> callback) {
            this.onCancel = callback;
            return this;
        }

        // ===== CONFIGURACIÓN AVANZADA =====

        public ActionBarBuilder async(boolean async) {
            this.asyncMode = async;
            return this;
        }

        public ActionBarBuilder sync() {
            return async(false);
        }

        public ActionBarBuilder async() {
            return async(true);
        }

        public ActionBarBuilder id(String customId) {
            this.customId = customId;
            return this;
        }

        // ===== CARGAR DESDE CONFIGURACIÓN =====

        public ActionBarBuilder loadFromConfig(ActionBarConfig config) {
            if (config == null || !config.isEnabled()) {
                return this;
            }

            this.text = config.getText();
            this.updateInterval = config.getUpdateInterval();

            switch (config.getType()) {
                case TIMED -> timed(config.getDuration(), config.getUpdateInterval());
                case COUNTDOWN -> countdown(config.getCountdownTime(), config.getCountdownFormat());
                case ANIMATED ->
                        animated(config.getAnimation(), config.getAnimationSpeed(), config.getDuration(), config.isLoop());
                case PERMANENT -> permanent(config.getRefreshInterval());
                case PROGRESS -> progress(config.getProgressCurrent(), config.getProgressMax(),
                        config.getProgressBarLength(), config.getProgressFilledChar(), config.getProgressEmptyChar());
                default -> single();
            }

            return this;
        }

        // ===== MÉTODOS DE ENVÍO =====

        public CompletableFuture<String> sendAsync(Player player) {
            if (player == null || !player.isOnline()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Jugador inválido"));
            }

            return CompletableFuture.supplyAsync(() -> {
                String actionBarId = customId != null ? customId : generateActionBarId();

                // Verificar si ya existe un action bar con este ID y cancelarlo
                if (customId != null && hasActionBar(player, customId)) {
                    cancelActionBar(player, customId);
                }

                // Crear contexto enriquecido
                ExyliaContext enrichedContext = enrichContext(context, player);

                // Crear instancia del action bar
                ActionBarInstance instance = new ActionBarInstance(
                        actionBarId, type, text, enrichedContext,
                        onStart, onUpdate, onComplete, onCancel
                );

                // Ejecutar según el tipo (en el hilo principal)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    BukkitTask task = executeActionBar(player, instance);
                    instance.setTask(task);
                    storeActionBarInstance(player, actionBarId, instance);
                    fireEvent(ActionBarEvent.START, new ActionBarEventData(player, actionBarId, instance, null));
                });

                return actionBarId;
            }, asyncMode ? asyncExecutor : Runnable::run);
        }

        public String send(Player player) {
            if (player == null || !player.isOnline()) {
                throw new IllegalArgumentException("Jugador inválido");
            }

            String actionBarId = customId != null ? customId : generateActionBarId();

            if (customId != null && hasActionBar(player, customId)) {
                cancelActionBar(player, customId);
            }

            ExyliaContext enrichedContext = enrichContext(context, player);

            ActionBarInstance instance = new ActionBarInstance(
                    actionBarId, type, text, enrichedContext,
                    onStart, onUpdate, onComplete, onCancel
            );

            BukkitTask task = executeActionBar(player, instance);
            instance.setTask(task);
            storeActionBarInstance(player, actionBarId, instance);

            fireEvent(ActionBarEvent.START, new ActionBarEventData(player, actionBarId, instance, null));

            return actionBarId;
        }

        public ActionBarConfiguration build() {
            return new ActionBarConfiguration(this);
        }

        // ===== MÉTODOS AUXILIARES =====

        private ExyliaContext enrichContext(ExyliaContext originalContext, Player player) {
            String actionBarId = customId != null ? customId : generateActionBarId();
            return originalContext.copy()
                    .withPlayer(player)
                    .withCurrentTime()
                    .put("actionbar_id", actionBarId)
                    .put("server_name", plugin.getServer().getName());
        }

        private BukkitTask executeActionBar(Player player, ActionBarInstance instance) {
            return switch (type) {
                case SINGLE -> executeSingle(player, instance);
                case TIMED -> executeTimed(player, instance, duration, updateInterval);
                case COUNTDOWN -> executeCountdown(player, instance, countdownTime, countdownFormat);
                case ANIMATED -> executeAnimated(player, instance, animation, animationSpeed, animationDuration, loop);
                case PERMANENT -> executePermanent(player, instance, refreshInterval);
                case PROGRESS -> executeProgress(player, instance, progressCurrent, progressMax,
                        progressBarLength, progressFilledChar, progressEmptyChar);
            };
        }
    }

    // ==================== EJECUCIÓN DE TIPOS DE ACTION BAR ====================

    private static BukkitTask executeSingle(Player player, ActionBarInstance instance) {
        String processedText = processPlaceholders(instance.getOriginalText(), player, instance.getContext());
        MessageUtils.sendActionBarAsync(player, processedText);

        return Bukkit.getScheduler().runTaskLater(plugin, () -> {
            fireEvent(ActionBarEvent.COMPLETE, new ActionBarEventData(player, instance.getId(), instance, null));
            removeActionBarInstance(player, instance.getId());
        }, 60L);
    }

    private static BukkitTask executeTimed(Player player, ActionBarInstance instance, double duration, long updateInterval) {
        return new BukkitRunnable() {
            private double timeElapsed = 0.0;
            private final double maxTime = duration;

            @Override
            public void run() {
                if (!player.isOnline() || timeElapsed >= maxTime) {
                    fireEvent(ActionBarEvent.COMPLETE, new ActionBarEventData(player, instance.getId(), instance, null));
                    removeActionBarInstance(player, instance.getId());
                    cancel();
                    return;
                }

                ExyliaContext currentContext = instance.getContext().copy()
                        .put("time_elapsed", timeElapsed)
                        .put("time_remaining", maxTime - timeElapsed)
                        .put("time_percentage", (timeElapsed / maxTime) * 100)
                        .put("time_progress", timeElapsed / maxTime);

                String text = processPlaceholders(instance.getOriginalText(), player, currentContext);
                MessageUtils.sendActionBarAsync(player, text);

                fireEvent(ActionBarEvent.UPDATE, new ActionBarEventData(player, instance.getId(), instance,
                        Map.of("timeElapsed", timeElapsed, "timeRemaining", maxTime - timeElapsed)));

                timeElapsed += (double) updateInterval / 20.0;
            }
        }.runTaskTimer(plugin, 0L, updateInterval);
    }

    private static BukkitTask executeCountdown(Player player, ActionBarInstance instance, double timeInSeconds, String format) {
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
                        MessageUtils.sendActionBarAsync(player, finalText);
                    }
                    fireEvent(ActionBarEvent.COMPLETE, new ActionBarEventData(player, instance.getId(), instance,
                            Map.of("timeLeft", timeLeft, "completed", timeLeft <= 0)));
                    removeActionBarInstance(player, instance.getId());
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
                MessageUtils.sendActionBarAsync(player, text);

                fireEvent(ActionBarEvent.UPDATE, new ActionBarEventData(player, instance.getId(), instance,
                        Map.of("timeLeft", timeLeft, "timePercentage", (timeLeft / timeInSeconds) * 100)));

                timeLeft -= 1.0;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private static BukkitTask executeAnimated(Player player, ActionBarInstance instance, ActionBarAnimation animation,
                                              long speed, double duration, boolean loop) {
        return new BukkitRunnable() {
            private int step = 0;
            private long ticksElapsed = 0;
            private final long maxTicks = (long) (duration * 20);

            @Override
            public void run() {
                if (!player.isOnline() || (!loop && ticksElapsed >= maxTicks)) {
                    fireEvent(ActionBarEvent.COMPLETE, new ActionBarEventData(player, instance.getId(), instance, null));
                    removeActionBarInstance(player, instance.getId());
                    cancel();
                    return;
                }

                String animatedText = applyAnimation(instance.getOriginalText(), animation, step);

                ExyliaContext animContext = instance.getContext().copy()
                        .put("animation_step", step)
                        .put("animation_progress", (double) ticksElapsed / maxTicks)
                        .put("animation_percentage", ((double) ticksElapsed / maxTicks) * 100)
                        .put("animation_cycle", step / 10);

                String text = processPlaceholders(animatedText, player, animContext);
                MessageUtils.sendActionBarAsync(player, text);

                fireEvent(ActionBarEvent.UPDATE, new ActionBarEventData(player, instance.getId(), instance,
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

    private static BukkitTask executePermanent(Player player, ActionBarInstance instance, long refreshInterval) {
        return new BukkitRunnable() {
            private long updateCount = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    fireEvent(ActionBarEvent.COMPLETE, new ActionBarEventData(player, instance.getId(), instance, null));
                    removeActionBarInstance(player, instance.getId());
                    cancel();
                    return;
                }

                ExyliaContext currentContext = instance.getContext().copy()
                        .put("update_count", updateCount)
                        .put("permanent_active", true)
                        .withCurrentTime();

                String text = processPlaceholders(instance.getOriginalText(), player, currentContext);
                MessageUtils.sendActionBarAsync(player, text);

                fireEvent(ActionBarEvent.UPDATE, new ActionBarEventData(player, instance.getId(), instance,
                        Map.of("updateCount", updateCount)));

                updateCount++;
            }
        }.runTaskTimer(plugin, 0L, refreshInterval);
    }

    private static BukkitTask executeProgress(Player player, ActionBarInstance instance, double current, double max,
                                              int barLength, char filledChar, char emptyChar) {
        ProgressData progress = new ProgressData(current, max, barLength, filledChar, emptyChar);

        ExyliaContext progressContext = instance.getContext().copy().add(progress);
        String text = processPlaceholders(instance.getOriginalText(), player, progressContext);
        MessageUtils.sendActionBarAsync(player, text);

        return Bukkit.getScheduler().runTaskLater(plugin, () -> {
            fireEvent(ActionBarEvent.COMPLETE, new ActionBarEventData(player, instance.getId(), instance, null));
            removeActionBarInstance(player, instance.getId());
        }, 60L);
    }

    // ==================== SISTEMA DE EVENTOS ====================

    public enum ActionBarEvent {
        START, UPDATE, COMPLETE, CANCEL
    }

    public static class ActionBarEventData {
        @Getter
        private final Player player;
        @Getter
        private final String actionBarId;
        @Getter
        private final ActionBarInstance instance;
        private final Map<String, Object> data;
        @Getter
        private final long timestamp;

        public ActionBarEventData(Player player, String actionBarId, ActionBarInstance instance, Map<String, Object> data) {
            this.player = player;
            this.actionBarId = actionBarId;
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

    public static void addEventListener(ActionBarEvent event, Consumer<ActionBarEventData> callback) {
        eventCallbacks.computeIfAbsent(event, k -> new ArrayList<>()).add(callback);
    }

    public static void removeEventListener(ActionBarEvent event, Consumer<ActionBarEventData> callback) {
        List<Consumer<ActionBarEventData>> callbacks = eventCallbacks.get(event);
        if (callbacks != null) {
            callbacks.remove(callback);
        }
    }

    private static void fireEvent(ActionBarEvent event, ActionBarEventData data) {
        List<Consumer<ActionBarEventData>> callbacks = eventCallbacks.get(event);
        if (callbacks != null) {
            for (Consumer<ActionBarEventData> callback : new ArrayList<>(callbacks)) {
                try {
                    callback.accept(data);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error en callback global de evento de action bar: " + e.getMessage());
                }
            }
        }

        Consumer<ActionBarEventData> instanceCallback = switch (event) {
            case START -> data.getInstance().getOnStart();
            case UPDATE -> data.getInstance().getOnUpdate();
            case COMPLETE -> data.getInstance().getOnComplete();
            case CANCEL -> data.getInstance().getOnCancel();
        };

        if (instanceCallback != null) {
            try {
                instanceCallback.accept(data);
            } catch (Exception e) {
                plugin.getLogger().warning("Error en callback específico de action bar: " + e.getMessage());
            }
        }
    }

    // ==================== CLASES DE SOPORTE ====================

    public static class ProgressData {
        public final double current;
        public final double max;
        public final int barLength;
        public final char filledChar;
        public final char emptyChar;

        public ProgressData(double current, double max, int barLength, char filledChar, char emptyChar) {
            this.current = Math.max(0, current);
            this.max = Math.max(1, max);
            this.barLength = Math.max(1, Math.min(50, barLength));
            this.filledChar = filledChar;
            this.emptyChar = emptyChar;
        }

        public ProgressData(double current, double max) {
            this(current, max, 20, '█', '░');
        }

        public double getPercentage() {
            return Math.min(100, Math.max(0, (current / max) * 100));
        }

        public boolean isComplete() {
            return current >= max;
        }

        public String getBar() {
            double percentage = getPercentage();
            int filled = (int) ((percentage / 100) * barLength);
            int empty = barLength - filled;

            return "&a" + String.valueOf(filledChar).repeat(filled) +
                    "&7" + String.valueOf(emptyChar).repeat(empty);
        }
    }

    public enum ActionBarAnimation {
        NONE, DOTS, LOADING, SPINNER, WAVE, BLINK, RAINBOW, TYPEWRITER
    }

    @FunctionalInterface
    public interface ActionBarTemplate {
        CompletableFuture<String> applyAsync(Player player, ExyliaContext context);

        default String apply(Player player, ExyliaContext context) {
            try {
                return applyAsync(player, context).get();
            } catch (Exception e) {
                throw new RuntimeException("Error aplicando plantilla de action bar", e);
            }
        }
    }

    public static class ActionBarConfiguration {
        private final ActionBarBuilder builder;

        ActionBarConfiguration(ActionBarBuilder builder) {
            this.builder = builder;
        }

        public CompletableFuture<String> applyAsync(Player player, ExyliaContext context) {
            return new ActionBarBuilder()
                    .text(builder.text)
                    .context(context.merge(builder.context))
                    .async(builder.asyncMode)
                    .sendAsync(player);
        }

        public String apply(Player player, ExyliaContext context) {
            return new ActionBarBuilder()
                    .text(builder.text)
                    .context(context.merge(builder.context))
                    .sync()
                    .send(player);
        }
    }

    @Getter
    public static class ActionBarInstance {
        private final String id;
        private final ActionBarType type;
        @Getter
        private final String originalText;
        private ExyliaContext context;
        @Setter
        private BukkitTask task;
        private final long createdAt;

        private final Consumer<ActionBarEventData> onStart;
        private final Consumer<ActionBarEventData> onUpdate;
        private final Consumer<ActionBarEventData> onComplete;
        private final Consumer<ActionBarEventData> onCancel;

        ActionBarInstance(String id, ActionBarType type, String originalText, ExyliaContext context,
                          Consumer<ActionBarEventData> onStart, Consumer<ActionBarEventData> onUpdate,
                          Consumer<ActionBarEventData> onComplete, Consumer<ActionBarEventData> onCancel) {
            this.id = id;
            this.type = type;
            this.originalText = originalText;
            this.context = context;
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
            return type == ActionBarType.PERMANENT;
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private static void initializeEventCallbacks() {
        for (ActionBarEvent event : ActionBarEvent.values()) {
            eventCallbacks.put(event, new ArrayList<>());
        }
    }

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

    private static String applyAnimation(String baseText, ActionBarAnimation animation, int step) {
        return switch (animation) {
            case DOTS -> {
                String[] dots = {"", ".", "..", "..."};
                yield baseText + dots[step % dots.length];
            }
            case LOADING -> {
                String[] loading = {"[   ]", "[▌  ]", "[██ ]", "[███]"};
                yield baseText.replace("%loading%", loading[step % loading.length]);
            }
            case SPINNER -> {
                String[] spinner = {"|", "/", "-", "\\"};
                yield baseText.replace("%spinner%", spinner[step % spinner.length]);
            }
            case WAVE -> {
                String[] wave = {"~", "~~", "~~~", "~~~~", "~~~~~", "~~~~", "~~~", "~~"};
                yield baseText.replace("%wave%", wave[step % wave.length]);
            }
            case BLINK -> (step % 2 == 0) ? baseText : "";
            case RAINBOW -> {
                String[] colors = {"&c", "&6", "&e", "&a", "&b", "&9", "&d"};
                String color = colors[step % colors.length];
                yield color + baseText;
            }
            case TYPEWRITER -> {
                if (baseText.isEmpty()) yield baseText;
                int visibleLength = step % (baseText.length() + 5);
                yield visibleLength <= baseText.length() ?
                        baseText.substring(0, Math.max(0, visibleLength)) : baseText;
            }
            default -> baseText;
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

    // ==================== API DE GESTIÓN ====================

    /**
     * Cancela un action bar específico de forma asíncrona
     */
    public static CompletableFuture<Boolean> cancelActionBarAsync(Player player, String actionBarId) {
        return CompletableFuture.supplyAsync(() -> cancelActionBar(player, actionBarId), asyncExecutor);
    }

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
        Bukkit.getScheduler().runTask(plugin, () ->
                MessageUtils.sendActionBarAsync(player, ""));

        fireEvent(ActionBarEvent.CANCEL, new ActionBarEventData(player, actionBarId, instance, null));
        removeActionBarInstance(player, actionBarId);

        return true;
    }

    /**
     * Cancela todos los action bars de un jugador de forma asíncrona
     */
    public static CompletableFuture<Integer> cancelAllActionBarsAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> cancelAllActionBars(player), asyncExecutor);
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
     * Obtiene todos los action bars activos de un jugador
     */
    public static Set<String> getActiveActionBars(Player player) {
        Map<String, ActionBarInstance> playerData = playerActionBars.get(player.getUniqueId());
        return playerData != null ? new HashSet<>(playerData.keySet()) : new HashSet<>();
    }

    /**
     * Obtiene información detallada de action bars activos
     */
    public static List<ActionBarInfo> getActionBarInfos(Player player) {
        Map<String, ActionBarInstance> playerData = playerActionBars.get(player.getUniqueId());
        if (playerData == null) return new ArrayList<>();

        return playerData.entrySet().stream()
                .map(entry -> new ActionBarInfo(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Información de un action bar activo
     */
    @Getter
    public static class ActionBarInfo {
        private final String id;
        private final ActionBarType type;
        private final long createdAt;
        private final boolean isActive;
        private final boolean isPermanent;

        ActionBarInfo(String id, ActionBarInstance instance) {
            this.id = id;
            this.type = instance.getType();
            this.createdAt = instance.getCreatedAt();
            this.isActive = instance.isActive();
            this.isPermanent = instance.isPermanent();
        }

        public long getAge() {
            return System.currentTimeMillis() - createdAt;
        }
    }

    /**
     * Obtiene todos los action bars permanentes activos de un jugador
     */
    public static Set<String> getPermanentActionBars(Player player) {
        return getActionBarInfos(player).stream()
                .filter(ActionBarInfo::isPermanent)
                .map(ActionBarInfo::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Verifica si un jugador tiene un action bar específico activo
     */
    public static boolean hasActionBar(Player player, String actionBarId) {
        return getActionBarInstance(player, actionBarId) != null;
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
    public static CompletableFuture<Integer> cancelAllPermanentActionBarsAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> permanentActionBars = getPermanentActionBars(player);
            int count = 0;
            for (String actionBarId : permanentActionBars) {
                if (cancelActionBar(player, actionBarId)) {
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
        for (UUID playerId : new HashSet<>(playerActionBars.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                cancelAllActionBars(player);
            }
        }

        playerActionBars.clear();
        actionBarTemplates.clear();
        eventCallbacks.clear();
    }

    // ==================== MÉTODOS DE CONVENIENCIA MODERNOS ====================

    /**
     * Crea un action bar de progreso moderno con ExyliaContext
     */
    public static CompletableFuture<String> createProgressActionBar(Player player, String textTemplate,
                                                                    double current, double max, int barLength, char filledChar, char emptyChar) {
        ProgressData progress = new ProgressData(current, max, barLength, filledChar, emptyChar);
        ExyliaContext context = ExyliaContext.create().add(progress);

        return create()
                .text(textTemplate)
                .progress(current, max, barLength, filledChar, emptyChar)
                .context(context)
                .sendAsync(player);
    }

    /**
     * Crea un action bar de progreso con configuración por defecto
     */
    public static CompletableFuture<String> createProgressActionBar(Player player, String textTemplate,
                                                                    double current, double max) {
        return createProgressActionBar(player, textTemplate, current, max, 20, '█', '░');
    }

    /**
     * Actualiza un action bar de progreso
     */
    public static CompletableFuture<Boolean> updateProgressActionBar(Player player, String actionBarId, double newCurrent, double max) {
        return CompletableFuture.supplyAsync(() -> {
            ActionBarInstance instance = getActionBarInstance(player, actionBarId);
            if (instance == null) return false;

            ProgressData newProgress = new ProgressData(newCurrent, max);
            ExyliaContext newContext = instance.getContext().copy().add(newProgress);

            instance.updateContext(newContext);
            return true;
        }, asyncExecutor);
    }

    /**
     * Crea un action bar con contador dinámico
     */
    public static CompletableFuture<String> createCounterActionBar(Player player, String text, String counterName, long initialValue) {
        CounterData counter = new CounterData(counterName, initialValue);
        ExyliaContext context = ExyliaContext.create().add(counter);

        return create()
                .text(text)
                .context(context)
                .permanent()
                .sendAsync(player);
    }

    /**
     * Actualiza un contador en un action bar
     */
    public static CompletableFuture<Boolean> updateCounter(Player player, String actionBarId, String counterName, long newValue) {
        return CompletableFuture.supplyAsync(() -> {
            ActionBarInstance instance = getActionBarInstance(player, actionBarId);
            if (instance == null) return false;

            CounterData counter = instance.getContext().find(CounterData.class);
            if (counter != null && counter.getName().equals(counterName)) {
                counter.setValue(newValue);
                return true;
            }
            return false;
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

    private enum ActionBarType {
        SINGLE, TIMED, COUNTDOWN, ANIMATED, PERMANENT, PROGRESS
    }
}
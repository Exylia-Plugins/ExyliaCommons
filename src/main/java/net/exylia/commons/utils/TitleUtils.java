package net.exylia.commons.utils;

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
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TitleUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, TitleInstance>> playerTitles = new ConcurrentHashMap<>();
    private static final Map<String, TitleTemplate> titleTemplates = new ConcurrentHashMap<>();
    private static final Map<TitleEvent, List<Consumer<TitleEventData>>> eventCallbacks = new ConcurrentHashMap<>();
    private static final Executor asyncExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "TitleUtils-Async");
        t.setDaemon(true);
        return t;
    });

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
        initializeEventCallbacks();
    }

    // ==================== API MODERNA ====================

    /**
     * Crea un builder para títulos modernos
     */
    public static TitleBuilder create() {
        return new TitleBuilder();
    }

    /**
     * Crea un título desde configuración
     */
    public static TitleBuilder fromConfig(TitleConfig config) {
        return new TitleBuilder().loadFromConfig(config);
    }

    /**
     * Crea y envía un título simple con contexto
     */
    public static CompletableFuture<String> sendTitle(Player player, String title, String subtitle, ExyliaContext context) {
        return create()
                .title(title)
                .subtitle(subtitle)
                .context(context)
                .sendAsync(player);
    }

    /**
     * Crea y envía un título simple sin contexto
     */
    public static CompletableFuture<String> sendTitle(Player player, String title, String subtitle) {
        return sendTitle(player, title, subtitle, ExyliaContext.create());
    }

    /**
     * Crea y envía un título simple con ID específico
     */
    public static CompletableFuture<String> sendTitle(Player player, String titleId, String title, String subtitle, ExyliaContext context) {
        return create()
                .id(titleId)
                .title(title)
                .subtitle(subtitle)
                .context(context)
                .sendAsync(player);
    }

    /**
     * Crea y envía un título simple con ID específico sin contexto
     */
    public static CompletableFuture<String> sendTitle(Player player, String titleId, String title, String subtitle) {
        return sendTitle(player, titleId, title, subtitle, ExyliaContext.create());
    }

    /**
     * Registra una plantilla de título reutilizable
     */
    public static void registerTemplate(String templateId, TitleTemplate template) {
        titleTemplates.put(templateId, template);
    }

    /**
     * Crea un título desde una plantilla
     */
    public static CompletableFuture<String> createFromTemplate(Player player, String templateId, ExyliaContext context) {
        TitleTemplate template = titleTemplates.get(templateId);
        if (template == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Template no encontrada: " + templateId));
        }

        return template.applyAsync(player, context);
    }

    /**
     * Actualiza un título existente con nuevo contexto
     */
    public static CompletableFuture<Boolean> updateTitle(Player player, String titleId, ExyliaContext newContext) {
        return CompletableFuture.supplyAsync(() -> {
            TitleInstance instance = getTitleInstance(player, titleId);
            if (instance == null) return false;

            instance.updateContext(newContext);
            return true;
        }, asyncExecutor);
    }

    // ==================== BUILDER PATTERN MODERNIZADO ====================

    public static class TitleBuilder {
        private String title = "";
        private String subtitle = "";
        private ExyliaContext context = ExyliaContext.create();
        private TitleType type = TitleType.SINGLE;
        private int fadeIn = 0;
        private int stay = 70;
        private int fadeOut = 10;

        // Configuraciones específicas por tipo
        private int repetitions = 3;
        private long delayBetween = 80;
        private double countdownTime = 10.0;
        private String countdownFormat = "%time%";
        private TitleAnimation animation = TitleAnimation.NONE;
        private long animationSpeed = 3;
        private double duration = 10.0;
        private boolean loop = false;
        private long refreshInterval = 60L;
        private final List<TitleStep> sequenceSteps = new ArrayList<>();

        // Callbacks modernos
        private Consumer<TitleEventData> onStart;
        private Consumer<TitleEventData> onUpdate;
        private Consumer<TitleEventData> onComplete;
        private Consumer<TitleEventData> onCancel;

        // Configuración asíncrona
        private boolean asyncMode = true;

        // ID personalizado
        private String customId = null;

        // ===== CONFIGURACIÓN BÁSICA =====

        public TitleBuilder title(String title) {
            this.title = title != null ? title : "";
            return this;
        }

        public TitleBuilder subtitle(String subtitle) {
            this.subtitle = subtitle != null ? subtitle : "";
            return this;
        }

        public TitleBuilder context(ExyliaContext context) {
            this.context = context != null ? context : ExyliaContext.create();
            return this;
        }

        public TitleBuilder addToContext(Object... objects) {
            this.context.addAll(objects);
            return this;
        }

        public TitleBuilder timing(int fadeIn, int stay, int fadeOut) {
            this.fadeIn = Math.max(0, fadeIn);
            this.stay = Math.max(1, stay);
            this.fadeOut = Math.max(0, fadeOut);
            return this;
        }

        public TitleBuilder fadeIn(int fadeIn) {
            this.fadeIn = Math.max(0, fadeIn);
            return this;
        }

        public TitleBuilder stay(int stay) {
            this.stay = Math.max(1, stay);
            return this;
        }

        public TitleBuilder fadeOut(int fadeOut) {
            this.fadeOut = Math.max(0, fadeOut);
            return this;
        }

        // ===== TIPOS DE TÍTULO =====

        public TitleBuilder single() {
            this.type = TitleType.SINGLE;
            return this;
        }

        public TitleBuilder repeated(int repetitions, long delayBetween) {
            this.type = TitleType.REPEATED;
            this.repetitions = Math.max(1, repetitions);
            this.delayBetween = Math.max(1, delayBetween);
            return this;
        }

        public TitleBuilder countdown(double timeInSeconds, String format) {
            this.type = TitleType.COUNTDOWN;
            this.countdownTime = Math.max(0.1, timeInSeconds);
            this.countdownFormat = format != null ? format : "%time%";
            return this;
        }

        public TitleBuilder countdown(double timeInSeconds) {
            return countdown(timeInSeconds, "%time%");
        }

        public TitleBuilder animated(TitleAnimation animation, long speed, double duration, boolean loop) {
            this.type = TitleType.ANIMATED;
            this.animation = animation != null ? animation : TitleAnimation.NONE;
            this.animationSpeed = Math.max(1, speed);
            this.duration = Math.max(0.1, duration);
            this.loop = loop;
            return this;
        }

        public TitleBuilder animated(TitleAnimation animation) {
            return animated(animation, 3, 10.0, false);
        }

        public TitleBuilder permanent(long refreshInterval) {
            this.type = TitleType.PERMANENT;
            this.refreshInterval = Math.max(1, refreshInterval);
            return this;
        }

        public TitleBuilder permanent() {
            return permanent(60L);
        }

        public TitleBuilder sequence() {
            this.type = TitleType.SEQUENCE;
            this.sequenceSteps.clear();
            return this;
        }

        public TitleBuilder addStep(String title, String subtitle, int fadeIn, int stay, int fadeOut, long delay) {
            this.sequenceSteps.add(new TitleStep(
                    title != null ? title : "",
                    subtitle != null ? subtitle : "",
                    Math.max(0, fadeIn),
                    Math.max(1, stay),
                    Math.max(0, fadeOut),
                    Math.max(0, delay)
            ));
            return this;
        }

        public TitleBuilder addStep(String title, String subtitle, long delay) {
            return addStep(title, subtitle, this.fadeIn, this.stay, this.fadeOut, delay);
        }

        public TitleBuilder addStep(String title, String subtitle) {
            return addStep(title, subtitle, 0);
        }

        // ===== CALLBACKS =====

        public TitleBuilder onStart(Consumer<TitleEventData> callback) {
            this.onStart = callback;
            return this;
        }

        public TitleBuilder onUpdate(Consumer<TitleEventData> callback) {
            this.onUpdate = callback;
            return this;
        }

        public TitleBuilder onComplete(Consumer<TitleEventData> callback) {
            this.onComplete = callback;
            return this;
        }

        public TitleBuilder onCancel(Consumer<TitleEventData> callback) {
            this.onCancel = callback;
            return this;
        }

        // ===== CONFIGURACIÓN AVANZADA =====

        public TitleBuilder async(boolean async) {
            this.asyncMode = async;
            return this;
        }

        public TitleBuilder sync() {
            return async(false);
        }

        public TitleBuilder async() {
            return async(true);
        }

        /**
         * Establece un ID personalizado para el título
         */
        public TitleBuilder id(String customId) {
            this.customId = customId;
            return this;
        }

        // ===== CARGAR DESDE CONFIGURACIÓN =====

        public TitleBuilder loadFromConfig(TitleConfig config) {
            if (config == null || !config.isEnabled()) {
                return this;
            }

            this.title = config.getTitle();
            this.subtitle = config.getSubtitle();
            this.fadeIn = config.getFadeIn();
            this.stay = config.getStay();
            this.fadeOut = config.getFadeOut();

            switch (config.getType()) {
                case REPEATED -> repeated(config.getRepetitions(), config.getDelayBetween());
                case COUNTDOWN -> countdown(config.getCountdownTime(), config.getCountdownFormat());
                case ANIMATED -> animated(config.getAnimation(), config.getAnimationSpeed(), config.getDuration(), config.isLoop());
                case PERMANENT -> permanent(config.getRefreshInterval());
                case SEQUENCE -> {
                    sequence();
                    for (TitleConfig.StepConfig step : config.getSteps()) {
                        addStep(step.getTitle(), step.getSubtitle(),
                                step.getFadeIn(), step.getStay(), step.getFadeOut(), step.getDelay());
                    }
                }
                default -> single();
            }

            return this;
        }

        // ===== MÉTODOS DE ENVÍO =====

        /**
         * Envía el título de forma asíncrona
         */
        public CompletableFuture<String> sendAsync(Player player) {
            if (player == null || !player.isOnline()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Jugador inválido"));
            }

            return CompletableFuture.supplyAsync(() -> {
                String titleId = customId != null ? customId : generateTitleId();

                // Verificar si ya existe un título con este ID y cancelarlo
                if (customId != null && hasTitle(player, customId)) {
                    cancelTitle(player, customId);
                }

                // Crear contexto enriquecido
                ExyliaContext enrichedContext = enrichContext(context, player);

                // Crear instancia del título
                TitleInstance instance = new TitleInstance(
                        titleId, type, title, subtitle, enrichedContext,
                        fadeIn, stay, fadeOut, onStart, onUpdate, onComplete, onCancel
                );

                // Ejecutar según el tipo (en el hilo principal)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    BukkitTask task = executeTitle(player, instance);
                    instance.setTask(task);
                    storeTitleInstance(player, titleId, instance);
                    fireEvent(TitleEvent.START, new TitleEventData(player, titleId, instance, null));
                });

                return titleId;
            }, asyncMode ? asyncExecutor : Runnable::run);
        }

        /**
         * Envía el título de forma síncrona
         */
        public String send(Player player) {
            if (player == null || !player.isOnline()) {
                throw new IllegalArgumentException("Jugador inválido");
            }

            String titleId = customId != null ? customId : generateTitleId();

            // Verificar si ya existe un título con este ID y cancelarlo
            if (customId != null && hasTitle(player, customId)) {
                cancelTitle(player, customId);
            }

            // Crear contexto enriquecido
            ExyliaContext enrichedContext = enrichContext(context, player);

            // Crear instancia del título
            TitleInstance instance = new TitleInstance(
                    titleId, type, title, subtitle, enrichedContext,
                    fadeIn, stay, fadeOut, onStart, onUpdate, onComplete, onCancel
            );

            // Ejecutar según el tipo
            BukkitTask task = executeTitle(player, instance);
            instance.setTask(task);
            storeTitleInstance(player, titleId, instance);

            // Disparar evento de inicio
            fireEvent(TitleEvent.START, new TitleEventData(player, titleId, instance, null));

            return titleId;
        }

        /**
         * Construye la configuración sin enviar (para plantillas)
         */
        public TitleConfiguration build() {
            return new TitleConfiguration(this);
        }

        // ===== MÉTODOS AUXILIARES =====

        private ExyliaContext enrichContext(ExyliaContext originalContext, Player player) {
            String titleId = customId != null ? customId : generateTitleId();
            return originalContext.copy()
                    .withPlayer(player)
                    .withCurrentTime()
                    .put("title_id", titleId)
                    .put("server_name", plugin.getServer().getName());
        }

        private BukkitTask executeTitle(Player player, TitleInstance instance) {
            return switch (type) {
                case SINGLE -> executeSingle(player, instance);
                case REPEATED -> executeRepeated(player, instance, repetitions, delayBetween);
                case COUNTDOWN -> executeCountdown(player, instance, countdownTime, countdownFormat);
                case ANIMATED -> executeAnimated(player, instance, animation, animationSpeed, duration, loop);
                case PERMANENT -> executePermanent(player, instance, refreshInterval);
                case SEQUENCE -> executeSequence(player, instance, sequenceSteps);
            };
        }
    }

    // ==================== EJECUCIÓN DE TIPOS DE TÍTULO (OPTIMIZADA) ====================

    private static BukkitTask executeSingle(Player player, TitleInstance instance) {
        // Procesar placeholders
        String processedTitle = processPlaceholders(instance.getOriginalTitle(), player, instance.getContext());
        String processedSubtitle = processPlaceholders(instance.getOriginalSubtitle(), player, instance.getContext());

        // Enviar título
        MessageUtils.sendTitleAsync(player, processedTitle, processedSubtitle,
                instance.getFadeIn(), instance.getStay(), instance.getFadeOut());

        // Auto-remover después del tiempo total
        long totalTime = instance.getFadeIn() + instance.getStay() + instance.getFadeOut() + 10;
        return Bukkit.getScheduler().runTaskLater(plugin, () -> {
            fireEvent(TitleEvent.COMPLETE, new TitleEventData(player, instance.getId(), instance, null));
            removeTitleInstance(player, instance.getId());
        }, totalTime);
    }

    private static BukkitTask executeRepeated(Player player, TitleInstance instance, int repetitions, long delayBetween) {
        return new BukkitRunnable() {
            private int currentRep = 0;

            @Override
            public void run() {
                if (!player.isOnline() || currentRep >= repetitions) {
                    fireEvent(TitleEvent.COMPLETE, new TitleEventData(player, instance.getId(), instance, null));
                    removeTitleInstance(player, instance.getId());
                    cancel();
                    return;
                }

                // Crear contexto actualizado para esta repetición
                ExyliaContext currentContext = instance.getContext().copy()
                        .put("repetition_current", currentRep + 1)
                        .put("repetition_total", repetitions)
                        .put("repetition_remaining", repetitions - currentRep - 1);

                // Procesar placeholders
                String title = processPlaceholders(instance.getOriginalTitle(), player, currentContext);
                String subtitle = processPlaceholders(instance.getOriginalSubtitle(), player, currentContext);

                MessageUtils.sendTitleAsync(player, title, subtitle,
                        instance.getFadeIn(), instance.getStay(), instance.getFadeOut());

                fireEvent(TitleEvent.UPDATE, new TitleEventData(player, instance.getId(), instance,
                        Map.of("repetition", currentRep + 1, "total", repetitions)));

                currentRep++;
            }
        }.runTaskTimer(plugin, 0L, delayBetween);
    }

    private static BukkitTask executeCountdown(Player player, TitleInstance instance, double timeInSeconds, String format) {
        return new BukkitRunnable() {
            private double timeLeft = timeInSeconds;

            @Override
            public void run() {
                if (!player.isOnline() || timeLeft <= 0) {
                    if (timeLeft <= 0 && player.isOnline()) {
                        // Título final con contexto completo
                        ExyliaContext finalContext = instance.getContext().copy()
                                .put("time", "0")
                                .put("time_seconds", 0)
                                .put("time_raw", 0.0)
                                .put("finished", true)
                                .put("countdown_completed", true);

                        String finalTitle = processPlaceholders(instance.getOriginalTitle(), player, finalContext);
                        String finalSubtitle = processPlaceholders(instance.getOriginalSubtitle(), player, finalContext);

                        MessageUtils.sendTitleAsync(player, finalTitle, finalSubtitle,
                                instance.getFadeIn(), instance.getStay(), instance.getFadeOut());
                    }
                    fireEvent(TitleEvent.COMPLETE, new TitleEventData(player, instance.getId(), instance,
                            Map.of("timeLeft", timeLeft, "completed", timeLeft <= 0)));
                    removeTitleInstance(player, instance.getId());
                    cancel();
                    return;
                }

                // Crear contexto actualizado con información de tiempo
                ExyliaContext currentContext = instance.getContext().copy()
                        .put("time", formatTime(timeLeft))
                        .put("time_seconds", (int) timeLeft)
                        .put("time_raw", timeLeft)
                        .put("time_percentage", (timeLeft / timeInSeconds) * 100)
                        .put("time_progress", 1.0 - (timeLeft / timeInSeconds));

                String title = processPlaceholders(instance.getOriginalTitle(), player, currentContext);
                String subtitle = processPlaceholders(instance.getOriginalSubtitle(), player, currentContext);

                MessageUtils.sendTitleAsync(player, title, subtitle,
                        instance.getFadeIn(), instance.getStay(), instance.getFadeOut());

                fireEvent(TitleEvent.UPDATE, new TitleEventData(player, instance.getId(), instance,
                        Map.of("timeLeft", timeLeft, "timePercentage", (timeLeft / timeInSeconds) * 100)));

                timeLeft -= 1.0;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private static BukkitTask executeAnimated(Player player, TitleInstance instance, TitleAnimation animation,
                                              long speed, double duration, boolean loop) {
        return new BukkitRunnable() {
            private int step = 0;
            private long ticksElapsed = 0;
            private final long maxTicks = (long) (duration * 20);

            @Override
            public void run() {
                if (!player.isOnline() || (!loop && ticksElapsed >= maxTicks)) {
                    fireEvent(TitleEvent.COMPLETE, new TitleEventData(player, instance.getId(), instance, null));
                    removeTitleInstance(player, instance.getId());
                    cancel();
                    return;
                }

                // Aplicar animación
                String[] animatedText = applyAnimation(instance.getOriginalTitle(), instance.getOriginalSubtitle(),
                        animation, step);

                // Crear contexto con información de animación
                ExyliaContext animContext = instance.getContext().copy()
                        .put("animation_step", step)
                        .put("animation_progress", (double) ticksElapsed / maxTicks)
                        .put("animation_percentage", ((double) ticksElapsed / maxTicks) * 100)
                        .put("animation_cycle", step / 10); // Ciclo cada 10 pasos

                String title = processPlaceholders(animatedText[0], player, animContext);
                String subtitle = processPlaceholders(animatedText[1], player, animContext);

                MessageUtils.sendTitleAsync(player, title, subtitle,
                        instance.getFadeIn(), instance.getStay(), instance.getFadeOut());

                fireEvent(TitleEvent.UPDATE, new TitleEventData(player, instance.getId(), instance,
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

    private static BukkitTask executePermanent(Player player, TitleInstance instance, long refreshInterval) {
        return new BukkitRunnable() {
            private long updateCount = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    fireEvent(TitleEvent.COMPLETE, new TitleEventData(player, instance.getId(), instance, null));
                    removeTitleInstance(player, instance.getId());
                    cancel();
                    return;
                }

                // Crear contexto con información de actualizaciones
                ExyliaContext currentContext = instance.getContext().copy()
                        .put("update_count", updateCount)
                        .put("permanent_active", true)
                        .withCurrentTime();

                String title = processPlaceholders(instance.getOriginalTitle(), player, currentContext);
                String subtitle = processPlaceholders(instance.getOriginalSubtitle(), player, currentContext);

                MessageUtils.sendTitleAsync(player, title, subtitle,
                        instance.getFadeIn(), instance.getStay(), instance.getFadeOut());

                fireEvent(TitleEvent.UPDATE, new TitleEventData(player, instance.getId(), instance,
                        Map.of("updateCount", updateCount)));

                updateCount++;
            }
        }.runTaskTimer(plugin, 0L, refreshInterval);
    }

    private static BukkitTask executeSequence(Player player, TitleInstance instance, List<TitleStep> steps) {
        return new BukkitRunnable() {
            private int currentStep = 0;
            private long nextExecutionTime = System.currentTimeMillis();

            @Override
            public void run() {
                if (!player.isOnline() || currentStep >= steps.size()) {
                    fireEvent(TitleEvent.COMPLETE, new TitleEventData(player, instance.getId(), instance, null));
                    removeTitleInstance(player, instance.getId());
                    cancel();
                    return;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime < nextExecutionTime) return;

                TitleStep step = steps.get(currentStep);

                // Crear contexto con información del paso
                ExyliaContext stepContext = instance.getContext().copy()
                        .put("step_number", currentStep + 1)
                        .put("total_steps", steps.size())
                        .put("step_remaining", steps.size() - currentStep - 1)
                        .put("step_percentage", ((double) (currentStep + 1) / steps.size()) * 100);

                String title = processPlaceholders(step.title(), player, stepContext);
                String subtitle = processPlaceholders(step.subtitle(), player, stepContext);

                MessageUtils.sendTitleAsync(player, title, subtitle,
                        step.fadeIn(), step.stay(), step.fadeOut());

                fireEvent(TitleEvent.UPDATE, new TitleEventData(player, instance.getId(), instance,
                        Map.of("currentStep", currentStep + 1, "totalSteps", steps.size())));

                nextExecutionTime = currentTime + (step.delay() * 50);
                currentStep++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==================== SISTEMA DE EVENTOS MEJORADO ====================

    public enum TitleEvent {
        START, UPDATE, COMPLETE, CANCEL
    }

    public static class TitleEventData {
        // Getters
        @Getter
        private final Player player;
        @Getter
        private final String titleId;
        @Getter
        private final TitleInstance instance;
        private final Map<String, Object> data;
        @Getter
        private final long timestamp;

        public TitleEventData(Player player, String titleId, TitleInstance instance, Map<String, Object> data) {
            this.player = player;
            this.titleId = titleId;
            this.instance = instance;
            this.data = data != null ? new HashMap<>(data) : new HashMap<>();
            this.timestamp = System.currentTimeMillis();
        }

        public Map<String, Object> getData() { return new HashMap<>(data); }

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

    public static void addEventListener(TitleEvent event, Consumer<TitleEventData> callback) {
        eventCallbacks.computeIfAbsent(event, k -> new ArrayList<>()).add(callback);
    }

    public static void removeEventListener(TitleEvent event, Consumer<TitleEventData> callback) {
        List<Consumer<TitleEventData>> callbacks = eventCallbacks.get(event);
        if (callbacks != null) {
            callbacks.remove(callback);
        }
    }

    private static void fireEvent(TitleEvent event, TitleEventData data) {
        // Ejecutar callbacks globales
        List<Consumer<TitleEventData>> callbacks = eventCallbacks.get(event);
        if (callbacks != null) {
            for (Consumer<TitleEventData> callback : new ArrayList<>(callbacks)) {
                try {
                    callback.accept(data);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error en callback global de evento de título: " + e.getMessage());
                }
            }
        }

        // Ejecutar callback específico de la instancia
        Consumer<TitleEventData> instanceCallback = switch (event) {
            case START -> data.getInstance().getOnStart();
            case UPDATE -> data.getInstance().getOnUpdate();
            case COMPLETE -> data.getInstance().getOnComplete();
            case CANCEL -> data.getInstance().getOnCancel();
        };

        if (instanceCallback != null) {
            try {
                instanceCallback.accept(data);
            } catch (Exception e) {
                plugin.getLogger().warning("Error en callback específico de título: " + e.getMessage());
            }
        }
    }

    // ==================== CLASES DE SOPORTE MODERNIZADAS ====================

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

        public void increment() { value++; }
        public void decrement() { value--; }

        public void add(long amount) { value += amount; }
    }

    public record TitleStep(String title, String subtitle, int fadeIn, int stay, int fadeOut, long delay) {
        public TitleStep {
            title = title != null ? title : "";
            subtitle = subtitle != null ? subtitle : "";
            fadeIn = Math.max(0, fadeIn);
            stay = Math.max(1, stay);
            fadeOut = Math.max(0, fadeOut);
            delay = Math.max(0, delay);
        }
    }

    public enum TitleAnimation {
        NONE, TYPEWRITER, FADE, RAINBOW, SHAKE, BOUNCE, GRADIENT, WAVE, BLINK
    }

    // ==================== PLANTILLAS ====================

    @FunctionalInterface
    public interface TitleTemplate {
        CompletableFuture<String> applyAsync(Player player, ExyliaContext context);

        default String apply(Player player, ExyliaContext context) {
            try {
                return applyAsync(player, context).get();
            } catch (Exception e) {
                throw new RuntimeException("Error aplicando plantilla de título", e);
            }
        }
    }

    public static class TitleConfiguration {
        private final TitleBuilder builder;

        TitleConfiguration(TitleBuilder builder) {
            this.builder = builder;
        }

        public CompletableFuture<String> applyAsync(Player player, ExyliaContext context) {
            return new TitleBuilder()
                    .title(builder.title)
                    .subtitle(builder.subtitle)
                    .context(context.merge(builder.context))
                    .timing(builder.fadeIn, builder.stay, builder.fadeOut)
                    .async(builder.asyncMode)
                    .sendAsync(player);
        }

        public String apply(Player player, ExyliaContext context) {
            return new TitleBuilder()
                    .title(builder.title)
                    .subtitle(builder.subtitle)
                    .context(context.merge(builder.context))
                    .timing(builder.fadeIn, builder.stay, builder.fadeOut)
                    .sync()
                    .send(player);
        }
    }

    // ==================== GESTIÓN DE INSTANCIAS MEJORADA ====================

    @Getter
    public static class TitleInstance {
        // Getters
        private final String id;
        private final TitleType type;
        @Getter
        private final String originalTitle;
        @Getter
        private final String originalSubtitle;
        private ExyliaContext context;
        private final int fadeIn, stay, fadeOut;
        @Setter
        private BukkitTask task;
        private final long createdAt;

        // Callbacks
        private final Consumer<TitleEventData> onStart;
        private final Consumer<TitleEventData> onUpdate;
        private final Consumer<TitleEventData> onComplete;
        private final Consumer<TitleEventData> onCancel;

        TitleInstance(String id, TitleType type, String originalTitle, String originalSubtitle,
                      ExyliaContext context, int fadeIn, int stay, int fadeOut,
                      Consumer<TitleEventData> onStart, Consumer<TitleEventData> onUpdate,
                      Consumer<TitleEventData> onComplete, Consumer<TitleEventData> onCancel) {
            this.id = id;
            this.type = type;
            this.originalTitle = originalTitle;
            this.originalSubtitle = originalSubtitle;
            this.context = context;
            this.fadeIn = fadeIn;
            this.stay = stay;
            this.fadeOut = fadeOut;
            this.onStart = onStart;
            this.onUpdate = onUpdate;
            this.onComplete = onComplete;
            this.onCancel = onCancel;
            this.createdAt = System.currentTimeMillis();
        }

        public long getAge() { return System.currentTimeMillis() - createdAt; }

        public void updateContext(ExyliaContext newContext) {
            this.context = newContext != null ? newContext : ExyliaContext.create();
        }

        public boolean isActive() {
            return task != null && !task.isCancelled();
        }

        public boolean isPermanent() {
            return type == TitleType.PERMANENT;
        }
    }

    // ==================== MÉTODOS AUXILIARES MEJORADOS ====================

    private static void initializeEventCallbacks() {
        for (TitleEvent event : TitleEvent.values()) {
            eventCallbacks.put(event, new ArrayList<>());
        }
    }

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

    private static String[] applyAnimation(String baseTitle, String baseSubtitle, TitleAnimation animation, int step) {
        return switch (animation) {
            case TYPEWRITER -> {
                String animTitle = !baseTitle.isEmpty() ?
                        baseTitle.substring(0, Math.min(step % (baseTitle.length() + 5), baseTitle.length())) : baseTitle;
                int subtitleStart = Math.max(0, (step - baseTitle.length()) % (baseSubtitle.length() + 5));
                String animSubtitle = !baseSubtitle.isEmpty() ?
                        baseSubtitle.substring(0, Math.min(subtitleStart, baseSubtitle.length())) : baseSubtitle;
                yield new String[]{animTitle, animSubtitle};
            }
            case FADE -> {
                String[] colors = {"&8", "&7", "&f", "&7", "&8"};
                String color = colors[step % colors.length];
                yield new String[]{color + baseTitle, color + baseSubtitle};
            }
            case RAINBOW -> {
                String[] colors = {"&c", "&6", "&e", "&a", "&b", "&9", "&d"};
                String color = colors[step % colors.length];
                yield new String[]{color + baseTitle, color + baseSubtitle};
            }
            case SHAKE -> {
                String[] spaces = {"", " ", "  ", " ", ""};
                String space = spaces[step % spaces.length];
                yield new String[]{space + baseTitle, space + baseSubtitle};
            }
            case BOUNCE -> {
                String effect = (step % 2 == 0) ? "&l" : "";
                yield new String[]{effect + baseTitle, effect + baseSubtitle};
            }
            case WAVE -> {
                String[] effects = {"", "&l", "&n", "&l", ""};
                String effect = effects[step % effects.length];
                yield new String[]{effect + baseTitle, effect + baseSubtitle};
            }
            case BLINK -> {
                String effect = (step % 4 < 2) ? "" : "&k";
                yield new String[]{effect + baseTitle, effect + baseSubtitle};
            }
            case GRADIENT -> {
                // Implementar gradiente de colores más avanzado
                String[] gradientColors = {"&c", "&6", "&e", "&a", "&b", "&9", "&d"};
                String color1 = gradientColors[step % gradientColors.length];
                String color2 = gradientColors[(step + 1) % gradientColors.length];
                yield new String[]{color1 + baseTitle + color2, color1 + baseSubtitle + color2};
            }
            default -> new String[]{baseTitle, baseSubtitle};
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

    // ==================== API DE GESTIÓN MEJORADA ====================

    /**
     * Cancela un título específico de forma asíncrona
     */
    public static CompletableFuture<Boolean> cancelTitleAsync(Player player, String titleId) {
        return CompletableFuture.supplyAsync(() -> cancelTitle(player, titleId), asyncExecutor);
    }

    /**
     * Cancela un título específico
     */
    public static boolean cancelTitle(Player player, String titleId) {
        TitleInstance instance = getTitleInstance(player, titleId);
        if (instance == null) return false;

        if (instance.getTask() != null && !instance.getTask().isCancelled()) {
            instance.getTask().cancel();
        }

        // Limpiar título actual
        Bukkit.getScheduler().runTask(plugin, () ->
                MessageUtils.sendTitleAsync(player, "", "", 0, 1, 0));

        fireEvent(TitleEvent.CANCEL, new TitleEventData(player, titleId, instance, null));
        removeTitleInstance(player, titleId);

        return true;
    }

    /**
     * Cancela todos los títulos de un jugador de forma asíncrona
     */
    public static CompletableFuture<Integer> cancelAllTitlesAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> cancelAllTitles(player), asyncExecutor);
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
     * Obtiene todos los títulos activos de un jugador
     */
    public static Set<String> getActiveTitles(Player player) {
        Map<String, TitleInstance> playerData = playerTitles.get(player.getUniqueId());
        return playerData != null ? new HashSet<>(playerData.keySet()) : new HashSet<>();
    }

    /**
     * Obtiene información detallada de títulos activos
     */
    public static List<TitleInfo> getTitleInfos(Player player) {
        Map<String, TitleInstance> playerData = playerTitles.get(player.getUniqueId());
        if (playerData == null) return new ArrayList<>();

        return playerData.entrySet().stream()
                .map(entry -> new TitleInfo(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Información de un título activo
     */
    @Getter
    public static class TitleInfo {
        private final String id;
        private final TitleType type;
        private final long createdAt;
        private final boolean isActive;
        private final boolean isPermanent;

        TitleInfo(String id, TitleInstance instance) {
            this.id = id;
            this.type = instance.getType();
            this.createdAt = instance.getCreatedAt();
            this.isActive = instance.isActive();
            this.isPermanent = instance.isPermanent();
        }
        public long getAge() { return System.currentTimeMillis() - createdAt; }
    }

    /**
     * Obtiene todos los títulos permanentes activos de un jugador
     */
    public static Set<String> getPermanentTitles(Player player) {
        return getTitleInfos(player).stream()
                .filter(TitleInfo::isPermanent)
                .map(TitleInfo::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Verifica si un jugador tiene un título específico activo
     */
    public static boolean hasTitle(Player player, String titleId) {
        return getTitleInstance(player, titleId) != null;
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
    public static CompletableFuture<Integer> cancelAllPermanentTitlesAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> permanentTitles = getPermanentTitles(player);
            int count = 0;
            for (String titleId : permanentTitles) {
                if (cancelTitle(player, titleId)) {
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
        // Cancelar todos los títulos activos
        for (UUID playerId : new HashSet<>(playerTitles.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                cancelAllTitles(player);
            }
        }

        playerTitles.clear();
        titleTemplates.clear();
        eventCallbacks.clear();
    }

    // ==================== MÉTODOS DE CONVENIENCIA MODERNOS ====================

    /**
     * Crea un título de progreso moderno con ExyliaContext
     */
    public static CompletableFuture<String> createProgressTitle(Player player, String titleTemplate, String subtitleTemplate,
                                                                double current, double max, int barLength, char filledChar, char emptyChar) {
        ProgressData progress = new ProgressData(current, max, barLength, filledChar, emptyChar);
        ExyliaContext context = ExyliaContext.create().add(progress);

        return create()
                .title(titleTemplate)
                .subtitle(subtitleTemplate)
                .context(context)
                .sendAsync(player);
    }

    /**
     * Crea un título de progreso con configuración por defecto
     */
    public static CompletableFuture<String> createProgressTitle(Player player, String titleTemplate, String subtitleTemplate,
                                                                double current, double max) {
        return createProgressTitle(player, titleTemplate, subtitleTemplate, current, max, 20, '█', '░');
    }

    /**
     * Actualiza un título de progreso
     */
    public static CompletableFuture<Boolean> updateProgressTitle(Player player, String titleId, double newCurrent, double max) {
        return CompletableFuture.supplyAsync(() -> {
            TitleInstance instance = getTitleInstance(player, titleId);
            if (instance == null) return false;

            ProgressData newProgress = new ProgressData(newCurrent, max);
            ExyliaContext newContext = instance.getContext().copy().add(newProgress);

            instance.updateContext(newContext);
            return true;
        }, asyncExecutor);
    }

    /**
     * Crea un título con contador dinámico
     */
    public static CompletableFuture<String> createCounterTitle(Player player, String title, String subtitle, String counterName, long initialValue) {
        CounterData counter = new CounterData(counterName, initialValue);
        ExyliaContext context = ExyliaContext.create().add(counter);

        return create()
                .title(title)
                .subtitle(subtitle)
                .context(context)
                .permanent()
                .sendAsync(player);
    }

    /**
     * Actualiza un contador en un título
     */
    public static CompletableFuture<Boolean> updateCounter(Player player, String titleId, String counterName, long newValue) {
        return CompletableFuture.supplyAsync(() -> {
            TitleInstance instance = getTitleInstance(player, titleId);
            if (instance == null) return false;

            CounterData counter = instance.getContext().find(CounterData.class);
            if (counter != null && counter.getName().equals(counterName)) {
                counter.setValue(newValue);
                return true;
            }
            return false;
        }, asyncExecutor);
    }
    // ==================== RECORDS Y ENUMS ====================

    private enum TitleType {
        SINGLE, REPEATED, COUNTDOWN, ANIMATED, SEQUENCE, PERMANENT
    }
}
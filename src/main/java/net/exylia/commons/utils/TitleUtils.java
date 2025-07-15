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
import java.util.stream.Collectors;

public class TitleUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, TitleInstance>> playerTitles = new ConcurrentHashMap<>();
    private static final Executor asyncExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "TitleUtils-Async");
        t.setDaemon(true);
        return t;
    });

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
    }

    // ==================== API SIMPLE ====================

    /**
     * Envía un título usando configuración
     */
    public static CompletableFuture<String> sendTitle(Player player, TitleConfig config, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Parámetros inválidos"));
        }

        return CompletableFuture.supplyAsync(() -> {
            String titleId = generateTitleId();

            // Crear contexto enriquecido
            ExyliaContext enrichedContext = context.copy()
                    .withPlayer(player)
                    .withCurrentTime()
                    .put("title_id", titleId);

            // Crear instancia
            TitleInstance instance = new TitleInstance(titleId, config, enrichedContext);

            // Ejecutar en hilo principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                BukkitTask task = executeTitle(player, instance);
                if (task != null) {
                    instance.setTask(task);
                }
                storeTitleInstance(player, titleId, instance);
            });

            return titleId;
        }, asyncExecutor);
    }

    /**
     * Envía un título sin contexto
     */
    public static CompletableFuture<String> sendTitle(Player player, TitleConfig config) {
        return sendTitle(player, config, ExyliaContext.create());
    }

    /**
     * Envía un título con ID personalizado
     */
    public static CompletableFuture<String> sendTitle(Player player, String titleId, TitleConfig config, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Parámetros inválidos"));
        }

        return CompletableFuture.supplyAsync(() -> {
            // Cancelar título existente si existe
            if (hasTitle(player, titleId)) {
                cancelTitle(player, titleId);
            }

            ExyliaContext enrichedContext = context.copy()
                    .withPlayer(player)
                    .withCurrentTime()
                    .put("title_id", titleId);

            TitleInstance instance = new TitleInstance(titleId, config, enrichedContext);

            Bukkit.getScheduler().runTask(plugin, () -> {
                BukkitTask task = executeTitle(player, instance);
                if (task != null) {
                    instance.setTask(task);
                }
                storeTitleInstance(player, titleId, instance);
            });

            return titleId;
        }, asyncExecutor);
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

                    MessageUtils.sendTitleAsync(player, processedTitle, processedSubtitle,
                            config.getFadeIn(), config.getStay(), config.getFadeOut());

                    updateCount++;
                }
            }.runTaskTimer(plugin, 0L, config.getRefreshInterval());
        } else {
            // Título normal (una vez)
            String processedTitle = processPlaceholders(config.getTitle(), player, instance.getContext());
            String processedSubtitle = processPlaceholders(config.getSubtitle(), player, instance.getContext());

            MessageUtils.sendTitleAsync(player, processedTitle, processedSubtitle,
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

        if (instance.getTask() != null && !instance.getTask().isCancelled()) {
            instance.getTask().cancel();
        }

        // Limpiar título enviando uno vacío
        Bukkit.getScheduler().runTask(plugin, () ->
                MessageUtils.sendTitleAsync(player, "", "", 0, 1, 0));

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
    public static CompletableFuture<Boolean> updateTitle(Player player, String titleId, ExyliaContext newContext) {
        return CompletableFuture.supplyAsync(() -> {
            TitleInstance instance = getTitleInstance(player, titleId);
            if (instance == null) return false;

            instance.updateContext(newContext);
            return true;
        }, asyncExecutor);
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
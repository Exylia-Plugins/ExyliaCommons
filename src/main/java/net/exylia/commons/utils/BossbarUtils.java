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

public class BossbarUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, BossBarInstance>> playerBossBars = new ConcurrentHashMap<>();
    private static final Executor asyncExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "BossbarUtils-Async");
        t.setDaemon(true);
        return t;
    });

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
    }

    // ==================== API SIMPLE ====================

    /**
     * Envía una boss bar usando configuración
     */
    public static CompletableFuture<String> sendBossBar(Player player, BossBarConfig config, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Parámetros inválidos"));
        }

        return CompletableFuture.supplyAsync(() -> {
            String bossBarId = generateBossBarId();

            ExyliaContext enrichedContext = context.copy()
                    .withPlayer(player)
                    .withCurrentTime()
                    .put("bossbar_id", bossBarId);

            BossBarInstance instance = new BossBarInstance(bossBarId, config, enrichedContext);

            Bukkit.getScheduler().runTask(plugin, () -> {
                BukkitTask task = executeBossBar(player, instance);
                if (task != null) {
                    instance.setTask(task);
                }
                storeBossBarInstance(player, bossBarId, instance);
            });

            return bossBarId;
        }, asyncExecutor);
    }

    /**
     * Envía una boss bar sin contexto
     */
    public static CompletableFuture<String> sendBossBar(Player player, BossBarConfig config) {
        return sendBossBar(player, config, ExyliaContext.create());
    }

    /**
     * Envía una boss bar con ID personalizado
     */
    public static CompletableFuture<String> sendBossBar(Player player, String bossBarId, BossBarConfig config, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Parámetros inválidos"));
        }

        return CompletableFuture.supplyAsync(() -> {
            if (hasBossBar(player, bossBarId)) {
                cancelBossBar(player, bossBarId);
            }

            ExyliaContext enrichedContext = context.copy()
                    .withPlayer(player)
                    .withCurrentTime()
                    .put("bossbar_id", bossBarId);

            BossBarInstance instance = new BossBarInstance(bossBarId, config, enrichedContext);

            Bukkit.getScheduler().runTask(plugin, () -> {
                BukkitTask task = executeBossBar(player, instance);
                if (task != null) {
                    instance.setTask(task);
                }
                storeBossBarInstance(player, bossBarId, instance);
            });

            return bossBarId;
        }, asyncExecutor);
    }

    // ==================== EJECUCIÓN ====================

    private static BukkitTask executeBossBar(Player player, BossBarInstance instance) {
        BossBarConfig config = instance.getConfig();

        // Procesar texto inicial
        String processedText = processPlaceholders(config.getText(), player, instance.getContext());

        // Crear y mostrar boss bar
        BossBar bossBar = MessageUtils.createBossBar(processedText, parseColor(config.getColor()), parseOverlay(config.getStyle()));
        bossBar.progress((float) config.getProgress());
        instance.setBossBar(bossBar);

        MessageUtils.showPlayerBossBar(player, bossBar);

        // Si es estática, no necesita task
        return null;
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
    public static CompletableFuture<Boolean> updateBossBar(Player player, String bossBarId, ExyliaContext newContext) {
        return CompletableFuture.supplyAsync(() -> {
            BossBarInstance instance = getBossBarInstance(player, bossBarId);
            if (instance == null) return false;

            instance.updateContext(newContext);

            // Actualizar texto si la boss bar está activa
            if (instance.getBossBar() != null) {
                Player targetPlayer = Bukkit.getPlayer(playerBossBars.entrySet().stream()
                        .filter(entry -> entry.getValue().containsValue(instance))
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(null));

                if (targetPlayer != null) {
                    String newText = processPlaceholders(instance.getConfig().getText(), targetPlayer, newContext);
                    instance.getBossBar().name(ColorUtils.parse(newText));
                }
            }

            return true;
        }, asyncExecutor);
    }

    /**
     * Actualiza el progreso de una boss bar
     */
    public static CompletableFuture<Boolean> updateBossBarProgress(Player player, String bossBarId, double newProgress) {
        return CompletableFuture.supplyAsync(() -> {
            BossBarInstance instance = getBossBarInstance(player, bossBarId);
            if (instance == null || instance.getBossBar() == null) return false;

            instance.getBossBar().progress((float) Math.max(0.0, Math.min(1.0, newProgress)));
            return true;
        }, asyncExecutor);
    }

    /**
     * Actualiza el color de una boss bar
     */
    public static CompletableFuture<Boolean> updateBossBarColor(Player player, String bossBarId, String newColor) {
        return CompletableFuture.supplyAsync(() -> {
            BossBarInstance instance = getBossBarInstance(player, bossBarId);
            if (instance == null || instance.getBossBar() == null) return false;

            instance.getBossBar().color(parseColor(newColor));
            return true;
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
    }
}
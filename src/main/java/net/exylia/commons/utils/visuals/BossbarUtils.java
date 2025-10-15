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

import static net.exylia.commons.utils.DebugUtils.logInternalWarn;
import static net.exylia.commons.utils.TimeFormatter.timeFormatter;

public class BossbarUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, BossBarInstance>> playerBossBars = new ConcurrentHashMap<>();

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
    }

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

    public static String sendBossBar(Player player, BossBarConfig config) {
        return sendBossBar(player, config, ExyliaContext.create());
    }

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

    public static String sendCountdownBossBar(Player player, String bossBarId, BossBarConfig config, long durationTicks, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

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

    public static String sendCountdownBossBar(Player player, BossBarConfig config, long durationTicks) {
        return sendCountdownBossBar(player, generateBossBarId(), config, durationTicks, ExyliaContext.create());
    }

    public static String sendCountdownBossBar(Player player, String bossBarId, BossBarConfig config, int durationSeconds, ExyliaContext context) {
        return sendCountdownBossBar(player, bossBarId, config, durationSeconds * 20L, context);
    }

    public static String sendCountdownBossBar(Player player, BossBarConfig config, int durationSeconds) {
        return sendCountdownBossBar(player, generateBossBarId(), config, durationSeconds * 20L, ExyliaContext.create());
    }

    private static BukkitTask executeCountdownBossBar(Player player, CountdownBossBarInstance instance) {
        BossBarConfig config = instance.getConfig();

        long totalTicks = instance.getDurationTicks();
        long initialSecondsRemaining = (totalTicks + 19) / 20;
        long initialMillisRemaining = totalTicks * 50;

        ExyliaContext initialContext = instance.getContext().copy()
                .put("time", initialSecondsRemaining)
                .put("time_formatted", timeFormatter.format(initialMillisRemaining))
                .put("ticks_remaining", totalTicks)
                .put("progress", 1.0)
                .put("update_count", 0L)
                .put("countdown_active", true)
                .withCurrentTime();

        String processedText = processPlaceholders(config.getText(), player, initialContext);
        BossBar bossBar = MessageUtils.createBossBar(processedText, parseColor(config.getColor()), parseOverlay(config.getStyle()));

        if (bossBar == null) {
            logInternalWarn("Failed to create BossBar for countdown");
            return null;
        }

        bossBar.progress(1.0f);
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

                long secondsRemaining = (ticksRemaining + 19) / 20;
                long millisRemaining = ticksRemaining * 50;
                double progress = totalTicks > 0 ? (double) ticksRemaining / totalTicks : 0.0;

                ExyliaContext currentContext = instance.getContext().copy()
                        .put("time", secondsRemaining)
                        .put("time_formatted", timeFormatter.format(millisRemaining))
                        .put("ticks_remaining", ticksRemaining)
                        .put("progress", progress)
                        .put("update_count", updateCount)
                        .put("countdown_active", true)
                        .withCurrentTime();

                String processedText = processPlaceholders(config.getText(), player, currentContext);

                BossBar currentBossBar = instance.getBossBar();
                if (currentBossBar != null) {
                    currentBossBar.name(ColorUtils.parse(processedText));
                    currentBossBar.progress((float) Math.max(0.0, Math.min(1.0, progress)));
                }

                ticksRemaining--;
                updateCount++;

                if (ticksRemaining < 0) {
                    if (instance.getOnComplete() != null) {
                        try {
                            instance.getOnComplete().run();
                        } catch (Exception e) {
                            logInternalWarn("Error ejecutando callback de countdown: " + e.getMessage());
                        }
                    }

                    removeBossBarInstance(player, instance.getId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

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
            return false;
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

    public static String sendCountdownBossBar(Player player, String bossBarId, BossBarConfig config, int durationSeconds,
                                              ExyliaContext context, Runnable onComplete) {
        String id = sendCountdownBossBar(player, bossBarId, config, durationSeconds, context);

        BossBarInstance instance = getBossBarInstance(player, id);
        if (instance instanceof CountdownBossBarInstance) {
            ((CountdownBossBarInstance) instance).onComplete(onComplete);
        }

        return id;
    }

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

    public static boolean isCountdownBossBar(Player player, String bossBarId) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        return instance instanceof CountdownBossBarInstance;
    }

    public static Set<String> getCountdownBossBars(Player player) {
        Map<String, BossBarInstance> playerData = playerBossBars.get(player.getUniqueId());
        if (playerData == null) return new HashSet<>();

        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof CountdownBossBarInstance)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static BukkitTask executeBossBar(Player player, BossBarInstance instance) {
        BossBarConfig config = instance.getConfig();

        if (config.isPermanent()) {
            String processedText = processPlaceholders(config.getText(), player, instance.getContext());
            BossBar bossBar = MessageUtils.createBossBar(processedText, parseColor(config.getColor()), parseOverlay(config.getStyle()));

            if (bossBar == null) {
                logInternalWarn("Failed to create BossBar");
                return null;
            }

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
                    BossBar currentBossBar = instance.getBossBar();
                    if (currentBossBar != null) {
                        currentBossBar.name(ColorUtils.parse(processedText));
                    }

                    updateCount++;
                }
            }.runTaskTimer(plugin, 0L, config.getUpdateInterval());
        } else {
            String processedText = processPlaceholders(config.getText(), player, instance.getContext());
            BossBar bossBar = MessageUtils.createBossBar(processedText, parseColor(config.getColor()), parseOverlay(config.getStyle()));

            if (bossBar == null) {
                logInternalWarn("Failed to create BossBar");
                return null;
            }

            bossBar.progress((float) config.getProgress());
            instance.setBossBar(bossBar);
            MessageUtils.showPlayerBossBar(player, bossBar);

            return null;
        }
    }

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

    public static boolean cancelBossBar(Player player, String bossBarId) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        if (instance == null) return false;

        if (instance instanceof CountdownBossBarInstance countdownInstance) {
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

        if (instance.getBossBar() != null) {
            MessageUtils.hidePlayerBossBar(player, instance.getBossBar());
        }

        removeBossBarInstance(player, bossBarId);
        return true;
    }

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

    public static boolean hasBossBar(Player player, String bossBarId) {
        return getBossBarInstance(player, bossBarId) != null;
    }

    public static Set<String> getActiveBossBars(Player player) {
        Map<String, BossBarInstance> playerData = playerBossBars.get(player.getUniqueId());
        return playerData != null ? new HashSet<>(playerData.keySet()) : new HashSet<>();
    }

    public static boolean updateBossBar(Player player, String bossBarId, ExyliaContext newContext) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        if (instance == null) return false;

        instance.updateContext(newContext);

        if (instance.getBossBar() != null) {
            String newText = processPlaceholders(instance.getConfig().getText(), player, newContext);
            instance.getBossBar().name(ColorUtils.parse(newText));
        }

        return true;
    }

    public static boolean updateBossBarProgress(Player player, String bossBarId, double newProgress) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        if (instance == null || instance.getBossBar() == null) return false;

        instance.getBossBar().progress((float) Math.max(0.0, Math.min(1.0, newProgress)));
        return true;
    }

    public static boolean updateBossBarColor(Player player, String bossBarId, String newColor) {
        BossBarInstance instance = getBossBarInstance(player, bossBarId);
        if (instance == null || instance.getBossBar() == null) return false;

        instance.getBossBar().color(parseColor(newColor));
        return true;
    }

    public static Set<String> getPermanentBossBars(Player player) {
        Map<String, BossBarInstance> playerData = playerBossBars.get(player.getUniqueId());
        if (playerData == null) return new HashSet<>();

        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue().isPermanent())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public static boolean hasPermanentBossBars(Player player) {
        return !getPermanentBossBars(player).isEmpty();
    }

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

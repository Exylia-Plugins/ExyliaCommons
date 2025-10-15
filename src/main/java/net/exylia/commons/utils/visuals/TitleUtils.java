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

import static net.exylia.commons.utils.DebugUtils.logInternalWarn;
import static net.exylia.commons.utils.TimeFormatter.timeFormatter;

public class TitleUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, TitleInstance>> playerTitles = new ConcurrentHashMap<>();

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
    }

    public static String sendTitle(Player player, TitleConfig config, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        String titleId = generateTitleId();

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

    public static String sendTitle(Player player, TitleConfig config) {
        return sendTitle(player, config, ExyliaContext.create());
    }

    public static String sendTitle(Player player, String titleId, TitleConfig config, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

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

    public static String sendCountdownTitle(Player player, String titleId, TitleConfig config, long durationTicks, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        if (hasTitle(player, titleId)) {
            cancelTitle(player, titleId);
        }

        ExyliaContext enrichedContext = context.copy()
                .withPlayer(player)
                .withCurrentTime()
                .put("title_id", titleId)
                .put("countdown_duration", durationTicks);

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

    public static String sendCountdownTitle(Player player, TitleConfig config, long durationTicks) {
        return sendCountdownTitle(player, generateTitleId(), config, durationTicks, ExyliaContext.create());
    }

    public static String sendCountdownTitle(Player player, String titleId, TitleConfig config, int durationSeconds, ExyliaContext context) {
        return sendCountdownTitle(player, titleId, config, durationSeconds * 20L, context);
    }

    public static String sendCountdownTitle(Player player, TitleConfig config, int durationSeconds) {
        return sendCountdownTitle(player, generateTitleId(), config, durationSeconds * 20L, ExyliaContext.create());
    }

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

                long secondsRemaining = (ticksRemaining + 19) / 20;  

                ExyliaContext currentContext = instance.getContext().copy()
                        .put("time", secondsRemaining)
                        .put("time_formatted", timeFormatter.format(secondsRemaining))
                        .put("ticks_remaining", ticksRemaining)
                        .put("update_count", updateCount)
                        .put("countdown_active", true)
                        .withCurrentTime();

                String processedTitle = processPlaceholders(config.getTitle(), player, currentContext);
                String processedSubtitle = processPlaceholders(config.getSubtitle(), player, currentContext);

                MessageUtils.sendTitle(player, processedTitle, processedSubtitle,
                        config.getFadeIn(), config.getStay(), config.getFadeOut());

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

                    removeTitleInstance(player, instance.getId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);  
    }

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
            return false;  
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

    public static String sendCountdownTitle(Player player, String titleId, TitleConfig config, int durationSeconds,
                                            ExyliaContext context, Runnable onComplete) {
        String id = sendCountdownTitle(player, titleId, config, durationSeconds, context);

        TitleInstance instance = getTitleInstance(player, id);
        if (instance instanceof CountdownTitleInstance) {
            ((CountdownTitleInstance) instance).onComplete(onComplete);
        }

        return id;
    }

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

    public static boolean isCountdownTitle(Player player, String titleId) {
        TitleInstance instance = getTitleInstance(player, titleId);
        return instance instanceof CountdownTitleInstance;
    }

    public static Set<String> getCountdownTitles(Player player) {
        Map<String, TitleInstance> playerData = playerTitles.get(player.getUniqueId());
        if (playerData == null) return new HashSet<>();

        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof CountdownTitleInstance)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static BukkitTask executeTitle(Player player, TitleInstance instance) {
        TitleConfig config = instance.getConfig();

        if (config.isPermanent()) {
             
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
            }.runTaskTimer(plugin, 0L, config.getUpdateInterval());
        } else {
             
            String processedTitle = processPlaceholders(config.getTitle(), player, instance.getContext());
            String processedSubtitle = processPlaceholders(config.getSubtitle(), player, instance.getContext());

            MessageUtils.sendTitle(player, processedTitle, processedSubtitle,
                    config.getFadeIn(), config.getStay(), config.getFadeOut());

            long totalTime = config.getFadeIn() + config.getStay() + config.getFadeOut() + 10;
            return Bukkit.getScheduler().runTaskLater(plugin, () -> {
                removeTitleInstance(player, instance.getId());
            }, totalTime);
        }
    }

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

    public static boolean cancelTitle(Player player, String titleId) {
        TitleInstance instance = getTitleInstance(player, titleId);
        if (instance == null) return false;

        if (instance instanceof CountdownTitleInstance countdownInstance) {
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

        MessageUtils.sendTitle(player, "", "", 0, 1, 0);

        removeTitleInstance(player, titleId);
        return true;
    }

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

    public static boolean hasTitle(Player player, String titleId) {
        return getTitleInstance(player, titleId) != null;
    }

    public static Set<String> getActiveTitles(Player player) {
        Map<String, TitleInstance> playerData = playerTitles.get(player.getUniqueId());
        return playerData != null ? new HashSet<>(playerData.keySet()) : new HashSet<>();
    }

    public static boolean updateTitle(Player player, String titleId, ExyliaContext newContext) {
        TitleInstance instance = getTitleInstance(player, titleId);
        if (instance == null) return false;

        instance.updateContext(newContext);
        return true;
    }

    public static Set<String> getPermanentTitles(Player player) {
        Map<String, TitleInstance> playerData = playerTitles.get(player.getUniqueId());
        if (playerData == null) return new HashSet<>();

        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue().isPermanent())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public static boolean hasPermanentTitles(Player player) {
        return !getPermanentTitles(player).isEmpty();
    }

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

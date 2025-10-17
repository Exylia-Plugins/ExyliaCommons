package net.exylia.commons.utils.visuals;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.config.components.ActionBarConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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

        ScheduledTask task = executeActionBar(player, instance);
        if (task != null) {
            instance.setTask(task);
        }
        storeActionBarInstance(player, actionBarId, instance);

        return actionBarId;
    }

    public static String sendActionBar(Player player, ActionBarConfig config) {
        return sendActionBar(player, config, ExyliaContext.create());
    }

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

        ScheduledTask task = executeActionBar(player, instance);
        if (task != null) {
            instance.setTask(task);
        }
        storeActionBarInstance(player, actionBarId, instance);

        return actionBarId;
    }

    public static String sendCountdownActionBar(Player player, String actionBarId, ActionBarConfig config, long durationTicks, ExyliaContext context) {
        if (player == null || !player.isOnline() || config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        if (hasActionBar(player, actionBarId)) {
            cancelActionBar(player, actionBarId);
        }

        ExyliaContext enrichedContext = context.copy()
                .withPlayer(player)
                .withCurrentTime()
                .put("actionbar_id", actionBarId)
                .put("countdown_duration", durationTicks);

        ActionBarConfig countdownConfig = new ActionBarConfig(
                config.getText(),
                config.getUpdateInterval()
        );

        CountdownActionBarInstance instance = new CountdownActionBarInstance(actionBarId, countdownConfig, enrichedContext, durationTicks);

        ScheduledTask task = executeCountdownActionBar(player, instance);
        if (task != null) {
            instance.setTask(task);
        }
        storeActionBarInstance(player, actionBarId, instance);

        return actionBarId;
    }

    public static String sendCountdownActionBar(Player player, ActionBarConfig config, long durationTicks) {
        return sendCountdownActionBar(player, generateActionBarId(), config, durationTicks, ExyliaContext.create());
    }

    public static String sendCountdownActionBar(Player player, String actionBarId, ActionBarConfig config, int durationSeconds, ExyliaContext context) {
        return sendCountdownActionBar(player, actionBarId, config, durationSeconds * 20L, context);
    }

    public static String sendCountdownActionBar(Player player, ActionBarConfig config, int durationSeconds) {
        return sendCountdownActionBar(player, generateActionBarId(), config, durationSeconds * 20L, ExyliaContext.create());
    }

    private static ScheduledTask executeCountdownActionBar(Player player, CountdownActionBarInstance instance) {
        ActionBarConfig config = instance.getConfig();

        return Schedulers.syncTimer(new Runnable() {
            private long ticksRemaining = instance.getDurationTicks();
            private long updateCount = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    removeActionBarInstance(player, instance.getId());
                    ActionBarInstance inst = getActionBarInstance(player, instance.getId());
                    if (inst != null && inst.getTask() != null) {
                        inst.getTask().cancel();
                    }
                    return;
                }

                long secondsRemaining = (ticksRemaining + 19) / 20;

                ExyliaContext currentContext = instance.getContext().copy()
                        .put("time", secondsRemaining)
                        .put("ticks_remaining", ticksRemaining)
                        .put("update_count", updateCount)
                        .put("countdown_active", true)
                        .withCurrentTime();

                String processedText = processPlaceholders(config.getText(), player, currentContext);

                MessageUtils.sendActionBar(player, processedText);

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

                    removeActionBarInstance(player, instance.getId());
                    ActionBarInstance inst = getActionBarInstance(player, instance.getId());
                    if (inst != null && inst.getTask() != null) {
                        inst.getTask().cancel();
                    }
                }
            }
        }, 0L, 1L);
    }

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
            return false;  
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

    public static String sendCountdownActionBar(Player player, String actionBarId, ActionBarConfig config, int durationSeconds,
                                                ExyliaContext context, Runnable onComplete) {
        String id = sendCountdownActionBar(player, actionBarId, config, durationSeconds, context);

        ActionBarInstance instance = getActionBarInstance(player, id);
        if (instance instanceof CountdownActionBarInstance) {
            ((CountdownActionBarInstance) instance).onComplete(onComplete);
        }

        return id;
    }

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

    public static boolean isCountdownActionBar(Player player, String actionBarId) {
        ActionBarInstance instance = getActionBarInstance(player, actionBarId);
        return instance instanceof CountdownActionBarInstance;
    }

    public static Set<String> getCountdownActionBars(Player player) {
        Map<String, ActionBarInstance> playerData = playerActionBars.get(player.getUniqueId());
        if (playerData == null) return new HashSet<>();

        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof CountdownActionBarInstance)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static ScheduledTask executeActionBar(Player player, ActionBarInstance instance) {
        ActionBarConfig config = instance.getConfig();

        if (config.isPermanent()) {
            return Schedulers.syncTimer(new Runnable() {
                private long updateCount = 0;

                @Override
                public void run() {
                    if (!player.isOnline()) {
                        removeActionBarInstance(player, instance.getId());
                        ActionBarInstance inst = getActionBarInstance(player, instance.getId());
                        if (inst != null && inst.getTask() != null) {
                            inst.getTask().cancel();
                        }
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
            }, 0L, config.getUpdateInterval());
        } else {
            String processedText = processPlaceholders(config.getText(), player, instance.getContext());
            MessageUtils.sendActionBar(player, processedText);

            return Schedulers.syncLater(() -> {
                removeActionBarInstance(player, instance.getId());
            }, 60L);
        }
    }

    @Getter
    public static class ActionBarInstance {
        private final String id;
        private final ActionBarConfig config;
        private ExyliaContext context;
        @Setter
        private ScheduledTask task;
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

    public static boolean cancelActionBar(Player player, String actionBarId) {
        ActionBarInstance instance = getActionBarInstance(player, actionBarId);
        if (instance == null) return false;

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

        MessageUtils.sendActionBar(player, "");

        removeActionBarInstance(player, actionBarId);
        return true;
    }

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

    public static boolean hasActionBar(Player player, String actionBarId) {
        return getActionBarInstance(player, actionBarId) != null;
    }

    public static Set<String> getActiveActionBars(Player player) {
        Map<String, ActionBarInstance> playerData = playerActionBars.get(player.getUniqueId());
        return playerData != null ? new HashSet<>(playerData.keySet()) : new HashSet<>();
    }

    public static boolean updateActionBar(Player player, String actionBarId, ExyliaContext newContext) {
        ActionBarInstance instance = getActionBarInstance(player, actionBarId);
        if (instance == null) return false;

        instance.updateContext(newContext);
        return true;
    }

    public static Set<String> getPermanentActionBars(Player player) {
        Map<String, ActionBarInstance> playerData = playerActionBars.get(player.getUniqueId());
        if (playerData == null) return new HashSet<>();

        return playerData.entrySet().stream()
                .filter(entry -> entry.getValue().isPermanent())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public static boolean hasPermanentActionBars(Player player) {
        return !getPermanentActionBars(player).isEmpty();
    }

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

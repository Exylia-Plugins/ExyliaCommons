package net.exylia.commons.v2.tasks.config;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.tasks.model.TaskCategory;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Getter
@Builder
public class TaskConfig {
    @Builder.Default
    private final Map<TaskCategory, PoolConfig> poolConfigs = createDefaultPoolConfigs();

    @Builder.Default
    private final long keepAliveTime = TasksDefaults.Tasks.Settings.KEEP_ALIVE_SECONDS;

    @Builder.Default
    private final TimeUnit keepAliveUnit = TimeUnit.SECONDS;

    @Builder.Default
    private final long defaultTimeout = TasksDefaults.Tasks.Settings.DEFAULT_TIMEOUT_SECONDS;

    @Builder.Default
    private final TimeUnit defaultTimeoutUnit = TimeUnit.SECONDS;

    @Builder.Default
    private final long shutdownTimeout = TasksDefaults.Tasks.Settings.SHUTDOWN_TIMEOUT_SECONDS;

    @Builder.Default
    private final boolean enableStats = TasksDefaults.Tasks.Stats.ENABLED;

    @Builder.Default
    private final boolean trackDurations = TasksDefaults.Tasks.Stats.TRACK_DURATIONS;

    @Builder.Default
    private final boolean enableMonitoring = TasksDefaults.Tasks.Monitoring.ENABLED;

    @Builder.Default
    private final long monitoringInterval = TasksDefaults.Tasks.Monitoring.INTERVAL_SECONDS;

    @Builder.Default
    private final TimeUnit monitoringIntervalUnit = TimeUnit.SECONDS;

    @Builder.Default
    private final double highLoadThreshold = TasksDefaults.Tasks.Monitoring.HIGH_LOAD_THRESHOLD;

    @Builder.Default
    private final boolean logStatsOnShutdown = TasksDefaults.Tasks.Monitoring.LOG_STATS_ON_SHUTDOWN;

    private static Map<TaskCategory, PoolConfig> createDefaultPoolConfigs() {
        Map<TaskCategory, PoolConfig> configs = new EnumMap<>(TaskCategory.class);

        configs.put(TaskCategory.GENERAL, PoolConfig.builder()
            .corePoolSize(TasksDefaults.Tasks.Pools.General.CORE_SIZE)
            .maxPoolSize(TasksDefaults.Tasks.Pools.General.MAX_SIZE)
            .queueSize(TasksDefaults.Tasks.Pools.General.QUEUE_SIZE)
            .build());

        configs.put(TaskCategory.DATABASE, PoolConfig.builder()
            .corePoolSize(TasksDefaults.Tasks.Pools.Database.CORE_SIZE)
            .maxPoolSize(TasksDefaults.Tasks.Pools.Database.MAX_SIZE)
            .queueSize(TasksDefaults.Tasks.Pools.Database.QUEUE_SIZE)
            .build());

        configs.put(TaskCategory.IO, PoolConfig.builder()
            .corePoolSize(TasksDefaults.Tasks.Pools.IO.CORE_SIZE)
            .maxPoolSize(TasksDefaults.Tasks.Pools.IO.MAX_SIZE)
            .queueSize(TasksDefaults.Tasks.Pools.IO.QUEUE_SIZE)
            .build());

        configs.put(TaskCategory.COMPUTE, PoolConfig.builder()
            .corePoolSize(TasksDefaults.Tasks.Pools.Compute.CORE_SIZE)
            .maxPoolSize(TasksDefaults.Tasks.Pools.Compute.MAX_SIZE)
            .queueSize(TasksDefaults.Tasks.Pools.Compute.QUEUE_SIZE)
            .build());

        return configs;
    }

    public PoolConfig getPoolConfig(TaskCategory category) {
        return poolConfigs.getOrDefault(category, PoolConfig.defaults());
    }

    public static TaskConfig defaults() {
        return TaskConfig.builder().build();
    }

    public static TaskConfig fromDefaults() {
        return defaults();
    }

    @Getter
    @Builder
    public static class PoolConfig {
        @Builder.Default
        private final int corePoolSize = 4;

        @Builder.Default
        private final int maxPoolSize = 8;

        @Builder.Default
        private final int queueSize = 500;

        public static PoolConfig defaults() {
            return PoolConfig.builder().build();
        }
    }
}

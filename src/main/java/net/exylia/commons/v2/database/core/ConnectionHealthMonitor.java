package net.exylia.commons.v2.database.core;

import net.exylia.commons.v2.database.adapter.DatabaseAdapter;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.debug.api.DebugAPI;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionHealthMonitor {

    private static final int CHECK_INTERVAL_SECONDS = 30;
    private static final int MAX_RETRY_ATTEMPTS = 10;
    private static final long BASE_RETRY_DELAY_MS = 5_000;
    private static final long MAX_RETRY_DELAY_MS = 60_000;

    private final DatabaseAdapter adapter;
    private final Map<Class<?>, EntityMetadata> entityMetadataCache;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private ScheduledFuture<?> checkTask;

    public ConnectionHealthMonitor(DatabaseAdapter adapter, Map<Class<?>, EntityMetadata> entityMetadataCache) {
        this.adapter = adapter;
        this.entityMetadataCache = entityMetadataCache;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ExyliaDB-HealthMonitor");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        checkTask = scheduler.scheduleWithFixedDelay(
                this::checkHealth,
                CHECK_INTERVAL_SECONDS,
                CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        DebugAPI.logLibInfo("Database health monitor started (interval: " + CHECK_INTERVAL_SECONDS + "s)");
    }

    public void stop() {
        if (checkTask != null) {
            checkTask.cancel(false);
        }
        scheduler.shutdown();
    }

    private void checkHealth() {
        if (reconnecting.get()) return;
        if (adapter.isConnected()) return;

        DebugAPI.logLibWarn("Database connection lost, starting reconnect process...");
        if (reconnecting.compareAndSet(false, true)) {
            Thread reconnectThread = new Thread(this::attemptReconnect, "ExyliaDB-Reconnect");
            reconnectThread.setDaemon(true);
            reconnectThread.start();
        }
    }

    private void attemptReconnect() {
        long delay = BASE_RETRY_DELAY_MS;
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(delay);
                adapter.reconnect();
                restoreTables();
                DebugAPI.logLibInfo("Database reconnected successfully (attempt " + attempt + ")");
                reconnecting.set(false);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                reconnecting.set(false);
                return;
            } catch (Exception e) {
                delay = Math.min(delay * 2, MAX_RETRY_DELAY_MS);
                DebugAPI.logLibWarn("Reconnect attempt " + attempt + "/" + MAX_RETRY_ATTEMPTS + " failed: " + e.getMessage());
            }
        }
        DebugAPI.logLibError("Failed to reconnect to database after " + MAX_RETRY_ATTEMPTS + " attempts. Will retry on next health check.");
        reconnecting.set(false);
    }

    private void restoreTables() {
        for (Map.Entry<Class<?>, EntityMetadata> entry : entityMetadataCache.entrySet()) {
            try {
                adapter.createTable(entry.getValue());
                adapter.updateTable(entry.getValue());
            } catch (Exception e) {
                DebugAPI.logLibError("Failed to restore table '" + entry.getValue().getTableName() + "' after reconnect: " + e.getMessage());
            }
        }
    }
}

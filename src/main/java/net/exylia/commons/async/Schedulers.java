package net.exylia.commons.async;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @deprecated Use {@link net.exylia.commons.v2.tasks.api.Tasks} instead.
 */
@Deprecated(forRemoval = true)
public final class Schedulers {

    private Schedulers() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static SchedulerManager.TaskBuilder task(Runnable runnable) {
        return SchedulerManager.getInstance().task(runnable);
    }

    public static ScheduledTask sync(Runnable task) {
        return SchedulerManager.getInstance().runTask(task);
    }

    public static ScheduledTask syncLater(Runnable task, long delay, TimeUnit timeUnit) {
        return SchedulerManager.getInstance().runTaskLater(task, delay, timeUnit);
    }

    public static ScheduledTask syncLater(Runnable task, long ticks) {
        return syncLater(task, ticks * 50, TimeUnit.MILLISECONDS);
    }

    public static ScheduledTask syncTimer(Runnable task, long delay, long period, TimeUnit timeUnit) {
        return SchedulerManager.getInstance().runTaskTimer(task, delay, period, timeUnit);
    }

    public static ScheduledTask syncTimer(Runnable task, long delayTicks, long periodTicks) {
        return syncTimer(task, delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
    }

    public static ScheduledTask async(Runnable task) {
        return SchedulerManager.getInstance().runTaskAsync(task);
    }

    public static ScheduledTask asyncLater(Runnable task, long delay, TimeUnit timeUnit) {
        return SchedulerManager.getInstance().runTaskLaterAsync(task, delay, timeUnit);
    }

    public static ScheduledTask asyncLater(Runnable task, long ticks) {
        return asyncLater(task, ticks * 50, TimeUnit.MILLISECONDS);
    }

    public static ScheduledTask asyncTimer(Runnable task, long delay, long period, TimeUnit timeUnit) {
        return SchedulerManager.getInstance().runTaskTimerAsync(task, delay, period, timeUnit);
    }

    public static ScheduledTask asyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return asyncTimer(task, delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
    }

    public static ScheduledTask at(Location location, Runnable task) {
        return SchedulerManager.getInstance().runAtLocation(location, task);
    }

    public static ScheduledTask atLater(Location location, Runnable task, long delay, TimeUnit timeUnit) {
        return SchedulerManager.getInstance().runAtLocationLater(location, task, delay, timeUnit);
    }

    public static ScheduledTask atLater(Location location, Runnable task, long ticks) {
        return atLater(location, task, ticks * 50, TimeUnit.MILLISECONDS);
    }

    public static ScheduledTask at(Entity entity, Runnable task) {
        return SchedulerManager.getInstance().runAtEntity(entity, task);
    }

    public static ScheduledTask atLater(Entity entity, Runnable task, long delay, TimeUnit timeUnit) {
        return SchedulerManager.getInstance().runAtEntityLater(entity, task, delay, timeUnit);
    }

    public static ScheduledTask atLater(Entity entity, Runnable task, long ticks) {
        return atLater(entity, task, ticks * 50, TimeUnit.MILLISECONDS);
    }

    public static void runSync(Runnable task) {
        SchedulerManager.getInstance().runSync(task);
    }

    public static void runAsync(Runnable task) {
        SchedulerManager.getInstance().runAsync(task);
    }

    public static <T> void syncThenAsync(Consumer<T> syncTask, T data, Consumer<T> asyncTask) {
        SchedulerManager.getInstance().runSyncThenAsync(syncTask, data, asyncTask);
    }

    public static <T> void asyncThenSync(Consumer<T> asyncTask, T data, Consumer<T> syncTask) {
        SchedulerManager.getInstance().runAsyncThenSync(asyncTask, data, syncTask);
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return AsyncExecutor.getInstance().supplyAsync(supplier, false);
    }

    public static <T> CompletableFuture<T> supplyAsyncDb(Supplier<T> supplier) {
        return AsyncExecutor.getInstance().supplyAsync(supplier, true);
    }

    public static CompletableFuture<Void> runAsyncTask(Runnable task) {
        return AsyncExecutor.getInstance().runAsync(task, false);
    }

    public static CompletableFuture<Void> runAsyncDbTask(Runnable task) {
        return AsyncExecutor.getInstance().runAsync(task, true);
    }

    public static <T> CompletableFuture<T> supplyAsyncWithTimeout(Supplier<T> supplier, long timeout, TimeUnit unit) {
        return AsyncExecutor.getInstance().supplyAsyncWithTimeout(supplier, false, timeout, unit);
    }

    public static <T> CompletableFuture<T> supplyAsyncDbWithTimeout(Supplier<T> supplier, long timeout, TimeUnit unit) {
        return AsyncExecutor.getInstance().supplyAsyncWithTimeout(supplier, true, timeout, unit);
    }

    public static ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return AsyncExecutor.getInstance().schedule(task, delay, unit);
    }

    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return AsyncExecutor.getInstance().scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        return AsyncExecutor.getInstance().scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }

    public static boolean isMainThread() {
        return SchedulerManager.getInstance().isMainThread();
    }

    public static boolean isFolia() {
        return SchedulerManager.getInstance().isFolia();
    }

    public static boolean isOwnedByCurrentRegion(Location location) {
        return SchedulerManager.getInstance().isOwnedByCurrentRegion(location);
    }

    public static boolean isOwnedByCurrentRegion(World world, int chunkX, int chunkZ) {
        return SchedulerManager.getInstance().isOwnedByCurrentRegion(world, chunkX, chunkZ);
    }

    public static boolean isOwnedByCurrentRegion(Chunk chunk) {
        return SchedulerManager.getInstance().isOwnedByCurrentRegion(chunk);
    }

    public static boolean isOwnedByCurrentRegion(Entity entity) {
        return SchedulerManager.getInstance().isOwnedByCurrentRegion(entity);
    }

    public static AsyncExecutor.ExecutorStats getStats() {
        return AsyncExecutor.getInstance().getStats();
    }
}

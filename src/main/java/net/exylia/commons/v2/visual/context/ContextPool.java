package net.exylia.commons.v2.visual.context;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class ContextPool {
    private static final int POOL_SIZE = 50;
    private static final Queue<VisualContext> pool = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger activeCount = new AtomicInteger(0);
    private static final AtomicInteger totalCreated = new AtomicInteger(0);

    static {
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.offer(new VisualContext());
        }
    }

    private ContextPool() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static VisualContext acquire() {
        VisualContext context = pool.poll();
        if (context != null) {
            activeCount.incrementAndGet();
            return context;
        }

        totalCreated.incrementAndGet();
        activeCount.incrementAndGet();
        return new VisualContext();
    }

    public static void release(VisualContext context) {
        if (context == null) return;

        context.clear();
        activeCount.decrementAndGet();

        if (pool.size() < POOL_SIZE) {
            pool.offer(context);
        }
    }

    public static int getPoolSize() {
        return pool.size();
    }

    public static int getActiveCount() {
        return activeCount.get();
    }

    public static int getTotalCreated() {
        return totalCreated.get();
    }

    public static void clear() {
        pool.clear();
        activeCount.set(0);
    }
}

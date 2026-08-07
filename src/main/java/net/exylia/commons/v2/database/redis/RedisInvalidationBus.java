package net.exylia.commons.v2.database.redis;

import net.exylia.commons.v2.database.cache.CacheKey;
import net.exylia.commons.v2.debug.api.DebugAPI;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RedisInvalidationBus {

    private static final String TYPE_INVALIDATE = "I";
    private static final String TYPE_CLEAR = "C";
    private static final char SEP = '|';

    private final String serverId = UUID.randomUUID().toString().substring(0, 8);
    private final RedisConnectionPool pool;
    private final String channel;
    private final Map<String, Consumer<CacheKey>> invalidateHandlers = new ConcurrentHashMap<>();
    private final Map<String, Runnable> clearHandlers = new ConcurrentHashMap<>();
    private final JedisPubSub subscriber;

    public RedisInvalidationBus(RedisConnectionPool pool, String channel) {
        this.pool = pool;
        this.channel = channel;
        this.subscriber = new JedisPubSub() {
            @Override
            public void onMessage(String ch, String message) {
                handleMessage(message);
            }
        };
    }

    private static final long RECONNECT_DELAY_MS = 2000L;
    private volatile boolean running = false;
    private volatile Thread listenerThread;

    public void start() {
        running = true;
        listenerThread = new Thread(this::runWithReconnect, "Exylia-Redis-InvalidationBus");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void runWithReconnect() {
        while (running) {
            Jedis jedis = null;
            try {
                jedis = pool.createSubscriberConnection();
                DebugAPI.logLibInfo("Redis invalidation bus connected (channel=" + channel + ")");
                jedis.subscribe(subscriber, channel);
                // subscribe() blocks until unsubscribed/disconnected.
            } catch (Exception e) {
                if (running) {
                    DebugAPI.logLibWarn("Redis invalidation bus disconnected: " + e.getMessage()
                            + " — reconnecting in " + RECONNECT_DELAY_MS + "ms");
                }
            } finally {
                if (jedis != null) {
                    try { jedis.close(); } catch (Exception ignored) {}
                }
            }

            if (!running) break;
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (subscriber.isSubscribed()) {
                subscriber.unsubscribe();
            }
        } catch (Exception ignored) {}
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    public void registerHandler(String entityNamespace, Consumer<CacheKey> onInvalidate, Runnable onClear) {
        invalidateHandlers.put(entityNamespace, onInvalidate);
        clearHandlers.put(entityNamespace, onClear);
    }

    public void publishInvalidate(CacheKey key) {
        publish(TYPE_INVALIDATE + SEP + key.getNamespace() + SEP + key.getKey());
    }

    public void publishClear(String entityNamespace) {
        publish(TYPE_CLEAR + SEP + entityNamespace);
    }

    private void publish(String payload) {
        try {
            String message = serverId + SEP + payload;
            pool.execute(jedis -> jedis.publish(channel, message));
        } catch (Exception e) {
            DebugAPI.logLibWarn("Redis invalidation publish failed: " + e.getMessage());
        }
    }

    private void handleMessage(String message) {
        int firstPipe = message.indexOf(SEP);
        if (firstPipe == -1) return;

        String sender = message.substring(0, firstPipe);
        if (serverId.equals(sender)) return;

        String payload = message.substring(firstPipe + 1);
        int typePipe = payload.indexOf(SEP);
        if (typePipe == -1) return;

        String type = payload.substring(0, typePipe);
        String rest = payload.substring(typePipe + 1);

        switch (type) {
            case TYPE_INVALIDATE -> handleInvalidate(rest);
            case TYPE_CLEAR -> handleClear(rest);
        }
    }

    private void handleInvalidate(String rest) {
        int sep = rest.indexOf(SEP);
        if (sep == -1) return;
        String namespace = rest.substring(0, sep);
        String key = rest.substring(sep + 1);
        CacheKey cacheKey = new CacheKey(namespace, key);
        for (Map.Entry<String, Consumer<CacheKey>> entry : invalidateHandlers.entrySet()) {
            String base = entry.getKey();
            if (namespace.equals(base) || namespace.startsWith(base + ":")) {
                entry.getValue().accept(cacheKey);
            }
        }
    }

    private void handleClear(String entityNamespace) {
        Runnable handler = clearHandlers.get(entityNamespace);
        if (handler != null) {
            handler.run();
        }
    }
}

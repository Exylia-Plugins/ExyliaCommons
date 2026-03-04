package net.exylia.commons.v2.redis;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.debug.api.DebugAPI;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class SimpleRedisPubSub {

    private final SimpleRedis redis;
    private final Map<String, JedisPubSub> subscriptions = new ConcurrentHashMap<>();

    public void subscribe(String channel, Consumer<String> handler) {
        DebugAPI.logLibDebug("SimpleRedisPubSub.subscribe: Subscribing to channel '" + channel + "'");
        if (subscriptions.containsKey(channel)) {
            DebugAPI.logLibDebug("SimpleRedisPubSub.subscribe: Already subscribed to channel '" + channel + "', skipping");
            return;
        }

        JedisPubSub pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String ch, String message) {
                DebugAPI.logLibDebug("SimpleRedisPubSub: Received message on channel '" + ch + "' (length: " + message.length() + ")");
                redis.runSync(() -> handler.accept(message));
            }
        };

        subscriptions.put(channel, pubSub);
        DebugAPI.logLibDebug("SimpleRedisPubSub.subscribe: PubSub handler registered for channel '" + channel + "'");

        redis.runAsync(() -> {
            DebugAPI.logLibDebug("SimpleRedisPubSub.subscribe: Starting async subscription to channel '" + channel + "'");
            try (Jedis jedis = redis.getPool().getResource()) {
                DebugAPI.logLibDebug("SimpleRedisPubSub.subscribe: Calling jedis.subscribe for channel '" + channel + "'");
                jedis.subscribe(pubSub, channel);
            } catch (Exception e) {
                DebugAPI.logLibError("SimpleRedisPubSub.subscribe: Failed to subscribe to channel '" + channel + "' - " + e.getMessage());
            }
        });
        DebugAPI.logLibDebug("SimpleRedisPubSub.subscribe: Subscription initiated for channel '" + channel + "'");
    }

    public <T> void subscribeObject(String channel, Class<T> type, Consumer<T> handler) {
        DebugAPI.logLibDebug("SimpleRedisPubSub.subscribeObject: Subscribing to channel '" + channel + "' for type " + type.getSimpleName());
        subscribe(channel, message -> {
            try {
                Gson gson = redis.getGson();
                T object = gson.fromJson(message, type);
                DebugAPI.logLibDebug("SimpleRedisPubSub.subscribeObject: Deserialized object of type " + type.getSimpleName());
                handler.accept(object);
            } catch (Exception e) {
                DebugAPI.logLibError("SimpleRedisPubSub.subscribeObject: Failed to deserialize message on channel '" + channel + "' - " + e.getMessage());
            }
        });
    }

    public void unsubscribe(String channel) {
        DebugAPI.logLibDebug("SimpleRedisPubSub.unsubscribe: Unsubscribing from channel '" + channel + "'");
        JedisPubSub pubSub = subscriptions.remove(channel);
        if (pubSub != null && pubSub.isSubscribed()) {
            DebugAPI.logLibDebug("SimpleRedisPubSub.unsubscribe: PubSub is active, calling unsubscribe");
            pubSub.unsubscribe();
            DebugAPI.logLibDebug("SimpleRedisPubSub.unsubscribe: Unsubscribed from channel '" + channel + "'");
        } else {
            DebugAPI.logLibDebug("SimpleRedisPubSub.unsubscribe: No active subscription found for channel '" + channel + "'");
        }
    }

    public void unsubscribeAll() {
        DebugAPI.logLibDebug("SimpleRedisPubSub.unsubscribeAll: Unsubscribing from all channels (" + subscriptions.size() + " channels)");
        subscriptions.values().forEach(pubSub -> {
            if (pubSub.isSubscribed()) {
                DebugAPI.logLibDebug("SimpleRedisPubSub.unsubscribeAll: Unsubscribing from active subscription");
                pubSub.unsubscribe();
            }
        });
        subscriptions.clear();
        DebugAPI.logLibDebug("SimpleRedisPubSub.unsubscribeAll: All subscriptions cleared");
    }

    void shutdown() {
        DebugAPI.logLibDebug("SimpleRedisPubSub.shutdown: Shutting down PubSub system");
        unsubscribeAll();
        DebugAPI.logLibDebug("SimpleRedisPubSub.shutdown: Shutdown complete");
    }
}

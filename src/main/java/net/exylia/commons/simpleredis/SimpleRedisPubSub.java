package net.exylia.commons.simpleredis;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import net.exylia.commons.utils.DebugUtils;
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
        DebugUtils.logInternalDebug("SimpleRedisPubSub.subscribe: Subscribing to channel '" + channel + "'");
        if (subscriptions.containsKey(channel)) {
            DebugUtils.logInternalDebug("SimpleRedisPubSub.subscribe: Already subscribed to channel '" + channel + "', skipping");
            return;
        }

        JedisPubSub pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String ch, String message) {
                DebugUtils.logInternalDebug("SimpleRedisPubSub: Received message on channel '" + ch + "' (length: " + message.length() + ")");
                redis.runSync(() -> handler.accept(message));
            }
        };

        subscriptions.put(channel, pubSub);
        DebugUtils.logInternalDebug("SimpleRedisPubSub.subscribe: PubSub handler registered for channel '" + channel + "'");

        redis.runAsync(() -> {
            DebugUtils.logInternalDebug("SimpleRedisPubSub.subscribe: Starting async subscription to channel '" + channel + "'");
            try (Jedis jedis = redis.getPool().getResource()) {
                DebugUtils.logInternalDebug("SimpleRedisPubSub.subscribe: Calling jedis.subscribe for channel '" + channel + "'");
                jedis.subscribe(pubSub, channel);
            } catch (Exception e) {
                DebugUtils.logInternalError("SimpleRedisPubSub.subscribe: Failed to subscribe to channel '" + channel + "' - " + e.getMessage());
            }
        });
        DebugUtils.logInternalDebug("SimpleRedisPubSub.subscribe: Subscription initiated for channel '" + channel + "'");
    }

    public <T> void subscribeObject(String channel, Class<T> type, Consumer<T> handler) {
        DebugUtils.logInternalDebug("SimpleRedisPubSub.subscribeObject: Subscribing to channel '" + channel + "' for type " + type.getSimpleName());
        subscribe(channel, message -> {
            try {
                Gson gson = redis.getGson();
                T object = gson.fromJson(message, type);
                DebugUtils.logInternalDebug("SimpleRedisPubSub.subscribeObject: Deserialized object of type " + type.getSimpleName());
                handler.accept(object);
            } catch (Exception e) {
                DebugUtils.logInternalError("SimpleRedisPubSub.subscribeObject: Failed to deserialize message on channel '" + channel + "' - " + e.getMessage());
            }
        });
    }

    public void unsubscribe(String channel) {
        DebugUtils.logInternalDebug("SimpleRedisPubSub.unsubscribe: Unsubscribing from channel '" + channel + "'");
        JedisPubSub pubSub = subscriptions.remove(channel);
        if (pubSub != null && pubSub.isSubscribed()) {
            DebugUtils.logInternalDebug("SimpleRedisPubSub.unsubscribe: PubSub is active, calling unsubscribe");
            pubSub.unsubscribe();
            DebugUtils.logInternalDebug("SimpleRedisPubSub.unsubscribe: Unsubscribed from channel '" + channel + "'");
        } else {
            DebugUtils.logInternalDebug("SimpleRedisPubSub.unsubscribe: No active subscription found for channel '" + channel + "'");
        }
    }

    public void unsubscribeAll() {
        DebugUtils.logInternalDebug("SimpleRedisPubSub.unsubscribeAll: Unsubscribing from all channels (" + subscriptions.size() + " channels)");
        subscriptions.values().forEach(pubSub -> {
            if (pubSub.isSubscribed()) {
                DebugUtils.logInternalDebug("SimpleRedisPubSub.unsubscribeAll: Unsubscribing from active subscription");
                pubSub.unsubscribe();
            }
        });
        subscriptions.clear();
        DebugUtils.logInternalDebug("SimpleRedisPubSub.unsubscribeAll: All subscriptions cleared");
    }

    void shutdown() {
        DebugUtils.logInternalDebug("SimpleRedisPubSub.shutdown: Shutting down PubSub system");
        unsubscribeAll();
        DebugUtils.logInternalDebug("SimpleRedisPubSub.shutdown: Shutdown complete");
    }
}

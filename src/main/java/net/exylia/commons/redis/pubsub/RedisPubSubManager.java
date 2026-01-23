package net.exylia.commons.redis.pubsub;

import net.exylia.commons.redis.connection.RedisConnectionManager;
import net.exylia.commons.redis.pubsub.subcriptions.MultiChannelSubscription;
import net.exylia.commons.redis.pubsub.subcriptions.PatternSubscription;
import net.exylia.commons.redis.pubsub.subcriptions.RedisSubscription;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static net.exylia.commons.utils.DebugUtils.logInternalError;
import static net.exylia.commons.utils.DebugUtils.logInternalInfo;

@Deprecated
public class RedisPubSubManager {

    private final RedisConnectionManager connectionManager;
    private final ConcurrentHashMap<String, RedisSubscriber> subscribers;
    private final ExecutorService executorService;
    private volatile boolean initialized = false;

    public RedisPubSubManager(RedisConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.subscribers = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "ExyliaRedis-PubSub");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void initialize() {
        if (initialized) return;

        try {
            initialized = true;
            logInternalInfo("Sistema Pub/Sub de Redis inicializado");
        } catch (Exception e) {
            logInternalError("Error al inicializar Pub/Sub: " + e.getMessage());
            throw new RuntimeException("Fallo al inicializar Pub/Sub", e);
        }
    }

    public void publish(String channel, String message) {
        if (!initialized) {
            throw new IllegalStateException("PubSubManager no está inicializado");
        }

        executorService.submit(() -> {
            try (Jedis jedis = connectionManager.getConnection()) {
                jedis.publish(channel, message);
            } catch (Exception e) {
                logInternalError("Error publicando mensaje en canal '" + channel + "': " + e.getMessage());
            }
        });
    }

    public long publishSync(String channel, String message) {
        if (!initialized) {
            throw new IllegalStateException("PubSubManager no está inicializado");
        }

        try (Jedis jedis = connectionManager.getConnection()) {
            return jedis.publish(channel, message);
        } catch (Exception e) {
            logInternalError("Error publicando mensaje síncrono en canal '" + channel + "': " + e.getMessage());
            return 0;
        }
    }

    public RedisSubscription subscribe(String channel, Consumer<String> messageHandler) {
        return subscribe(channel, messageHandler, null, null);
    }

    public RedisSubscription subscribe(String channel,
                                       Consumer<String> messageHandler,
                                       Runnable onSubscribe,
                                       Runnable onUnsubscribe) {
        if (!initialized) {
            throw new IllegalStateException("PubSubManager no está inicializado");
        }

        RedisSubscriber subscriber = new SingleChannelSubscriber(
                channel, messageHandler, onSubscribe, onUnsubscribe
        );

        Future<?> future = executorService.submit(() -> {
            try (Jedis jedis = connectionManager.getConnection()) {
                jedis.subscribe(subscriber, channel);
            } catch (Exception e) {
                if (!subscriber.isUnsubscribed()) {
                    logInternalError("Error en suscripción al canal '" + channel + "': " + e.getMessage());
                }
            }
        });

        subscribers.put(channel, subscriber);

        return new RedisSubscription(channel, subscriber, future, this);
    }

    public MultiChannelSubscription subscribeMultiple(String[] channels,
                                                      Consumer<ChannelMessage> messageHandler) {
        return subscribeMultiple(channels, messageHandler, null, null);
    }

    public MultiChannelSubscription subscribeMultiple(String[] channels,
                                                      Consumer<ChannelMessage> messageHandler,
                                                      Consumer<String> onSubscribe,
                                                      Consumer<String> onUnsubscribe) {
        if (!initialized) {
            throw new IllegalStateException("PubSubManager no está inicializado");
        }

        MultiChannelSubscriber subscriber = new MultiChannelSubscriber(
                messageHandler, onSubscribe, onUnsubscribe
        );

        Future<?> future = executorService.submit(() -> {
            try (Jedis jedis = connectionManager.getConnection()) {
                jedis.subscribe(subscriber, channels);
            } catch (Exception e) {
                if (!subscriber.isUnsubscribed()) {
                    logInternalError("Error en suscripción múltiple: " + e.getMessage());
                }
            }
        });

        for (String channel : channels) {
            subscribers.put(channel, subscriber);
        }

        return new MultiChannelSubscription(channels, subscriber, future, this);
    }

    public PatternSubscription subscribePattern(String pattern,
                                                Consumer<PatternMessage> messageHandler) {
        return subscribePattern(pattern, messageHandler, null, null);
    }

    public PatternSubscription subscribePattern(String pattern,
                                                Consumer<PatternMessage> messageHandler,
                                                Consumer<String> onSubscribe,
                                                Consumer<String> onUnsubscribe) {
        if (!initialized) {
            throw new IllegalStateException("PubSubManager no está inicializado");
        }

        PatternSubscriber subscriber = new PatternSubscriber(
                pattern, messageHandler, onSubscribe, onUnsubscribe
        );

        Future<?> future = executorService.submit(() -> {
            try (Jedis jedis = connectionManager.getConnection()) {
                jedis.psubscribe(subscriber, pattern);
            } catch (Exception e) {
                if (!subscriber.isUnsubscribed()) {
                    logInternalError("Error en suscripción por patrón '" + pattern + "': " + e.getMessage());
                }
            }
        });

        return new PatternSubscription(pattern, subscriber, future, this);
    }

    public void unsubscribe(String channel) {
        RedisSubscriber subscriber = subscribers.remove(channel);
        if (subscriber != null && !subscriber.isUnsubscribed()) {
            subscriber.unsubscribe(channel);
        }
    }

    public void unsubscribeAll() {
        for (RedisSubscriber subscriber : subscribers.values()) {
            if (!subscriber.isUnsubscribed()) {
                subscriber.unsubscribe();
            }
        }
        subscribers.clear();
    }

    public synchronized void shutdown() {
        if (!initialized) return;

        logInternalInfo("Cerrando sistema Pub/Sub...");

        try {
             
            unsubscribeAll();

            executorService.shutdown();

            initialized = false;
            logInternalInfo("Sistema Pub/Sub cerrado correctamente");

        } catch (Exception e) {
            logInternalError("Error al cerrar Pub/Sub: " + e.getMessage());
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getActiveSubscriptions() {
        return subscribers.size();
    }

    public static abstract class RedisSubscriber extends JedisPubSub {
        public abstract boolean isUnsubscribed();
    }

    private static class SingleChannelSubscriber extends RedisSubscriber {
        private final String channel;
        private final Consumer<String> messageHandler;
        private final Runnable onSubscribe;
        private final Runnable onUnsubscribe;

        public SingleChannelSubscriber(String channel, Consumer<String> messageHandler,
                                       Runnable onSubscribe, Runnable onUnsubscribe) {
            this.channel = channel;
            this.messageHandler = messageHandler;
            this.onSubscribe = onSubscribe;
            this.onUnsubscribe = onUnsubscribe;
        }

        @Override
        public void onMessage(String channel, String message) {
            try {
                if (messageHandler != null) {
                    messageHandler.accept(message);
                }
            } catch (Exception e) {
                logInternalError("Error procesando mensaje de canal '" + channel + "': " + e.getMessage());
            }
        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            logInternalInfo("Suscrito al canal: " + channel);
            if (onSubscribe != null) {
                try {
                    onSubscribe.run();
                } catch (Exception e) {
                    logInternalError("Error en callback onSubscribe: " + e.getMessage());
                }
            }
        }

        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {
            logInternalInfo("Desuscrito del canal: " + channel);
            if (onUnsubscribe != null) {
                try {
                    onUnsubscribe.run();
                } catch (Exception e) {
                    logInternalError("Error en callback onUnsubscribe: " + e.getMessage());
                }
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return false;
        }
    }

    private static class MultiChannelSubscriber extends RedisSubscriber {
        private final Consumer<ChannelMessage> messageHandler;
        private final Consumer<String> onSubscribe;
        private final Consumer<String> onUnsubscribe;

        public MultiChannelSubscriber(Consumer<ChannelMessage> messageHandler,
                                      Consumer<String> onSubscribe,
                                      Consumer<String> onUnsubscribe) {
            this.messageHandler = messageHandler;
            this.onSubscribe = onSubscribe;
            this.onUnsubscribe = onUnsubscribe;
        }

        @Override
        public void onMessage(String channel, String message) {
            try {
                if (messageHandler != null) {
                    messageHandler.accept(new ChannelMessage(channel, message));
                }
            } catch (Exception e) {
                logInternalError("Error procesando mensaje multicanal de '" + channel + "': " + e.getMessage());
            }
        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            logInternalInfo("Suscrito al canal multicanal: " + channel);
            if (onSubscribe != null) {
                try {
                    onSubscribe.accept(channel);
                } catch (Exception e) {
                    logInternalError("Error en callback onSubscribe multicanal: " + e.getMessage());
                }
            }
        }

        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {
            logInternalInfo("Desuscrito del canal multicanal: " + channel);
            if (onUnsubscribe != null) {
                try {
                    onUnsubscribe.accept(channel);
                } catch (Exception e) {
                    logInternalError("Error en callback onUnsubscribe multicanal: " + e.getMessage());
                }
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return false;
        }
    }

    private static class PatternSubscriber extends RedisSubscriber {
        private final String pattern;
        private final Consumer<PatternMessage> messageHandler;
        private final Consumer<String> onSubscribe;
        private final Consumer<String> onUnsubscribe;

        public PatternSubscriber(String pattern, Consumer<PatternMessage> messageHandler,
                                 Consumer<String> onSubscribe, Consumer<String> onUnsubscribe) {
            this.pattern = pattern;
            this.messageHandler = messageHandler;
            this.onSubscribe = onSubscribe;
            this.onUnsubscribe = onUnsubscribe;
        }

        @Override
        public void onPMessage(String pattern, String channel, String message) {
            try {
                if (messageHandler != null) {
                    messageHandler.accept(new PatternMessage(pattern, channel, message));
                }
            } catch (Exception e) {
                logInternalError("Error procesando mensaje de patrón '" + pattern + "': " + e.getMessage());
            }
        }

        @Override
        public void onPSubscribe(String pattern, int subscribedChannels) {
            logInternalInfo("Suscrito al patrón: " + pattern);
            if (onSubscribe != null) {
                try {
                    onSubscribe.accept(pattern);
                } catch (Exception e) {
                    logInternalError("Error en callback onPSubscribe: " + e.getMessage());
                }
            }
        }

        @Override
        public void onPUnsubscribe(String pattern, int subscribedChannels) {
            logInternalInfo("Desuscrito del patrón: " + pattern);
            if (onUnsubscribe != null) {
                try {
                    onUnsubscribe.accept(pattern);
                } catch (Exception e) {
                    logInternalError("Error en callback onPUnsubscribe: " + e.getMessage());
                }
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return false;
        }
    }
}

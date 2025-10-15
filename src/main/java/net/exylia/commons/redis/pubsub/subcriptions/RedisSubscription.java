package net.exylia.commons.redis.pubsub.subcriptions;

import net.exylia.commons.redis.pubsub.RedisPubSubManager;
import redis.clients.jedis.JedisPubSub;
import java.util.concurrent.Future;

public class RedisSubscription {

    private final String channel;
    private final JedisPubSub subscriber;
    private final Future<?> subscriptionFuture;
    private final RedisPubSubManager manager;
    private volatile boolean cancelled = false;

    public RedisSubscription(String channel, JedisPubSub subscriber,
                             Future<?> subscriptionFuture, RedisPubSubManager manager) {
        this.channel = channel;
        this.subscriber = subscriber;
        this.subscriptionFuture = subscriptionFuture;
        this.manager = manager;
    }

    public void cancel() {
        if (cancelled) return;

        try {
            if (subscriber.isSubscribed()) {
                subscriber.unsubscribe(channel);
            }

            if (subscriptionFuture != null && !subscriptionFuture.isDone()) {
                subscriptionFuture.cancel(true);
            }

            cancelled = true;

        } catch (Exception e) {
             
            cancelled = true;
        }
    }

    public boolean isActive() {
        return !cancelled && subscriber.isSubscribed() &&
                (subscriptionFuture == null || !subscriptionFuture.isDone());
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public String getChannel() {
        return channel;
    }

    public JedisPubSub getSubscriber() {
        return subscriber;
    }
}

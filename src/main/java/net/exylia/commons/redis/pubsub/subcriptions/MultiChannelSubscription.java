package net.exylia.commons.redis.pubsub.subcriptions;

import net.exylia.commons.redis.pubsub.RedisPubSubManager;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.Future;

public class MultiChannelSubscription {

    private final String[] channels;
    private final JedisPubSub subscriber;
    private final Future<?> subscriptionFuture;
    private final RedisPubSubManager manager;
    private volatile boolean cancelled = false;

    public MultiChannelSubscription(String[] channels, JedisPubSub subscriber,
                                    Future<?> subscriptionFuture, RedisPubSubManager manager) {
        this.channels = channels.clone();
        this.subscriber = subscriber;
        this.subscriptionFuture = subscriptionFuture;
        this.manager = manager;
    }

    public void cancel() {
        if (cancelled) return;

        try {
            if (subscriber.isSubscribed()) {
                subscriber.unsubscribe(channels);
            }

            if (subscriptionFuture != null && !subscriptionFuture.isDone()) {
                subscriptionFuture.cancel(true);
            }

            cancelled = true;

        } catch (Exception e) {
            cancelled = true;
        }
    }

    public void cancel(String channel) {
        try {
            if (subscriber.isSubscribed()) {
                subscriber.unsubscribe(channel);
            }
        } catch (Exception e) {
             
        }
    }

    public boolean isActive() {
        return !cancelled && subscriber.isSubscribed() &&
                (subscriptionFuture == null || !subscriptionFuture.isDone());
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public String[] getChannels() {
        return channels.clone();
    }

    public JedisPubSub getSubscriber() {
        return subscriber;
    }
}

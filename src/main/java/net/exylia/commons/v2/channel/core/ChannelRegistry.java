package net.exylia.commons.v2.channel.core;

import net.exylia.commons.v2.channel.model.Channel;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelRegistry {

    private final ConcurrentHashMap<String, Channel> channels;

    public ChannelRegistry() {
        this.channels = new ConcurrentHashMap<>();
    }

    public void register(Channel channel) {
        channels.put(channel.getId(), channel);
    }

    public void unregister(String channelId) {
        channels.remove(channelId);
    }

    public Optional<Channel> get(String channelId) {
        return Optional.ofNullable(channels.get(channelId));
    }

    public boolean has(String channelId) {
        return channels.containsKey(channelId);
    }

    public Set<String> getAllChannelIds() {
        return channels.keySet();
    }

    public int getActiveCount() {
        return channels.size();
    }

    public void clear() {
        channels.clear();
    }
}

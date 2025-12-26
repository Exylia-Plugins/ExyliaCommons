package net.exylia.commons.v2.conversation.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerConversationIndex {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final int MAX_SIZE = 10000;

    private final Cache<UUID, Set<String>> cache;

    public PlayerConversationIndex() {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(TTL)
                .maximumSize(MAX_SIZE)
                .recordStats()
                .build();
    }

    public void addConversation(UUID playerId, String conversationId) {
        Set<String> conversations = cache.get(playerId, k -> ConcurrentHashMap.newKeySet());
        if (conversations != null) {
            conversations.add(conversationId);
        }
    }

    public void removeConversation(UUID playerId, String conversationId) {
        Set<String> conversations = cache.getIfPresent(playerId);
        if (conversations != null) {
            conversations.remove(conversationId);
            if (conversations.isEmpty()) {
                cache.invalidate(playerId);
            }
        }
    }

    public Set<String> getConversations(UUID playerId) {
        Set<String> conversations = cache.getIfPresent(playerId);
        return conversations != null ? conversations : Collections.emptySet();
    }

    public void invalidatePlayer(UUID playerId) {
        cache.invalidate(playerId);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    public long size() {
        return cache.estimatedSize();
    }

    public double hitRate() {
        return cache.stats().hitRate();
    }
}

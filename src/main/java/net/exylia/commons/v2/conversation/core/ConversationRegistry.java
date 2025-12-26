package net.exylia.commons.v2.conversation.core;

import net.exylia.commons.v2.conversation.model.Conversation;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ConversationRegistry {

    private final ConcurrentHashMap<String, Conversation> conversations;

    public ConversationRegistry() {
        this.conversations = new ConcurrentHashMap<>();
    }

    public void register(Conversation conversation) {
        conversations.put(conversation.getId(), conversation);
    }

    public void unregister(String conversationId) {
        conversations.remove(conversationId);
    }

    public Optional<Conversation> get(String conversationId) {
        return Optional.ofNullable(conversations.get(conversationId));
    }

    public Set<Conversation> getPlayerConversations(UUID playerId) {
        return conversations.values().stream()
                .filter(conv -> conv.hasParticipant(playerId))
                .collect(Collectors.toSet());
    }

    public boolean has(String conversationId) {
        return conversations.containsKey(conversationId);
    }

    public int getActiveCount() {
        return conversations.size();
    }

    public void clear() {
        conversations.clear();
    }
}

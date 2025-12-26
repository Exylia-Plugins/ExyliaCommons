package net.exylia.commons.v2.conversation.api;

import net.exylia.commons.v2.conversation.core.ConversationManager;
import net.exylia.commons.v2.conversation.model.Conversation;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConversationAPI {

    private ConversationAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(Plugin plugin) {
        ConversationManager.initialize(plugin);
    }

    public static void start(String id, String recipientFormat, String senderFormat, Collection<Player> participants) {
        ConversationManager.getInstance().startConversation(id, recipientFormat, senderFormat, participants);
    }

    public static void end(String conversationId) {
        ConversationManager.getInstance().endConversation(conversationId);
    }

    public static boolean isInConversation(Player player) {
        if (player == null) {
            return false;
        }
        return ConversationManager.getInstance().isInConversation(player.getUniqueId());
    }

    public static Set<Conversation> getPlayerConversations(Player player) {
        if (player == null) {
            return Collections.emptySet();
        }
        return ConversationManager.getInstance().getPlayerConversations(player.getUniqueId());
    }

    public static Set<String> getPlayerConversationIds(Player player) {
        return getPlayerConversations(player).stream()
                .map(Conversation::getId)
                .collect(Collectors.toSet());
    }

    public static int getActiveCount() {
        return ConversationManager.getInstance().getActiveCount();
    }

    public static void clearAll() {
        ConversationManager.getInstance().clearAll();
    }

    public static void addParticipant(String conversationId, Player player) {
        ConversationManager.getInstance().addParticipant(conversationId, player);
    }

    public static void removeParticipant(String conversationId, Player player) {
        ConversationManager.getInstance().removeParticipant(conversationId, player);
    }

    public static boolean conversationExists(String conversationId) {
        return ConversationManager.getInstance().conversationExists(conversationId);
    }
}

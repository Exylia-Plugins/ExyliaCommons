package net.exylia.commons.v2.conversation.listener;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.conversation.core.ConversationManager;
import net.exylia.commons.v2.conversation.model.Conversation;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ChatInterceptListener implements Listener {

    private final ConversationManager manager;

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID senderId = sender.getUniqueId();

        Set<Conversation> conversations = manager.getPlayerConversations(senderId);

        if (conversations.isEmpty()) {
            return;
        }

        DebugAPI.logLibDebug(DebugCategory.CONVERSATION,
            String.format("[CHAT] Player %s sent message in %d conversation(s)",
                sender.getName(), conversations.size()));

        Set<UUID> allParticipants = conversations.stream()
                .flatMap(conv -> conv.getParticipants().stream())
                .collect(Collectors.toSet());

        event.getRecipients().removeIf(p -> allParticipants.contains(p.getUniqueId()));

        String message = event.getMessage();

        for (Conversation conv : conversations) {
            for (UUID participantId : conv.getParticipants()) {
                Player participant = Bukkit.getPlayer(participantId);
                if (participant == null || !participant.isOnline()) {
                    continue;
                }

                boolean isSender = participantId.equals(senderId);
                String format = isSender ? conv.getSenderFormat() : conv.getRecipientFormat();

                PlaceholderContext context = PlaceholderContext.create()
                        .withPlayer(sender)
                        .put("player", sender.getName())
                        .put("message", message)
                        .put("recipient", participant.getName());

                    DebugAPI.logLibDebug(DebugCategory.CONVERSATION,
                        String.format("[CHAT] Sending normal message to %s", participant.getName()));
                    net.exylia.commons.v2.visual.api.MessageAPI.send(participant, format, context);
            }
        }
    }
}

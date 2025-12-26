package net.exylia.commons.v2.conversation.listener;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.conversation.core.ConversationManager;
import net.exylia.commons.v2.conversation.model.Conversation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class PlayerCleanupListener implements Listener {

    private final ConversationManager manager;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        Set<Conversation> conversations = manager.getPlayerConversations(playerId);

        for (Conversation conv : conversations) {
            conv.removeParticipant(playerId);

            if (conv.getParticipants().size() <= 1) {
                manager.endConversation(conv.getId());
            }
        }

        manager.getPlayerIndex().invalidatePlayer(playerId);
    }
}

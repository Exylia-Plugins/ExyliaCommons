package net.exylia.commons.v2.conversation.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public class Conversation {

    private final String id;
    private final String recipientFormat;
    private final String senderFormat;
    private final Set<UUID> participants;

    public static Conversation create(String id, String recipientFormat, String senderFormat) {
        return new Conversation(id, recipientFormat, senderFormat, ConcurrentHashMap.newKeySet());
    }

    public boolean hasParticipant(UUID playerId) {
        return participants.contains(playerId);
    }

    public boolean hasParticipant(Player player) {
        return player != null && hasParticipant(player.getUniqueId());
    }

    public void addParticipant(UUID playerId) {
        participants.add(playerId);
    }

    public void removeParticipant(UUID playerId) {
        participants.remove(playerId);
    }
}

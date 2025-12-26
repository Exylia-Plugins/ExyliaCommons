package net.exylia.commons.v2.conversation.core;

import lombok.Getter;
import net.exylia.commons.v2.conversation.cache.PlayerConversationIndex;
import net.exylia.commons.v2.conversation.listener.ChatInterceptListener;
import net.exylia.commons.v2.conversation.listener.PlayerCleanupListener;
import net.exylia.commons.v2.conversation.model.Conversation;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ConversationManager {

    private static volatile ConversationManager instance;
    private static final Object LOCK = new Object();

    @Getter
    private final Plugin plugin;
    private final ConversationRegistry registry;
    @Getter
    private final PlayerConversationIndex playerIndex;
    private boolean initialized;

    private ConversationManager(Plugin plugin) {
        this.plugin = plugin;
        this.registry = new ConversationRegistry();
        this.playerIndex = new PlayerConversationIndex();
        this.initialized = false;
    }

    public static ConversationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ConversationManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static void initialize(Plugin plugin) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ConversationManager(plugin);
                    instance.init();
                }
            }
        }
    }

    private void init() {
        registerListeners();
        initialized = true;
    }

    private void registerListeners() {
        ChatInterceptListener chatListener = new ChatInterceptListener(this);
        PlayerCleanupListener cleanupListener = new PlayerCleanupListener(this);

        plugin.getServer().getPluginManager().registerEvents(chatListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(cleanupListener, plugin);
    }

    public void startConversation(String id, String recipientFormat, String senderFormat, Collection<Player> participants) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty");
        }

        if (recipientFormat == null || recipientFormat.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient format cannot be null or empty");
        }

        if (senderFormat == null || senderFormat.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender format cannot be null or empty");
        }

        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participants cannot be null or empty");
        }

        if (participants.size() < 2) {
            throw new IllegalArgumentException("Conversation must have at least 2 participants");
        }

        Set<Player> onlineParticipants = participants.stream()
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toSet());

        if (onlineParticipants.size() < 2) {
            throw new IllegalArgumentException("At least 2 participants must be online");
        }

        Conversation conversation = Conversation.create(id, recipientFormat, senderFormat);

        for (Player player : onlineParticipants) {
            UUID playerId = player.getUniqueId();
            conversation.addParticipant(playerId);
            playerIndex.addConversation(playerId, id);

            DebugAPI.logLibDebug(DebugCategory.CONVERSATION,
                String.format("[START] Added participant: %s", player.getName()));
        }

        registry.register(conversation);
        DebugAPI.logLibDebug(DebugCategory.CONVERSATION,
            String.format("[START] Conversation '%s' registered successfully", id));
    }

    public void endConversation(String conversationId) {
        DebugAPI.logLibDebug(DebugCategory.CONVERSATION,
            String.format("[END] Ending conversation '%s'", conversationId));

        registry.get(conversationId).ifPresent(conversation -> {
            for (UUID participantId : conversation.getParticipants()) {
                playerIndex.removeConversation(participantId, conversationId);
            }
            registry.unregister(conversationId);
            DebugAPI.logLibDebug(DebugCategory.CONVERSATION,
                String.format("[END] Conversation '%s' unregistered successfully", conversationId));
        });
    }

    public Set<Conversation> getPlayerConversations(UUID playerId) {
        Set<String> conversationIds = playerIndex.getConversations(playerId);

        if (conversationIds.isEmpty()) {
            return Collections.emptySet();
        }

        return conversationIds.stream()
                .map(registry::get)
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .collect(Collectors.toSet());
    }

    public boolean isInConversation(UUID playerId) {
        return !playerIndex.getConversations(playerId).isEmpty();
    }

    public int getActiveCount() {
        return registry.getActiveCount();
    }

    public void addParticipant(String conversationId, Player player) {
        if (conversationId == null || player == null) {
            return;
        }

        registry.get(conversationId).ifPresent(conversation -> {
            UUID playerId = player.getUniqueId();
            conversation.addParticipant(playerId);
            playerIndex.addConversation(playerId, conversationId);
        });
    }

    public void removeParticipant(String conversationId, Player player) {
        if (conversationId == null || player == null) {
            return;
        }

        registry.get(conversationId).ifPresent(conversation -> {
            UUID playerId = player.getUniqueId();
            conversation.removeParticipant(playerId);
            playerIndex.removeConversation(playerId, conversationId);
        });
    }

    public boolean conversationExists(String conversationId) {
        return conversationId != null && registry.get(conversationId).isPresent();
    }

    public void clearAll() {
        registry.clear();
        playerIndex.invalidateAll();
    }

    public void shutdown() {
        clearAll();
        initialized = false;
    }
}

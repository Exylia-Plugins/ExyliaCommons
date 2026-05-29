package net.exylia.commons.v2.chat.core;

import net.exylia.commons.v2.chat.detect.BedrockDetector;
import net.exylia.commons.v2.chat.detect.DialogCapabilityDetector;
import net.exylia.commons.v2.chat.handler.ChatInputHandler;
import net.exylia.commons.v2.chat.handler.DialogInputHandler;
import net.exylia.commons.v2.chat.handler.FloodgateInputHandler;
import net.exylia.commons.v2.chat.handler.InventoryInputHandler;
import net.exylia.commons.v2.chat.request.BooleanInputRequest;
import net.exylia.commons.v2.chat.request.ConfirmationRequest;
import net.exylia.commons.v2.chat.request.InputRequest;
import net.exylia.commons.v2.chat.request.MultiNumberInputRequest;
import net.exylia.commons.v2.chat.request.NumberFieldDef;
import net.exylia.commons.v2.chat.request.NumberInputRequest;
import net.exylia.commons.v2.chat.request.SingleOptionRequest;
import net.exylia.commons.v2.chat.request.TextInputRequest;
import net.exylia.commons.v2.chat.session.InputSession;
import net.exylia.commons.v2.chat.session.InputSession.HandlerType;
import net.exylia.commons.v2.config.schema.ConfigSchemaRegistry;
import net.exylia.commons.v2.chat.config.ChatInputDefaults;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInputManager implements Listener {

    private static ChatInputManager instance;
    private static Plugin plugin;

    private final Map<UUID, InputSession> sessions = new ConcurrentHashMap<>();
    private final ChatInputHandler chatHandler = new ChatInputHandler();
    private final InventoryInputHandler inventoryHandler = new InventoryInputHandler();
    private DialogInputHandler dialogHandler;
    private FloodgateInputHandler bedrockHandler;

    private ChatInputManager() {}

    public static void init(Plugin pluginInstance) {
        if (instance != null) return;
        plugin = pluginInstance;
        instance = new ChatInputManager();
        ConfigSchemaRegistry.ensureDefaults(ChatInputDefaults.class);
        plugin.getServer().getPluginManager().registerEvents(instance, plugin);
        plugin.getServer().getPluginManager().registerEvents(instance.chatHandler, plugin);
        plugin.getServer().getPluginManager().registerEvents(instance.inventoryHandler, plugin);
        if (DialogCapabilityDetector.isServerDialogCapable()) {
            instance.dialogHandler = new DialogInputHandler();
            instance.dialogHandler.registerEventListener();
        }
        if (BedrockDetector.isFloodgateAvailable()) {
            instance.bedrockHandler = new FloodgateInputHandler();
        }
    }

    public static void shutdown() {
        if (instance == null) return;
        instance.sessions.values().forEach(session -> {
            if (session.isActive()) {
                getHandlerFor(instance, session.getHandlerType()).close(session);
            }
        });
        instance.sessions.clear();
        if (instance.dialogHandler != null) {
            instance.dialogHandler.unregisterEventListener();
        }
        HandlerList.unregisterAll(instance);
        HandlerList.unregisterAll(instance.chatHandler);
        HandlerList.unregisterAll(instance.inventoryHandler);
        instance = null;
        plugin = null;
    }

    public static ChatInputManager getInstance() {
        return instance;
    }

    public void submit(Player player, InputRequest<?> request) {
        cancelSessionInternal(player);

        HandlerType handlerType = resolveHandlerType(player, request);

        if (request instanceof MultiNumberInputRequest multiReq && handlerType != HandlerType.DIALOG) {
            startMultiNumberSequential(player, multiReq, 0, new LinkedHashMap<>());
            return;
        }

        DebugAPI.logLibDebug("[ChatInput] " + player.getName() + " submit → handler=" + handlerType + " requestType=" + request.getClass().getSimpleName());
        InputSession session = new InputSession(player, request, handlerType);
        sessions.put(player.getUniqueId(), session);

        boolean shown = switch (handlerType) {
            case DIALOG -> dialogHandler.show(session);
            case CHAT -> { chatHandler.show(session); yield true; }
            case INVENTORY -> { inventoryHandler.show(session); yield true; }
            case BEDROCK -> { bedrockHandler.show(session); yield true; }
        };

        if (!shown) {
            DebugAPI.logLibDebug("[ChatInput] " + player.getName() + " DIALOG show() returned false — falling back");
            sessions.remove(player.getUniqueId());
            if (request instanceof MultiNumberInputRequest multiReq) {
                startMultiNumberSequential(player, multiReq, 0, new LinkedHashMap<>());
                return;
            }
            HandlerType fallback = fallbackHandlerType(request);
            DebugAPI.logLibDebug("[ChatInput] " + player.getName() + " fallback handler=" + fallback);
            InputSession fallbackSession = new InputSession(player, request, fallback);
            sessions.put(player.getUniqueId(), fallbackSession);
            if (fallback == HandlerType.CHAT) {
                chatHandler.show(fallbackSession);
            } else {
                inventoryHandler.show(fallbackSession);
            }
        }
    }

    private void startMultiNumberSequential(Player player, MultiNumberInputRequest original, int fieldIndex, Map<String, Number> collected) {
        List<NumberFieldDef> fields = original.getFields();
        if (fieldIndex >= fields.size()) {
            original.accept(collected);
            return;
        }
        NumberFieldDef field = fields.get(fieldIndex);
        NumberInputRequest req = new NumberInputRequest(player, field.label(), field.decimals());
        req.setMin(field.min());
        req.setMax(field.max());
        req.setOnCancel(original.getOnCancel());
        req.setOnResponse(val -> {
            collected.put(field.key(), val);
            startMultiNumberSequential(player, original, fieldIndex + 1, collected);
        });
        submit(player, req);
    }

    public void cancelSession(Player player) {
        InputSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        session.setActive(false);
        getHandlerFor(this, session.getHandlerType()).close(session);
        Runnable onCancel = session.getRequest().getOnCancel();
        if (onCancel != null) onCancel.run();
    }

    public void handleCancelFromHandler(Player player) {
        InputSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        session.setActive(false);
        Runnable onCancel = session.getRequest().getOnCancel();
        if (onCancel != null) onCancel.run();
    }

    @SuppressWarnings("unchecked")
    public void handleCompleteFromHandler(UUID playerUuid, Object value) {
        InputSession session = sessions.remove(playerUuid);
        if (session == null) return;
        session.setActive(false);
        ((InputRequest<Object>) session.getRequest()).accept(value);
    }

    public InputSession getActiveSession(UUID playerUuid) {
        return sessions.get(playerUuid);
    }

    public boolean hasActiveSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelSession(event.getPlayer());
    }

    private void cancelSessionInternal(Player player) {
        InputSession existing = sessions.remove(player.getUniqueId());
        if (existing == null) return;
        existing.setActive(false);
        getHandlerFor(this, existing.getHandlerType()).close(existing);
    }

    private HandlerType resolveHandlerType(Player player, InputRequest<?> request) {
        if (request.isForceChat()) return HandlerType.CHAT;
        if (BedrockDetector.isBedrockPlayer(player)) {
            if (BedrockDetector.isFloodgateConfirmed(player)) {
                DebugAPI.logLibDebug("[ChatInput] " + player.getName() + " Floodgate confirmed → BEDROCK handler");
                return HandlerType.BEDROCK;
            }
            HandlerType fallback = fallbackHandlerType(request);
            DebugAPI.logLibDebug("[ChatInput] " + player.getName() + " Bedrock detected (UUID/prefix) but Floodgate API unconfirmed → fallback=" + fallback);
            return fallback;
        }
        if (DialogCapabilityDetector.canUseDialog(player)) return HandlerType.DIALOG;
        return fallbackHandlerType(request);
    }

    private HandlerType fallbackHandlerType(InputRequest<?> request) {
        if (request instanceof TextInputRequest || request instanceof NumberInputRequest || request instanceof MultiNumberInputRequest) {
            return HandlerType.CHAT;
        }
        return HandlerType.INVENTORY;
    }

    private interface HandlerRef {
        void close(InputSession session);
    }

    private static HandlerRef getHandlerFor(ChatInputManager mgr, HandlerType type) {
        return switch (type) {
            case DIALOG -> mgr.dialogHandler != null ? mgr.dialogHandler::close : session -> {};
            case CHAT -> mgr.chatHandler::close;
            case INVENTORY -> mgr.inventoryHandler::close;
            case BEDROCK -> mgr.bedrockHandler != null ? mgr.bedrockHandler::close : session -> {};
        };
    }
}

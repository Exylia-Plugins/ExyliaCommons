package net.exylia.commons.v2.chat.handler;

import net.exylia.commons.v2.chat.config.ChatInputDefaults;
import net.exylia.commons.v2.chat.core.ChatInputManager;
import net.exylia.commons.v2.chat.request.InputRequest;
import net.exylia.commons.v2.chat.request.NumberInputRequest;
import net.exylia.commons.v2.chat.request.TextInputRequest;
import net.exylia.commons.v2.chat.session.InputSession;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.visual.api.MessageAPI;
import net.exylia.commons.v2.visual.api.TitleAPI;
import net.exylia.commons.v2.visual.builder.TitleBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class ChatInputHandler implements Listener {

    public void show(InputSession session) {
        Player player = session.getPlayer();
        InputRequest<?> request = session.getRequest();
        MessageAPI.send(player, request.getPrompt());
        if (ChatInputDefaults.ChatInput.ChatFallback.SHOW_TITLE) {
            String titleText = request.getTitleText() != null ? request.getTitleText() : request.getPrompt();
            TitleAPI.send(player, TitleBuilder.create()
                .title(titleText)
                .subtitle(ChatInputDefaults.ChatInput.ChatFallback.SUBTITLE_TEXT)
                .times(10, 10000000, 20)
                .build()
            );
        }
    }

    public void close(InputSession session) {
        TitleAPI.cancelAll(session.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        InputSession session = ChatInputManager.getInstance().getActiveSession(uuid);
        if (session == null || session.getHandlerType() != InputSession.HandlerType.CHAT) return;

        event.setCancelled(true);
        String input = event.getMessage();

        TaskAPI.sync(() -> handleInput(event.getPlayer(), input));
    }

    private void handleInput(Player player, String input) {
        UUID uuid = player.getUniqueId();
        InputSession session = ChatInputManager.getInstance().getActiveSession(uuid);
        if (session == null) return;

        if (input.equalsIgnoreCase(ChatInputDefaults.ChatInput.ChatFallback.CANCEL_WORD)) {
            TitleAPI.cancelAll(player);
            ChatInputManager.getInstance().handleCancelFromHandler(player);
            MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.CANCEL_MESSAGE);
            return;
        }

        InputRequest<?> request = session.getRequest();

        if (request instanceof TextInputRequest textReq) {
            handleTextInput(player, uuid, textReq, input);
        } else if (request instanceof NumberInputRequest numReq) {
            handleNumberInput(player, uuid, numReq, input);
        }
    }

    private void handleTextInput(Player player, UUID uuid, TextInputRequest request, String input) {
        if (request.getTransformer() != null) {
            input = request.getTransformer().apply(input);
        }
        if (request.getMaxLength() > 0 && input.length() > request.getMaxLength()) {
            MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE);
            return;
        }
        if (request.getValidator() != null && !request.getValidator().test(input)) {
            MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE);
            return;
        }
        TitleAPI.cancelAll(player);
        ChatInputManager.getInstance().handleCompleteFromHandler(uuid, input);
    }

    private void handleNumberInput(Player player, UUID uuid, NumberInputRequest request, String input) {
        try {
            Number value;
            if (request.isDecimals()) {
                double d = Double.parseDouble(input);
                if (d < request.getMin() || d > request.getMax()) {
                    MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE);
                    return;
                }
                if (request.getValidator() != null && !request.getValidator().test(d)) {
                    MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE);
                    return;
                }
                value = d;
            } else {
                long l = Long.parseLong(input);
                if (l < request.getMin() || l > request.getMax()) {
                    MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE);
                    return;
                }
                if (request.getValidator() != null && !request.getValidator().test(l)) {
                    MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE);
                    return;
                }
                value = l;
            }
            TitleAPI.cancelAll(player);
            ChatInputManager.getInstance().handleCompleteFromHandler(uuid, value);
        } catch (NumberFormatException e) {
            MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE);
        }
    }
}

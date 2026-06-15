package net.exylia.commons.v2.chat.session;

import lombok.Getter;
import net.exylia.commons.v2.chat.request.InputRequest;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class InputSession {

    public enum HandlerType {
        DIALOG,
        CHAT,
        INVENTORY,
        BEDROCK
    }

    private final UUID sessionId;
    private final Player player;
    private final InputRequest<?> request;
    private final HandlerType handlerType;
    private volatile boolean active = true;

    public InputSession(Player player, InputRequest<?> request, HandlerType handlerType) {
        this.sessionId = UUID.randomUUID();
        this.player = player;
        this.request = request;
        this.handlerType = handlerType;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

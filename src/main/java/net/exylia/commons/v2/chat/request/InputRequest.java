package net.exylia.commons.v2.chat.request;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@Getter
public abstract class InputRequest<T> {

    private final Player player;
    private final String prompt;

    @Setter
    private Runnable onCancel;

    @Setter
    private boolean forceChat;

    @Setter
    private String titleText;

    protected InputRequest(Player player, String prompt) {
        this.player = player;
        this.prompt = prompt;
    }

    public abstract void accept(T value);
}

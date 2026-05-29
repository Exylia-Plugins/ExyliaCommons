package net.exylia.commons.v2.chat.request;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

@Getter
@Setter
public class BooleanInputRequest extends InputRequest<Boolean> {

    private Consumer<Boolean> onResponse;

    public BooleanInputRequest(Player player, String prompt) {
        super(player, prompt);
    }

    @Override
    public void accept(Boolean value) {
        if (onResponse != null) {
            onResponse.accept(value);
        }
    }
}

package net.exylia.commons.v2.chat.request;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@Getter
@Setter
public class TextInputRequest extends InputRequest<String> {

    private int maxLength = 256;
    private Predicate<String> validator;
    private UnaryOperator<String> transformer;
    private Consumer<String> onResponse;

    public TextInputRequest(Player player, String prompt) {
        super(player, prompt);
    }

    @Override
    public void accept(String value) {
        if (onResponse != null) {
            onResponse.accept(value);
        }
    }
}

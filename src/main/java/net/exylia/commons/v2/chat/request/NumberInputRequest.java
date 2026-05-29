package net.exylia.commons.v2.chat.request;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
@Setter
public class NumberInputRequest extends InputRequest<Number> {

    private double min = -Double.MAX_VALUE;
    private double max = Double.MAX_VALUE;
    private boolean decimals = false;
    private Consumer<Number> onResponse;
    private Predicate<Number> validator;

    public NumberInputRequest(Player player, String prompt, boolean decimals) {
        super(player, prompt);
        this.decimals = decimals;
    }

    @Override
    public void accept(Number value) {
        if (onResponse != null) {
            onResponse.accept(value);
        }
    }
}

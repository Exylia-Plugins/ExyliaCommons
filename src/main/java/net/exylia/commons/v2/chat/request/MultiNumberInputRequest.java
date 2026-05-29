package net.exylia.commons.v2.chat.request;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public class MultiNumberInputRequest extends InputRequest<Map<String, Number>> {

    private final List<NumberFieldDef> fields = new ArrayList<>();

    @Setter
    private Consumer<Map<String, Number>> onResponse;

    public MultiNumberInputRequest(Player player, String prompt) {
        super(player, prompt);
    }

    public void addField(NumberFieldDef field) {
        fields.add(field);
    }

    @Override
    public void accept(Map<String, Number> value) {
        if (onResponse != null) {
            onResponse.accept(value);
        }
    }
}

package net.exylia.commons.v2.chat.request;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Getter
@Setter
public class SingleOptionRequest extends InputRequest<String> {

    private final List<OptionEntry> options = new ArrayList<>();
    private Consumer<String> onResponse;
    private int columns = 1;

    public SingleOptionRequest(Player player, String prompt) {
        super(player, prompt);
    }

    public void addOption(String key, String label) {
        options.add(new OptionEntry(key, label));
    }

    public List<OptionEntry> getOptions() {
        return Collections.unmodifiableList(options);
    }

    @Override
    public void accept(String value) {
        if (onResponse != null) {
            onResponse.accept(value);
        }
    }
}

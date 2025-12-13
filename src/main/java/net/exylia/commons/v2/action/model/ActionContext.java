package net.exylia.commons.v2.action.model;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.action.parser.ParsedArguments;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class ActionContext {
    private final Player player;
    private final ActionSource source;
    @Builder.Default
    private final Map<String, Object> data = new HashMap<>();
    private final ParsedArguments arguments;
    private final Action action;
    @Builder.Default
    private final String defaultNamespace = "global";
    @Builder.Default
    private final long executionStartTime = System.currentTimeMillis();
    @Builder.Default
    private final UUID executionId = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    public boolean hasData(String key) {
        return data.containsKey(key);
    }

    public ActionContext withData(String key, Object value) {
        Map<String, Object> newData = new HashMap<>(this.data);
        newData.put(key, value);
        return toBuilder().data(newData).build();
    }
}

package net.exylia.commons.v2.visual.context;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VisualContext {
    private final Map<String, Object> data;
    private final Map<Class<?>, Object> typedData;

    public VisualContext() {
        this.data = new ConcurrentHashMap<>();
        this.typedData = new ConcurrentHashMap<>();
    }

    public static VisualContext empty() {
        return new VisualContext();
    }

    public static VisualContext from(ExyliaContext exyliaContext) {
        VisualContext context = new VisualContext();
        if (exyliaContext != null) {
            for (String key : exyliaContext.getKeys()) {
                Object value = exyliaContext.get(key);
                if (value != null) {
                    context.data.put(key, value);
                }
            }
            for (Class<?> type : exyliaContext.getTypes()) {
                Object value = exyliaContext.get(type);
                if (value != null) {
                    context.typedData.put(type, value);
                }
            }
        }
        return context;
    }

    public VisualContext put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public <T> VisualContext put(Class<T> type, T value) {
        typedData.put(type, value);
        return this;
    }

    public Object get(String key) {
        return data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        return (T) typedData.get(type);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public <T> boolean has(Class<T> type) {
        return typedData.containsKey(type);
    }

    public VisualContext withPlayer(Player player) {
        put("player", player);
        put("player_name", player.getName());
        put("player_uuid", player.getUniqueId().toString());
        put(Player.class, player);
        return this;
    }

    public VisualContext withCurrentTime() {
        long currentTime = System.currentTimeMillis();
        put("current_time", currentTime);
        put("current_time_seconds", currentTime / 1000);
        return this;
    }

    public ExyliaContext toExyliaContext() {
        ExyliaContext exyliaContext = ExyliaContext.create();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            exyliaContext.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Class<?>, Object> entry : typedData.entrySet()) {
            exyliaContext.add((Class<Object>) entry.getKey(), entry.getValue());
        }
        return exyliaContext;
    }

    @SuppressWarnings("unchecked")
    public PlaceholderContext toPlaceholderContext() {
        PlaceholderContext placeholderContext = PlaceholderContext.create();

        Player player = get(Player.class);
        if (player != null) {
            placeholderContext.withPlayer(player);
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            placeholderContext.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Class<?>, Object> entry : typedData.entrySet()) {
            placeholderContext.with((Class<Object>) entry.getKey(), entry.getValue());
        }

        return placeholderContext;
    }

    public VisualContext copy() {
        VisualContext newContext = new VisualContext();
        newContext.data.putAll(this.data);
        newContext.typedData.putAll(this.typedData);
        return newContext;
    }

    public void copyFrom(VisualContext other) {
        if (other != null) {
            this.data.putAll(other.data);
            this.typedData.putAll(other.typedData);
        }
    }

    public void clear() {
        data.clear();
        typedData.clear();
    }

    public int hash() {
        return data.hashCode() + typedData.hashCode();
    }

    public Map<String, Object> getAllData() {
        return new HashMap<>(data);
    }
}
package net.exylia.commons.v2.placeholders.context;

import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceholderContext {
    private final Map<Class<?>, Object> typedData;
    private final Map<String, Object> keyedData;
    private Player player;

    public PlaceholderContext() {
        this.typedData = new ConcurrentHashMap<>();
        this.keyedData = new ConcurrentHashMap<>();
    }

    public static PlaceholderContext create() {
        return new PlaceholderContext();
    }

    public PlaceholderContext with(Object object) {
        if (object != null) {
            typedData.put(object.getClass(), object);
        }
        return this;
    }

    public <T> PlaceholderContext with(Class<T> type, T object) {
        if (object != null) {
            typedData.put(type, object);
        }
        return this;
    }

    public PlaceholderContext put(String key, Object value) {
        if (key != null && value != null) {
            keyedData.put(key, value);
        }
        return this;
    }

    public PlaceholderContext withPlayer(Player player) {
        this.player = player;
        if (player != null) {
            typedData.put(Player.class, player);
        }
        return this;
    }

    public PlaceholderContext withCurrentTime() {
        long currentMillis = System.currentTimeMillis();
        keyedData.put("current_time", currentMillis);
        keyedData.put("timestamp", currentMillis / 1000);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        Object value = typedData.get(type);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public Object get(String key) {
        return keyedData.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public Player getPlayer() {
        return player;
    }

    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> type) {
        T result = get(type);
        if (result != null) {
            return result;
        }

        for (Object value : keyedData.values()) {
            if (type.isInstance(value)) {
                return (T) value;
            }
        }

        return null;
    }

    public boolean has(Class<?> type) {
        return typedData.containsKey(type);
    }

    public boolean has(String key) {
        return keyedData.containsKey(key);
    }

    public PlaceholderContext copy() {
        PlaceholderContext copy = new PlaceholderContext();
        copy.typedData.putAll(this.typedData);
        copy.keyedData.putAll(this.keyedData);
        copy.player = this.player;
        return copy;
    }

    public void clear() {
        typedData.clear();
        keyedData.clear();
        player = null;
    }

    public int size() {
        return typedData.size() + keyedData.size();
    }

    public boolean isEmpty() {
        return typedData.isEmpty() && keyedData.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public net.exylia.commons.placeholders.ExyliaContext toExyliaContext() {
        net.exylia.commons.placeholders.ExyliaContext context = net.exylia.commons.placeholders.ExyliaContext.create();

        for (Map.Entry<Class<?>, Object> entry : typedData.entrySet()) {
            addToExyliaContextSafe(context, entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Object> entry : keyedData.entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }

        return context;
    }

    @SuppressWarnings("unchecked")
    private <T> void addToExyliaContextSafe(net.exylia.commons.placeholders.ExyliaContext context, Class<?> type, Object value) {
        context.add((Class<T>) type, (T) value);
    }
}

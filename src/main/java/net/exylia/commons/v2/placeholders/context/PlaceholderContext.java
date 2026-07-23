package net.exylia.commons.v2.placeholders.context;

import lombok.Getter;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PlaceholderContext {
    private final Map<Class<?>, Object> typedData;
    private final Map<String, Object> keyedData;
    @Getter
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

    public PlaceholderContext put(String key, Supplier<?> supplier) {
        if (key != null && supplier != null) {
            keyedData.put(key, supplier);
        }
        return this;
    }

    public PlaceholderContext addNumeric(String key, double delta) {
        if (key == null) return this;
        Object existing = keyedData.get(key);
        double current = existing instanceof Number number ? number.doubleValue() : 0.0;
        keyedData.put(key, current + delta);
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
        Object value = keyedData.get(key);
        if (value instanceof Supplier<?> supplier) {
            return supplier.get();
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> type) {
        T result = get(type);
        if (result != null) {
            return result;
        }

        for (Object value : typedData.values()) {
            if (type.isInstance(value)) {
                return (T) value;
            }
        }

        for (Object value : keyedData.values()) {
            Object resolved = value instanceof Supplier<?> s ? s.get() : value;
            if (type.isInstance(resolved)) {
                return (T) resolved;
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

    public PlaceholderContext merge(PlaceholderContext other) {
        if (other != null) {
            this.typedData.putAll(other.typedData);
            this.keyedData.putAll(other.keyedData);
            if (other.player != null) {
                this.player = other.player;
            }
        }
        return this;
    }

    public PlaceholderContext copyAndMerge(PlaceholderContext other) {
        PlaceholderContext merged = this.copy();
        return merged.merge(other);
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

    public String resolve(String template) {
        if (template == null || template.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : keyedData.entrySet()) {
            Object raw = entry.getValue() instanceof Supplier<?> s ? s.get() : entry.getValue();
            if (raw != null) {
                result = result.replace("%" + entry.getKey() + "%", String.valueOf(raw));
            }
        }
        return result;
    }

}

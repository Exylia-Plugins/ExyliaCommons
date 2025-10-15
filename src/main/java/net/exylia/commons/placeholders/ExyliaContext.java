package net.exylia.commons.placeholders;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ExyliaContext {

    private final Map<Class<?>, Object> typedData = new ConcurrentHashMap<>();

    private final Map<String, Object> keyedData = new ConcurrentHashMap<>();

    private final Map<String, Supplier<Object>> dynamicData = new ConcurrentHashMap<>();

    public ExyliaContext() {}

    public static ExyliaContext create() {
        return new ExyliaContext();
    }

    public static ExyliaContext of(Object... objects) {
        ExyliaContext exyliaContext = new ExyliaContext();
        exyliaContext.addAll(objects);
        return exyliaContext;
    }

    public static ExyliaContext of(Map<String, Object> data) {
        ExyliaContext exyliaContext = new ExyliaContext();
        exyliaContext.putAll(data);
        return exyliaContext;
    }

    public ExyliaContext add(Object object) {
        if (object != null) {
            typedData.put(object.getClass(), object);
        }
        return this;
    }

    public ExyliaContext addAll(Object... objects) {
        for (Object obj : objects) {
            add(obj);
        }
        return this;
    }

    public ExyliaContext addAll(Collection<Object> objects) {
        for (Object obj : objects) {
            add(obj);
        }
        return this;
    }

    public <T> ExyliaContext add(Class<T> type, T object) {
        if (object != null) {
            typedData.put(type, object);
        }
        return this;
    }

    public ExyliaContext put(String key, Object value) {
        if (key != null && value != null) {
            keyedData.put(key, value);
        }
        return this;
    }

    public ExyliaContext putAll(Map<String, Object> data) {
        if (data != null) {
            keyedData.putAll(data);
        }
        return this;
    }

    public ExyliaContext putDynamic(String key, Supplier<Object> supplier) {
        if (key != null && supplier != null) {
            dynamicData.put(key, supplier);
        }
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
        if (value != null) {
            return value;
        }

        Supplier<Object> supplier = dynamicData.get(key);
        if (supplier != null) {
            return supplier.get();
        }

        return null;
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

        for (Object value : keyedData.values()) {
            if (type.isInstance(value)) {
                return (T) value;
            }
        }

        for (Supplier<Object> supplier : dynamicData.values()) {
            try {
                Object value = supplier.get();
                if (type.isInstance(value)) {
                    return (T) value;
                }
            } catch (Exception ignored) {
                 
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(Class<T> type) {
        List<T> results = new ArrayList<>();

        T typed = get(type);
        if (typed != null) {
            results.add(typed);
        }

        for (Object value : keyedData.values()) {
            if (type.isInstance(value)) {
                results.add((T) value);
            }
        }

        for (Supplier<Object> supplier : dynamicData.values()) {
            try {
                Object value = supplier.get();
                if (type.isInstance(value)) {
                    results.add((T) value);
                }
            } catch (Exception ignored) {
                 
            }
        }

        return results;
    }

    public boolean has(Class<?> type) {
        return typedData.containsKey(type);
    }

    public boolean has(String key) {
        return keyedData.containsKey(key) || dynamicData.containsKey(key);
    }

    public boolean contains(Class<?> type) {
        return find(type) != null;
    }

    public ExyliaContext merge(ExyliaContext other) {
        if (other != null) {
            this.typedData.putAll(other.typedData);
            this.keyedData.putAll(other.keyedData);
            this.dynamicData.putAll(other.dynamicData);
        }
        return this;
    }

    public ExyliaContext copy() {
        ExyliaContext copy = new ExyliaContext();
        copy.typedData.putAll(this.typedData);
        copy.keyedData.putAll(this.keyedData);
        copy.dynamicData.putAll(this.dynamicData);
        return copy;
    }

    public ExyliaContext createChild() {
        return copy();
    }

    public ExyliaContext clear() {
        typedData.clear();
        keyedData.clear();
        dynamicData.clear();
        return this;
    }

    public String processPlaceholders(String text, Player player) {
        return PlaceholderSystemManager.getInstance().process(text, player, this.getAllObjects());
    }

    public String processPlaceholders(String text) {
        return processPlaceholders(text, null);
    }

    public Object[] getAllObjects() {
        List<Object> allObjects = new ArrayList<>();

        allObjects.add(this);

        allObjects.addAll(typedData.values());

        allObjects.addAll(keyedData.values());

        for (Supplier<Object> supplier : dynamicData.values()) {
            try {
                Object value = supplier.get();
                if (value != null) {
                    allObjects.add(value);
                }
            } catch (Exception ignored) {
                 
            }
        }

        return allObjects.toArray();
    }

    public int size() {
        return typedData.size() + keyedData.size() + dynamicData.size();
    }

    public boolean isEmpty() {
        return typedData.isEmpty() && keyedData.isEmpty() && dynamicData.isEmpty();
    }

    public Set<String> getKeys() {
        Set<String> keys = new HashSet<>(keyedData.keySet());
        keys.addAll(dynamicData.keySet());
        return keys;
    }

    public Set<Class<?>> getTypes() {
        return new HashSet<>(typedData.keySet());
    }

    public ExyliaContext withPlayer(Player player) {
        return add(Player.class, player);
    }

    public ExyliaContext withCurrentTime() {
        return putDynamic("current_time", System::currentTimeMillis)
                .putDynamic("current_date", () -> new Date().toString());
    }

    public ExyliaContext withCounter(String key) {
        final int[] counter = {0};
        return putDynamic(key, () -> ++counter[0]);
    }

    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Context{");

        if (!typedData.isEmpty()) {
            sb.append("typed=[");
            typedData.forEach((type, value) ->
                    sb.append(type.getSimpleName()).append(":").append(value.getClass().getSimpleName()).append(", "));
            sb.append("], ");
        }

        if (!keyedData.isEmpty()) {
            sb.append("keyed=[");
            keyedData.forEach((key, value) ->
                    sb.append(key).append(":").append(value.getClass().getSimpleName()).append(", "));
            sb.append("], ");
        }

        if (!dynamicData.isEmpty()) {
            sb.append("dynamic=[");
            dynamicData.keySet().forEach(key -> sb.append(key).append(", "));
            sb.append("], ");
        }

        sb.append("total=").append(size()).append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return getDebugInfo();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ExyliaContext that = (ExyliaContext) obj;
        return Objects.equals(typedData, that.typedData) &&
                Objects.equals(keyedData, that.keyedData);
         
    }

    @Override
    public int hashCode() {
        return Objects.hash(typedData, keyedData);
    }
}

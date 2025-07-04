package net.exylia.commons.placeholders;

import org.bukkit.entity.Player;

import java.util.function.Supplier;

/**
 * Builder avanzado para crear contextos complejos
 */
class ExyliaContextBuilder {
    private final ExyliaContext exyliaContext = new ExyliaContext();

    public static ExyliaContextBuilder create() {
        return new ExyliaContextBuilder();
    }

    public ExyliaContextBuilder add(Object object) {
        exyliaContext.add(object);
        return this;
    }

    public <T> ExyliaContextBuilder add(Class<T> type, T object) {
        exyliaContext.add(type, object);
        return this;
    }

    public ExyliaContextBuilder put(String key, Object value) {
        exyliaContext.put(key, value);
        return this;
    }

    public ExyliaContextBuilder putDynamic(String key, Supplier<Object> supplier) {
        exyliaContext.putDynamic(key, supplier);
        return this;
    }

    public ExyliaContextBuilder withPlayer(Player player) {
        exyliaContext.withPlayer(player);
        return this;
    }

    public ExyliaContextBuilder withCurrentTime() {
        exyliaContext.withCurrentTime();
        return this;
    }

    public ExyliaContextBuilder merge(ExyliaContext other) {
        exyliaContext.merge(other);
        return this;
    }

    public ExyliaContext build() {
        return exyliaContext;
    }
}
package net.exylia.commons.databaseV2.serialization.builtin;

import net.exylia.commons.databaseV2.serialization.Deserializer;
import net.exylia.commons.databaseV2.serialization.Serializer;

public class EnumSerializer implements Serializer<Enum<?>> {

    public static final EnumSerializer INSTANCE = new EnumSerializer();

    @Override
    public String serialize(Enum<?> value) {
        return value == null ? null : value.name();
    }
}

class EnumDeserializer<E extends Enum<E>> implements Deserializer<E> {

    @Override
    @SuppressWarnings("unchecked")
    public E deserialize(String value, Class<E> type) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

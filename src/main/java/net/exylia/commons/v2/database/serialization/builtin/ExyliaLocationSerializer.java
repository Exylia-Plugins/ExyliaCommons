package net.exylia.commons.v2.database.serialization.builtin;

import net.exylia.commons.v2.database.serialization.Deserializer;
import net.exylia.commons.v2.database.serialization.Serializer;
import net.exylia.commons.v2.teleport.model.ExyliaLocation;

public class ExyliaLocationSerializer implements Serializer<ExyliaLocation> {

    public static final ExyliaLocationSerializer INSTANCE = new ExyliaLocationSerializer();

    @Override
    public String serialize(ExyliaLocation value) {
        if (value == null) return null;
        return value.toString();
    }
}

class ExyliaLocationDeserializer implements Deserializer<ExyliaLocation> {

    static final ExyliaLocationDeserializer INSTANCE = new ExyliaLocationDeserializer();

    @Override
    public ExyliaLocation deserialize(String value, Class<ExyliaLocation> type) {
        if (value == null || value.isEmpty()) return null;
        try {
            return ExyliaLocation.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

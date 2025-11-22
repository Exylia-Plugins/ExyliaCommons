package net.exylia.commons.databaseV2.serialization.builtin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.exylia.commons.databaseV2.serialization.Deserializer;
import net.exylia.commons.databaseV2.serialization.Serializer;

public class ComponentSerializer implements Serializer<Component> {

    public static final ComponentSerializer INSTANCE = new ComponentSerializer();

    @Override
    public String serialize(Component value) {
        if (value == null) {
            return null;
        }
        return JSONComponentSerializer.json().serialize(value);
    }
}

class ComponentDeserializer implements Deserializer<Component> {

    @Override
    public Component deserialize(String value, Class<Component> type) {
        if (value == null || value.isEmpty()) {
            return Component.empty();
        }

        try {
            return JSONComponentSerializer.json().deserialize(value);
        } catch (Exception e) {
            return Component.text(value);
        }
    }
}

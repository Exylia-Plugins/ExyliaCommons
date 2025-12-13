package net.exylia.commons.v2.database.serialization.builtin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.exylia.commons.v2.database.serialization.Deserializer;
import net.exylia.commons.v2.database.serialization.Serializer;
import net.exylia.commons.v2.hologram.model.HologramData;

public class HologramDataSerializer implements Serializer<HologramData> {

    public static final HologramDataSerializer INSTANCE = new HologramDataSerializer();
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public String serialize(HologramData value) {
        if (value == null) {
            return null;
        }
        return GSON.toJson(value);
    }
}

class HologramDataDeserializer implements Deserializer<HologramData> {

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public HologramData deserialize(String value, Class<HologramData> type) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return GSON.fromJson(value, HologramData.class);
        } catch (Exception e) {
            return null;
        }
    }
}

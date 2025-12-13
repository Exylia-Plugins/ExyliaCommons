package net.exylia.commons.v2.database.serialization.builtin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.exylia.commons.v2.database.serialization.Deserializer;
import net.exylia.commons.v2.database.serialization.Serializer;
import net.exylia.commons.v2.visual.model.BossBarData;

public class BossBarDataSerializer implements Serializer<BossBarData> {

    public static final BossBarDataSerializer INSTANCE = new BossBarDataSerializer();
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public String serialize(BossBarData value) {
        if (value == null) {
            return null;
        }
        return GSON.toJson(value);
    }
}

class BossBarDataDeserializer implements Deserializer<BossBarData> {

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public BossBarData deserialize(String value, Class<BossBarData> type) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return GSON.fromJson(value, BossBarData.class);
        } catch (Exception e) {
            return null;
        }
    }
}

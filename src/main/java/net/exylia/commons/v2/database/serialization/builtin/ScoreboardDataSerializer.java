package net.exylia.commons.v2.database.serialization.builtin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.exylia.commons.v2.database.serialization.Deserializer;
import net.exylia.commons.v2.database.serialization.Serializer;
import net.exylia.commons.v2.scoreboard.model.ScoreboardData;

public class ScoreboardDataSerializer implements Serializer<ScoreboardData> {

    public static final ScoreboardDataSerializer INSTANCE = new ScoreboardDataSerializer();
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public String serialize(ScoreboardData value) {
        if (value == null) {
            return null;
        }
        return GSON.toJson(value);
    }
}

class ScoreboardDataDeserializer implements Deserializer<ScoreboardData> {

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public ScoreboardData deserialize(String value, Class<ScoreboardData> type) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return GSON.fromJson(value, ScoreboardData.class);
        } catch (Exception e) {
            return null;
        }
    }
}

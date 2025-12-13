package net.exylia.commons.v2.database.serialization.builtin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.exylia.commons.v2.database.serialization.Deserializer;
import net.exylia.commons.v2.database.serialization.Serializer;
import net.exylia.commons.v2.visual.model.ActionBarData;

public class ActionBarDataSerializer implements Serializer<ActionBarData> {

    public static final ActionBarDataSerializer INSTANCE = new ActionBarDataSerializer();
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public String serialize(ActionBarData value) {
        if (value == null) {
            return null;
        }
        return GSON.toJson(value);
    }
}

class ActionBarDataDeserializer implements Deserializer<ActionBarData> {

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public ActionBarData deserialize(String value, Class<ActionBarData> type) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return GSON.fromJson(value, ActionBarData.class);
        } catch (Exception e) {
            return null;
        }
    }
}

package net.exylia.commons.v2.database.serialization.builtin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.exylia.commons.v2.region.selection.Selection;
import net.exylia.commons.v2.database.serialization.Deserializer;
import net.exylia.commons.v2.database.serialization.Serializer;
import org.bukkit.Location;

public class SelectionSerializer implements Serializer<Selection> {

    public static final SelectionSerializer INSTANCE = new SelectionSerializer();
    private static final Gson GSON = new Gson();

    @Override
    public String serialize(Selection value) {
        if (value == null || !value.isComplete()) {
            return null;
        }

        JsonObject json = new JsonObject();
        json.addProperty("world", value.getPos1().getWorld().getName());
        json.addProperty("x1", value.getPos1().getX());
        json.addProperty("y1", value.getPos1().getY());
        json.addProperty("z1", value.getPos1().getZ());
        json.addProperty("x2", value.getPos2().getX());
        json.addProperty("y2", value.getPos2().getY());
        json.addProperty("z2", value.getPos2().getZ());

        return GSON.toJson(json);
    }
}

class SelectionDeserializer implements Deserializer<Selection> {

    private static final Gson GSON = new Gson();

    @Override
    public Selection deserialize(String value, Class<Selection> type) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            JsonObject json = GSON.fromJson(value, JsonObject.class);

            String worldName = json.get("world").getAsString();
            double x1 = json.get("x1").getAsDouble();
            double y1 = json.get("y1").getAsDouble();
            double z1 = json.get("z1").getAsDouble();
            double x2 = json.get("x2").getAsDouble();
            double y2 = json.get("y2").getAsDouble();
            double z2 = json.get("z2").getAsDouble();

            Location pos1 = new Location(WorldResolver.find(worldName), x1, y1, z1);
            Location pos2 = new Location(WorldResolver.find(worldName), x2, y2, z2);

            return Selection.of(pos1, pos2);
        } catch (Exception e) {
            return null;
        }
    }
}

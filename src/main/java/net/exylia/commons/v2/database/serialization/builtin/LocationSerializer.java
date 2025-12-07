package net.exylia.commons.v2.database.serialization.builtin;

import net.exylia.commons.v2.database.serialization.Deserializer;
import net.exylia.commons.v2.database.serialization.Serializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocationSerializer implements Serializer<Location> {

    public static final LocationSerializer INSTANCE = new LocationSerializer();

    @Override
    public String serialize(Location value) {
        if (value == null) {
            return null;
        }
        return String.format("%s,%.2f,%.2f,%.2f,%.2f,%.2f",
                value.getWorld().getName(),
                value.getX(),
                value.getY(),
                value.getZ(),
                value.getYaw(),
                value.getPitch());
    }
}

class LocationDeserializer implements Deserializer<Location> {

    @Override
    public Location deserialize(String value, Class<Location> type) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        String[] parts = value.split(",");
        if (parts.length < 5) {
            return null;
        }

        try {
            String world = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;

            return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}

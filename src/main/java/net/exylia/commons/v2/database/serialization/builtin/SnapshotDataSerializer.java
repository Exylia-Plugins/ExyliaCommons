package net.exylia.commons.v2.database.serialization.builtin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.exylia.commons.v2.database.serialization.Deserializer;
import net.exylia.commons.v2.database.serialization.Serializer;
import net.exylia.commons.v2.snapshot.model.SnapshotData;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotDataSerializer implements Serializer<SnapshotData> {

    public static final SnapshotDataSerializer INSTANCE = new SnapshotDataSerializer();
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public String serialize(SnapshotData value) {
        if (value == null) {
            return null;
        }

        try {
            Map<String, Object> data = new HashMap<>();

            data.put("gameMode", value.getGameMode() != null ? value.getGameMode().name() : null);

            data.put("armor", serializeItemArray(value.getArmor()));
            data.put("inventory", serializeItemArray(value.getInventory()));
            data.put("offHand", serializeItem(value.getOffHand()));

            data.put("health", value.getHealth());
            data.put("maxHealth", value.getMaxHealth());

            data.put("foodLevel", value.getFoodLevel());
            data.put("saturation", value.getSaturation());

            data.put("level", value.getLevel());
            data.put("exp", value.getExp());

            data.put("potionEffects", value.getPotionEffects());

            data.put("allowFlight", value.isAllowFlight());
            data.put("flying", value.isFlying());
            data.put("flySpeed", value.getFlySpeed());

            return GSON.toJson(data);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> serializeItemArray(ItemStack[] items) {
        if (items == null) {
            return null;
        }

        List<String> serialized = new ArrayList<>();
        for (ItemStack item : items) {
            serialized.add(serializeItem(item));
        }
        return serialized;
    }

    private String serializeItem(ItemStack item) {
        return ItemStackSerializer.INSTANCE.serialize(item);
    }
}

class SnapshotDataDeserializer implements Deserializer<SnapshotData> {

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    @SuppressWarnings("unchecked")
    public SnapshotData deserialize(String value, Class<SnapshotData> type) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            Map<String, Object> data = GSON.fromJson(value, Map.class);

            SnapshotData snapshotData = new SnapshotData();

            String gameModeStr = (String) data.get("gameMode");
            if (gameModeStr != null) {
                snapshotData.setGameMode(GameMode.valueOf(gameModeStr));
            }

            snapshotData.setArmor(deserializeItemArray((List<String>) data.get("armor")));
            snapshotData.setInventory(deserializeItemArray((List<String>) data.get("inventory")));
            snapshotData.setOffHand(deserializeItem((String) data.get("offHand")));

            snapshotData.setHealth(getDouble(data, "health"));
            snapshotData.setMaxHealth(getDouble(data, "maxHealth"));

            snapshotData.setFoodLevel(getInt(data, "foodLevel"));
            snapshotData.setSaturation(getFloat(data, "saturation"));

            snapshotData.setLevel(getInt(data, "level"));
            snapshotData.setExp(getFloat(data, "exp"));

            List<Map<String, Object>> effectsRaw = (List<Map<String, Object>>) data.get("potionEffects");
            if (effectsRaw != null) {
                List<SnapshotData.PotionEffectData> effects = new ArrayList<>();
                for (Map<String, Object> effectMap : effectsRaw) {
                    SnapshotData.PotionEffectData effectData = new SnapshotData.PotionEffectData();
                    effectData.setType((String) effectMap.get("type"));
                    effectData.setDuration(((Number) effectMap.get("duration")).intValue());
                    effectData.setAmplifier(((Number) effectMap.get("amplifier")).intValue());
                    effectData.setAmbient((Boolean) effectMap.get("ambient"));
                    effectData.setParticles((Boolean) effectMap.get("particles"));
                    effectData.setIcon((Boolean) effectMap.get("icon"));
                    effects.add(effectData);
                }
                snapshotData.setPotionEffects(effects);
            }

            snapshotData.setAllowFlight((Boolean) data.get("allowFlight"));
            snapshotData.setFlying((Boolean) data.get("flying"));
            snapshotData.setFlySpeed(getFloat(data, "flySpeed"));

            return snapshotData;
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack[] deserializeItemArray(List<String> items) {
        if (items == null) {
            return null;
        }

        ItemStack[] array = new ItemStack[items.size()];
        for (int i = 0; i < items.size(); i++) {
            array[i] = deserializeItem(items.get(i));
        }
        return array;
    }

    private ItemStack deserializeItem(String item) {
        if (item == null) {
            return null;
        }
        return new ItemStackDeserializer().deserialize(item, ItemStack.class);
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private float getFloat(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return 0.0f;
    }
}

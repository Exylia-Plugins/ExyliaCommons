package net.exylia.commons.v2.cooldown.core;

import com.google.gson.*;
import net.exylia.commons.v2.cooldown.model.ItemCooldown;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.Material;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemCooldownStorage {

    private static final Gson GSON = new GsonBuilder().create();
    private final File cooldownsDir;

    public ItemCooldownStorage(File dataFolder) {
        this.cooldownsDir = new File(dataFolder, "database/cooldowns");
        this.cooldownsDir.mkdirs();
    }

    public Map<String, ItemCooldown> load(UUID playerId) {
        File file = fileFor(playerId);
        if (!file.exists()) return Collections.emptyMap();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonArray array = GSON.fromJson(reader, JsonArray.class);
            if (array == null || array.isEmpty()) return Collections.emptyMap();
            Map<String, ItemCooldown> result = new ConcurrentHashMap<>();
            long now = System.currentTimeMillis();
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                String id = obj.get("id").getAsString();
                long expiryTimeMs = obj.get("expiryTimeMs").getAsLong();
                long durationMs = obj.get("durationMs").getAsLong();
                if (expiryTimeMs <= now) continue;
                Material material = obj.has("material") ? Material.getMaterial(obj.get("material").getAsString()) : null;
                result.put(id, ItemCooldown.fromStorage(id, playerId, expiryTimeMs, durationMs, material));
            }
            return result;
        } catch (Exception e) {
            DebugAPI.logLibError("Failed to load cooldowns for " + playerId + ": " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    public void save(UUID playerId, Map<String, ItemCooldown> cooldowns) {
        JsonArray array = new JsonArray();
        for (ItemCooldown cd : cooldowns.values()) {
            if (cd.isExpired()) continue;
            JsonObject obj = new JsonObject();
            obj.addProperty("id", cd.getId());
            obj.addProperty("expiryTimeMs", cd.getExpiryTimeMs());
            obj.addProperty("durationMs", cd.getDurationMs());
            if (cd.hasMaterial()) obj.addProperty("material", cd.getMaterial().name());
            array.add(obj);
        }
        File file = fileFor(playerId);
        if (array.isEmpty()) {
            if (file.exists()) file.delete();
            return;
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(array, writer);
        } catch (Exception e) {
            DebugAPI.logLibError("Failed to save cooldowns for " + playerId + ": " + e.getMessage());
        }
    }

    private File fileFor(UUID playerId) {
        return new File(cooldownsDir, playerId + ".json");
    }
}

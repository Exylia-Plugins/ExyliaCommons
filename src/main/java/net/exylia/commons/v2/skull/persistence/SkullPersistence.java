package net.exylia.commons.v2.skull.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SkullPersistence {

    private final File cacheFile;
    private final long ttlMillis;
    private final Map<String, SkullPersistenceEntry> store;
    private final Gson gson;
    private volatile boolean dirty = false;

    public SkullPersistence(File dataFolder, long ttlMillis) {
        File cacheDir = new File(dataFolder, "database/cache");
        cacheDir.mkdirs();
        this.cacheFile = new File(cacheDir, "skull_cache.json");
        this.ttlMillis = ttlMillis;
        this.store = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void load() {
        if (!cacheFile.exists()) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "No persistent skull cache found, starting fresh");
            return;
        }

        try (Reader reader = new FileReader(cacheFile)) {
            Type type = new TypeToken<Map<String, SkullPersistenceEntry>>() {}.getType();
            Map<String, SkullPersistenceEntry> loaded = gson.fromJson(reader, type);
            if (loaded == null || loaded.isEmpty()) {
                return;
            }

            long now = System.currentTimeMillis();
            loaded.forEach((key, entry) -> {
                if (now - entry.fetchedAt() <= ttlMillis) {
                    store.put(key, entry);
                }
            });

            int discarded = loaded.size() - store.size();
            DebugAPI.logLibInfo(DebugCategory.SKULL, "Loaded " + store.size() + " skulls from persistent cache"
                    + (discarded > 0 ? " (" + discarded + " expired entries discarded)" : ""));

            if (discarded > 0) {
                dirty = true;
            }
        } catch (IOException e) {
            DebugAPI.logLibError(DebugCategory.SKULL, "Failed to load persistent skull cache", e);
        }
    }

    public void save() {
        if (!dirty) {
            return;
        }

        try (Writer writer = new FileWriter(cacheFile)) {
            gson.toJson(store, writer);
            dirty = false;
            DebugAPI.logLibInfo(DebugCategory.SKULL, "Saved " + store.size() + " skulls to persistent cache");
        } catch (IOException e) {
            DebugAPI.logLibError(DebugCategory.SKULL, "Failed to save persistent skull cache", e);
        }
    }

    public Optional<String> getTexture(String playerName) {
        SkullPersistenceEntry entry = store.get(playerName.toLowerCase());
        if (entry == null) {
            return Optional.empty();
        }
        if (isExpired(entry)) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Persistent cache STALE for player: " + playerName);
            return Optional.empty();
        }
        DebugAPI.logLibDebug(DebugCategory.SKULL, "Persistent cache HIT for player: " + playerName);
        return Optional.of(entry.texture());
    }

    public Optional<String> getCachedUUID(String playerName) {
        SkullPersistenceEntry entry = store.get(playerName.toLowerCase());
        return entry != null ? Optional.of(entry.uuid()) : Optional.empty();
    }

    public void store(String playerName, String uuid, String texture) {
        store.put(playerName.toLowerCase(), new SkullPersistenceEntry(uuid, texture, System.currentTimeMillis()));
        dirty = true;
        DebugAPI.logLibDebug(DebugCategory.SKULL, "Stored persistent skull for player: " + playerName);
    }

    public void invalidate(String playerName) {
        if (store.remove(playerName.toLowerCase()) != null) {
            dirty = true;
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Invalidated persistent skull for player: " + playerName);
        }
    }

    public void clear() {
        store.clear();
        dirty = true;
    }

    public int size() {
        return store.size();
    }

    private boolean isExpired(SkullPersistenceEntry entry) {
        return System.currentTimeMillis() - entry.fetchedAt() > ttlMillis;
    }
}

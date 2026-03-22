package net.exylia.commons.v2.skull.fetcher;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.skull.config.SkullConfig;
import net.exylia.commons.v2.skull.core.SkullCache;
import net.exylia.commons.v2.skull.core.SkullExecutor;
import net.exylia.commons.v2.skull.persistence.SkullPersistence;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TextureFetcher {

    private final MojangFetcher mojangFetcher;
    private final SkullExecutor executor;
    private final SkullCache cache;
    private final SkullConfig config;
    private final SkullPersistence persistence;

    public TextureFetcher(MojangFetcher mojangFetcher, SkullExecutor executor, SkullCache cache,
                          SkullConfig config, SkullPersistence persistence) {
        this.mojangFetcher = mojangFetcher;
        this.executor = executor;
        this.cache = cache;
        this.config = config;
        this.persistence = persistence;
    }

    public String encodeURLToBase64(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    public CompletableFuture<Optional<String>> fetchPlayerTextureAsync(String playerName) {
        if (playerName == null || playerName.isEmpty() || playerName.contains("%")) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Invalid player name: " + playerName);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        if (persistence != null) {
            Optional<String> persistedTexture = persistence.getTexture(playerName);
            if (persistedTexture.isPresent()) {
                return CompletableFuture.completedFuture(persistedTexture);
            }
        }

        if (cache.isRateLimited()) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Texture fetch async skipped for " + playerName + ": rate limited");
            return CompletableFuture.completedFuture(Optional.empty());
        }

        DebugAPI.logLibDebug(DebugCategory.SKULL, "Starting async texture fetch for player: " + playerName);
        return executor.submit(() -> {
            String uuid = persistence != null ? persistence.getCachedUUID(playerName).orElse(null) : null;

            if (uuid == null) {
                Optional<String> uuidOpt = mojangFetcher.fetchPlayerUUID(playerName);
                if (uuidOpt.isEmpty()) {
                    DebugAPI.logLibWarn(DebugCategory.SKULL, "No UUID returned for player: " + playerName);
                    return Optional.empty();
                }
                uuid = uuidOpt.get();
                if ("NOT_FOUND".equals(uuid)) {
                    DebugAPI.logLibDebug(DebugCategory.SKULL, "Player not found, using default texture: " + playerName);
                    return Optional.of(config.getDefaultTexture());
                }
            }

            Optional<String> texture = mojangFetcher.fetchPlayerTexture(uuid);
            if (texture.isPresent() && persistence != null) {
                persistence.store(playerName, uuid, texture.get());
            }
            return texture;
        });
    }
}

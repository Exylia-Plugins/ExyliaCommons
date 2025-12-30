package net.exylia.commons.v2.skull.fetcher;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.skull.config.SkullConfig;
import net.exylia.commons.v2.skull.core.SkullCache;
import net.exylia.commons.v2.skull.core.SkullExecutor;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TextureFetcher {

    private final MojangFetcher mojangFetcher;
    private final SkullExecutor executor;
    private final SkullCache cache;
    private final SkullConfig config;

    public TextureFetcher(MojangFetcher mojangFetcher, SkullExecutor executor, SkullCache cache, SkullConfig config) {
        this.mojangFetcher = mojangFetcher;
        this.executor = executor;
        this.cache = cache;
        this.config = config;
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

        if (cache.isRateLimited()) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Texture fetch async skipped for " + playerName + ": rate limited");
            return CompletableFuture.completedFuture(Optional.empty());
        }

        DebugAPI.logLibDebug(DebugCategory.SKULL, "Starting async texture fetch for player: " + playerName);
        return executor.submit(() -> {
            Optional<String> uuidOpt = mojangFetcher.fetchPlayerUUID(playerName);

            if (uuidOpt.isEmpty()) {
                DebugAPI.logLibWarn(DebugCategory.SKULL, "No UUID returned for player: " + playerName);
                return Optional.empty();
            }

            String uuid = uuidOpt.get();
            if ("NOT_FOUND".equals(uuid)) {
                DebugAPI.logLibDebug(DebugCategory.SKULL, "Player not found, using default texture: " + playerName);
                return Optional.of(config.getDefaultTexture());
            }

            Optional<String> texture = mojangFetcher.fetchPlayerTexture(uuid);
            if (texture.isPresent()) {
                DebugAPI.logLibDebug(DebugCategory.SKULL, "Texture fetch completed for player: " + playerName);
            }
            return texture;
        });
    }
}

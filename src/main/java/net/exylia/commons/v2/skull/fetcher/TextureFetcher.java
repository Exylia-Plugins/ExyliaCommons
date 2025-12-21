package net.exylia.commons.v2.skull.fetcher;

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
            return CompletableFuture.completedFuture(Optional.empty());
        }

        if (cache.isRateLimited()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return executor.submit(() -> {
            Optional<String> uuidOpt = mojangFetcher.fetchPlayerUUID(playerName);

            if (uuidOpt.isEmpty()) {
                return Optional.empty();
            }

            String uuid = uuidOpt.get();
            if ("NOT_FOUND".equals(uuid)) {
                return Optional.of(config.getDefaultTexture());
            }

            return mojangFetcher.fetchPlayerTexture(uuid);
        });
    }
}

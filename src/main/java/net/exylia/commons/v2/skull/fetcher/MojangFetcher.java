package net.exylia.commons.v2.skull.fetcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.java.Log;
import net.exylia.commons.v2.skull.config.SkullConfig;
import net.exylia.commons.v2.skull.core.SkullCache;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Log
public class MojangFetcher {

    private final HttpClient httpClient;
    private final SkullConfig config;
    private final SkullCache cache;

    public MojangFetcher(SkullConfig config, SkullCache cache) {
        this.config = config;
        this.cache = cache;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getHttpTimeout()))
                .build();
    }

    public Optional<String> fetchPlayerUUID(String playerName) {
        if (cache.isRateLimited()) {
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getMojangApiUrl() + playerName))
                    .timeout(Duration.ofSeconds(config.getHttpTimeout()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return handleUUIDResponse(response, playerName);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private Optional<String> handleUUIDResponse(HttpResponse<String> response, String playerName) {
        return switch (response.statusCode()) {
            case 200 -> parseUUID(response.body());
            case 404 -> Optional.of("NOT_FOUND");
            case 429 -> {
                cache.setRateLimitBackoff(config.getRateLimitBackoff());
                yield Optional.empty();
            }
            default -> Optional.empty();
        };
    }

    private Optional<String> parseUUID(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return Optional.ofNullable(json.get("id").getAsString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<String> fetchPlayerTexture(String uuid) {
        if (cache.isRateLimited()) {
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getMojangSessionUrl() + uuid))
                    .timeout(Duration.ofSeconds(config.getHttpTimeout()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return handleTextureResponse(response);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private Optional<String> handleTextureResponse(HttpResponse<String> response) {
        return switch (response.statusCode()) {
            case 200 -> parseTexture(response.body());
            case 429 -> {
                cache.setRateLimitBackoff(config.getRateLimitBackoff());
                yield Optional.empty();
            }
            default -> Optional.empty();
        };
    }

    private Optional<String> parseTexture(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("properties") && json.get("properties").isJsonArray()) {
                var properties = json.getAsJsonArray("properties");
                for (var property : properties) {
                    var prop = property.getAsJsonObject();
                    if ("textures".equals(prop.get("name").getAsString())) {
                        return Optional.ofNullable(prop.get("value").getAsString());
                    }
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

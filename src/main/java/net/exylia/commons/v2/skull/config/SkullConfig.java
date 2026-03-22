package net.exylia.commons.v2.skull.config;

import lombok.Getter;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
public class SkullConfig {

    private int threadPoolSize = 4;
    private int maxCacheSize = 100000;
    private long cacheExpiration = 86400000L;
    private long rateLimitBackoff = 60000L;
    private long networkErrorBackoff = 5000L;
    private int httpTimeout = 10;
    private int cleanupInterval = 28800;
    private boolean enableMetrics = true;
    private String defaultTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGFkNzJhMDdkYTI3ZDg1YzA5MTY4ODFlNTUyMmVlZWQxZTNkYWYyMTdhMzhjMWEifX19";

    private String mojangApiUrl = "https://api.mojang.com/users/profiles/minecraft/";
    private String mojangSessionUrl = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private int batchDelay = 150;
    private int preloadDelay = 50;

    private File dataFolder = null;
    private long persistentCacheTtl = 604800000L; // 7 days
}

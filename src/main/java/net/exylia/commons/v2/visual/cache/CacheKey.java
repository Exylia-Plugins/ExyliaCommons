package net.exylia.commons.v2.visual.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@EqualsAndHashCode
public class CacheKey {
    private final String text;
    private final UUID playerId;
    private final int contextHash;

    private CacheKey(String text, UUID playerId, int contextHash) {
        this.text = text;
        this.playerId = playerId;
        this.contextHash = contextHash;
    }

    public static CacheKey of(String text, UUID playerId, int contextHash) {
        return new CacheKey(text, playerId, contextHash);
    }

    public static CacheKey global(String text) {
        return new CacheKey(text, null, 0);
    }

    @Override
    public String toString() {
        return "CacheKey{" +
                "text='" + text + '\'' +
                ", playerId=" + playerId +
                ", contextHash=" + contextHash +
                '}';
    }
}

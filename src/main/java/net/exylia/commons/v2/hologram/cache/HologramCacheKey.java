package net.exylia.commons.v2.hologram.cache;

import java.util.Objects;

public class HologramCacheKey {
    public record PlayerVisibilityKey(String playerId, String hologramId) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PlayerVisibilityKey that)) return false;
            return Objects.equals(playerId, that.playerId) &&
                    Objects.equals(hologramId, that.hologramId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerId, hologramId);
        }
    }
}

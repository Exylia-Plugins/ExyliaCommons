package net.exylia.commons.v2.hologram.cache;

import org.bukkit.Location;

import java.util.Objects;

public class HologramCacheKey {
    public record LocationKey(String world, int x, int y, int z, double radius) {
        public LocationKey(Location location, double radius) {
            this(
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    radius
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LocationKey that)) return false;
            return x == that.x &&
                    y == that.y &&
                    z == that.z &&
                    Double.compare(that.radius, radius) == 0 &&
                    Objects.equals(world, that.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, y, z, radius);
        }
    }

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

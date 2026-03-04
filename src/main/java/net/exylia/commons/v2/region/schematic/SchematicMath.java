package net.exylia.commons.v2.region.schematic;

import org.bukkit.Location;
import org.bukkit.World;

final class SchematicMath {

    private static final long BLOCK_BUDGET_NANOS = 800_000L;
    private static final long SECTION_BUDGET_NANOS = 1_200_000L;
    private static final long CHUNK_BUDGET_NANOS = 4_000_000L;
    private static final long CHUNK_SNAPSHOT_BUDGET_NANOS = 2_000_000L;

    private SchematicMath() {
    }

    static SchematicBounds computeBounds(World world, Location min, Location max, SchematicManager.SchematicType type) {
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();

        if (type == SchematicManager.SchematicType.CHUNK) {
            int chunkMinX = minX >> 4;
            int chunkMaxX = maxX >> 4;
            int chunkMinZ = minZ >> 4;
            int chunkMaxZ = maxZ >> 4;

            int expandedMinX = chunkMinX << 4;
            int expandedMaxX = (chunkMaxX << 4) + 15;
            int expandedMinZ = chunkMinZ << 4;
            int expandedMaxZ = (chunkMaxZ << 4) + 15;

            return new SchematicBounds(
                expandedMinX, minY, expandedMinZ,
                expandedMaxX, maxY, expandedMaxZ,
                minX - expandedMinX, 0, minZ - expandedMinZ
            );
        }

        if (type == SchematicManager.SchematicType.SECTIONS) {
            int worldMinY = world.getMinHeight();
            int worldMaxY = world.getMaxHeight() - 1;

            int expandedMinX = floorTo16(minX);
            int expandedMinY = Math.max(worldMinY, floorTo16(minY));
            int expandedMinZ = floorTo16(minZ);
            int expandedMaxX = ceilTo16(maxX);
            int expandedMaxY = Math.min(worldMaxY, ceilTo16(maxY));
            int expandedMaxZ = ceilTo16(maxZ);

            return new SchematicBounds(
                expandedMinX,
                expandedMinY,
                expandedMinZ,
                expandedMaxX,
                expandedMaxY,
                expandedMaxZ,
                minX - expandedMinX,
                minY - expandedMinY,
                minZ - expandedMinZ
            );
        }

        return new SchematicBounds(minX, minY, minZ, maxX, maxY, maxZ, 0, 0, 0);
    }

    static long getBudgetNanos(SchematicManager.SchematicType type) {
        if (type == SchematicManager.SchematicType.CHUNK) {
            return CHUNK_BUDGET_NANOS;
        }
        if (type == SchematicManager.SchematicType.SECTIONS) {
            return SECTION_BUDGET_NANOS;
        }
        return BLOCK_BUDGET_NANOS;
    }

    static long getSnapshotBudgetNanos() {
        return CHUNK_SNAPSHOT_BUDGET_NANOS;
    }

    static long calculateMaxCursor(SchematicManager.SchematicType type, int width, int height, int length) {
        if (type == SchematicManager.SchematicType.SECTIONS) {
            long sectionX = ceilDiv(width, 16);
            long sectionY = ceilDiv(height, 16);
            long sectionZ = ceilDiv(length, 16);
            return sectionX * sectionY * sectionZ * 4096L;
        }

        if (type == SchematicManager.SchematicType.CHUNK) {
            long chunkX = ceilDiv(width, 16);
            long chunkZ = ceilDiv(length, 16);
            return chunkX * chunkZ * 256L * height;
        }

        return (long) width * height * length;
    }

    static SchematicCoordinate resolveCoordinate(SchematicManager.SchematicType type, long cursor, int width, int height, int length) {
        if (cursor < 0) {
            return null;
        }

        if (type == SchematicManager.SchematicType.BLOCK || type == SchematicManager.SchematicType.FAWE || type == SchematicManager.SchematicType.AUTO) {
            long plane = (long) width * length;
            long y = cursor / plane;
            if (y >= height) {
                return null;
            }
            long rest = cursor % plane;
            int z = (int) (rest / width);
            int x = (int) (rest % width);
            return new SchematicCoordinate(x, (int) y, z);
        }

        if (type == SchematicManager.SchematicType.SECTIONS) {
            int sectionXCount = ceilDiv(width, 16);
            int sectionYCount = ceilDiv(height, 16);
            int sectionZCount = ceilDiv(length, 16);

            long sectionIndex = cursor / 4096L;
            int innerIndex = (int) (cursor % 4096L);

            int sectionX = (int) (sectionIndex % sectionXCount);
            int sectionZ = (int) ((sectionIndex / sectionXCount) % sectionZCount);
            int sectionY = (int) (sectionIndex / (long) (sectionXCount * sectionZCount));

            if (sectionY >= sectionYCount) {
                return null;
            }

            int innerX = innerIndex & 15;
            int innerZ = (innerIndex >> 4) & 15;
            int innerY = (innerIndex >> 8) & 15;

            int x = (sectionX << 4) + innerX;
            int y = (sectionY << 4) + innerY;
            int z = (sectionZ << 4) + innerZ;

            if (x >= width || y >= height || z >= length) {
                return null;
            }

            return new SchematicCoordinate(x, y, z);
        }

        int chunkXCount = ceilDiv(width, 16);
        int chunkZCount = ceilDiv(length, 16);

        long chunkIndex = cursor / (256L * height);
        long chunkInner = cursor % (256L * height);

        int chunkX = (int) (chunkIndex % chunkXCount);
        int chunkZ = (int) (chunkIndex / chunkXCount);

        if (chunkZ >= chunkZCount) {
            return null;
        }

        int y = (int) (chunkInner / 256L);
        int flat = (int) (chunkInner % 256L);
        int innerX = flat & 15;
        int innerZ = (flat >> 4) & 15;

        int x = (chunkX << 4) + innerX;
        int z = (chunkZ << 4) + innerZ;

        if (x >= width || y >= height || z >= length) {
            return null;
        }

        return new SchematicCoordinate(x, y, z);
    }

    static int toIndex(int x, int y, int z, int width, int length) {
        return (y * length + z) * width + x;
    }

    static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    private static int floorTo16(int value) {
        return Math.floorDiv(value, 16) * 16;
    }

    private static int ceilTo16(int value) {
        return (Math.floorDiv(value, 16) * 16) + 15;
    }
}

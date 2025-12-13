package net.exylia.commons.v2.region.visual;

import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ParticleRenderer {
    private static final double DEFAULT_STEP = 0.5;
    private static final int MAX_PARTICLES_PER_FRAME = 500;
    private static final double MAX_VIEW_DISTANCE_SQUARED = 1024.0;

    public void renderBorders(Region region, Player viewer, Color color, double step) {
        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();

        List<Location> borderPoints = calculateBorderPoints(min, max, step);

        int particleCount = 0;
        Location viewerLocation = viewer.getLocation();

        for (Location point : borderPoints) {
            if (particleCount >= MAX_PARTICLES_PER_FRAME) {
                break;
            }

            if (point.distanceSquared(viewerLocation) > MAX_VIEW_DISTANCE_SQUARED) {
                continue;
            }

            viewer.spawnParticle(Particle.WAX_ON, point, 1, 0, 0, 0, 0);

            particleCount++;
        }
    }

    public void renderBorders(Region region, Player viewer, Color color) {
        renderBorders(region, viewer, color, DEFAULT_STEP);
    }

    public List<Location> calculateBorderPoints(Location min, Location max, double step) {
        List<Location> points = new ArrayList<>();

        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX() + 1;
        int maxY = max.getBlockY() + 1;
        int maxZ = max.getBlockZ() + 1;

        for (double x = minX; x <= maxX; x += step) {
            addEdgePoint(points, min.getWorld(), x, minY, minZ);
            addEdgePoint(points, min.getWorld(), x, minY, maxZ);
            addEdgePoint(points, min.getWorld(), x, maxY, minZ);
            addEdgePoint(points, min.getWorld(), x, maxY, maxZ);
        }

        for (double y = minY; y <= maxY; y += step) {
            addEdgePoint(points, min.getWorld(), minX, y, minZ);
            addEdgePoint(points, min.getWorld(), minX, y, maxZ);
            addEdgePoint(points, min.getWorld(), maxX, y, minZ);
            addEdgePoint(points, min.getWorld(), maxX, y, maxZ);
        }

        for (double z = minZ; z <= maxZ; z += step) {
            addEdgePoint(points, min.getWorld(), minX, minY, z);
            addEdgePoint(points, min.getWorld(), minX, maxY, z);
            addEdgePoint(points, min.getWorld(), maxX, minY, z);
            addEdgePoint(points, min.getWorld(), maxX, maxY, z);
        }

        return points;
    }

    private void addEdgePoint(List<Location> points, org.bukkit.World world, double x, double y, double z) {
        points.add(new Location(world, x, y, z));
    }

    public double calculateOptimalStep(long volume) {
        if (volume < 1000) {
            return 0.25;
        } else if (volume < 10000) {
            return 0.5;
        } else if (volume < 100000) {
            return 1.0;
        } else {
            return 2.0;
        }
    }
}

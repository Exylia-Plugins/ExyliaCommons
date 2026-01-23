package net.exylia.commons.region.cloning;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.region.RegionManager;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.regeneration.RegionRegenerationManager;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Deprecated
public class RegionCloner {
    private static RegionCloner instance;

    private final RegionManager regionManager;
    private final RegionRegenerationManager regenerationManager;
    private final ConcurrentHashMap<String, CloneOperation> activeClones;
    private final AtomicLong cloneIdCounter;

    private RegionCloner() {
        this.regionManager = RegionManager.getInstance();
        this.regenerationManager = RegionRegenerationManager.getInstance();
        this.activeClones = new ConcurrentHashMap<>();
        this.cloneIdCounter = new AtomicLong(System.currentTimeMillis());
    }

    public static RegionCloner getInstance() {
        if (instance == null) {
            instance = new RegionCloner();
        }
        return instance;
    }

    public CompletableFuture<Region> cloneRegion(Region sourceRegion, Location targetCenter) {
        return cloneRegion(sourceRegion, targetCenter, null, null);
    }

    public CompletableFuture<Region> cloneRegion(Region sourceRegion, Location targetCenter, String customName) {
        return cloneRegion(sourceRegion, targetCenter, customName, null);
    }

    public CompletableFuture<Region> cloneRegion(Region sourceRegion, Location targetCenter, World targetWorld) {
        if (targetWorld != null) {
            targetCenter = targetCenter.clone();
            targetCenter.setWorld(targetWorld);
        }
        return cloneRegion(sourceRegion, targetCenter, null, targetWorld);
    }

    public CompletableFuture<Region> cloneRegion(Region sourceRegion, Location targetCenter, String customName, World targetWorld) {
        String operationId = generateOperationId();
        CloneOperation operation = new CloneOperation(operationId, sourceRegion, targetCenter, customName, targetWorld);
        activeClones.put(operationId, operation);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeClone(operation);
            } catch (Exception e) {
                operation.setError(e);
                throw new RuntimeException("Error cloning region: " + e.getMessage(), e);
            } finally {
                activeClones.remove(operationId);
            }
        });
    }

    private Region executeClone(CloneOperation operation) throws Exception {
        Region sourceRegion = operation.getSourceRegion();
        Location targetCenter = operation.getTargetCenter();

        operation.setPhase(ClonePhase.CALCULATING_POSITIONS);

        if (!sourceRegion.hasSchematic()) {
            operation.setPhase(ClonePhase.SAVING_SOURCE_SCHEMATIC);
            boolean saved = sourceRegion.saveSchematic().join();
            if (!saved) {
                throw new RuntimeException("Failed to save source region schematic");
            }
        }

        operation.setPhase(ClonePhase.CREATING_TARGET_REGION);

        Selection sourceSelection = sourceRegion.getSelection();
        Location sourceCenter = sourceSelection.getCenter();
        Location sourceMin = sourceSelection.getMinimumPoint();
        Location sourceMax = sourceSelection.getMaximumPoint();

        int sizeX = sourceMax.getBlockX() - sourceMin.getBlockX();
        int sizeY = sourceMax.getBlockY() - sourceMin.getBlockY();
        int sizeZ = sourceMax.getBlockZ() - sourceMin.getBlockZ();

        int halfSizeX = sizeX / 2;
        int halfSizeY = sizeY / 2;
        int halfSizeZ = sizeZ / 2;

        Location newMin = new Location(
                operation.getTargetWorld() != null ? operation.getTargetWorld() : targetCenter.getWorld(),
                targetCenter.getBlockX() - halfSizeX,
                targetCenter.getBlockY() - halfSizeY,
                targetCenter.getBlockZ() - halfSizeZ
        );

        Location newMax = new Location(
                newMin.getWorld(),
                newMin.getBlockX() + sizeX,
                newMin.getBlockY() + sizeY,
                newMin.getBlockZ() + sizeZ
        );

        Selection newSelection = new Selection(newMin, newMax);
        String cloneName = operation.getCustomName() != null ?
                operation.getCustomName() :
                generateCloneName(sourceRegion.getId());

        Region cloneRegion = regionManager.createRegion(cloneName, newSelection);
        if (cloneRegion == null) {
            throw new RuntimeException("Failed to create clone region");
        }

        operation.setCloneRegion(cloneRegion);
        operation.setPhase(ClonePhase.COPYING_PROPERTIES);

        copyRegionProperties(sourceRegion, cloneRegion);

        operation.setPhase(ClonePhase.PASTING_STRUCTURE);

        boolean pasted = pasteRegionStructure(sourceRegion, cloneRegion, newMin);
        if (!pasted) {
            regionManager.unregisterRegion(cloneRegion.getId());
            throw new RuntimeException("Failed to paste region structure");
        }

        operation.setPhase(ClonePhase.SAVING_CLONE_SCHEMATIC);

        boolean schematicSaved = cloneRegion.saveSchematic().join();
        if (!schematicSaved) {
            throw new RuntimeException("Failed to save clone schematic");
        }

        operation.setPhase(ClonePhase.COMPLETED);

        return cloneRegion;
    }

    private void copyRegionProperties(Region source, Region clone) {
        clone.setDisplayName(source.getDisplayName() + " (Clone)");
        clone.setDescription("Clone of: " + source.getDescription());
        clone.setPriority(source.getPriority());

        clone.setFlags(source.getConfiguredFlags());

        if (source.hasMetadata()) {
            clone.setMetadata(source.getMetadataCopy());
        }

        clone.setOwners(source.getOwners());
        clone.setMembers(source.getMembers());

        if (source.getOnEnter() != null) {
            clone.setOnEnter(source.getOnEnter());
        }
        if (source.getOnExit() != null) {
            clone.setOnExit(source.getOnExit());
        }
        if (source.getOnMove() != null) {
            clone.setOnMove(source.getOnMove());
        }
    }

    private boolean pasteRegionStructure(Region sourceRegion, Region cloneRegion, Location targetMinCorner) {
        try {
            return regenerationManager.pasteRegionSchematicAt(sourceRegion, targetMinCorner).join();
        } catch (Exception e) {
            return false;
        }
    }

    public CompletableFuture<Boolean> cloneMultipleRegions(Region[] sourceRegions, Location[] targetCenters) {
        if (sourceRegions.length != targetCenters.length) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                for (int i = 0; i < sourceRegions.length; i++) {
                    Region cloned = cloneRegion(sourceRegions[i], targetCenters[i]).join();
                    if (cloned == null) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    public CompletableFuture<Region> cloneRegionWithOffset(Region sourceRegion, int offsetX, int offsetY, int offsetZ) {
        Location sourceCenter = sourceRegion.getSelection().getCenter();
        Location targetCenter = sourceCenter.clone().add(offsetX, offsetY, offsetZ);
        return cloneRegion(sourceRegion, targetCenter);
    }

    public boolean isCloneInProgress(String regionId) {
        return activeClones.values().stream()
                .anyMatch(op -> op.getSourceRegion().getId().equals(regionId) && !op.isCompleted());
    }

    public CloneOperation getCloneOperation(String operationId) {
        return activeClones.get(operationId);
    }

    public int getActiveCloneCount() {
        return activeClones.size();
    }

    private String generateOperationId() {
        return "clone_op_" + cloneIdCounter.getAndIncrement();
    }

    private String generateCloneName(String sourceId) {
        return sourceId + "_clone_" + System.currentTimeMillis();
    }

    public void cleanup() {
        activeClones.clear();
    }

    @Getter
    public static class CloneOperation {
        private final String operationId;
        private final Region sourceRegion;
        private final Location targetCenter;
        private final String customName;
        private final World targetWorld;
        private final long startTime;

        private ClonePhase phase;
        @Setter
        private Region cloneRegion;
        private Exception error;
        private long completionTime;

        public CloneOperation(String operationId, Region sourceRegion, Location targetCenter,
                              String customName, World targetWorld) {
            this.operationId = operationId;
            this.sourceRegion = sourceRegion;
            this.targetCenter = targetCenter.clone();
            this.customName = customName;
            this.targetWorld = targetWorld;
            this.startTime = System.currentTimeMillis();
            this.phase = ClonePhase.INITIALIZING;
        }

        public void setPhase(ClonePhase phase) {
            this.phase = phase;
            if (phase == ClonePhase.COMPLETED || phase == ClonePhase.FAILED) {
                this.completionTime = System.currentTimeMillis();
            }
        }

        public void setError(Exception error) {
            this.error = error;
            setPhase(ClonePhase.FAILED);
        }

        public boolean isCompleted() {
            return phase == ClonePhase.COMPLETED;
        }

        public boolean isFailed() {
            return phase == ClonePhase.FAILED;
        }

        public long getDurationMs() {
            if (completionTime > 0) {
                return completionTime - startTime;
            }
            return System.currentTimeMillis() - startTime;
        }

        public double getProgressPercentage() {
            return switch (phase) {
                case INITIALIZING -> 0.0;
                case CALCULATING_POSITIONS -> 10.0;
                case SAVING_SOURCE_SCHEMATIC -> 20.0;
                case CREATING_TARGET_REGION -> 40.0;
                case COPYING_PROPERTIES -> 60.0;
                case PASTING_STRUCTURE -> 80.0;
                case SAVING_CLONE_SCHEMATIC -> 90.0;
                case COMPLETED -> 100.0;
                case FAILED -> -1.0;
            };
        }
    }

    public enum ClonePhase {
        INITIALIZING,
        CALCULATING_POSITIONS,
        SAVING_SOURCE_SCHEMATIC,
        CREATING_TARGET_REGION,
        COPYING_PROPERTIES,
        PASTING_STRUCTURE,
        SAVING_CLONE_SCHEMATIC,
        COMPLETED,
        FAILED
    }
}

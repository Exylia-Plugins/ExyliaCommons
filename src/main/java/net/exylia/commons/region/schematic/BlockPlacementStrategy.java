package net.exylia.commons.region.schematic;

/**
 * Strategy for block placement performance vs. CPU load balance
 */
public enum BlockPlacementStrategy {
    /**
     * FAST: Places all blocks as quickly as possible (~700ms)
     * High CPU impact, all at once
     */
    FAST(0, 0, "Fast placement - all blocks immediately"),

    /**
     * BATCH_WITH_YIELDS: Processes blocks in batches with CPU yields (~1500-2000ms)
     * Moderate CPU impact, distributed processing
     * Default: 10,000 blocks per batch
     */
    BATCH_WITH_YIELDS(10000, 1, "Batch processing with CPU yields"),

    /**
     * TICK_BASED: Places 1 chunk per server tick (~2000-3000ms)
     * Low CPU impact, very smooth and distributed
     * Each tick = 50ms, maximum control
     */
    TICK_BASED(0, 50, "Tick-based placement - 1 chunk per tick"),

    /**
     * HYBRID: Fast chunk preload, distributed block placement (~1000-1500ms)
     * Balanced: quick start, distributed placement
     * Default: places blocks from 3-5 chunks per tick
     */
    HYBRID(0, 3, "Hybrid - fast preload, distributed placement");

    public final int blocksPerBatch;
    public final int ticksBetweenBatches;
    public final String description;

    BlockPlacementStrategy(int blocksPerBatch, int ticksBetweenBatches, String description) {
        this.blocksPerBatch = blocksPerBatch;
        this.ticksBetweenBatches = ticksBetweenBatches;
        this.description = description;
    }

    public static BlockPlacementStrategy getDefault() {
        return HYBRID;
    }

    public static BlockPlacementStrategy fromString(String name) {
        try {
            return BlockPlacementStrategy.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return getDefault();
        }
    }
}

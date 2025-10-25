package net.exylia.commons.region.schematic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for block placement behavior
 */
@Getter
@Builder
@AllArgsConstructor
public class BlockPlacementConfig {
    private final BlockPlacementStrategy strategy;
    private final int blocksPerBatch;
    private final int ticksBetweenBatches;

    /**
     * Create a default configuration
     */
    public static BlockPlacementConfig getDefault() {
        BlockPlacementStrategy strategy = BlockPlacementStrategy.FAST;
        return BlockPlacementConfig.builder()
            .strategy(strategy)
            .blocksPerBatch(strategy.blocksPerBatch)
            .ticksBetweenBatches(strategy.ticksBetweenBatches)
            .build();
    }

    /**
     * Create configuration from strategy
     */
    public static BlockPlacementConfig fromStrategy(BlockPlacementStrategy strategy) {
        return BlockPlacementConfig.builder()
            .strategy(strategy)
            .blocksPerBatch(strategy.blocksPerBatch)
            .ticksBetweenBatches(strategy.ticksBetweenBatches)
            .build();
    }

    /**
     * Create custom configuration
     */
    public static BlockPlacementConfigBuilder custom() {
        return builder().strategy(BlockPlacementStrategy.BATCH_WITH_YIELDS);
    }
}

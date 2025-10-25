package net.exylia.commons.region.schematic;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

@Getter
public class BlockDataStore {
    private final short blockId;
    private final String blockDataString;

    public BlockDataStore(Material material, BlockData blockData) {
        this.blockId = MaterialRegistry.getId(material);
        this.blockDataString = blockData.getAsString();
    }

    public BlockDataStore(short blockId, String blockDataString) {
        this.blockId = blockId;
        this.blockDataString = blockDataString;
    }

    public BlockData toBlockData() {
        try {
            Material material = MaterialRegistry.getMaterial(blockId);
            if (material == null || material == Material.AIR) {
                return Material.AIR.createBlockData();
            }
            return org.bukkit.Bukkit.createBlockData(blockDataString);
        } catch (Exception e) {
            Material material = MaterialRegistry.getMaterial(blockId);
            return material != null ? material.createBlockData() : Material.AIR.createBlockData();
        }
    }

    public String getAsString() {
        return blockDataString;
    }

    @Override
    public String toString() {
        return blockDataString;
    }
}

package net.exylia.commons.region.schematic;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

@Getter
public class SchematicData {
    private final int width;
    private final int height;
    private final int length;
    private final short[] blockIds;
    private final String[] blockDataStrings;

    public SchematicData(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
        int totalBlocks = width * height * length;
        this.blockIds = new short[totalBlocks];
        this.blockDataStrings = new String[totalBlocks];
    }

    public void setBlock(int x, int y, int z, short blockId, String blockDataString) {
        int index = getIndex(x, y, z);
        blockIds[index] = blockId;
        blockDataStrings[index] = blockDataString;
    }

    public void setBlock(int x, int y, int z, Material material, BlockData blockData) {
        int index = getIndex(x, y, z);
        blockIds[index] = MaterialRegistry.getId(material);
        blockDataStrings[index] = blockData.getAsString();
    }

    public short getBlockId(int x, int y, int z) {
        return blockIds[getIndex(x, y, z)];
    }

    public String getBlockDataString(int x, int y, int z) {
        return blockDataStrings[getIndex(x, y, z)];
    }

    public BlockData getBlockData(int x, int y, int z) {
        try {
            String dataString = getBlockDataString(x, y, z);
            if (dataString == null || dataString.isEmpty()) {
                Material material = MaterialRegistry.getMaterial(getBlockId(x, y, z));
                return material != null ? material.createBlockData() : Material.AIR.createBlockData();
            }
            return org.bukkit.Bukkit.createBlockData(dataString);
        } catch (Exception e) {
            Material material = MaterialRegistry.getMaterial(getBlockId(x, y, z));
            return material != null ? material.createBlockData() : Material.AIR.createBlockData();
        }
    }

    private int getIndex(int x, int y, int z) {
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= length) {
            throw new IndexOutOfBoundsException(
                String.format("Index out of bounds: (%d,%d,%d) for schematic (%d,%d,%d)",
                    x, y, z, width, height, length)
            );
        }
        return y * (width * length) + z * width + x;
    }

    public int getTotalBlocks() {
        return width * height * length;
    }
}

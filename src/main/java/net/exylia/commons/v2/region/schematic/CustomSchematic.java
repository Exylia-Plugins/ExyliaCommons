package net.exylia.commons.v2.region.schematic;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

import java.util.List;

final class CustomSchematic {

    private final String worldName;
    private final int width;
    private final int height;
    private final int length;
    private final int anchorX;
    private final int anchorY;
    private final int anchorZ;
    private final SchematicManager.SchematicType storedType;
    private final List<String> palette;
    private final short[] data;

    private volatile BlockData[] cachedPaletteData;
    private volatile Object[] cachedNmsStates;
    private volatile boolean nmsStatesFailed = false;

    CustomSchematic(String worldName, int width, int height, int length,
                    int anchorX, int anchorY, int anchorZ,
                    SchematicManager.SchematicType storedType,
                    List<String> palette, short[] data) {
        this.worldName = worldName;
        this.width = width;
        this.height = height;
        this.length = length;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.storedType = storedType;
        this.palette = palette;
        this.data = data;
    }

    String worldName() { return worldName; }
    int width() { return width; }
    int height() { return height; }
    int length() { return length; }
    int anchorX() { return anchorX; }
    int anchorY() { return anchorY; }
    int anchorZ() { return anchorZ; }
    SchematicManager.SchematicType storedType() { return storedType; }
    List<String> palette() { return palette; }
    short[] data() { return data; }

    BlockData[] getOrComputePaletteData() {
        BlockData[] result = cachedPaletteData;
        if (result != null) return result;
        synchronized (this) {
            result = cachedPaletteData;
            if (result != null) return result;
            result = new BlockData[palette.size()];
            for (int i = 0; i < palette.size(); i++) {
                result[i] = Bukkit.createBlockData(palette.get(i));
            }
            cachedPaletteData = result;
        }
        return result;
    }

    Object[] getOrComputeNmsStates() {
        if (nmsStatesFailed || !NmsBlockHelper.AVAILABLE) return null;
        Object[] result = cachedNmsStates;
        if (result != null) return result;
        synchronized (this) {
            if (nmsStatesFailed) return null;
            result = cachedNmsStates;
            if (result != null) return result;
            try {
                result = NmsBlockHelper.toNmsStates(getOrComputePaletteData());
                cachedNmsStates = result;
            } catch (Throwable t) {
                nmsStatesFailed = true;
                return null;
            }
        }
        return result;
    }
}

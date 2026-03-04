package net.exylia.commons.v2.region.schematic;

import java.util.List;

record CustomSchematic(
    String worldName,
    int width,
    int height,
    int length,
    int anchorX,
    int anchorY,
    int anchorZ,
    SchematicManager.SchematicType storedType,
    List<String> palette,
    int[] data
) {
}

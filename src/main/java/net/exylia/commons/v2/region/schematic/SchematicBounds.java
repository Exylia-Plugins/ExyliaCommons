package net.exylia.commons.v2.region.schematic;

record SchematicBounds(
    int minX,
    int minY,
    int minZ,
    int maxX,
    int maxY,
    int maxZ,
    int anchorX,
    int anchorY,
    int anchorZ
) {
}

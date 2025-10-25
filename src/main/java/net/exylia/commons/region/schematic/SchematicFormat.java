package net.exylia.commons.region.schematic;

import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SchematicFormat {
    private static final byte FORMAT_VERSION = 2;

    public static byte[] serialize(SchematicData data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (GZIPOutputStream gzip = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzip)) {

            dos.writeByte(FORMAT_VERSION);
            dos.writeInt(data.getWidth());
            dos.writeInt(data.getHeight());
            dos.writeInt(data.getLength());

            int totalBlocks = data.getTotalBlocks();
            dos.writeInt(totalBlocks);

            for (int i = 0; i < totalBlocks; i++) {
                dos.writeShort(data.getBlockIds()[i]);
                String blockDataString = data.getBlockDataStrings()[i];
                if (blockDataString == null || blockDataString.isEmpty()) {
                    dos.writeUTF("");
                } else {
                    dos.writeUTF(blockDataString);
                }
            }

            dos.flush();
        }

        return baos.toByteArray();
    }

    public static SchematicData deserialize(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        try (GZIPInputStream gzip = new GZIPInputStream(bais);
             DataInputStream dis = new DataInputStream(gzip)) {

            byte version = dis.readByte();
            if (version != FORMAT_VERSION && version != 1) {
                throw new IOException("Unsupported schematic format version: " + version);
            }

            int width = dis.readInt();
            int height = dis.readInt();
            int length = dis.readInt();

            SchematicData schematicData = new SchematicData(width, height, length);
            int totalBlocks = dis.readInt();

            for (int i = 0; i < totalBlocks; i++) {
                short blockId = dis.readShort();

                String blockDataString;
                if (version >= 2) {
                    blockDataString = dis.readUTF();
                } else {
                    byte blockData = dis.readByte();
                    blockDataString = MaterialRegistry.getMaterial(blockId).createBlockData().getAsString();
                }

                schematicData.getBlockIds()[i] = blockId;
                schematicData.getBlockDataStrings()[i] = blockDataString;
            }

            return schematicData;

        } catch (Exception e) {
            DebugUtils.logInternalError("Error deserializing schematic: " + e.getMessage());
            throw new IOException(e);
        }
    }

    public static void captureRegion(SchematicData data, Block minBlock, Block maxBlock) {
        int minX = Math.min(minBlock.getX(), maxBlock.getX());
        int maxX = Math.max(minBlock.getX(), maxBlock.getX());
        int minY = Math.min(minBlock.getY(), maxBlock.getY());
        int maxY = Math.max(minBlock.getY(), maxBlock.getY());
        int minZ = Math.min(minBlock.getZ(), maxBlock.getZ());
        int maxZ = Math.max(minBlock.getZ(), maxBlock.getZ());

        int offsetX = 0, offsetY = 0, offsetZ = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = minBlock.getWorld().getBlockAt(x, y, z);
                    Material material = block.getType();
                    org.bukkit.block.data.BlockData blockData = block.getBlockData();

                    data.setBlock(offsetX, offsetY, offsetZ, material, blockData);

                    offsetZ++;
                }
                offsetZ = 0;
                offsetY++;
            }
            offsetY = 0;
            offsetX++;
        }
    }

    public static void pasteRegion(SchematicData data, Block pasteLocation) {
        int baseX = pasteLocation.getX();
        int baseY = pasteLocation.getY();
        int baseZ = pasteLocation.getZ();

        for (int x = 0; x < data.getWidth(); x++) {
            for (int y = 0; y < data.getHeight(); y++) {
                for (int z = 0; z < data.getLength(); z++) {
                    short blockId = data.getBlockId(x, y, z);
                    Material material = MaterialRegistry.getMaterial(blockId);

                    if (material == null) continue;

                    try {
                        org.bukkit.block.data.BlockData blockData = data.getBlockData(x, y, z);
                        Block targetBlock = pasteLocation.getWorld()
                            .getBlockAt(baseX + x, baseY + y, baseZ + z);
                        targetBlock.setBlockData(blockData, false);
                    } catch (Exception e) {
                        Block targetBlock = pasteLocation.getWorld()
                            .getBlockAt(baseX + x, baseY + y, baseZ + z);
                        targetBlock.setType(material, false);
                    }
                }
            }
        }
    }
}

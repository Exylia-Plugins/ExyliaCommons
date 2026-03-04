package net.exylia.commons.v2.region.schematic;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class CustomSchematicCodec {

    private static final int MAGIC = 0x45585343;
    private static final int VERSION = 1;

    private CustomSchematicCodec() {
    }

    static void write(File file, CustomSchematic schematic) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeUTF(schematic.worldName() == null ? "" : schematic.worldName());
            out.writeInt(schematic.width());
            out.writeInt(schematic.height());
            out.writeInt(schematic.length());
            out.writeInt(schematic.anchorX());
            out.writeInt(schematic.anchorY());
            out.writeInt(schematic.anchorZ());
            out.writeUTF(schematic.storedType().name());

            out.writeInt(schematic.palette().size());
            for (String value : schematic.palette()) {
                out.writeUTF(value);
            }

            out.writeInt(schematic.data().length);
            boolean useShort = schematic.palette().size() <= 0xFFFF;
            out.writeBoolean(useShort);
            if (useShort) {
                for (int value : schematic.data()) {
                    out.writeShort(value);
                }
            } else {
                for (int value : schematic.data()) {
                    out.writeInt(value);
                }
            }
        }
    }

    static CustomSchematic read(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IOException("Invalid schematic format");
            }

            int version = in.readInt();
            if (version != VERSION) {
                throw new IOException("Unsupported schematic version: " + version);
            }

            String worldName = in.readUTF();
            int width = in.readInt();
            int height = in.readInt();
            int length = in.readInt();
            int anchorX = in.readInt();
            int anchorY = in.readInt();
            int anchorZ = in.readInt();
            SchematicManager.SchematicType type = parseType(in.readUTF(), SchematicManager.SchematicType.SECTIONS);

            int paletteSize = in.readInt();
            List<String> palette = new ArrayList<>(paletteSize);
            for (int i = 0; i < paletteSize; i++) {
                palette.add(in.readUTF());
            }

            int dataSize = in.readInt();
            int[] data = new int[dataSize];
            boolean useShort = in.readBoolean();
            if (useShort) {
                for (int i = 0; i < dataSize; i++) {
                    data[i] = in.readUnsignedShort();
                }
            } else {
                for (int i = 0; i < dataSize; i++) {
                    data[i] = in.readInt();
                }
            }

            return new CustomSchematic(worldName, width, height, length, anchorX, anchorY, anchorZ, type, palette, data);
        }
    }

    private static SchematicManager.SchematicType parseType(String value, SchematicManager.SchematicType fallback) {
        try {
            return SchematicManager.SchematicType.valueOf(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}

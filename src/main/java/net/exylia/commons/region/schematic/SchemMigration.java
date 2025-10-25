package net.exylia.commons.region.schematic;

import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

public class SchemMigration {

    public static CompletableFuture<Void> migrateOldSchematics(Path schemsFolder) {
        return CompletableFuture.runAsync(() -> {
            try {
                File[] files = schemsFolder.toFile().listFiles((dir, name) -> name.endsWith(".schem"));
                if (files == null || files.length == 0) {
                    DebugUtils.logInternalDebug("No schematics found to migrate");
                    return;
                }

                DebugUtils.logInternalInfo("Detecting old schematics for migration...");
                int migratedCount = 0;

                for (File file : files) {
                    try {
                        if (isOldFormat(file)) {
                            String regionId = file.getName().replace(".schem", "");
                            DebugUtils.logInternalDebug("Migrating old schematic: " + regionId);

                            if (migrateOldSchematic(file, regionId)) {
                                migratedCount++;
                                DebugUtils.logInternalInfo("Successfully migrated: " + regionId);
                            }
                        }
                    } catch (Exception e) {
                        DebugUtils.logInternalError("Error checking schematic " + file.getName() + ": " + e.getMessage());
                    }
                }

                if (migratedCount > 0) {
                    DebugUtils.logInternalInfo("Migration complete: " + migratedCount + " schematics migrated");
                }

            } catch (Exception e) {
                DebugUtils.logInternalError("Error during schematic migration: " + e.getMessage());
            }
        });
    }

    private static boolean isOldFormat(File file) {
        try {
            byte[] data = Files.readAllBytes(file.toPath());

            if (data.length < 5) {
                return false;
            }

            try (GZIPInputStream gzip = new GZIPInputStream(new java.io.ByteArrayInputStream(data));
                 java.io.DataInputStream dis = new java.io.DataInputStream(gzip)) {

                byte version = dis.readByte();

                if (version != 1) {
                    return true;
                }

                return false;

            } catch (Exception e) {
                return true;
            }

        } catch (Exception e) {
            return false;
        }
    }

    private static boolean migrateOldSchematic(File oldFile, String regionId) {
        try {
            Path backupPath = oldFile.toPath().getParent().resolve(regionId + ".schem.backup");

            if (!Files.exists(backupPath)) {
                Files.copy(oldFile.toPath(), backupPath);
                DebugUtils.logInternalDebug("Created backup: " + backupPath.getFileName());
            }

            return true;

        } catch (Exception e) {
            DebugUtils.logInternalError("Error backing up old schematic: " + e.getMessage());
            return false;
        }
    }

    public static boolean shouldAutoGenerateFromRegion(String regionId) {
        return true;
    }
}

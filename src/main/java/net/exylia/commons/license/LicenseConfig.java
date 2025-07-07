package net.exylia.commons.license;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LicenseConfig {
    private final ExyliaPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public LicenseConfig(ExyliaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "license.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void createDefaultConfig() {
        plugin.getDataFolder().mkdirs();
        try {
            configFile.createNewFile();

            // Escribir el archivo YAML manualmente con comentarios
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("# =================================================\n");
                writer.write("# LICENSE CONFIGURATION - " + plugin.getName().toUpperCase() + "\n");
                writer.write("# =================================================\n");
                writer.write("#\n");
                writer.write("# This plugin requires a valid license.\n");
                writer.write("# Join our Discord server for more information.\n");
                writer.write("# https://discord.exylia.net/\n");
                writer.write("#\n");
                writer.write("# key: Your license key (format: XXXXX-XXXXX-XXXXX-XXXXX-XXXXX)\n");
                writer.write("# =================================================\n");
                writer.write("\n");
                writer.write("license:\n");
                writer.write("  key: \"\"\n");
            }

            // Cargar la configuración después de escribirla
            config = YamlConfiguration.loadConfiguration(configFile);

            DebugUtils.logInternalSuccess("Archivo de licencia creado: " + configFile.getPath());
            DebugUtils.logInternalWarn("LICENCIA REQUERIDA: Configura tu clave en license.yml");
        } catch (IOException e) {
            DebugUtils.logInternalError("Error creando archivo de licencia: " + e.getMessage());
        }
    }

    public String getLicenseKey() {
        return config.getString("license.key", "").trim();
    }

    public void setLicenseKey(String key) {
        config.set("license.key", key);
        save();
    }

    private void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando configuración de licencia: " + e.getMessage());
        }
    }
}
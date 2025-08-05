package net.exylia.commons.license;

import com.hapangama.SunLicenseAPI;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.List;
import java.util.logging.Logger;

public class SunLicenseUtil {

    private final ExyliaPlugin plugin;

    public SunLicenseUtil(ExyliaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean initializeLicense() {
        try {
            validateLicense();
            DebugUtils.logInternalSuccess("License validated successfully!");
            return true;
        } catch (Exception e) {
            logLicenseError(e.getMessage());
            return false;
        }
    }

    private void validateLicense() throws Exception {
        File licenseFile = new File(plugin.getDataFolder(), "license.yml");

        if (!licenseFile.exists()) {
            createLicenseFile(licenseFile);
            throw new Exception("License file has been created at: " + licenseFile.getPath() +
                    " | Please edit the file and add your valid license key, then restart the server.");
        }

        String licenseKey = readLicenseKey(licenseFile);

        if (licenseKey == null) {
            throw new Exception("Invalid or missing license key in: " + licenseFile.getPath() +
                    " | Please add a valid license key and restart the server.");
        }

        try {
            SunLicenseAPI api = SunLicenseAPI.getLicense(licenseKey, plugin.getProductID(), plugin.getDescription().getVersion(), "https://licenses.exylia.net/");
            api.setIp(getPublicIP());
            plugin.setApi(api);
        } catch (Exception e) {
            throw new Exception("License validation failed: " + e.getMessage() +
                    " | Please check your license key or contact support.");
        }
    }

    private void createLicenseFile(File licenseFile) throws Exception {
        try {
            licenseFile.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(licenseFile)) {
                writer.write("# =================================================\n");
                writer.write("# LICENSE CONFIGURATION - " + plugin.getName().toUpperCase() + "\n");
                writer.write("# =================================================\n");
                writer.write("#\n");
                writer.write("# This plugin requires a valid license.\n");
                writer.write("# Join our Discord server for more information.\n");
                writer.write("# https://discord.exylia.net/\n");
                writer.write("#\n");
                writer.write("# =================================================\n");
                writer.write("# license:\n");
                writer.write("#   key: Your license key (format: XXXXX-XXXXX-XXXXX-XXXXX-XXXXX)\n");
                writer.write("# =================================================\n");
                writer.write("\n");
                writer.write("license:\n");
                writer.write("  key: \"\"\n");
            }
        } catch (IOException e) {
            throw new Exception("Failed to create license file: " + e.getMessage());
        }
    }

    private String readLicenseKey(File licenseFile) throws Exception {
        try {
            List<String> lines = Files.readAllLines(licenseFile.toPath());

            boolean inLicenseSection = false;
            for (String line : lines) {
                line = line.trim();
                if (line.equals("license:")) {
                    inLicenseSection = true;
                    continue;
                }
                if (inLicenseSection && line.startsWith("key:")) {
                    String keyValue = line.substring(4).trim();
                    if (keyValue.startsWith("\"") && keyValue.endsWith("\"")) {
                        keyValue = keyValue.substring(1, keyValue.length() - 1);
                    }
                    if (!keyValue.isEmpty() && !keyValue.equals("YOUR-LICENSE-KEY-HERE")) {
                        return keyValue;
                    }
                }
                if (inLicenseSection && line.endsWith(":") && !line.startsWith("key:")) {
                    inLicenseSection = false;
                }
            }
            return null;
        } catch (IOException e) {
            throw new Exception("Failed to read license file: " + e.getMessage());
        }
    }

    private void logLicenseError(String message) {
        DebugUtils.logInternalError("========================================");
        DebugUtils.logInternalError("LICENSE ERROR - " + plugin.getName().toUpperCase());
        DebugUtils.logInternalError("========================================");
        DebugUtils.logInternalError("");
        DebugUtils.logInternalError(message);
        DebugUtils.logInternalError("");
        DebugUtils.logInternalError("Plugin has been disabled.");
        DebugUtils.logInternalError("Join our Discord for support: https://discord.exylia.net/");
        DebugUtils.logInternalError("========================================");
    }

    public String getPublicIP() {
        try {
            URL url = new URL("https://api.ipify.org");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            return in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }
}
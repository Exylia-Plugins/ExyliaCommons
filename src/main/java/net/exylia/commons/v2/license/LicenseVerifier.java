package net.exylia.commons.v2.license;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.license.provider.LicenseProvider;
import net.exylia.commons.v2.license.provider.LukittuProvider;
import net.exylia.commons.v2.license.provider.SunLicenseProvider;
import net.exylia.commons.v2.license.result.LicenseValidationResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.exylia.commons.utils.DebugUtils.*;

public class LicenseVerifier {
    private final ExyliaPlugin plugin;
    private final List<LicenseProvider> providers;

    public LicenseVerifier(ExyliaPlugin plugin) {
        this.plugin = plugin;
        this.providers = new ArrayList<>();

        providers.add(new SunLicenseProvider());
        providers.add(new LukittuProvider());
        providers.sort(Comparator.comparingInt(LicenseProvider::getPriority).reversed());
    }

    public boolean validate() {
        logInternalInfo("Validating license for " + plugin.getName() + "...");

        String licenseKey = loadLicenseKey();
        if (licenseKey == null) {
            return false;
        }

        for (LicenseProvider provider : providers) {
            LicenseValidationResult result = provider.validate(plugin, licenseKey);

            if (result.isValid()) {
                logInternalSuccess("License validated successfully");
                return true;
            }
        }

        printFailure();
        return false;
    }

    private String loadLicenseKey() {
        File licenseFile = new File(plugin.getDataFolder(), "license.yml");

        if (!licenseFile.exists()) {
            createLicenseFile(licenseFile);
            logInternalError("License file created. Please add your license key and restart.");
            logInternalError("File: " + licenseFile.getPath());
            return null;
        }

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
                    String key = line.substring(4).trim();
                    if (key.startsWith("\"") && key.endsWith("\"")) {
                        key = key.substring(1, key.length() - 1);
                    }
                    if (!key.isEmpty() && !key.equals("YOUR-LICENSE-KEY-HERE")) {
                        return key;
                    }
                }
                if (inLicenseSection && line.endsWith(":") && !line.startsWith("key:")) {
                    inLicenseSection = false;
                }
            }

            logInternalError("No license key configured. Please add your license key and restart.");
            return null;

        } catch (IOException e) {
            logInternalError("Failed to read license file: " + e.getMessage());
            return null;
        }
    }

    private void createLicenseFile(File file) {
        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("# =================================================\n");
                writer.write("# LICENSE CONFIGURATION - " + plugin.getName().toUpperCase() + "\n");
                writer.write("# =================================================\n");
                writer.write("#\n");
                writer.write("# This plugin requires a valid license.\n");
                writer.write("# Purchase at: https://exylia.net/\n");
                writer.write("# Support: https://discord.exylia.net/\n");
                writer.write("#\n");
                writer.write("# =================================================\n\n");
                writer.write("license:\n");
                writer.write("  key: \"YOUR-LICENSE-KEY-HERE\"\n");
            }
        } catch (IOException e) {
            logInternalError("Failed to create license file: " + e.getMessage());
        }
    }

    private void printFailure() {
        logInternalError("License validation failed.");
        logInternalError("Please verify your license key is correct and try again.");
        logInternalError("Need help? https://discord.exylia.net/");
    }
}

package net.exylia.commons.v2.license.provider;

import com.hapangama.SunLicenseAPI;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.license.result.LicenseValidationResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

public class SunLicenseProvider implements LicenseProvider {
    private static final String BASE_URL;

    static {
        byte[] decoded = Base64.getDecoder().decode("aHR0cHM6Ly9saWNlbnNlcy5leHlsaWEubmV0Lw==");
        BASE_URL = new String(decoded);
    }

    @Override
    public String getName() {
        return "SunLicense";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public LicenseValidationResult validate(ExyliaPlugin plugin, String licenseKey) {
        logInternalDebug("[SunLicense] Starting validation...");

        try {
            SunLicenseAPI api = SunLicenseAPI.getLicense(
                    licenseKey,
                    plugin.getProductID(),
                    plugin.getDescription().getVersion(),
                    BASE_URL
            );

            String ip = fetchPublicIP();
            api.setIp(ip);

            logInternalDebug("[SunLicense] Validating with server...");
            api.validate();

            plugin.setSunLicenseAPI(api);

            logInternalDebug("[SunLicense] Validation successful");
            return LicenseValidationResult.success(getName());

        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            logInternalDebug("[SunLicense] Validation failed: " + errorMessage);

            LicenseValidationResult.ErrorType errorType = parseErrorType(errorMessage);
            return LicenseValidationResult.failure(
                    getName(),
                    errorType,
                    "SunLicense validation failed",
                    errorMessage
            );
        }
    }

    private String fetchPublicIP() {
        try {
            URL url = new URL("https://api.ipify.org");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            return reader.readLine();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private LicenseValidationResult.ErrorType parseErrorType(String message) {
        if (message == null) return LicenseValidationResult.ErrorType.UNKNOWN;

        String lower = message.toLowerCase();
        if (lower.contains("expired")) return LicenseValidationResult.ErrorType.LICENSE_EXPIRED;
        if (lower.contains("suspended") || lower.contains("banned")) return LicenseValidationResult.ErrorType.LICENSE_SUSPENDED;
        if (lower.contains("hwid")) return LicenseValidationResult.ErrorType.HWID_MISMATCH;
        if (lower.contains("ip")) return LicenseValidationResult.ErrorType.IP_MISMATCH;
        if (lower.contains("product")) return LicenseValidationResult.ErrorType.PRODUCT_MISMATCH;
        if (lower.contains("invalid") || lower.contains("not found")) return LicenseValidationResult.ErrorType.INVALID_LICENSE_KEY;
        if (lower.contains("connection") || lower.contains("timeout")) return LicenseValidationResult.ErrorType.CONNECTION_ERROR;
        if (lower.contains("server") || lower.contains("500") || lower.contains("503")) return LicenseValidationResult.ErrorType.SERVER_ERROR;

        return LicenseValidationResult.ErrorType.UNKNOWN;
    }
}

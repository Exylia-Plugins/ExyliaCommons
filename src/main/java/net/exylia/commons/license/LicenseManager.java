package net.exylia.commons.license;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;

public class LicenseManager {
    private static final String LICENSE_API_URL = "https://license-api.exylia.net/api/licenses/verify/";
    private static final String USER_AGENT = "MinecraftPlugin/1.0";

    private final ExyliaPlugin plugin;
    private final boolean isRequired;
    private LicenseConfig licenseConfig;
    private String licenseKey;
    private boolean isVerified = false;
    private boolean isValidating = false;

    public LicenseManager(ExyliaPlugin plugin, boolean isRequired) {
        this.plugin = plugin;
        this.isRequired = isRequired;
    }

    public CompletableFuture<Void> initializeAndVerify() {
        if (!isRequired) {
            isVerified = true;
            DebugUtils.logInternalSuccess("This plugin is free. No license required.");
            return CompletableFuture.completedFuture(null);
        }

        if (!initializeLicense()) {
            return CompletableFuture.failedFuture(
                    new RuntimeException("Error initializing license system")
            );
        }

        return verifyLicense().thenCompose(result -> {
            if (result.isValid()) {
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.failedFuture(
                        new RuntimeException("License verification failed: " + result.getMessage())
                );
            }
        });
    }

    private boolean initializeLicense() {
        try {
            licenseConfig = new LicenseConfig(plugin);
            licenseKey = licenseConfig.getLicenseKey();

            if (licenseKey == null || licenseKey.trim().isEmpty()) {
                DebugUtils.logInternalError("LICENSE REQUIRED - CONFIG REQUIRED");
                DebugUtils.logInternalError("This plugin requires a license key.");
                DebugUtils.logInternalError("Configure your license key in plugins/" + plugin.getName() + "/license.yml");
                DebugUtils.logInternalError("Join our Discord for help: https://discord.exylia.net/");
                return false;
            }

            DebugUtils.logInternalInfo("License system initialized for: " + plugin.getName());
            return true;
        } catch (Exception e) {
            DebugUtils.logInternalError("Error inicializando sistema de licencias: " + e.getMessage());
            return false;
        }
    }

    private CompletableFuture<LicenseResult> verifyLicense() {
        if (isValidating) {
            return CompletableFuture.completedFuture(
                    new LicenseResult(false, "Validación ya en progreso", LicenseResult.ErrorType.VALIDATION_IN_PROGRESS)
            );
        }

        if (!isRequired) {
            // Plugin gratuito - siempre válido
            isVerified = true;
            return CompletableFuture.completedFuture(
                    new LicenseResult(true, "Plugin gratuito - no requiere licencia", LicenseResult.ErrorType.NONE)
            );
        }

        if (licenseKey == null || licenseKey.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    new LicenseResult(false, "Licencia requerida pero no configurada", LicenseResult.ErrorType.MISSING_LICENSE)
            );
        }

        isValidating = true;
        DebugUtils.logInternalInfo("Verifying license: " + maskLicenseKey(licenseKey));

        return CompletableFuture.supplyAsync(() -> {
            try {
                LicenseResult result = performLicenseVerification();
                handleLicenseResult(result);
                return result;
            } catch (Exception e) {
                DebugUtils.logInternalError("Error verificando licencia: " + e.getMessage());
                return new LicenseResult(false, "Error interno: " + e.getMessage(), LicenseResult.ErrorType.CONNECTION_ERROR);
            } finally {
                isValidating = false;
            }
        });
    }

    private void handleLicenseResult(LicenseResult result) {
        if (result.isValid()) {
            isVerified = true;
            DebugUtils.logInternalSuccess("Valid license for " + plugin.getName());
        } else {
            isVerified = false;
            DebugUtils.logInternalError("INVALID LICENSE FOR " + plugin.getName());
            DebugUtils.logInternalError("Reason: " + result.getMessage());

            switch (result.getErrorType()) {
                case INVALID_LICENSE:
                    DebugUtils.logInternalError("Your license key is invalid or has expired.");
                    DebugUtils.logInternalError("Join our Discord server to get a new license key. https://discord.exylia.net/");
                    break;
                case CONNECTION_ERROR:
                    DebugUtils.logInternalError("Error with the connection to the license server.");
                    DebugUtils.logInternalError("Check your internet connection and try again.");
                    break;
                case SERVER_ERROR:
                    DebugUtils.logInternalError("Error in the license server.");
                    DebugUtils.logInternalError("Please try again later or contact the support team. https://discord.exylia.net/");
                    break;
                default:
                    DebugUtils.logInternalError("Unknown error type: " + result.getErrorType());
                    break;
            }

            DebugUtils.logInternalError("Plugin will be disabled due to invalid license.");
        }
    }

    private String maskLicenseKey(String key) {
        if (key == null || key.length() < 10) {
            return "***";
        }
        return key.substring(0, 5) + "-*****-*****-*****-" + key.substring(key.length() - 5);
    }

    private LicenseResult performLicenseVerification() {
        try {
            String serverHWID = getHWID();
            String serverIP = getServerIP();

            // Crear payload JSON
            JsonObject payload = new JsonObject();
            payload.addProperty("key", licenseKey);
            payload.addProperty("hwid", serverHWID);
            payload.addProperty("plugin_version", plugin.getDescription().getVersion());
            payload.addProperty("server_name", Bukkit.getServer().getName());
            payload.addProperty("minecraft_version", Bukkit.getVersion());

            // Realizar petición HTTP
            HttpURLConnection connection = createConnection();
            sendPayload(connection, payload.toString());

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection);

            DebugUtils.logInternalInfo("Respuesta del servidor de licencias: " + responseCode);

            if (responseCode == 200 || (responseCode == 404 && isValidJson(responseBody))) {
                try {
                    JsonObject response = new Gson().fromJson(responseBody, JsonObject.class);

                    if (response.has("valid") && response.has("message")) {
                        boolean valid = response.get("valid").getAsBoolean();
                        String message = response.get("message").getAsString();

                        if (valid) {
                            DebugUtils.logInternalSuccess("Licencia verificada exitosamente para " + plugin.getName());
                            return new LicenseResult(true, message, LicenseResult.ErrorType.NONE);
                        } else {
                            DebugUtils.logInternalError("Licencia inválida para " + plugin.getName() + ": " + message);
                            return new LicenseResult(false, message, LicenseResult.ErrorType.INVALID_LICENSE);
                        }
                    } else {
                        String errorMsg = "Respuesta del servidor inválida: " + responseBody;
                        DebugUtils.logInternalError(errorMsg);
                        return new LicenseResult(false, errorMsg, LicenseResult.ErrorType.SERVER_ERROR);
                    }
                } catch (Exception jsonException) {
                    String errorMsg = "Error del servidor: " + responseCode + " - " + responseBody;
                    DebugUtils.logInternalError(errorMsg);
                    return new LicenseResult(false, errorMsg, LicenseResult.ErrorType.SERVER_ERROR);
                }
            } else {
                String errorMsg = "Error del servidor: " + responseCode + " - " + responseBody;
                DebugUtils.logInternalError(errorMsg);
                return new LicenseResult(false, errorMsg, LicenseResult.ErrorType.SERVER_ERROR);
            }

        } catch (IOException e) {
            String errorMsg = "Error de conexión con servidor de licencias: " + e.getMessage();
            DebugUtils.logInternalError(errorMsg);
            return new LicenseResult(false, errorMsg, LicenseResult.ErrorType.CONNECTION_ERROR);
        } catch (Exception e) {
            String errorMsg = "Error inesperado: " + e.getMessage();
            DebugUtils.logInternalError(errorMsg);
            return new LicenseResult(false, errorMsg, LicenseResult.ErrorType.UNEXPECTED_ERROR);
        }
    }

    private boolean isValidJson(String jsonString) {
        try {
            new Gson().fromJson(jsonString, JsonObject.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private HttpURLConnection createConnection() throws IOException {
        URL url = new URL(LICENSE_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);

        return connection;
    }

    private void sendPayload(HttpURLConnection connection, String payload) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getResponseCode() >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();

        if (inputStream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String getHWID() {
        try {
            // Obtener MAC
            StringBuilder macBuilder = new StringBuilder();
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface net = networks.nextElement();
                byte[] mac = net.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    for (byte b : mac) {
                        macBuilder.append(String.format("%02X", b));
                    }
                    break;
                }
            }

            String hostname = InetAddress.getLocalHost().getHostName();
            String os = System.getProperty("os.name");
            String user = System.getProperty("user.name");

            String rawHWID = macBuilder + "-" + hostname + "-" + os + "-" + user;

            // Hasheamos para que no sea legible directamente
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawHWID.getBytes(StandardCharsets.UTF_8));

            // Convertimos a hex
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "UNKNOWN";
        }
    }

    private String getServerIP() {
        try {
            return Bukkit.getIp().isEmpty() ? "localhost" : Bukkit.getIp();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public boolean isVerified() {
        return isVerified;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public boolean isValidating() {
        return isValidating;
    }

    public LicenseConfig getLicenseConfig() {
        return licenseConfig;
    }
}
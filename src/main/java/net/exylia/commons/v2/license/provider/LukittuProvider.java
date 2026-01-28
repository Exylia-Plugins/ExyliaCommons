package net.exylia.commons.v2.license.provider;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.license.result.LicenseValidationResult;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LukittuProvider implements LicenseProvider {

    private static final String TEAM_ID;
    private static final String PUBLIC_KEY;
    private static final String VERIFY_URL;
    private static final int TIMEOUT_MS = 10000;
    private static final Gson GSON = new Gson();
    private static final Map<String, String> ERROR_MESSAGES;
    private static final Map<String, LicenseValidationResult.ErrorType> ERROR_TYPES;

    static {
        TEAM_ID = "824ea588-c7b2-4cf3-b9af-25cff6b5385c";
        PUBLIC_KEY = "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUFtZkF5UUZSV3hRUGRDRndrSzJXTQpSeHhaWVRiWnpsTHBRNWE1dkhBdENQU3ZWaXRIRGVVN0FyeC94UW5pRUVGQW5rOVZRK2VLVGxEZWd6WjRwNU9lCjZQajdQSXNwVFBzL1pNWjNtMlFYN0ZOWkV6TnBTQjk2VjBuQ1RUWFlYdjVUNndEbWVDOEhhWG5iVXFNUUJkZmYKR0ZaUjYrajU4clhBR1I2Q2o5Vkh2MVJyRmhuWEU2cDNXemcxS1F4dG9wVElxNlMzMGtJaGZLeFN4YWh4blVjVQowcmt1N3pOWkFpMGp4RHNESE9hU1lmSmFKbllTUk04bXdvb0tRSWFNNmFWdVIzYUt2MU84aTNHcXJmZFUwcFFUCkFjTWMzZU9nb0poam0yVzR5U2Z5RFZsR3lmZkxpbTgzWHhOWDU0SzlLMEJhUm9wc3Z1R1lSNXpVYjFNT05Ea1gKOHdJREFRQUIKLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0tCg==";
        VERIFY_URL = "https://app.lukittu.com/api/v1/client/teams/" + TEAM_ID + "/verification/verify";

        Map<String, String> messages = new HashMap<>();
        messages.put("INTERNAL_SERVER_ERROR", "An unexpected error occurred on the license server.");
        messages.put("BAD_REQUEST", "The license request was malformed.");
        messages.put("LICENSE_NOT_FOUND", "License key not found or invalid.");
        messages.put("VALID", "License is valid.");
        messages.put("IP_LIMIT_REACHED", "IP address limit reached. Contact support if needed.");
        messages.put("HWID_LIMIT_REACHED", "Hardware ID limit reached. Contact support if needed.");
        messages.put("DEVICE_LIMIT_REACHED", "Device limit reached. Contact support if needed.");
        messages.put("PRODUCT_NOT_FOUND", "Product not found for this license.");
        messages.put("CUSTOMER_NOT_FOUND", "Customer information not found.");
        messages.put("LICENSE_EXPIRED", "License has expired.");
        messages.put("LICENSE_SUSPENDED", "License is suspended.");
        messages.put("TEAM_NOT_FOUND", "License team not found.");
        messages.put("RATE_LIMIT", "Too many requests. Please wait and try again.");
        messages.put("HARDWARE_IDENTIFIER_BLACKLISTED", "Hardware identifier is blacklisted.");
        messages.put("COUNTRY_BLACKLISTED", "Your country is not allowed.");
        messages.put("IP_BLACKLISTED", "Your IP address is blacklisted.");
        messages.put("RELEASE_NOT_FOUND", "Release not found.");
        messages.put("FORBIDDEN", "Access denied.");
        ERROR_MESSAGES = Collections.unmodifiableMap(messages);

        Map<String, LicenseValidationResult.ErrorType> types = new HashMap<>();
        types.put("LICENSE_NOT_FOUND", LicenseValidationResult.ErrorType.INVALID_LICENSE_KEY);
        types.put("LICENSE_EXPIRED", LicenseValidationResult.ErrorType.LICENSE_EXPIRED);
        types.put("LICENSE_SUSPENDED", LicenseValidationResult.ErrorType.LICENSE_SUSPENDED);
        types.put("RATE_LIMIT", LicenseValidationResult.ErrorType.RATE_LIMITED);
        types.put("RELEASE_NOT_FOUND", LicenseValidationResult.ErrorType.PRODUCT_MISMATCH);
        types.put("PRODUCT_NOT_FOUND", LicenseValidationResult.ErrorType.PRODUCT_MISMATCH);
        types.put("IP_LIMIT_REACHED", LicenseValidationResult.ErrorType.IP_MISMATCH);
        types.put("IP_BLACKLISTED", LicenseValidationResult.ErrorType.IP_MISMATCH);
        types.put("HWID_LIMIT_REACHED", LicenseValidationResult.ErrorType.HWID_MISMATCH);
        types.put("DEVICE_LIMIT_REACHED", LicenseValidationResult.ErrorType.HWID_MISMATCH);
        types.put("HARDWARE_IDENTIFIER_BLACKLISTED", LicenseValidationResult.ErrorType.HWID_MISMATCH);
        types.put("BAD_REQUEST", LicenseValidationResult.ErrorType.SERVER_ERROR);
        types.put("INTERNAL_SERVER_ERROR", LicenseValidationResult.ErrorType.SERVER_ERROR);
        ERROR_TYPES = Collections.unmodifiableMap(types);
    }

    @Override
    public String getName() {
        return "Lukittu";
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public LicenseValidationResult validate(ExyliaPlugin plugin, String licenseKey) {
        if (TEAM_ID.isEmpty() || PUBLIC_KEY.isEmpty()) {
            return LicenseValidationResult.failure(getName(), LicenseValidationResult.ErrorType.SERVER_ERROR,
                    "Lukittu not configured", null);
        }

        String productId = plugin.getLukittuProductId();
        if (productId == null || productId.isEmpty()) {
            return LicenseValidationResult.failure(getName(), LicenseValidationResult.ErrorType.PRODUCT_MISMATCH,
                    "Lukittu product ID not configured", null);
        }

        try {
            String challenge = generateChallenge();
            String hardwareId = getHardwareIdentifier();

            JsonObject payload = new JsonObject();
            payload.addProperty("licenseKey", licenseKey);
            payload.addProperty("productId", productId);
            payload.addProperty("challenge", challenge);
            payload.addProperty("hardwareIdentifier", hardwareId);

            HttpURLConnection connection = createConnection();
            sendPayload(connection, GSON.toJson(payload));

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection);

            if (responseCode != 200) {
                return handleErrorResponse(responseBody);
            }

            return handleSuccessResponse(responseBody, challenge);

        } catch (Exception e) {
            return LicenseValidationResult.failure(getName(), LicenseValidationResult.ErrorType.CONNECTION_ERROR,
                    "Could not connect to license server", null);
        }
    }

    private LicenseValidationResult handleSuccessResponse(String responseBody, String challenge) {
        try {
            JsonObject response = GSON.fromJson(responseBody, JsonObject.class);

            if (!response.has("result")) {
                return LicenseValidationResult.failure(getName(), LicenseValidationResult.ErrorType.SERVER_ERROR,
                        "Invalid server response", null);
            }

            JsonObject result = response.getAsJsonObject("result");
            String code = result.has("code") ? result.get("code").getAsString() : "UNKNOWN";

            if (!result.has("valid") || !result.get("valid").getAsBoolean()) {
                String message = ERROR_MESSAGES.getOrDefault(code, "License validation failed");
                LicenseValidationResult.ErrorType errorType = ERROR_TYPES.getOrDefault(code, LicenseValidationResult.ErrorType.UNKNOWN);
                return LicenseValidationResult.failure(getName(), errorType, message, code);
            }

            if (result.has("challengeResponse") && !PUBLIC_KEY.isEmpty()) {
                String challengeResponse = result.get("challengeResponse").getAsString();
                if (!verifySignature(challenge, challengeResponse)) {
                    return LicenseValidationResult.failure(getName(), LicenseValidationResult.ErrorType.SIGNATURE_INVALID,
                            "Security verification failed", null);
                }
            }

            return LicenseValidationResult.success(getName());

        } catch (Exception e) {
            return LicenseValidationResult.failure(getName(), LicenseValidationResult.ErrorType.SERVER_ERROR,
                    "Failed to process server response", null);
        }
    }

    private LicenseValidationResult handleErrorResponse(String responseBody) {
        try {
            JsonObject response = GSON.fromJson(responseBody, JsonObject.class);
            if (response.has("result")) {
                JsonObject result = response.getAsJsonObject("result");
                String code = result.has("code") ? result.get("code").getAsString() : "UNKNOWN";
                String message = ERROR_MESSAGES.getOrDefault(code, "License server error");
                LicenseValidationResult.ErrorType errorType = ERROR_TYPES.getOrDefault(code, LicenseValidationResult.ErrorType.SERVER_ERROR);
                return LicenseValidationResult.failure(getName(), errorType, message, code);
            }
        } catch (Exception ignored) {}

        return LicenseValidationResult.failure(getName(), LicenseValidationResult.ErrorType.SERVER_ERROR,
                "License server error", null);
    }

    private boolean verifySignature(String challenge, String signedChallenge) {
        try {
            byte[] decoded = Base64.getDecoder().decode(PUBLIC_KEY);
            byte[] keyBytes;

            String decodedStr = new String(decoded, StandardCharsets.UTF_8);
            if (decodedStr.contains("-----BEGIN")) {
                String pem = decodedStr
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "");
                keyBytes = Base64.getDecoder().decode(pem);
            } else {
                keyBytes = decoded;
            }

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(challenge.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = hexToBytes(signedChallenge);
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    private String generateChallenge() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String getHardwareIdentifier() {
        String osName = System.getProperty("os.name", "unknown");
        String osArch = System.getProperty("os.arch", "unknown");
        String osVersion = System.getProperty("os.version", "unknown");
        String userName = System.getProperty("user.name", "unknown");
        return Base64.getEncoder().encodeToString(
                (osName + osArch + osVersion + userName).getBytes(StandardCharsets.UTF_8)
        );
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private HttpURLConnection createConnection() throws IOException {
        URL url = new URL(VERIFY_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        return connection;
    }

    private void sendPayload(HttpURLConnection connection, String payload) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getResponseCode() >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();

        if (inputStream == null) return "";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}

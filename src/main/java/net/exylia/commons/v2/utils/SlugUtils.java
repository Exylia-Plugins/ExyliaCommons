package net.exylia.commons.v2.utils;

import java.util.Optional;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    );
    private static final Pattern VALID_SLUG = Pattern.compile("^[a-z0-9][a-z0-9_-]*$");
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-z0-9_-]");
    private static final Pattern REPEATED_SEPARATORS = Pattern.compile("[_-]{2,}");
    private static final Pattern LEADING_TRAILING_SEPARATORS = Pattern.compile("^[_-]+|[_-]+$");

    private SlugUtils() {}

    public static boolean isUUID(String value) {
        if (value == null || value.length() != 36) return false;
        return UUID_PATTERN.matcher(value).matches();
    }

    public static boolean isValid(String value) {
        if (value == null || value.isEmpty()) return false;
        return VALID_SLUG.matcher(value).matches();
    }

    public static Optional<String> sanitize(String raw) {
        if (raw == null) return Optional.empty();
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return Optional.empty();
        if (isUUID(trimmed.toLowerCase())) return Optional.of(trimmed.toLowerCase());

        String result = trimmed.toLowerCase();
        result = result.replace(' ', '_');
        result = INVALID_CHARS.matcher(result).replaceAll("");
        result = REPEATED_SEPARATORS.matcher(result).replaceAll(m -> String.valueOf(m.group().charAt(0)));
        result = LEADING_TRAILING_SEPARATORS.matcher(result).replaceAll("");

        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }
}

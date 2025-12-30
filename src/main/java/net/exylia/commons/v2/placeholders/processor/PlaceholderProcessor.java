package net.exylia.commons.v2.placeholders.processor;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.placeholders.papi.PapiAdapter;
import net.exylia.commons.v2.placeholders.registry.PlaceholderRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderProcessor {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");
    private static final PlaceholderRegistry registry = PlaceholderRegistry.getInstance();

    private PlaceholderProcessor() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String process(String text, Player player, PlaceholderContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        long startTime = System.nanoTime();
        String result = text;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        int placeholdersFound = 0;

        while (matcher.find()) {
            placeholdersFound++;
            String placeholderName = matcher.group(1);
            Object resolved = registry.resolve(placeholderName, player, context);
            String replacement = objectToString(resolved, matcher.group(0));

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        result = sb.toString();

        if (player != null) {
            try {
                result = PapiAdapter.getInstance().setPlaceholders(player, result);
            } catch (Exception e) {
                DebugAPI.logLibError(DebugCategory.PLACEHOLDER, "Error processing PAPI placeholders: " + e.getMessage());
            }
        }

        double millis = (System.nanoTime() - startTime) / 1_000_000.0;
        if (placeholdersFound > 0) {
            DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER,
                String.format("Processed %d placeholder(s) in %.3fms", placeholdersFound, millis));
        }

        return result;
    }

    public static String process(String text, Player player) {
        return process(text, player, null);
    }

    public static String process(String text, PlaceholderContext context) {
        return process(text, null, context);
    }

    public static String process(String text) {
        return process(text, null, null);
    }

    public static CompletableFuture<String> processAsync(String text, Player player, PlaceholderContext context) {
        if (text == null || text.isEmpty()) {
            return CompletableFuture.completedFuture(text);
        }

        long startTime = System.nanoTime();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        List<String> placeholders = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();

        while (matcher.find()) {
            placeholders.add(matcher.group(1));
            starts.add(matcher.start());
            ends.add(matcher.end());
        }

        if (placeholders.isEmpty()) {
            return CompletableFuture.completedFuture(text);
        }

        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER,
            String.format("Processing %d placeholder(s) asynchronously", placeholders.size()));

        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (String placeholder : placeholders) {
            futures.add(registry.resolveAsync(placeholder, player, context));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    String result = text;
                    for (int i = placeholders.size() - 1; i >= 0; i--) {
                        String replacement = futures.get(i).join();
                        int start = starts.get(i);
                        int end = ends.get(i);
                        String originalPlaceholder = text.substring(start, end);
                        if (replacement == null || replacement.isEmpty()) {
                            replacement = originalPlaceholder;
                        }
                        result = result.substring(0, start) + replacement + result.substring(end);
                    }

                    double millis = (System.nanoTime() - startTime) / 1_000_000.0;
                    DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER,
                        String.format("Async processing completed in %.3fms", millis));

                    return result;
                })
                .exceptionally(throwable -> {
                    DebugAPI.logLibError(DebugCategory.PLACEHOLDER,
                        "Error in async placeholder processing: " + throwable.getMessage(), throwable);
                    return text;
                });
    }

    public static CompletableFuture<String> processAsync(String text, Player player) {
        return processAsync(text, player, null);
    }

    public static CompletableFuture<String> processAsync(String text, PlaceholderContext context) {
        return processAsync(text, null, context);
    }

    public static CompletableFuture<String> processAsync(String text) {
        return processAsync(text, null, null);
    }

    public static List<String> extractPlaceholders(String text) {
        List<String> placeholders = new ArrayList<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);

        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }

        return placeholders;
    }

    public static boolean containsPlaceholders(String text) {
        return text != null && PLACEHOLDER_PATTERN.matcher(text).find();
    }

    private static String objectToString(Object obj, String originalPlaceholder) {
        if (obj == null) {
            return originalPlaceholder;
        }

        if (obj instanceof Component component) {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }

        return obj.toString();
    }
}

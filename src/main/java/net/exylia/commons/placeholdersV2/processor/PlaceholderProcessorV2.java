package net.exylia.commons.placeholdersV2.processor;

import net.exylia.commons.placeholdersV2.context.PlaceholderContext;
import net.exylia.commons.placeholdersV2.registry.PlaceholderRegistryV2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderProcessorV2 {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");
    private static final PlaceholderRegistryV2 registry = PlaceholderRegistryV2.getInstance();

    private PlaceholderProcessorV2() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String process(String text, Player player, PlaceholderContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String placeholderName = matcher.group(1);
            Object resolved = registry.resolve(placeholderName, player, context);
            String replacement = objectToString(resolved);

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
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
                        result = result.substring(0, start) + replacement + result.substring(end);
                    }
                    return result;
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

    private static String objectToString(Object obj) {
        if (obj == null) {
            return "";
        }

        if (obj instanceof Component component) {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }

        return obj.toString();
    }
}

package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.visual.color.ColorPresetManager;
import net.exylia.commons.v2.visual.color.ColorProcessor;
import net.exylia.commons.v2.visual.color.FontTransformer;
import net.exylia.commons.v2.visual.color.MessageCenterer;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class ColorAPI {
    private ColorAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        ColorPresetManager.getInstance().initialize(plugin);
    }

    public static void initialize(JavaPlugin plugin, Map<String, String> customPresets) {
        ColorPresetManager.getInstance().initialize(plugin, customPresets);
    }

    public static Component parse(String message) {
        return net.exylia.commons.v2.visual.cache.ComponentCache.getOrParse(message);
    }

    public static CompletableFuture<Component> parseAsync(String message) {
        return CompletableFuture.supplyAsync(() -> parse(message));
    }

    public static List<Component> parse(List<String> messages) {
        return messages.stream()
                .map(ColorAPI::parse)
                .collect(Collectors.toList());
    }

    public static CompletableFuture<List<Component>> parseAsync(List<String> messages) {
        return CompletableFuture.supplyAsync(() -> parse(messages));
    }

    public static String parseToString(String message) {
        return ColorProcessor.processString(message);
    }

    public static String stripColors(String message) {
        return ColorProcessor.stripColors(message);
    }

    public static String stripColors(Component component) {
        return ColorProcessor.stripColors(component);
    }

    public static String normalizeColor(String input) {
        return ColorProcessor.normalizeColor(input);
    }

    public static String generateRandomHexColor() {
        int red, green, blue;
        do {
            red = ThreadLocalRandom.current().nextInt(50, 220);
            green = ThreadLocalRandom.current().nextInt(50, 220);
            blue = ThreadLocalRandom.current().nextInt(50, 220);
        } while (isTooLight(red, green, blue) || isTooDark(red, green, blue));

        return String.format("<#%02x%02x%02x>", red, green, blue);
    }

    private static boolean isTooLight(int red, int green, int blue) {
        return (red + green + blue) > 600;
    }

    private static boolean isTooDark(int red, int green, int blue) {
        return (red + green + blue) < 200;
    }

    public static String centerMessage(String message) {
        return MessageCenterer.center(message);
    }

    public static List<String> centerMessages(List<String> messages) {
        return MessageCenterer.centerMultiple(messages);
    }

    public static String centerMessageWithWidth(String message, int maxWidth) {
        return MessageCenterer.centerWithMaxWidth(message, maxWidth);
    }

    public static boolean fitsInChat(String message) {
        return MessageCenterer.fitsInChat(message);
    }

    public static int getPixelWidth(String message) {
        return MessageCenterer.getPixelWidth(message);
    }

    public static String applyFont(String message, FontTransformer.FontType fontType) {
        return FontTransformer.transform(message, fontType, false);
    }

    public static String applyFont(String message, FontTransformer.FontType fontType, boolean forceUpperCase) {
        return FontTransformer.transform(message, fontType, forceUpperCase);
    }

    public static String applyFont(String message, String fontType) {
        return FontTransformer.transform(message, fontType, false);
    }

    public static String applyFont(String message, String fontType, boolean forceUpperCase) {
        return FontTransformer.transform(message, fontType, forceUpperCase);
    }

    public static String getColorPreset(String presetName) {
        return ColorPresetManager.getInstance().getColorPreset(presetName);
    }

    public static Map<String, String> getAllColorPresets() {
        return ColorPresetManager.getInstance().getAllColorPresets();
    }

    public static void addColorPreset(String name, String colorCode) {
        ColorPresetManager.getInstance().addCustomColorPreset(name, colorCode);
    }

    public static void addColorPresets(Map<String, String> customPresets) {
        ColorPresetManager.getInstance().addCustomColorPresets(customPresets);
    }

    public static void reloadPresets() {
        ColorPresetManager.getInstance().reload();
    }

    public static void clearCache() {
        ColorProcessor.clearCache();
        net.exylia.commons.v2.visual.cache.ColorCache.clear();
        net.exylia.commons.v2.visual.cache.ComponentCache.clear();
    }

    public static boolean isInitialized() {
        return ColorPresetManager.getInstance().isInitialized();
    }
}

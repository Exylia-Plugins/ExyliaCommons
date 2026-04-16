package net.exylia.commons.v2.chat.api;

import net.exylia.commons.v2.chat.config.ChatInputConfig;
import net.exylia.commons.v2.chat.core.ChatInputManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class ChatInputAPI {

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_]{3,}$");

    private ChatInputAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void init(Plugin pluginInstance) {
        ChatInputManager.init(pluginInstance);
    }

    public static void ask(Player player, String prompt, Consumer<String> callback) {
        ask(player, ChatInputConfig.builder().prompt(prompt).build(), callback);
    }

    public static void ask(Player player, ChatInputConfig config, Consumer<String> callback) {
        ChatInputManager.getInstance().startSession(player, config, callback);
    }

    public static void askInt(Player player, String prompt, Consumer<Integer> callback) {
        askInt(player, ChatInputConfig.builder().prompt(prompt).build(), Integer.MIN_VALUE, Integer.MAX_VALUE, false, callback);
    }

    public static void askInt(Player player, String prompt, int min, int max, Consumer<Integer> callback) {
        askInt(player, ChatInputConfig.builder().prompt(prompt).build(), min, max, false, callback);
    }

    public static void askInt(Player player, String prompt, int min, int max, boolean allowUnlimited, Consumer<Integer> callback) {
        askInt(player, ChatInputConfig.builder().prompt(prompt).build(), min, max, allowUnlimited, callback);
    }

    public static void askInt(Player player, ChatInputConfig config, Consumer<Integer> callback) {
        askInt(player, config, Integer.MIN_VALUE, Integer.MAX_VALUE, false, callback);
    }

    public static void askInt(Player player, ChatInputConfig config, int min, int max, Consumer<Integer> callback) {
        askInt(player, config, min, max, false, callback);
    }

    public static void askInt(Player player, ChatInputConfig config, int min, int max, boolean allowUnlimited, Consumer<Integer> callback) {
        String rangeMsg = (min != Integer.MIN_VALUE || max != Integer.MAX_VALUE)
                ? " between " + min + " and " + max
                : "";
        String unlimitedHint = allowUnlimited ? " (-1 = unlimited)" : "";

        ChatInputConfig built = config.toBuilder()
                .validator(input -> {
                    try {
                        int value = Integer.parseInt(input);
                        if (allowUnlimited && value == -1) return true;
                        return value >= min && value <= max;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .invalidMessage("&cMust be a number" + rangeMsg + unlimitedHint)
                .build();

        ask(player, built, input -> callback.accept(Integer.parseInt(input)));
    }

    public static void askDouble(Player player, String prompt, Consumer<Double> callback) {
        askDouble(player, ChatInputConfig.builder().prompt(prompt).build(), -Double.MAX_VALUE, Double.MAX_VALUE, callback);
    }

    public static void askDouble(Player player, String prompt, double min, double max, Consumer<Double> callback) {
        askDouble(player, ChatInputConfig.builder().prompt(prompt).build(), min, max, callback);
    }

    public static void askDouble(Player player, ChatInputConfig config, Consumer<Double> callback) {
        askDouble(player, config, -Double.MAX_VALUE, Double.MAX_VALUE, callback);
    }

    public static void askDouble(Player player, ChatInputConfig config, double min, double max, Consumer<Double> callback) {
        String rangeMsg = (min != -Double.MAX_VALUE || max != Double.MAX_VALUE)
                ? " between " + min + " and " + max
                : "";

        ChatInputConfig built = config.toBuilder()
                .validator(input -> {
                    try {
                        double value = Double.parseDouble(input);
                        return value >= min && value <= max;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .invalidMessage("&cMust be a number" + rangeMsg)
                .build();

        ask(player, built, input -> callback.accept(Double.parseDouble(input)));
    }

    public static void askId(Player player, String prompt, Consumer<String> callback) {
        askId(player, ChatInputConfig.builder().prompt(prompt).build(), callback);
    }

    public static void askId(Player player, ChatInputConfig config, Consumer<String> callback) {
        ChatInputConfig built = config.toBuilder()
                .validator(input -> input != null && ID_PATTERN.matcher(input).matches())
                .invalidMessage("&cInvalid ID. Min 3 chars, only letters, numbers, - and _.")
                .build();
        ask(player, built, callback);
    }

    public static void cancel(Player player) {
        ChatInputManager.getInstance().cancelSession(player);
    }

    public static boolean hasActiveSession(Player player) {
        return ChatInputManager.getInstance().hasSession(player);
    }
}

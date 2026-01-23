package net.exylia.commons.v2.chat.api;

import net.exylia.commons.v2.chat.config.ChatInputConfig;
import net.exylia.commons.v2.chat.core.ChatInputManager;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public final class ChatInputAPI {

    private ChatInputAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void ask(Player player, String prompt, Consumer<String> callback) {
        ask(player, ChatInputConfig.builder().prompt(prompt).build(), callback);
    }

    public static void ask(Player player, ChatInputConfig config, Consumer<String> callback) {
        ChatInputManager.getInstance().startSession(player, config, callback);
    }

    public static void askInt(Player player, String prompt, Consumer<Integer> callback) {
        askInt(player, prompt, Integer.MIN_VALUE, Integer.MAX_VALUE, callback);
    }

    public static void askInt(Player player, String prompt, int min, int max, Consumer<Integer> callback) {
        askInt(player, prompt, min, max, false, callback);
    }

    public static void askInt(Player player, String prompt, int min, int max, boolean allowUnlimited, Consumer<Integer> callback) {
        String rangeMsg = (min != Integer.MIN_VALUE || max != Integer.MAX_VALUE)
                ? " between " + min + " and " + max
                : "";
        String unlimitedHint = allowUnlimited ? " (-1 = unlimited)" : "";

        ChatInputConfig config = ChatInputConfig.builder()
                .prompt(prompt)
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

        ask(player, config, input -> callback.accept(Integer.parseInt(input)));
    }

    public static void askDouble(Player player, String prompt, Consumer<Double> callback) {
        askDouble(player, prompt, -Double.MAX_VALUE, Double.MAX_VALUE, callback);
    }

    public static void askDouble(Player player, String prompt, double min, double max, Consumer<Double> callback) {
        String rangeMsg = (min != -Double.MAX_VALUE || max != Double.MAX_VALUE)
                ? " between " + min + " and " + max
                : "";

        ChatInputConfig config = ChatInputConfig.builder()
                .prompt(prompt)
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

        ask(player, config, input -> callback.accept(Double.parseDouble(input)));
    }

    public static void cancel(Player player) {
        ChatInputManager.getInstance().cancelSession(player);
    }

    public static boolean hasActiveSession(Player player) {
        return ChatInputManager.getInstance().hasSession(player);
    }
}

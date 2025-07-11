package net.exylia.commons.chat.input;

import net.exylia.commons.utils.MessageUtils;
import org.bukkit.entity.Player;

public class InputHandlers {

    public static InputHandler<String> text(String prompt) {
        return new InputHandler<String>() {
            @Override
            public void onStart(Player player) {
                MessageUtils.sendMessage(player, "{info}" + prompt);
                MessageUtils.sendMessage(player, "{warning}Type 'cancel' to cancel");
            }

            @Override
            public InputResult onInput(Player player, String input) {
                if (input.equalsIgnoreCase("cancel")) {
                    return InputResult.cancel("{error}Cancelled");
                }
                return InputResult.success(input);
            }
        };
    }

    public static InputHandler<Integer> number(String prompt, int min, int max) {
        return new InputHandler<Integer>() {
            @Override
            public void onStart(Player player) {
                MessageUtils.sendMessage(player, "{info}" + prompt);
                MessageUtils.sendMessage(player, "{warning}Range: " + min + " - " + max);
            }

            @Override
            public InputResult onInput(Player player, String input) {
                if (input.equalsIgnoreCase("cancel")) {
                    return InputResult.cancel();
                }
                try {
                    int number = Integer.parseInt(input);
                    if (number < min || number > max) {
                        return InputResult.invalid("{error}Number must be between " + min + " and " + max);
                    }
                    return InputResult.success(number);
                } catch (NumberFormatException e) {
                    return InputResult.invalid("{error}Invalid number");
                }
            }
        };
    }
}

package net.exylia.commons.chat.input;

import net.exylia.commons.utils.visuals.MessageUtils;
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

    public static InputHandler<String> id(String prompt) {
        return new InputHandler<String>() {
            @Override
            public void onStart(Player player) {
                MessageUtils.sendMessage(player, "{info}" + prompt);
                MessageUtils.sendMessage(player, "{warning}Format: lowercase_with_underscores (e.g., kit_de_ejemplo)");
                MessageUtils.sendMessage(player, "{warning}Type 'cancel' to cancel");
            }

            @Override
            public InputResult onInput(Player player, String input) {
                if (input.equalsIgnoreCase("cancel")) {
                    return InputResult.cancel("{error}Cancelled");
                }

                // Convertir a formato de ID válido
                String formattedId = formatToId(input);

                // Verificar que no esté vacío después del formateo
                if (formattedId.isEmpty()) {
                    return InputResult.invalid("{error}ID cannot be empty after formatting");
                }

                // Mostrar el resultado formateado si es diferente del input original
                if (!input.equals(formattedId)) {
                    MessageUtils.sendMessage(player, "{success}Formatted ID: " + formattedId);
                }

                return InputResult.success(formattedId);
            }
        };
    }

    public static InputHandler<Integer> number(String prompt, int min, int max) {
        return new InputHandler<Integer>() {
            @Override
            public void onStart(Player player) {
                MessageUtils.sendMessage(player, "{info}" + prompt);

                // Si min y max son -1, significa cualquier número
                if (min == -1 && max == -1) {
                    MessageUtils.sendMessage(player, "{warning}Any number is allowed");
                } else {
                    MessageUtils.sendMessage(player, "{warning}Range: " + min + " - " + max);
                }

                MessageUtils.sendMessage(player, "{warning}Type 'cancel' to cancel");
            }

            @Override
            public InputResult onInput(Player player, String input) {
                if (input.equalsIgnoreCase("cancel")) {
                    return InputResult.cancel("{error}Cancelled");
                }

                try {
                    int number = Integer.parseInt(input);

                    // Si min y max son -1, aceptar cualquier número
                    if (min == -1 && max == -1) {
                        return InputResult.success(number);
                    }

                    // Verificar rango normal
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

    private static String formatToId(String input) {
        return input
                .toLowerCase() // Convertir a minúsculas
                .replaceAll("[\\s\\-]+", "_") // Espacios y guiones -> guiones bajos
                .replaceAll("[^a-z0-9_]", "") // Remover caracteres no alfanuméricos
                .replaceAll("_{2,}", "_") // Remover guiones bajos duplicados
                .replaceAll("^_+|_+$", ""); // Remover guiones bajos al inicio/final
    }
}
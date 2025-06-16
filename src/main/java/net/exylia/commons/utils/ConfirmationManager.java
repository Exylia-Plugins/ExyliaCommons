package net.exylia.commons.utils;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Sistema de confirmación que permite crear flujos donde el jugador
 * debe escribir algo en el chat como parte de un proceso.
 */
public class ConfirmationManager implements Listener {

    private static ConfirmationManager instance;
    private final Map<UUID, ConfirmationData> waitingPlayers;
    private final JavaPlugin plugin;

    private ConfirmationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.waitingPlayers = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Inicializa el sistema de confirmación
     * @param plugin Tu plugin principal
     */
    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new ConfirmationManager(plugin);
        }
    }

    /**
     * Obtiene la instancia del sistema de confirmación
     * @return La instancia del ConfirmationManager
     */
    public static ConfirmationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ConfirmationManager no ha sido inicializado. Llama a initialize() primero.");
        }
        return instance;
    }

    /**
     * Pone a un jugador en estado de confirmación
     * @param player El jugador
     * @param prompt El mensaje que se le mostrará al jugador
     * @param onConfirm Función que se ejecutará cuando el jugador escriba algo
     * @param onCancel Función que se ejecutará si el jugador cancela (opcional)
     * @param validator Función para validar el input del jugador (opcional)
     */
    public void requestConfirmation(Player player, String prompt, Consumer<String> onConfirm,
                                    Runnable onCancel, InputValidator validator) {

        ConfirmationData data = new ConfirmationData(prompt, onConfirm, onCancel, validator);
        waitingPlayers.put(player.getUniqueId(), data);

        // Envía el mensaje de prompt al jugador
        MessageUtils.sendMessageAsync(player, "<#59a4ff>" + prompt);
        MessageUtils.sendMessageAsync(player, "<#e7cfff>Write your answer or type <#a33b53>cancel<#e7cfff> to cancel.");
    }

    public void requestConfirmation(Player player, String prompt, Consumer<String> onConfirm) {
        requestConfirmation(player, prompt, onConfirm, null, null);
    }

    public void requestConfirmation(Player player, String prompt, Consumer<String> onConfirm,
                                    InputValidator validator) {
        requestConfirmation(player, prompt, onConfirm, null, validator);
    }

    /**
     * Verifica si un jugador está en estado de confirmación
     * @param player El jugador
     * @return true si está esperando confirmación
     */
    public boolean isWaitingForConfirmation(Player player) {
        return waitingPlayers.containsKey(player.getUniqueId());
    }

    /**
     * Cancela manualmente la confirmación de un jugador
     * @param player El jugador
     */
    public void cancelConfirmation(Player player) {
        ConfirmationData data = waitingPlayers.remove(player.getUniqueId());
        if (data != null && data.onCancel != null) {
            data.onCancel.run();
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!waitingPlayers.containsKey(playerId)) {
            return;
        }

        event.setCancelled(true);

        ConfirmationData data = waitingPlayers.get(playerId);
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (plainMessage.equalsIgnoreCase("cancelar") || plainMessage.equalsIgnoreCase("cancel")) {
            waitingPlayers.remove(playerId);
            MessageUtils.sendMessageAsync(player, "<#a33b53>Operation cancelled.");

            if (data.onCancel != null) {
                plugin.getServer().getScheduler().runTask(plugin, data.onCancel);
            }
            return;
        }


        // Valida el input si hay un validador
        if (data.validator != null) {
            ValidationResult result = data.validator.validate(plainMessage);
            if (!result.isValid()) {
                MessageUtils.sendMessageAsync(player, "<#a33b53>" + result.getErrorMessage());
                MessageUtils.sendMessageAsync(player, "<#e7cfff>Please try again or type <#a33b53>cancel<#e7cfff> to cancel.");
                return;
            }
        }

        // Remueve al jugador del estado de confirmación
        waitingPlayers.remove(playerId);

        // Ejecuta la función de confirmación en el hilo principal
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            data.onConfirm.accept(plainMessage);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpia al jugador de la lista si se desconecta
        waitingPlayers.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Clase interna para almacenar los datos de confirmación
     */
    private static class ConfirmationData {
        final String prompt;
        final Consumer<String> onConfirm;
        final Runnable onCancel;
        final InputValidator validator;

        ConfirmationData(String prompt, Consumer<String> onConfirm, Runnable onCancel, InputValidator validator) {
            this.prompt = prompt;
            this.onConfirm = onConfirm;
            this.onCancel = onCancel;
            this.validator = validator;
        }
    }

    /**
     * Interfaz para validar el input del jugador
     */
    public interface InputValidator {
        ValidationResult validate(String input);
    }

    /**
     * Clase para el resultado de la validación
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Validadores comunes que puedes usar
     */
    public static class CommonValidators {

        /**
         * Valida que el input no esté vacío
         */
        public static final InputValidator NOT_EMPTY = input -> {
            if (input == null || input.trim().isEmpty()) {
                return ValidationResult.invalid("The input cannot be empty.");
            }
            return ValidationResult.valid();
        };

        /**
         * Valida que el input sea un número
         */
        public static final InputValidator NUMERIC = input -> {
            try {
                Integer.parseInt(input);
                return ValidationResult.valid();
            } catch (NumberFormatException e) {
                return ValidationResult.invalid("The input must be a number.");
            }
        };

        /**
         * Valida que el input tenga una longitud mínima
         */
        public static InputValidator minLength(int minLength) {
            return input -> {
                if (input.length() < minLength) {
                    return ValidationResult.invalid("The input must be at least " + minLength + " characters long.");
                }
                return ValidationResult.valid();
            };
        }

        /**
         * Valida que el input tenga una longitud máxima
         */
        public static InputValidator maxLength(int maxLength) {
            return input -> {
                if (input.length() > maxLength) {
                    return ValidationResult.invalid("The input must be at most " + maxLength + " characters long.");
                }
                return ValidationResult.valid();
            };
        }

        /**
         * Valida que el input solo contenga caracteres alfanuméricos
         */
        public static final InputValidator ALPHANUMERIC = input -> {
            if (!input.matches("^[a-zA-Z0-9]+$")) {
                return ValidationResult.invalid("The input must only contain letters and numbers.");
            }
            return ValidationResult.valid();
        };

        public static final InputValidator ALPHANUMERIC_WITH_HYPHENS = input -> {
            if (!input.matches("^[a-zA-Z0-9-]+$")) {
                return ValidationResult.invalid("The input must only contain letters, numbers, and hyphens.");
            }
            return ValidationResult.valid();
        };

        /**
         * Comb         ina múltiples validadores
         */
        public static InputValidator combine(InputValidator... validators) {
            return input -> {
                for (InputValidator validator : validators) {
                    ValidationResult result = validator.validate(input);
                    if (!result.isValid()) {
                        return result;
                    }
                }
                return ValidationResult.valid();
            };
        }
    }
}
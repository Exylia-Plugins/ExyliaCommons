package net.exylia.commons.chat.input;

import org.bukkit.entity.Player;

/**
 * Custom input handler interface
 * Implement this to create your own input types
 */
public interface InputHandler<T> {

    /**
     * Called when input starts
     * Send instructions to the player here
     */
    void onStart(Player player);

    /**
     * Process player input
     *
     * @param player The player
     * @param input The chat message from player
     * @return InputResult indicating what to do next
     */
    InputResult onInput(Player player, String input);
}
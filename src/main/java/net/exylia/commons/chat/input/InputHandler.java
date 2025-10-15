package net.exylia.commons.chat.input;

import org.bukkit.entity.Player;

public interface InputHandler<T> {

    void onStart(Player player);

    InputResult onInput(Player player, String input);
}

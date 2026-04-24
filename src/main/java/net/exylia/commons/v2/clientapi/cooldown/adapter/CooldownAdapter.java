package net.exylia.commons.v2.clientapi.cooldown.adapter;

import net.exylia.commons.v2.clientapi.cooldown.model.CooldownDefinition;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface CooldownAdapter {

    boolean isAvailable();

    boolean supportsPlayer(Player player);

    void display(Player player, CooldownDefinition definition);

    void remove(Player player, String name);

    void removeAll(Player player);

    default void cleanupPlayer(UUID uuid) {}

    default void shutdown() {}
}

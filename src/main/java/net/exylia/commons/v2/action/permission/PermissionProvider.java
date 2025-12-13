package net.exylia.commons.v2.action.permission;

import org.bukkit.entity.Player;

public interface PermissionProvider {
    boolean hasPermission(Player player, String permission);

    default boolean isEnabled() {
        return true;
    }
}

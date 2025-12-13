package net.exylia.commons.v2.action.permission;

import org.bukkit.entity.Player;

public class BasicPermissionProvider implements PermissionProvider {

    @Override
    public boolean hasPermission(Player player, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        return player.hasPermission(permission);
    }
}

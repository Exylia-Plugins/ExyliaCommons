package net.exylia.commons.v2.action.permission;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultPermissionProvider implements PermissionProvider {
    private final Permission vaultPermission;
    private final boolean enabled;

    public VaultPermissionProvider() {
        Permission vault = null;
        boolean available = false;

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
            if (rsp != null) {
                vault = rsp.getProvider();
                available = true;
            }
        }

        this.vaultPermission = vault;
        this.enabled = available;
    }

    @Override
    public boolean hasPermission(Player player, String permission) {
        if (!enabled || permission == null || permission.isEmpty()) {
            return player.hasPermission(permission);
        }
        return vaultPermission.has(player, permission);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

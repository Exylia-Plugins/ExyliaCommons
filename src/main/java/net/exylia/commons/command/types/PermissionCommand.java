package net.exylia.commons.command.types;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.command.ExyliaCommand;
import net.exylia.commons.v2.config.Messages;
import net.exylia.commons.utils.visuals.MessageUtils;
import org.bukkit.command.CommandSender;

import java.util.List;

@Getter
public abstract class PermissionCommand extends ExyliaCommand {

    private final String permission;
    private final boolean playerOnly;

    public PermissionCommand(ExyliaPlugin plugin, String name, String permission, boolean playerOnly) {
        this(plugin, name, null, permission, playerOnly);
    }

    public PermissionCommand(ExyliaPlugin plugin, String name, List<String> aliases, String permission, boolean playerOnly) {
        super(plugin, name, aliases);
        this.permission = permission;
        this.playerOnly = playerOnly;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
         
        if (playerOnly && !isPlayer(sender)) {
            onPlayerOnly(sender);
            return true;
        }

        if (permission != null && !shouldSkipMainPermissionCheck(sender, args) && !hasPermission(sender, permission)) {
            onPermissionDenied(sender);
            return true;
        }

        return onCommand(sender, label, args);
    }

    protected boolean shouldSkipMainPermissionCheck(CommandSender sender, String[] args) {
        return false;
    }

    protected abstract boolean onCommand(CommandSender sender, String label, String[] args);

    protected void onPlayerOnly(CommandSender sender) {
        MessageUtils.sendMessage(sender, Messages.get("system.player_only"));
    }

    protected void onPermissionDenied(CommandSender sender) {
        MessageUtils.sendMessage(sender, Messages.get("system.no_permission"));
    }
}

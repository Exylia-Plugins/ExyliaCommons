package net.exylia.commons.command.types;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.configSimple.Messages;
import net.exylia.commons.utils.visuals.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public abstract class ToggleCommand extends PermissionCommand {

    @Getter
    private final String permissionOthers;
    private final List<String> subCommands = Arrays.asList("on", "off", "toggle");

    public ToggleCommand(ExyliaPlugin plugin, String name, String permission, String permissionOthers) {
        this(plugin, name, null, permission, permissionOthers);
    }

    public ToggleCommand(ExyliaPlugin plugin, String name, List<String> aliases, String permission, String permissionOthers) {
        super(plugin, name, aliases, permission, true);  
        this.permissionOthers = permissionOthers;
    }

    @Override
    protected boolean onCommand(CommandSender sender, String label, String[] args) {
        Player player = getPlayer(sender);

        if (args.length == 0) {
            return handleToggle(player, player);
        }

        String action = args[0].toLowerCase();
        Player target;

        if (args.length >= 2) {
             
            if (permissionOthers != null && !hasPermission(sender, permissionOthers)) {
                onPermissionDenied(sender);
                return true;
            }

            target = getPlayer(args[1]);
            if (target == null) {
                onPlayerNotFound(sender, args[1]);
                return true;
            }
        } else {
            target = player;
        }

        switch (action) {
            case "on":
                return handleEnable(player, target);
            case "off":
                return handleDisable(player, target);
            case "toggle":
                return handleToggle(player, target);
            default:
                showUsage(sender);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterSuggestions(subCommands, args[0]);
        } else if (args.length == 2 && hasPermission(sender, permissionOthers)) {
            return getOnlinePlayerNames(args[1]);
        }
        return super.onTabComplete(sender, command, alias, args);
    }

    protected boolean handleEnable(Player sender, Player target) {
        enableFeature(target);

        boolean self = sender.equals(target);
        if (self) {
            sendEnableMessage(sender);
        } else {
            sendEnableOtherMessage(sender, target);
        }

        return true;
    }

    protected boolean handleDisable(Player sender, Player target) {
        disableFeature(target);

        boolean self = sender.equals(target);
        if (self) {
            sendDisableMessage(sender);
        } else {
            sendDisableOtherMessage(sender, target);
        }

        return true;
    }

    protected boolean handleToggle(Player sender, Player target) {
        boolean enabled = toggleFeature(target);

        boolean self = sender.equals(target);
        if (self) {
            if (enabled) {
                sendEnableMessage(sender);
            } else {
                sendDisableMessage(sender);
            }
        } else {
            if (enabled) {
                sendEnableOtherMessage(sender, target);
            } else {
                sendDisableOtherMessage(sender, target);
            }
        }

        return true;
    }

    protected void onPlayerNotFound(CommandSender sender, String name) {
        MessageUtils.sendMessage(sender, Messages.get("system.player_not_found"));
    }

    protected void showUsage(CommandSender sender) {
        MessageUtils.sendMessage(sender, Messages.get("system.commands.usage", "%usage%", getName() + " [on|off|toggle] [player]"));
    }

    protected abstract void sendEnableMessage(Player player);

    protected abstract void sendDisableMessage(Player player);

    protected abstract void sendEnableOtherMessage(Player sender, Player target);

    protected abstract void sendDisableOtherMessage(Player sender, Player target);

    protected abstract void enableFeature(Player player);

    protected abstract void disableFeature(Player player);

    protected abstract boolean toggleFeature(Player player);
}

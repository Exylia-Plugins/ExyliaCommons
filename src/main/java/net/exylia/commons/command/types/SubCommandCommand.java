package net.exylia.commons.command.types;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.command.ExyliaCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public abstract class SubCommandCommand extends PermissionCommand {

    public SubCommandCommand(ExyliaPlugin plugin, String name, String permission, boolean playerOnly) {
        super(plugin, name, permission, playerOnly);
    }

    public SubCommandCommand(ExyliaPlugin plugin, String name, List<String> aliases, String permission, boolean playerOnly) {
        super(plugin, name, aliases, permission, playerOnly);
    }

    @Override
    protected boolean onCommand(CommandSender sender, String label, String[] args) {
         
        if (args.length == 0) {
            showHelp(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String subPermission = getSubCommandPermission(subCommand);

        if (subPermission != null && !hasPermission(sender, subPermission)) {
            onPermissionDenied(sender);
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        return executeSubCommand(sender, label, subCommand, subArgs);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterSuggestions(getAvailableSubCommands(sender), args[0]);
        } else if (args.length > 1) {
            String subCommand = args[0].toLowerCase();
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

            String subPermission = getSubCommandPermission(subCommand);
            if (subPermission != null && !hasPermission(sender, subPermission)) {
                return super.onTabComplete(sender, command, alias, args);
            }

            return tabCompleteSubCommand(sender, subCommand, subArgs);
        }

        return super.onTabComplete(sender, command, alias, args);
    }

    protected abstract List<String> getAvailableSubCommands(CommandSender sender);

    protected abstract String getSubCommandPermission(String subCommand);

    protected abstract boolean executeSubCommand(CommandSender sender, String label, String subCommand, String[] args);

    protected abstract List<String> tabCompleteSubCommand(CommandSender sender, String subCommand, String[] args);

    protected abstract void showHelp(CommandSender sender, String label);
}

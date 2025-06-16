package net.exylia.commons.command.types;

import net.exylia.commons.command.annotation.CommandInfo;
import net.exylia.commons.command.annotation.SubCommandInfo;
import net.exylia.commons.utils.ColorUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AutoSubCommandCommand extends PermissionCommand {

    private final Map<String, SubCommandInfo> subCommandInfoMap = new HashMap<>();
    private CommandInfo mainCommandInfo;

    public AutoSubCommandCommand(JavaPlugin plugin, String name, String permission, boolean playerOnly) {
        super(plugin, name, permission, playerOnly);
        loadSubCommandInfo();
    }

    public AutoSubCommandCommand(JavaPlugin plugin, String name, List<String> aliases, String permission, boolean playerOnly) {
        super(plugin, name, aliases, permission, playerOnly);
        loadSubCommandInfo();
    }

    private void loadSubCommandInfo() {
        Class<?> clazz = this.getClass();

        // Cargar información del comando principal
        if (clazz.isAnnotationPresent(CommandInfo.class)) {
            mainCommandInfo = clazz.getAnnotation(CommandInfo.class);
        }

        // Cargar información de subcomandos
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(CommandInfo.class)) {
                CommandInfo info = method.getAnnotation(CommandInfo.class);
                String methodName = method.getName();

                String subCommandName = extractSubCommandName(methodName);

                String permission = info.permission().isEmpty() ?
                        getSubCommandPermission(subCommandName) : info.permission();

                SubCommandInfo subInfo = new SubCommandInfo(
                        subCommandName,
                        info.usage(),
                        permission,
                        info.playerOnly(),
                        info.aliases(),
                        info.order()
                );

                subCommandInfoMap.put(subCommandName.toLowerCase(), subInfo);

                // registrar aliases
                for (String alias : info.aliases()) {
                    subCommandInfoMap.put(alias.toLowerCase(), subInfo);
                }
            }
        }
    }

    private String extractSubCommandName(String methodName) {
        if (methodName.startsWith("execute")) {
            String name = methodName.substring(7);// execute
            return name.toLowerCase();
        }
        return methodName.toLowerCase();
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

        SubCommandInfo info = subCommandInfoMap.get(subCommand);
        if (info != null && info.playerOnly() && !(sender instanceof Player)) {
            onPlayerOnly(sender);
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

    protected List<String> getAvailableSubCommands(CommandSender sender) {
        return subCommandInfoMap.values().stream()
                .filter(info -> {
                    String permission = info.permission();
                    return permission == null || hasPermission(sender, permission);
                })
                .filter(info -> !info.playerOnly() || sender instanceof Player)
                .map(SubCommandInfo::name)
                .distinct()
                .collect(Collectors.toList());
    }

    protected final void showHelp(CommandSender sender, String label) {
        sender.sendMessage("");
        sender.sendMessage(ColorUtils.parse("<#8a51c4>" + plugin.getName() + " &8&l•&r <#aa76de>ᴀʏᴜᴅᴀ ᴅᴇ ᴄᴏᴍᴀɴᴅᴏꜱ"));
        sender.sendMessage("");
        sender.sendMessage(ColorUtils.parse("<#ffc58f><> &8&l•&r <#e7cfff>Requerido &8| <#59a4ff>[] &8&l•&r <#e7cfff>Opcional"));
        sender.sendMessage("");

        // Mostrar uso del comando principal si existe
        if (mainCommandInfo != null && !mainCommandInfo.usage().isEmpty()) {
            sender.sendMessage(ColorUtils.parse("<#8fffc1>/" + label + " <#a1ffc3>"));
            sender.sendMessage("");
        }

        // Obtener subcomandos disponibles y ordenarlos
        List<SubCommandInfo> availableCommands = subCommandInfoMap.values().stream()
                .filter(info -> {
                    String permission = info.permission();
                    return permission == null || hasPermission(sender, permission);
                })
                .filter(info -> !info.playerOnly() || sender instanceof Player)
                .distinct()
                .sorted(Comparator.comparingInt(SubCommandInfo::order)
                        .thenComparing(SubCommandInfo::name))
                .toList();

        if (availableCommands.isEmpty()) {
            sender.sendMessage("§cNo tienes permisos para ningún subcomando.");
            return;
        }

        // subcomandos
        for (SubCommandInfo info : availableCommands) {
            sender.sendMessage(ColorUtils.parse("<#8fffc1>/" + label + " <#a1ffc3>" + info.usage()));
        }
        sender.sendMessage("");
    }

    // Métodos abstractos que deben implementar las subclases
    protected abstract String getSubCommandPermission(String subCommand);
    protected abstract boolean executeSubCommand(CommandSender sender, String label, String subCommand, String[] args);
    protected abstract List<String> tabCompleteSubCommand(CommandSender sender, String subCommand, String[] args);

    protected SubCommandInfo getSubCommandInfo(String subCommand) {
        return subCommandInfoMap.get(subCommand.toLowerCase());
    }

    protected Collection<SubCommandInfo> getAllSubCommands() {
        return subCommandInfoMap.values();
    }
}
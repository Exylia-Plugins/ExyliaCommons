package net.exylia.commons.command.types;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.command.annotation.CommandInfo;
import net.exylia.commons.command.annotation.SubCommandInfo;
import net.exylia.commons.command.annotation.DefaultAction;
import net.exylia.commons.config.base.MessagesBase;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.StringUtils.camelCaseToKebabCase;

public abstract class AutoSubCommandCommand extends PermissionCommand {

    private final Map<String, SubCommandInfo> subCommandInfoMap = new HashMap<>();
    private CommandInfo mainCommandInfo;
    private DefaultAction defaultAction;

    public AutoSubCommandCommand(ExyliaPlugin plugin, String name, String permission, boolean playerOnly) {
        super(plugin, name, permission, playerOnly);
        loadSubCommandInfo();
    }

    public AutoSubCommandCommand(ExyliaPlugin plugin, String name, List<String> aliases, String permission, boolean playerOnly) {
        super(plugin, name, aliases, permission, playerOnly);
        loadSubCommandInfo();
    }

    @Override
    protected boolean shouldSkipMainPermissionCheck(CommandSender sender, String[] args) {
        if (args.length == 0 && defaultAction != null &&
                defaultAction.value() == DefaultAction.ActionType.EXECUTE_SUBCOMMAND &&
                !defaultAction.subcommand().isEmpty()) {
            return true;
        }
        return false;
    }

    private void loadSubCommandInfo() {
        Class<?> clazz = this.getClass();

        // Cargar información del comando principal
        if (clazz.isAnnotationPresent(CommandInfo.class)) {
            mainCommandInfo = clazz.getAnnotation(CommandInfo.class);
        }

        // Cargar configuración de acción por defecto
        if (clazz.isAnnotationPresent(DefaultAction.class)) {
            defaultAction = clazz.getAnnotation(DefaultAction.class);
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
            String name = methodName.substring(7); // Remover "execute"
            return camelCaseToKebabCase(name);
        }
        return camelCaseToKebabCase(methodName);
    }


    @Override
    protected boolean onCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            if (defaultAction != null && defaultAction.value() == DefaultAction.ActionType.EXECUTE_SUBCOMMAND) {
                String defaultSubCommand = defaultAction.subcommand();
                if (!defaultSubCommand.isEmpty()) {
                    String subPermission = getSubCommandPermission(defaultSubCommand);
                    if (subPermission != null && !hasPermission(sender, subPermission)) {
                        onPermissionDenied(sender);
                        return true;
                    }

                    SubCommandInfo info = subCommandInfoMap.get(defaultSubCommand.toLowerCase());
                    if (info != null && info.playerOnly() && !(sender instanceof Player)) {
                        onPlayerOnly(sender);
                        return true;
                    }

                    SubCommandContext context = new SubCommandContext(
                            sender,
                            defaultSubCommand,
                            label,
                            new String[0],
                            new String[]{defaultSubCommand}
                    );
                    return executeSubCommand(context);
                }
            }

            return executeNoArgs(sender, label);
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

        SubCommandContext context = new SubCommandContext(
                sender,
                subCommand,
                label,
                Arrays.copyOfRange(args, 1, args.length),
                args
        );

        return executeSubCommand(context);
    }

    protected boolean executeNoArgs(CommandSender sender, String label) {
        showHelp(sender, label);
        return true;
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

            SubCommandContext context = new SubCommandContext(
                    sender,
                    subCommand,
                    alias,
                    subArgs,
                    args
            );

            return tabCompleteSubCommand(context);
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
        MessageUtils.sendMessage(sender, MessagesBase.get("commands.help.header", "%plugin_name%", plugin.getName()));

        // Mostrar uso del comando principal si existe
        if (mainCommandInfo != null && !mainCommandInfo.usage().isEmpty()) {
            MessageUtils.sendMessage(sender, MessagesBase.get("commands.help.usage", "%label%", label, "%usage%", mainCommandInfo.usage()));
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
            MessageUtils.sendMessage(sender, MessagesBase.get("commands.no_subcommands"));
            return;
        }

        // subcomandos
        for (SubCommandInfo info : availableCommands) {
            MessageUtils.sendMessage(sender, MessagesBase.get("commands.help.usage", "%label%", label, "%usage%", info.usage()));
        }
        sender.sendMessage("");
    }

    protected abstract String getSubCommandPermission(String subCommand);
    protected abstract boolean executeSubCommand(SubCommandContext context);
    protected abstract List<String> tabCompleteSubCommand(SubCommandContext context);

    protected SubCommandInfo getSubCommandInfo(String subCommand) {
        return subCommandInfoMap.get(subCommand.toLowerCase());
    }

    protected Collection<SubCommandInfo> getAllSubCommands() {
        return subCommandInfoMap.values();
    }

    @Getter
    public static class SubCommandContext {
        private final CommandSender sender;
        private final String subCommand;
        private final String label;
        private final String[] subArgs;
        private final String[] fullArgs;

        public SubCommandContext(CommandSender sender, String subCommand, String label, String[] subArgs, String[] fullArgs) {
            this.sender = sender;
            this.subCommand = subCommand;
            this.label = label;
            this.subArgs = subArgs;
            this.fullArgs = fullArgs;
        }

        // Métodos de conveniencia
        public Player getPlayer() { return sender instanceof Player ? (Player) sender : null; }
        public boolean isPlayer() { return sender instanceof Player; }
        public int getSubArgsLength() { return subArgs.length; }
        public String getSubArg(int index) { return index < subArgs.length ? subArgs[index] : null; }
    }
}
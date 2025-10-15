package net.exylia.commons.command.types;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.command.annotation.CommandInfo;
import net.exylia.commons.command.annotation.SubCommandInfo;
import net.exylia.commons.command.annotation.DefaultAction;
import net.exylia.commons.configSimple.Messages;
import net.exylia.commons.utils.visuals.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AutoSubCommandCommand extends PermissionCommand {

    private final Map<String, CommandNode> commandTree = new HashMap<>();
    private final Map<String, Method> methodMap = new HashMap<>();
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

        if (clazz.isAnnotationPresent(CommandInfo.class)) {
            mainCommandInfo = clazz.getAnnotation(CommandInfo.class);
        }

        if (clazz.isAnnotationPresent(DefaultAction.class)) {
            defaultAction = clazz.getAnnotation(DefaultAction.class);
        }

        List<MethodInfo> methods = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(CommandInfo.class)) {
                CommandInfo info = method.getAnnotation(CommandInfo.class);
                String methodName = method.getName();

                List<String> commandPath = extractCommandPath(methodName);

                methods.add(new MethodInfo(method, info, commandPath));
            }
        }

        buildCommandTree(methods);
    }

    private List<String> extractCommandPath(String methodName) {
         
        String cleanName = methodName.startsWith("execute") ?
                methodName.substring(7) : methodName;

        List<String> parts = splitByCapitalLetters(cleanName);

        return parts.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    private List<String> splitByCapitalLetters(String input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (Character.isUpperCase(c) && current.length() > 0) {
                 
                result.add(current.toString());
                current = new StringBuilder();
            }

            current.append(c);
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    private void buildCommandTree(List<MethodInfo> methods) {
        for (MethodInfo methodInfo : methods) {
            List<String> path = methodInfo.commandPath;

            if (path.isEmpty()) {
                continue;
            }

            String rootCommand = path.get(0);
            CommandNode rootNode = commandTree.computeIfAbsent(rootCommand, k ->
                    new CommandNode(k, methodInfo.commandInfo, methodInfo.method));

            if (path.size() == 1) {
                rootNode.method = methodInfo.method;
                rootNode.commandInfo = methodInfo.commandInfo;
                methodMap.put(rootCommand, methodInfo.method);
            } else {
                 
                CommandNode currentNode = rootNode;

                for (int i = 1; i < path.size(); i++) {
                    String subCommand = path.get(i);
                    boolean isLast = (i == path.size() - 1);

                    CommandNode childNode = currentNode.children.computeIfAbsent(subCommand, k ->
                            new CommandNode(k, isLast ? methodInfo.commandInfo : null, isLast ? methodInfo.method : null));

                    if (isLast) {
                        childNode.method = methodInfo.method;
                        childNode.commandInfo = methodInfo.commandInfo;
                         
                        String fullPath = String.join(".", path);
                        methodMap.put(fullPath, methodInfo.method);
                    }

                    currentNode = childNode;
                }
            }
        }
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

                    CommandNode node = commandTree.get(defaultSubCommand.toLowerCase());
                    if (node != null && node.commandInfo != null && node.commandInfo.playerOnly() && !(sender instanceof Player)) {
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

        CommandNode node = commandTree.get(subCommand);
        if (node != null && node.getCommandInfo() != null && node.getCommandInfo().playerOnly() && !(sender instanceof Player)) {
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
             
            return getAvailableSubCommands(sender, args[0]);
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

    protected List<String> getAvailableSubCommands(CommandSender sender, String partial) {
        return commandTree.keySet().stream()
                .filter(command -> {
                    CommandNode node = commandTree.get(command);
                    if (node.getCommandInfo() == null) return true;  

                    String permission = node.getCommandInfo().permission().isEmpty() ?
                            getSubCommandPermission(command) : node.getCommandInfo().permission();

                    boolean hasPermission = permission == null || hasPermission(sender, permission);
                    boolean canExecute = !node.getCommandInfo().playerOnly() || sender instanceof Player;

                    return hasPermission && canExecute;
                })
                .filter(command -> command.startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<String> getAvailableSubCommands(String parentCommand, CommandSender sender, String partial) {
        CommandNode parentNode = commandTree.get(parentCommand.toLowerCase());
        if (parentNode == null) {
            return new ArrayList<>();
        }

        return parentNode.children.keySet().stream()
                .filter(subCommand -> {
                    CommandNode node = parentNode.children.get(subCommand);
                    if (node.getCommandInfo() == null) return true;

                    String permission = node.getCommandInfo().permission().isEmpty() ?
                            getSubCommandPermission(parentCommand + "." + subCommand) : node.getCommandInfo().permission();

                    boolean hasPermission = permission == null || hasPermission(sender, permission);
                    boolean canExecute = !node.getCommandInfo().playerOnly() || sender instanceof Player;

                    return hasPermission && canExecute;
                })
                .filter(subCommand -> subCommand.startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    protected final void showHelp(CommandSender sender, String label) {
        MessageUtils.sendMessage(sender, Messages.get("system.commands.help.header", "%plugin_name%", plugin.getName()));

        if (mainCommandInfo != null && !mainCommandInfo.usage().isEmpty()) {
            MessageUtils.sendMessage(sender, Messages.get("system.commands.help.usage", "%label%", label, "%usage%", mainCommandInfo.usage()));
        }

        List<CommandNode> availableCommands = new ArrayList<>();

        for (CommandNode node : commandTree.values()) {
            if (node.getCommandInfo() == null) continue;  

            String permission = node.getCommandInfo().permission().isEmpty() ?
                    getSubCommandPermission(node.getName()) : node.getCommandInfo().permission();

            boolean hasPermission = permission == null || hasPermission(sender, permission);
            boolean canExecute = !node.getCommandInfo().playerOnly() || sender instanceof Player;

            if (hasPermission && canExecute) {
                availableCommands.add(node);
            }
        }

        if (availableCommands.isEmpty()) {
            MessageUtils.sendMessage(sender, Messages.get("system.commands.no_subcommands"));
            return;
        }

        availableCommands.sort(Comparator.comparingInt((CommandNode node) ->
                        node.getCommandInfo() != null ? node.getCommandInfo().order() : 100)
                .thenComparing(CommandNode::getName));

        for (CommandNode node : availableCommands) {
            MessageUtils.sendMessage(sender, Messages.get("system.commands.help.usage", "%label%", label, "%usage%", node.getCommandInfo().usage()));
        }
        sender.sendMessage("");
    }

    protected abstract String getSubCommandPermission(String subCommand);
    protected abstract boolean executeSubCommand(SubCommandContext context);
    protected abstract List<String> tabCompleteSubCommand(SubCommandContext context);

    protected CommandNode getSubCommandNode(String subCommand) {
        return commandTree.get(subCommand.toLowerCase());
    }

    protected Collection<CommandNode> getAllCommandNodes() {
        return commandTree.values();
    }

    protected Method getSubCommandMethod(String subCommand) {
        return methodMap.get(subCommand.toLowerCase());
    }

    private static class MethodInfo {
        final Method method;
        final CommandInfo commandInfo;
        final List<String> commandPath;

        MethodInfo(Method method, CommandInfo commandInfo, List<String> commandPath) {
            this.method = method;
            this.commandInfo = commandInfo;
            this.commandPath = commandPath;
        }
    }

    @Getter
    public static class CommandNode {
        private final String name;
        private CommandInfo commandInfo;
        private Method method;
        private final Map<String, CommandNode> children = new HashMap<>();

        CommandNode(String name, CommandInfo commandInfo, Method method) {
            this.name = name;
            this.commandInfo = commandInfo;
            this.method = method;
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }

        public boolean isExecutable() {
            return method != null;
        }

        public String getName() {
            return name;
        }

        public CommandInfo getCommandInfo() {
            return commandInfo;
        }

        public Method getMethod() {
            return method;
        }

        public Map<String, CommandNode> getChildren() {
            return children;
        }
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

        public Player getPlayer() { return sender instanceof Player ? (Player) sender : null; }
        public boolean isPlayer() { return sender instanceof Player; }
        public int getSubArgsLength() { return subArgs.length; }
        public String getSubArg(int index) { return index < subArgs.length ? subArgs[index] : null; }
        public String[] getArgs() { return fullArgs; }
    }
}

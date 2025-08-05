package net.exylia.commons.command;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.DebugUtils.logInternalError;
import static net.exylia.commons.utils.DebugUtils.logInternalInfo;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public abstract class ExyliaCommand implements CommandExecutor, TabCompleter {

    protected final ExyliaPlugin plugin;
    @Getter
    private final String name;
    @Getter
    private final List<String> aliases;

    // Cache simple para evitar múltiples registros
    private static final Map<String, ExyliaCommand> registeredCommands = new ConcurrentHashMap<>();
    private boolean isRegistered = false;

    /**
     * Constructor básico
     */
    public ExyliaCommand(ExyliaPlugin plugin, String name) {
        this(plugin, name, new ArrayList<>());
    }

    /**
     * Constructor con aliases
     */
    public ExyliaCommand(ExyliaPlugin plugin, String name, List<String> aliases) {
        this.plugin = plugin;
        this.name = name;
        this.aliases = aliases != null ? aliases : new ArrayList<>();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return execute(sender, label, args);
    }

    /**
     * A implementar con la lógica del comando
     */
    public abstract boolean execute(CommandSender sender, String label, String[] args);

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }

    public boolean register() {
        if (isRegistered) {
            logInternalWarn("Comando " + name + " ya está registrado");
            return true;
        }

        try {
            final String lowerName = name.toLowerCase();
            if (registeredCommands.containsKey(lowerName)) {
                logInternalWarn("Comando " + name + " ya existe en el cache");
                return false;
            }

            final PluginCommand command = createPluginCommand(name);
            if (command == null) {
                logInternalError("No se pudo crear PluginCommand para " + name);
                return false;
            }

            // Configurar el comando
            command.setExecutor(this);
            command.setTabCompleter(this);

            if (!aliases.isEmpty()) {
                command.setAliases(aliases);
            }

            registerCommandInMap(command);
            registeredCommands.put(lowerName, this);
            isRegistered = true;

            logInternalInfo("Comando " + name + " registrado exitosamente");
            return true;

        } catch (Exception e) {
            logInternalError("Error registrando comando " + name + ": " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    private PluginCommand createPluginCommand(String label) {
        try {
            final Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            return constructor.newInstance(label, plugin);
        } catch (Exception ex) {
            logInternalError("No se pudo crear PluginCommand: " + ex.getMessage());
            return null;
        }
    }

    private void registerCommandInMap(Command command) {
        final CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            throw new IllegalStateException("CommandMap no disponible");
        }

        commandMap.register(command.getLabel(), command);

        if (!command.isRegistered()) {
            throw new IllegalStateException("Comando /" + command.getLabel() + " no se pudo registrar correctamente");
        }
    }

    private CommandMap getCommandMap() {
        try {
            final Class<?> craftServer = Class.forName("org.bukkit.craftbukkit." + getServerVersion() + ".CraftServer");
            return (CommandMap) craftServer.getDeclaredMethod("getCommandMap").invoke(Bukkit.getServer());
        } catch (Exception ex) {
            try {
                final Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                return (CommandMap) commandMapField.get(Bukkit.getServer());
            } catch (Exception ex2) {
                logInternalError("No se pudo obtener CommandMap: " + ex2.getMessage());
                return null;
            }
        }
    }

    private String getServerVersion() {
        final String packageName = Bukkit.getServer().getClass().getPackage().getName();
        final String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        return version.equals("craftbukkit") ? "" : version + ".";
    }

    public void unregister() {
        if (!isRegistered) return;

        try {
            unregisterFromServer(name, true);

            registeredCommands.remove(name.toLowerCase());
            isRegistered = false;

            logInternalInfo("Comando " + name + " desregistrado");
        } catch (Exception e) {
            logInternalError("Error desregistrando comando " + name + ": " + e.getMessage());
        }
    }

    private void unregisterFromServer(String label, boolean removeAliases) {
        try {
            // Desregistrar el commandMap del comando mismo
            final PluginCommand command = Bukkit.getPluginCommand(label);

            if (command != null) {
                final Field commandField = Command.class.getDeclaredField("commandMap");
                commandField.setAccessible(true);

                if (command.isRegistered()) {
                    command.unregister((CommandMap) commandField.get(command));
                }
            }

            // Eliminar comando + aliases del command map del servidor
            final Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);

            final Map<String, Command> cmdMap = (Map<String, Command>) field.get(getCommandMap());

            cmdMap.remove(label);

            if (command != null && removeAliases) {
                for (final String alias : command.getAliases()) {
                    cmdMap.remove(alias);
                }
            }

        } catch (Exception ex) {
            throw new RuntimeException("Failed to unregister command /" + label, ex);
        }
    }

    /**
     * Verifica si el comando está registrado (verificación simple)
     */
    public boolean isRegistered() {
        return isRegistered && Bukkit.getPluginCommand(name) != null;
    }

    // Métodos de utilidad (sin cambios)
    protected boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    protected Player getPlayer(CommandSender sender) {
        return isPlayer(sender) ? (Player) sender : null;
    }

    protected boolean isPlayerOnline(String name) {
        return Bukkit.getPlayer(name) != null;
    }

    protected Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }

    protected List<String> filterSuggestions(List<String> suggestions, String arg) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
                .collect(Collectors.toList());
    }

    protected List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    protected List<String> getOnlinePlayerNames(String arg) {
        return filterSuggestions(getOnlinePlayerNames(), arg);
    }

    protected boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }
}
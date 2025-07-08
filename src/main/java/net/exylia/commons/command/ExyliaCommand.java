package net.exylia.commons.command;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.DebugUtils.logInternalError;

/**
 * Clase base para crear comandos de forma sencilla
 */
public abstract class ExyliaCommand implements CommandExecutor, TabCompleter {

    protected final ExyliaPlugin plugin;
    @Getter
    private final String name;
    @Getter
    private final List<String> aliases;

    /**
     * Constructor básico
     *
     * @param plugin Instancia del plugin
     * @param name Nombre del comando
     */
    public ExyliaCommand(ExyliaPlugin plugin, String name) {
        this(plugin, name, new ArrayList<>());
    }

    /**
     * Constructor con aliases
     *
     * @param plugin Instancia del plugin
     * @param name Nombre del comando
     * @param aliases Lista de aliases
     */
    public ExyliaCommand(ExyliaPlugin plugin, String name, List<String> aliases) {
        this.plugin = plugin;
        this.name = name;
        this.aliases = aliases != null ? aliases : new ArrayList<>();
    }

    /**
     * Se ejecuta cuando se llama al comando
     *
     * @param sender Quien ejecuta el comando
     * @param command Objeto Command
     * @param label Label utilizada
     * @param args Argumentos
     * @return true si se ha manejado correctamente
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return execute(sender, label, args);
    }

    /**
     * A implementar con la lógica del comando
     *
     * @param sender Quien ejecuta el comando
     * @param label Label utilizada
     * @param args Argumentos
     * @return true si se ha manejado correctamente
     */
    public abstract boolean execute(CommandSender sender, String label, String[] args);

    /**
     * Para autocompletar el comando
     *
     * @param sender Quien ejecuta el comando
     * @param command Objeto Command
     * @param alias Alias utilizado
     * @param args Argumentos actuales
     * @return Lista de sugerencias o null
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }

    /**
     * Registra el comando en el servidor
     *
     * @return true si se registró correctamente
     */
    public boolean register() {
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand cmd = constructor.newInstance(name, plugin);

            cmd.setExecutor(this);
            cmd.setTabCompleter(this);

            if (!aliases.isEmpty()) {
                cmd.setAliases(aliases);
            }

            Bukkit.getCommandMap().register(plugin.getName().toLowerCase(), cmd);
            return true;

        } catch (Exception e) {
            logInternalError("Error al registrar el comando " + name + ": " + e.getMessage());
            return false;
        }
    }

    // Métodos de ayuda para todos los comandos

    /**
     * Verifica si el remitente es un jugador
     *
     * @param sender Remitente a verificar
     * @return true si es un jugador
     */
    protected boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    /**
     * Obtiene el jugador si el remitente es un jugador
     *
     * @param sender Remitente
     * @return Jugador o null
     */
    protected Player getPlayer(CommandSender sender) {
        return isPlayer(sender) ? (Player) sender : null;
    }

    /**
     * Verifica si un jugador está online
     *
     * @param name Nombre del jugador
     * @return true si está online
     */
    protected boolean isPlayerOnline(String name) {
        return Bukkit.getPlayer(name) != null;
    }

    /**
     * Obtiene un jugador por su nombre
     *
     * @param name Nombre del jugador
     * @return Jugador o null si no está online
     */
    protected Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }

    /**
     * Filtra una lista de sugerencias basada en el argumento actual
     *
     * @param suggestions Lista de sugerencias
     * @param arg Argumento actual para filtrar
     * @return Lista filtrada
     */
    protected List<String> filterSuggestions(List<String> suggestions, String arg) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una lista de nombres de jugadores online
     *
     * @return Lista de nombres
     */
    protected List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una lista filtrada de nombres de jugadores online
     *
     * @param arg Argumento para filtrar
     * @return Lista filtrada
     */
    protected List<String> getOnlinePlayerNames(String arg) {
        return filterSuggestions(getOnlinePlayerNames(), arg);
    }

    /**
     * Verifica si un remitente tiene un permiso
     *
     * @param sender Remitente
     * @param permission Permiso a verificar
     * @return true si tiene el permiso
     */
    protected boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }
}

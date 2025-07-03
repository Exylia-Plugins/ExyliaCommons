package net.exylia.commons.command.types;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.command.ExyliaCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Clase base para comandos que necesitan subcomandos
 * Gestiona automáticamente la estructura de subcomandos
 */
public abstract class SubCommandCommand extends PermissionCommand {

    /**
     * Constructor
     *
     * @param plugin Instancia del plugin
     * @param name Nombre del comando
     * @param permission Permiso necesario o null
     * @param playerOnly Si solo jugadores pueden ejecutarlo
     */
    public SubCommandCommand(ExyliaPlugin plugin, String name, String permission, boolean playerOnly) {
        super(plugin, name, permission, playerOnly);
    }

    /**
     * Constructor con aliases
     *
     * @param plugin Instancia del plugin
     * @param name Nombre del comando
     * @param aliases Aliases del comando
     * @param permission Permiso necesario o null
     * @param playerOnly Si solo jugadores pueden ejecutarlo
     */
    public SubCommandCommand(ExyliaPlugin plugin, String name, List<String> aliases, String permission, boolean playerOnly) {
        super(plugin, name, aliases, permission, playerOnly);
    }

    @Override
    protected boolean onCommand(CommandSender sender, String label, String[] args) {
        // Si no hay argumentos, mostrar ayuda
        if (args.length == 0) {
            showHelp(sender, label);
            return true;
        }

        // Buscar subcomando
        String subCommand = args[0].toLowerCase();
        String subPermission = getSubCommandPermission(subCommand);

        // Verificar permiso del subcomando
        if (subPermission != null && !hasPermission(sender, subPermission)) {
            onPermissionDenied(sender);
            return true;
        }

        // Crear array de argumentos sin el subcomando
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        // Ejecutar el subcomando
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

            // Verificar permiso
            String subPermission = getSubCommandPermission(subCommand);
            if (subPermission != null && !hasPermission(sender, subPermission)) {
                return super.onTabComplete(sender, command, alias, args);
            }

            return tabCompleteSubCommand(sender, subCommand, subArgs);
        }

        return super.onTabComplete(sender, command, alias, args);
    }

    /**
     * Retorna los subcomandos disponibles para este sender
     *
     * @param sender Quien ejecuta el comando
     * @return Lista de subcomandos
     */
    protected abstract List<String> getAvailableSubCommands(CommandSender sender);

    /**
     * Retorna el permiso necesario para un subcomando
     *
     * @param subCommand Subcomando
     * @return Permiso o null si no requiere
     */
    protected abstract String getSubCommandPermission(String subCommand);

    /**
     * Ejecuta un subcomando
     *
     * @param sender Quien ejecuta el comando
     * @param label Label utilizada
     * @param subCommand Subcomando a ejecutar
     * @param args Argumentos del subcomando
     * @return true si se manejó correctamente
     */
    protected abstract boolean executeSubCommand(CommandSender sender, String label, String subCommand, String[] args);

    /**
     * Autocompletado para un subcomando
     *
     * @param sender Quien ejecuta el comando
     * @param subCommand Subcomando actual
     * @param args Argumentos actuales
     * @return Lista de sugerencias
     */
    protected abstract List<String> tabCompleteSubCommand(CommandSender sender, String subCommand, String[] args);

    /**
     * Muestra la ayuda del comando
     *
     * @param sender A quien mostrar
     * @param label Label utilizada
     */
    protected abstract void showHelp(CommandSender sender, String label);
}

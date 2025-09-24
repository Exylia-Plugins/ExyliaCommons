package net.exylia.commons.command.types;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.command.ExyliaCommand;
import net.exylia.commons.config.base.MessagesBase;
import net.exylia.commons.utils.visuals.MessageUtils;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Clase base para comandos que pueden requerir permisos
 * y pueden ser ejecutados solo por jugadores
 */
@Getter
public abstract class PermissionCommand extends ExyliaCommand {

    private final String permission;
    private final boolean playerOnly;

    /**
     * Constructor
     *
     * @param plugin Instancia del plugin
     * @param name Nombre del comando
     * @param permission Permiso necesario o null si no requiere
     * @param playerOnly Si solo jugadores pueden ejecutarlo
     */
    public PermissionCommand(ExyliaPlugin plugin, String name, String permission, boolean playerOnly) {
        this(plugin, name, null, permission, playerOnly);
    }

    /**
     * Constructor con aliases
     *
     * @param plugin Instancia del plugin
     * @param name Nombre del comando
     * @param aliases Aliases del comando
     * @param permission Permiso necesario o null si no requiere
     * @param playerOnly Si solo jugadores pueden ejecutarlo
     */
    public PermissionCommand(ExyliaPlugin plugin, String name, List<String> aliases, String permission, boolean playerOnly) {
        super(plugin, name, aliases);
        this.permission = permission;
        this.playerOnly = playerOnly;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        // Verificar si el comando es solo para jugadores
        if (playerOnly && !isPlayer(sender)) {
            onPlayerOnly(sender);
            return true;
        }

        if (permission != null && !shouldSkipMainPermissionCheck(sender, args) && !hasPermission(sender, permission)) {
            onPermissionDenied(sender);
            return true;
        }

        // Ejecutar el comando
        return onCommand(sender, label, args);
    }

    /**
     * Determina si se debe omitir la verificación del permiso principal
     * Puede ser sobrescrito por subclases para implementar lógica específica
     *
     * @param sender Quien ejecuta el comando
     * @param args Argumentos del comando
     * @return true si se debe omitir la verificación del permiso principal
     */
    protected boolean shouldSkipMainPermissionCheck(CommandSender sender, String[] args) {
        return false;
    }

    /**
     * A implementar con la lógica del comando
     *
     * @param sender Quien ejecuta el comando
     * @param label Label utilizada
     * @param args Argumentos
     * @return true si se manejó correctamente
     */
    protected abstract boolean onCommand(CommandSender sender, String label, String[] args);

    /**
     * Llamado cuando un no-jugador ejecuta un comando solo para jugadores
     *
     * @param sender Quien ejecutó el comando
     */
    protected void onPlayerOnly(CommandSender sender) {
        MessageUtils.sendMessage(sender, MessagesBase.get("system.player_only"));
    }

    /**
     * Llamado cuando no se tiene permiso para ejecutar el comando
     *
     * @param sender Quien ejecutó el comando
     */
    protected void onPermissionDenied(CommandSender sender) {
        MessageUtils.sendMessage(sender, MessagesBase.get("system.no_permission"));
    }
}
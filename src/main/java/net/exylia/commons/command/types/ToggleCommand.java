package net.exylia.commons.command.types;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.config.base.MessagesBase;
import net.exylia.commons.utils.visuals.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Clase base para comandos de tipo toggle (activar/desactivar)
 * Esta clase es flexible y no depende de ninguna estructura específica
 */
public abstract class ToggleCommand extends PermissionCommand {

    @Getter
    private final String permissionOthers;
    private final List<String> subCommands = Arrays.asList("on", "off", "toggle");

    /**
     * Constructor
     *
     * @param plugin Instancia del plugin
     * @param name Nombre del comando
     * @param permission Permiso necesario
     * @param permissionOthers Permiso para usar en otros jugadores
     */
    public ToggleCommand(ExyliaPlugin plugin, String name, String permission, String permissionOthers) {
        this(plugin, name, null, permission, permissionOthers);
    }

    /**
     * Constructor con aliases
     *
     * @param plugin Instancia del plugin
     * @param name Nombre del comando
     * @param aliases Aliases del comando
     * @param permission Permiso necesario
     * @param permissionOthers Permiso para usar en otros jugadores
     */
    public ToggleCommand(ExyliaPlugin plugin, String name, List<String> aliases, String permission, String permissionOthers) {
        super(plugin, name, aliases, permission, true); // Comando solo para jugadores
        this.permissionOthers = permissionOthers;
    }

    @Override
    protected boolean onCommand(CommandSender sender, String label, String[] args) {
        Player player = getPlayer(sender);

        // /comando - Toggle para el propio jugador
        if (args.length == 0) {
            return handleToggle(player, player);
        }

        // /comando <on|off|toggle> [jugador]
        String action = args[0].toLowerCase();
        Player target;

        if (args.length >= 2) {
            // Verificar permiso para usar en otros
            if (permissionOthers != null && !hasPermission(sender, permissionOthers)) {
                onPermissionDenied(sender);
                return true;
            }

            // Obtener jugador objetivo
            target = getPlayer(args[1]);
            if (target == null) {
                onPlayerNotFound(sender, args[1]);
                return true;
            }
        } else {
            target = player;
        }

        // Ejecutar la acción
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

    /**
     * Maneja la activación de la característica
     *
     * @param sender Quien ejecutó el comando
     * @param target Jugador objetivo
     * @return true si se manejó correctamente
     */
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

    /**
     * Maneja la desactivación de la característica
     *
     * @param sender Quien ejecutó el comando
     * @param target Jugador objetivo
     * @return true si se manejó correctamente
     */
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

    /**
     * Maneja el toggle de la característica
     *
     * @param sender Quien ejecutó el comando
     * @param target Jugador objetivo
     * @return true si se manejó correctamente
     */
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

    /**
     * Llamado cuando no se encuentra al jugador
     *
     * @param sender Quien ejecutó el comando
     * @param name Nombre del jugador no encontrado
     */
    protected void onPlayerNotFound(CommandSender sender, String name) {
        MessageUtils.sendMessage(sender, MessagesBase.get("system.player_not_found"));
    }

    /**
     * Muestra el uso del comando
     *
     * @param sender A quien mostrar
     */
    protected void showUsage(CommandSender sender) {
        MessageUtils.sendMessage(sender, MessagesBase.get("system.commands.usage", "%usage%", getName() + " [on|off|toggle] [player]"));
    }

    /**
     * Envía mensaje de activación al jugador
     *
     * @param player Jugador
     */
    protected abstract void sendEnableMessage(Player player);

    /**
     * Envía mensaje de desactivación al jugador
     *
     * @param player Jugador
     */
    protected abstract void sendDisableMessage(Player player);

    /**
     * Envía mensaje de activación para otro jugador
     *
     * @param sender Quien ejecutó el comando
     * @param target Jugador objetivo
     */
    protected abstract void sendEnableOtherMessage(Player sender, Player target);

    /**
     * Envía mensaje de desactivación para otro jugador
     *
     * @param sender Quien ejecutó el comando
     * @param target Jugador objetivo
     */
    protected abstract void sendDisableOtherMessage(Player sender, Player target);

    /**
     * Activa la característica para un jugador
     *
     * @param player Jugador
     */
    protected abstract void enableFeature(Player player);

    /**
     * Desactiva la característica para un jugador
     *
     * @param player Jugador
     */
    protected abstract void disableFeature(Player player);

    /**
     * Toggle la característica para un jugador
     *
     * @param player Jugador
     * @return true si quedó activada, false si quedó desactivada
     */
    protected abstract boolean toggleFeature(Player player);
}

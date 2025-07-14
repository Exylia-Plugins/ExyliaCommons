package net.exylia.commons.command;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static net.exylia.commons.utils.DebugUtils.*;

/**
 * Gestor de comandos para plugins
 * Permite registrar y gestionar comandos fácilmente con manejo de errores mejorado
 */
public class CommandManager {

    private final JavaPlugin plugin;
    private final Map<String, ExyliaCommand> commands;
    private final Map<String, String> aliasMap;

    /**
     * Constructor
     *
     * @param plugin Plugin al que pertenece este gestor
     */
    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.commands = new HashMap<>();
        this.aliasMap = new HashMap<>();
    }

    /**
     * Registra un comando con reintentos automáticos
     *
     * @param command Comando a registrar
     * @return true si se registró correctamente
     */
    public boolean registerCommand(ExyliaCommand command) {
        return registerCommand(command, true);
    }

    /**
     * Registra un comando
     *
     * @param command Comando a registrar
     * @param useRetries Si usar reintentos automáticos
     * @return true si se registró correctamente
     */
    public boolean registerCommand(ExyliaCommand command, boolean useRetries) {
        String cmdName = command.getName().toLowerCase();

        // Guardar comando en el mapa
        commands.put(cmdName, command);

        // Registrar aliases
        for (String alias : command.getAliases()) {
            aliasMap.put(alias.toLowerCase(), cmdName);
        }

        boolean success;
        if (useRetries) {
            success = command.register(3, 100L); // 3 intentos con 100ms de delay
        } else {
            success = command.register();
        }

        if (success) {
            logInternalInfo("Command /" + command.getName() + " registered successfully");
        } else {
            logInternalError("Falló el registro del comando /" + command.getName());
        }

        return success;
    }

    /**
     * Registra múltiples comandos a la vez con manejo de errores
     *
     * @param commands Comandos a registrar
     * @return Número de comandos registrados exitosamente
     */
    public int registerCommands(ExyliaCommand... commands) {
        int successCount = 0;
        for (ExyliaCommand command : commands) {
            if (registerCommand(command)) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * Registra todos los comandos de una lista
     *
     * @param commands Lista de comandos
     * @return Número de comandos registrados exitosamente
     */
    public int registerCommands(List<ExyliaCommand> commands) {
        int successCount = 0;
        for (ExyliaCommand command : commands) {
            if (registerCommand(command)) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * Registra comandos de forma asíncrona con callback
     *
     * @param commands Lista de comandos a registrar
     * @return CompletableFuture con el resultado del registro
     */
    public CompletableFuture<CommandRegistrationResult> registerCommandsAsync(List<ExyliaCommand> commands) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            int successCount = 0;
            int totalCommands = commands.size();
            List<String> failedCommands = new ArrayList<>();

            for (ExyliaCommand command : commands) {
                try {
                    if (registerCommand(command)) {
                        successCount++;
                    } else {
                        failedCommands.add(command.getName());
                    }
                } catch (Exception e) {
                    logInternalError("Error registrando comando " + command.getName() + ": " + e.getMessage());
                    failedCommands.add(command.getName());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            return new CommandRegistrationResult(successCount, totalCommands, failedCommands, duration);
        });
    }

    /**
     * Registra comandos con delay entre cada uno (útil para versiones problemáticas)
     *
     * @param commands Lista de comandos
     * @param delayTicks Delay en ticks entre cada registro
     */
    public void registerCommandsWithDelay(List<ExyliaCommand> commands, long delayTicks) {
        if (commands.isEmpty()) return;

        AtomicInteger index = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        new BukkitRunnable() {
            @Override
            public void run() {
                int currentIndex = index.getAndIncrement();

                if (currentIndex >= commands.size()) {
                    cancel();
                    logInternalInfo("Registro de comandos completado: " +
                            successCount.get() + "/" + commands.size() + " exitosos");
                    return;
                }

                ExyliaCommand command = commands.get(currentIndex);
                try {
                    if (registerCommand(command, false)) { // Sin reintentos automáticos
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    logInternalError("Error registrando comando " + command.getName() + ": " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 0L, delayTicks);
    }

    /**
     * Verifica si todos los comandos están registrados correctamente
     *
     * @return true si todos están registrados
     */
    public boolean verifyAllCommands() {
        for (ExyliaCommand command : commands.values()) {
            if (Bukkit.getPluginCommand(command.getName()) == null) {
                logInternalWarn("Comando " + command.getName() + " no está registrado correctamente");
                return false;
            }
        }
        return true;
    }

    /**
     * Re-registra todos los comandos (útil para reloads)
     *
     * @return Número de comandos re-registrados exitosamente
     */
    public int reregisterAllCommands() {
        logInternalInfo("Re-registrando todos los comandos...");
        int successCount = 0;

        for (ExyliaCommand command : new ArrayList<>(commands.values())) {
            try {
                if (command.register(3, 150L)) {
                    successCount++;
                }
            } catch (Exception e) {
                logInternalError("Error re-registrando comando " + command.getName() + ": " + e.getMessage());
            }
        }

        logInternalInfo("Re-registro completado: " + successCount + "/" + commands.size() + " comandos");
        return successCount;
    }

    /**
     * Obtiene un comando por su nombre o alias
     *
     * @param name Nombre o alias del comando
     * @return Comando o null si no existe
     */
    public ExyliaCommand getCommand(String name) {
        String lowercaseName = name.toLowerCase();

        // Verificar si es un alias
        if (aliasMap.containsKey(lowercaseName)) {
            return commands.get(aliasMap.get(lowercaseName));
        }

        return commands.get(lowercaseName);
    }

    /**
     * Obtiene todos los comandos registrados
     *
     * @return Lista con todos los comandos
     */
    public List<ExyliaCommand> getCommands() {
        return new ArrayList<>(commands.values());
    }

    /**
     * Obtiene estadísticas de los comandos registrados
     *
     * @return Información sobre el estado de los comandos
     */
    public CommandStats getStats() {
        int totalCommands = commands.size();
        int totalAliases = aliasMap.size();
        int registeredCommands = 0;

        for (ExyliaCommand command : commands.values()) {
            if (Bukkit.getPluginCommand(command.getName()) != null) {
                registeredCommands++;
            }
        }

        return new CommandStats(totalCommands, totalAliases, registeredCommands);
    }

    /**
     * Desregistra todos los comandos
     * Útil para recargar el plugin
     */
    public void unregisterAll() {
        logInternalInfo("Desregistrando " + commands.size() + " comandos...");
        commands.clear();
        aliasMap.clear();
        // Nota: Los comandos seguirán registrados en Bukkit hasta que el plugin se desactive
    }

    /**
     * Resultado del registro de comandos
     */
    public static class CommandRegistrationResult {
        public final int successCount;
        public final int totalCount;
        public final List<String> failedCommands;
        public final long durationMs;
        public final boolean allSuccessful;

        public CommandRegistrationResult(int successCount, int totalCount, List<String> failedCommands, long durationMs) {
            this.successCount = successCount;
            this.totalCount = totalCount;
            this.failedCommands = failedCommands;
            this.durationMs = durationMs;
            this.allSuccessful = successCount == totalCount;
        }

        @Override
        public String toString() {
            return String.format("CommandRegistrationResult{success=%d/%d, duration=%dms, failed=%s}",
                    successCount, totalCount, durationMs, failedCommands);
        }
    }

    /**
     * Estadísticas de comandos
     */
    public static class CommandStats {
        public final int totalCommands;
        public final int totalAliases;
        public final int registeredCommands;

        public CommandStats(int totalCommands, int totalAliases, int registeredCommands) {
            this.totalCommands = totalCommands;
            this.totalAliases = totalAliases;
            this.registeredCommands = registeredCommands;
        }

        @Override
        public String toString() {
            return String.format("CommandStats{commands=%d/%d registered, aliases=%d}",
                    registeredCommands, totalCommands, totalAliases);
        }
    }
}
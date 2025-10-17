package net.exylia.commons.command;

import net.exylia.commons.async.Schedulers;
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

public class CommandManager {

    private final JavaPlugin plugin;
    private final Map<String, ExyliaCommand> commands;
    private final Map<String, String> aliasMap;
    private final Map<String, Integer> retryCount;
    private final int maxRetries = 3;

    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.commands = new HashMap<>();
        this.aliasMap = new HashMap<>();
        this.retryCount = new HashMap<>();
    }

    public boolean registerCommand(ExyliaCommand command) {
        String cmdName = command.getName().toLowerCase();

        if (commands.containsKey(cmdName)) {
            logInternalWarn("Comando " + cmdName + " ya está registrado");
            return true;
        }

        boolean success = attemptRegistration(command);

        if (success) {
             
            commands.put(cmdName, command);
            for (String alias : command.getAliases()) {
                aliasMap.put(alias.toLowerCase(), cmdName);
            }

            scheduleVerification(command);

        } else {
             
            int currentRetries = retryCount.getOrDefault(cmdName, 0);
            if (currentRetries < maxRetries) {
                retryCount.put(cmdName, currentRetries + 1);
                logInternalWarn("Reintentando registro de " + cmdName + " (intento " + (currentRetries + 1) + "/" + maxRetries + ")");

                Schedulers.syncLater(() -> {
                    registerCommand(command);
                }, 20L * (currentRetries + 1));

                return false;
            } else {
                logInternalError("Falló el registro del comando /" + command.getName() + " después de " + maxRetries + " intentos");
                retryCount.remove(cmdName);
            }
        }

        return success;
    }

    private boolean attemptRegistration(ExyliaCommand command) {
        try {
            boolean registered = command.register();

            if (registered) {
                 
                if (command.isRegistered()) {
                    retryCount.remove(command.getName().toLowerCase());
                    return true;
                } else {
                    logInternalWarn("Registro reportado como exitoso pero verificación falló para " + command.getName());
                    return false;
                }
            }

            return false;
        } catch (Exception e) {
            logInternalError("Error durante registro de " + command.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void scheduleVerification(ExyliaCommand command) {

        Schedulers.syncLater(() -> {
            if (!command.isRegistered()) {
                logInternalError("Verificación post-registro falló para " + command.getName());

                if (retryCount.getOrDefault(command.getName().toLowerCase(), 0) < maxRetries) {
                    logInternalInfo("Iniciando re-registro automático para " + command.getName());
                    registerCommand(command);
                }
            }
        }, 40L);

        Schedulers.syncLater(() -> {
            verifyPlayerAccess(command);
        }, 100L);
    }

    private void verifyPlayerAccess(ExyliaCommand command) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;  
        }

        try {
             
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                try {
                    player.updateCommands();
                } catch (Exception e) {
                    player.sendMessage("");
                }
            }

            Schedulers.syncLater(() -> {
                boolean accessible = testCommandAccessibility(command);
                if (!accessible) {
                    logInternalWarn("Comando " + command.getName() + " no accesible para jugadores, forzando sincronización completa...");
                    forceGlobalCommandSync();
                }
            }, 20L);

        } catch (Exception e) {
            logInternalWarn("Error verificando acceso de jugadores: " + e.getMessage());
        }
    }

    private boolean testCommandAccessibility(ExyliaCommand command) {
        try {
             
            org.bukkit.command.CommandMap commandMap = Bukkit.getServer().getCommandMap();
            org.bukkit.command.Command cmd = commandMap.getCommand(command.getName());

            if (cmd == null) {
                cmd = commandMap.getCommand(plugin.getName() + ":" + command.getName());
            }

            return cmd != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void forceGlobalCommandSync() {
        try {
            logInternalInfo("Forzando sincronización global de comandos...");

            int delay = 0;
            for (ExyliaCommand command : commands.values()) {
                final int currentDelay = delay;
                Schedulers.syncLater(() -> {
                    try {
                        command.unregister();
                        Schedulers.syncLater(() -> {
                            attemptRegistration(command);
                        }, 2L);
                    } catch (Exception e) {
                        logInternalError("Error en sincronización global para " + command.getName());
                    }
                }, currentDelay);
                delay += 3;
            }

            Schedulers.syncLater(() -> {
                for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                    try {
                        player.updateCommands();
                    } catch (Exception e) {

                    }
                }
                logInternalInfo("Sincronización global completada");
            }, delay + 20L);

        } catch (Exception e) {
            logInternalError("Error en sincronización global: " + e.getMessage());
        }
    }

    public CommandRegistrationSummary registerCommands(ExyliaCommand... commands) {
        List<String> successful = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        for (ExyliaCommand command : commands) {
            if (registerCommand(command)) {
                successful.add(command.getName());
            } else {
                failed.add(command.getName());
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        return new CommandRegistrationSummary(
                successful, failed, duration
        );
    }

    public CommandRegistrationSummary registerCommands(List<ExyliaCommand> commands) {
        return registerCommands(commands.toArray(new ExyliaCommand[0]));
    }

    public CompletableFuture<CommandRegistrationResult> registerCommandsAsync(List<ExyliaCommand> commands) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            int successCount = 0;
            int totalCommands = commands.size();
            List<String> failedCommands = new ArrayList<>();
            List<String> retriedCommands = new ArrayList<>();

            for (ExyliaCommand command : commands) {
                try {
                    String cmdName = command.getName();
                    boolean hadRetries = retryCount.containsKey(cmdName.toLowerCase());

                    if (registerCommand(command)) {
                        successCount++;
                        if (hadRetries) {
                            retriedCommands.add(cmdName);
                        }
                    } else {
                        failedCommands.add(cmdName);
                    }
                } catch (Exception e) {
                    logInternalError("Error registrando comando " + command.getName() + ": " + e.getMessage());
                    failedCommands.add(command.getName());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            return new CommandRegistrationResult(successCount, totalCommands, failedCommands, retriedCommands, duration);
        });
    }

    public CommandVerificationResult verifyAllCommands() {
        List<String> verified = new ArrayList<>();
        List<String> unverified = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (ExyliaCommand command : commands.values()) {
            String cmdName = command.getName();

            if (command.isRegistered()) {
                verified.add(cmdName);
            } else {
                unverified.add(cmdName);

                if (Bukkit.getPluginCommand(cmdName) == null) {
                    missing.add(cmdName);
                }
            }
        }

        CommandVerificationResult result = new CommandVerificationResult(verified, unverified, missing);

        if (!unverified.isEmpty()) {
            logInternalWarn("Comandos no verificados: " + unverified);
        }

        if (!missing.isEmpty()) {
            logInternalError("Comandos faltantes en Bukkit: " + missing);
        }

        return result;
    }

    public CommandRegistrationSummary reregisterAllCommands() {
        logInternalInfo("Re-registrando todos los comandos...");

        retryCount.clear();

        List<String> successful = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        for (ExyliaCommand command : new ArrayList<>(commands.values())) {
            try {
                command.unregister();
            } catch (Exception e) {
                logInternalWarn("Error desregistrando " + command.getName() + ": " + e.getMessage());
            }
        }

        Schedulers.syncLater(() -> {

            for (ExyliaCommand command : new ArrayList<>(commands.values())) {
                try {
                    if (attemptRegistration(command)) {
                        successful.add(command.getName());
                    } else {
                        failed.add(command.getName());
                    }
                } catch (Exception e) {
                    logInternalError("Error re-registrando comando " + command.getName() + ": " + e.getMessage());
                    failed.add(command.getName());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            CommandRegistrationSummary summary = new CommandRegistrationSummary(successful, failed, duration);
            logInternalInfo("Re-registro completado: " + summary);
        }, 1L);

        return new CommandRegistrationSummary(successful, failed, System.currentTimeMillis() - startTime);
    }

    public ExyliaCommand getCommand(String name) {
        String lowercaseName = name.toLowerCase();

        if (aliasMap.containsKey(lowercaseName)) {
            return commands.get(aliasMap.get(lowercaseName));
        }

        return commands.get(lowercaseName);
    }

    public List<ExyliaCommand> getCommands() {
        return new ArrayList<>(commands.values());
    }

    public CommandStats getStats() {
        int totalCommands = commands.size();
        int totalAliases = aliasMap.size();
        int registeredCommands = 0;
        int verifiedCommands = 0;
        int failedCommands = 0;

        for (ExyliaCommand command : commands.values()) {
            if (Bukkit.getPluginCommand(command.getName()) != null) {
                registeredCommands++;
            }

            if (command.isRegistered()) {
                verifiedCommands++;
            } else {
                failedCommands++;
            }
        }

        return new CommandStats(totalCommands, totalAliases, registeredCommands, verifiedCommands, failedCommands);
    }

    public void emergencyCommandSync() {
        logInternalInfo("Iniciando sincronización de emergencia de comandos...");

        Schedulers.sync(() -> {
            try {

                for (ExyliaCommand command : commands.values()) {
                    command.unregister();
                }

                System.gc();

                AtomicInteger delay = new AtomicInteger(5);
                for (ExyliaCommand command : commands.values()) {
                    Schedulers.syncLater(() -> {
                        attemptRegistration(command);

                        Schedulers.syncLater(() -> {
                            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                                try {
                                    player.updateCommands();
                                } catch (Exception e) {
                                    try {

                                        player.performCommand("help");
                                    } catch (Exception ex) {

                                        player.sendMessage("§aComandos actualizados. Si tienes problemas, usa §e/" +
                                                plugin.getName().toLowerCase() + ":" + command.getName());
                                    }
                                }
                            }
                        }, 3L);

                    }, delay.getAndAdd(10));
                }

                Schedulers.syncLater(() -> {
                    CommandVerificationResult result = verifyAllCommands();
                    logInternalInfo("Sincronización de emergencia completada: " + result);

                    if (!result.allVerified) {
                        logInternalWarn("Algunos comandos siguen sin verificarse. " +
                                "Los jugadores pueden usar los comandos con el prefijo: /" +
                                plugin.getName().toLowerCase() + ":nombrecomando");
                    }
                }, delay.get() + 40L);

            } catch (Exception e) {
                logInternalError("Error en sincronización de emergencia: " + e.getMessage());
            }
        });
    }

    public void unregisterAll() {
        logInternalInfo("Desregistrando " + commands.size() + " comandos...");

        for (ExyliaCommand command : commands.values()) {
            try {
                command.unregister();
            } catch (Exception e) {
                logInternalWarn("Error desregistrando " + command.getName() + ": " + e.getMessage());
            }
        }

        commands.clear();
        aliasMap.clear();
        retryCount.clear();
    }

    public static class CommandRegistrationResult {
        public final int successCount;
        public final int totalCount;
        public final List<String> failedCommands;
        public final List<String> retriedCommands;
        public final long durationMs;
        public final boolean allSuccessful;

        public CommandRegistrationResult(int successCount, int totalCount, List<String> failedCommands,
                                         List<String> retriedCommands, long durationMs) {
            this.successCount = successCount;
            this.totalCount = totalCount;
            this.failedCommands = failedCommands;
            this.retriedCommands = retriedCommands;
            this.durationMs = durationMs;
            this.allSuccessful = successCount == totalCount;
        }

        @Override
        public String toString() {
            return String.format("CommandRegistrationResult{success=%d/%d, duration=%dms, failed=%s, retried=%s}",
                    successCount, totalCount, durationMs, failedCommands, retriedCommands);
        }
    }

    public static class CommandRegistrationSummary {
        public final List<String> successful;
        public final List<String> failed;
        public final long durationMs;
        public final int totalCount;
        public final boolean allSuccessful;

        public CommandRegistrationSummary(List<String> successful, List<String> failed, long durationMs) {
            this.successful = successful;
            this.failed = failed;
            this.durationMs = durationMs;
            this.totalCount = successful.size() + failed.size();
            this.allSuccessful = failed.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("Summary{success=%d/%d, duration=%dms, failed=%s}",
                    successful.size(), totalCount, durationMs, failed);
        }
    }

    public static class CommandVerificationResult {
        public final List<String> verified;
        public final List<String> unverified;
        public final List<String> missing;
        public final boolean allVerified;

        public CommandVerificationResult(List<String> verified, List<String> unverified, List<String> missing) {
            this.verified = verified;
            this.unverified = unverified;
            this.missing = missing;
            this.allVerified = unverified.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("Verification{verified=%d, unverified=%d, missing=%d}",
                    verified.size(), unverified.size(), missing.size());
        }
    }

    public static class CommandStats {
        public final int totalCommands;
        public final int totalAliases;
        public final int registeredCommands;
        public final int verifiedCommands;
        public final int failedCommands;

        public CommandStats(int totalCommands, int totalAliases, int registeredCommands,
                            int verifiedCommands, int failedCommands) {
            this.totalCommands = totalCommands;
            this.totalAliases = totalAliases;
            this.registeredCommands = registeredCommands;
            this.verifiedCommands = verifiedCommands;
            this.failedCommands = failedCommands;
        }

        @Override
        public String toString() {
            return String.format("CommandStats{total=%d, registered=%d, verified=%d, failed=%d, aliases=%d}",
                    totalCommands, registeredCommands, verifiedCommands, failedCommands, totalAliases);
        }
    }
}

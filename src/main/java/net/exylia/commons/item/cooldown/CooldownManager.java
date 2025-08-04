package net.exylia.commons.item.cooldown;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.item.ItemManager;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

/**
 * Sistema de cooldown persistente con soporte para double (precisión decimal)
 * Almacena datos en memoria para rendimiento y persiste en archivos JSON
 */
public class CooldownManager {

    private static CooldownManager instance;
    private final JavaPlugin plugin;
    private final Gson gson;

    // Estructura: jugador -> item -> tiempo de expiración en milisegundos
    private final Map<UUID, Map<String, Long>> playerCooldowns;

    // Callbacks para eventos de cooldown
    private final Map<String, Consumer<CooldownEvent>> cooldownCallbacks;

    // Configuración
    private final File cooldownDataFile;
    private final long saveIntervalTicks;
    private final long cleanupIntervalTicks;

    // Tareas programadas
    private BukkitTask saveTask;
    private BukkitTask cleanupTask;

    // Control de inicialización
    private boolean initialized = false;

    private CooldownManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.playerCooldowns = new ConcurrentHashMap<>();
        this.cooldownCallbacks = new ConcurrentHashMap<>();

        // Configuración por defecto
        this.cooldownDataFile = new File(plugin.getDataFolder(), "cooldowns.json");
        this.saveIntervalTicks = 1200L; // 1 minuto
        this.cleanupIntervalTicks = 6000L; // 5 minutos
    }

    /**
     * Inicializa el sistema de cooldowns
     * @param plugin Plugin que usa el sistema
     */
    public static void initialize(JavaPlugin plugin) {
        if (instance != null && instance.initialized) {
            return;
        }

        instance = new CooldownManager(plugin);
        instance.start();
    }

    /**
     * Obtiene la instancia del manager
     * @return Instancia del CooldownManager
     */
    public static CooldownManager getInstance() {
        if (instance == null || !instance.initialized) {
            throw new IllegalStateException("CooldownManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Verifica si el manager está inicializado
     * @return true si está inicializado
     */
    public static boolean isInitialized() {
        return instance != null && instance.initialized;
    }

    // ===== MÉTODOS PRINCIPALES CON DOUBLE =====

    /**
     * Establece un cooldown para un jugador y item específico
     * @param player Jugador
     * @param itemId ID del item
     * @param cooldownSeconds Cooldown en segundos (acepta decimales)
     */
    public void setCooldown(Player player, String itemId, double cooldownSeconds) {
        setCooldown(player.getUniqueId(), itemId, cooldownSeconds);
    }

    /**
     * Establece un cooldown para un jugador y item específico
     * @param playerId UUID del jugador
     * @param itemId ID del item
     * @param cooldownSeconds Cooldown en segundos (acepta decimales)
     */
    public void setCooldown(UUID playerId, String itemId, double cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            removeCooldown(playerId, itemId);
            return;
        }

        long expirationTime = System.currentTimeMillis() + (long)(cooldownSeconds * 1000.0);

        playerCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(itemId.toLowerCase(), expirationTime);

        // Disparar evento de cooldown establecido
        triggerCooldownEvent(CooldownEventType.SET, playerId, itemId, cooldownSeconds);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.setCooldown(getMaterialFromItemId(itemId, player), (int) cooldownSeconds * 20);
            }
        }, 1);
    }

    /**
     * Verifica si un jugador tiene cooldown activo para un item
     * @param player Jugador
     * @param itemId ID del item
     * @return true si tiene cooldown activo
     */
    public boolean hasCooldown(Player player, String itemId) {
        return hasCooldown(player.getUniqueId(), itemId);
    }

    /**
     * Verifica si un jugador tiene cooldown activo para un item
     * @param playerId UUID del jugador
     * @param itemId ID del item
     * @return true si tiene cooldown activo
     */
    public boolean hasCooldown(UUID playerId, String itemId) {
        Map<String, Long> playerData = playerCooldowns.get(playerId);
        if (playerData == null) {
            return false;
        }

        Long expirationTime = playerData.get(itemId.toLowerCase());
        if (expirationTime == null) {
            return false;
        }

        if (System.currentTimeMillis() >= expirationTime) {
            // Cooldown expirado, remover automáticamente
            playerData.remove(itemId.toLowerCase());
            triggerCooldownEvent(CooldownEventType.EXPIRE, playerId, itemId, 0.0);
            return false;
        }

        return true;
    }

    /**
     * Obtiene el tiempo restante de cooldown en segundos (con decimales)
     * @param player Jugador
     * @param itemId ID del item
     * @return Segundos restantes (con precisión decimal)
     */
    public double getRemainingCooldown(Player player, String itemId) {
        return getRemainingCooldown(player.getUniqueId(), itemId);
    }

    /**
     * Obtiene el tiempo restante de cooldown en segundos (con decimales)
     * @param playerId UUID del jugador
     * @param itemId ID del item
     * @return Segundos restantes (con precisión decimal)
     */
    public double getRemainingCooldown(UUID playerId, String itemId) {
        Map<String, Long> playerData = playerCooldowns.get(playerId);
        if (playerData == null) {
            return 0.0;
        }

        Long expirationTime = playerData.get(itemId.toLowerCase());
        if (expirationTime == null) {
            return 0.0;
        }

        long remainingMs = expirationTime - System.currentTimeMillis();
        if (remainingMs <= 0) {
            // Cooldown expirado
            playerData.remove(itemId.toLowerCase());
            triggerCooldownEvent(CooldownEventType.EXPIRE, playerId, itemId, 0.0);
            return 0.0;
        }

        return remainingMs / 1000.0;
    }

    /**
     * Obtiene todos los cooldowns activos de un jugador
     * @param player Jugador
     * @return Map con item ID -> segundos restantes (double)
     */
    public Map<String, Double> getPlayerCooldowns(Player player) {
        return getPlayerCooldowns(player.getUniqueId());
    }

    /**
     * Obtiene todos los cooldowns activos de un jugador
     * @param playerId UUID del jugador
     * @return Map con item ID -> segundos restantes (double)
     */
    public Map<String, Double> getPlayerCooldowns(UUID playerId) {
        Map<String, Double> result = new ConcurrentHashMap<>();
        Map<String, Long> playerData = playerCooldowns.get(playerId);

        if (playerData != null) {
            long currentTime = System.currentTimeMillis();
            playerData.entrySet().removeIf(entry -> {
                long remainingMs = entry.getValue() - currentTime;
                if (remainingMs <= 0) {
                    triggerCooldownEvent(CooldownEventType.EXPIRE, playerId, entry.getKey(), 0.0);
                    return true; // Remover expirado
                }
                result.put(entry.getKey(), remainingMs / 1000.0);
                return false; // Mantener activo
            });
        }

        return result;
    }

    // ===== MÉTODOS DE REMOCIÓN =====

    /**
     * Remueve el cooldown de un jugador para un item específico
     * @param player Jugador
     * @param itemId ID del item
     */
    public void removeCooldown(Player player, String itemId) {
        removeCooldown(player.getUniqueId(), itemId);
    }

    /**
     * Remueve el cooldown de un jugador para un item específico
     * @param playerId UUID del jugador
     * @param itemId ID del item
     */
    public void removeCooldown(UUID playerId, String itemId) {
        Map<String, Long> playerData = playerCooldowns.get(playerId);
        if (playerData != null) {
            Long removed = playerData.remove(itemId.toLowerCase());
            if (removed != null) {
                triggerCooldownEvent(CooldownEventType.REMOVE, playerId, itemId, 0.0);
            }

            // Limpiar el map del jugador si está vacío
            if (playerData.isEmpty()) {
                playerCooldowns.remove(playerId);
            }
        }
    }

    /**
     * Remueve todos los cooldowns de un jugador
     * @param player Jugador
     */
    public void removeAllCooldowns(Player player) {
        removeAllCooldowns(player.getUniqueId());
    }

    /**
     * Remueve todos los cooldowns de un jugador
     * @param playerId UUID del jugador
     */
    public void removeAllCooldowns(UUID playerId) {
        Map<String, Long> removed = playerCooldowns.remove(playerId);
        if (removed != null && !removed.isEmpty()) {
            triggerCooldownEvent(CooldownEventType.CLEAR_ALL, playerId, null, 0.0);
        }
    }

    // ===== SISTEMA DE CALLBACKS =====

    /**
     * Registra un callback para eventos de cooldown de un item específico
     * @param itemId ID del item
     * @param callback Callback a ejecutar
     */
    public void registerCooldownCallback(String itemId, Consumer<CooldownEvent> callback) {
        cooldownCallbacks.put(itemId.toLowerCase(), callback);
    }

    /**
     * Remueve un callback de cooldown
     * @param itemId ID del item
     */
    public void unregisterCooldownCallback(String itemId) {
        cooldownCallbacks.remove(itemId.toLowerCase());
    }

    private void triggerCooldownEvent(CooldownEventType type, UUID playerId, String itemId, double seconds) {
        if (itemId != null) {
            Consumer<CooldownEvent> callback = cooldownCallbacks.get(itemId.toLowerCase());
            if (callback != null) {
                CooldownEvent event = new CooldownEvent(type, playerId, itemId, seconds);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(event));
            }
        }
    }

    // ===== PERSISTENCIA =====

    /**
     * Guarda los cooldowns en el archivo JSON
     */
    public void saveCooldowns() {
        try {
            // Crear directorio si no existe
            if (!cooldownDataFile.getParentFile().exists()) {
                cooldownDataFile.getParentFile().mkdirs();
            }

            // Limpiar cooldowns expirados antes de guardar
            cleanupExpiredCooldowns();

            // Convertir a formato serializable
            Map<String, Map<String, Long>> saveData = new ConcurrentHashMap<>();
            playerCooldowns.forEach((uuid, cooldowns) -> {
                if (!cooldowns.isEmpty()) {
                    saveData.put(uuid.toString(), cooldowns);
                }
            });

            // Escribir archivo
            try (FileWriter writer = new FileWriter(cooldownDataFile)) {
                gson.toJson(saveData, writer);
            }

        } catch (IOException e) {
            plugin.getLogger().warning("Error saving cooldowns: " + e.getMessage());
        }
    }

    /**
     * Carga los cooldowns desde el archivo JSON
     */
    public void loadCooldowns() {
        if (!cooldownDataFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(cooldownDataFile)) {
            Type type = new TypeToken<Map<String, Map<String, Long>>>(){}.getType();
            Map<String, Map<String, Long>> loadedData = gson.fromJson(reader, type);

            if (loadedData != null) {
                playerCooldowns.clear();
                loadedData.forEach((uuidStr, cooldowns) -> {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        playerCooldowns.put(uuid, new ConcurrentHashMap<>(cooldowns));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in cooldown data: " + uuidStr);
                    }
                });

                // Limpiar cooldowns expirados después de cargar
                cleanupExpiredCooldowns();
            }

        } catch (IOException e) {
            plugin.getLogger().warning("Error loading cooldowns: " + e.getMessage());
        }
    }

    // ===== UTILIDADES =====

    /**
     * Limpia todos los cooldowns expirados
     */
    public void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();

        playerCooldowns.entrySet().removeIf(playerEntry -> {
            UUID playerId = playerEntry.getKey();
            Map<String, Long> playerData = playerEntry.getValue();

            playerData.entrySet().removeIf(itemEntry -> {
                if (currentTime >= itemEntry.getValue()) {
                    triggerCooldownEvent(CooldownEventType.EXPIRE, playerId, itemEntry.getKey(), 0.0);
                    return true;
                }
                return false;
            });

            return playerData.isEmpty();
        });
    }

    /**
     * Obtiene estadísticas del sistema de cooldowns
     * @return String con estadísticas
     */
    public String getStats() {
        int totalPlayers = playerCooldowns.size();
        int totalCooldowns = playerCooldowns.values().stream()
                .mapToInt(Map::size)
                .sum();

        return String.format("Players with cooldowns: %d, Total active cooldowns: %d",
                totalPlayers, totalCooldowns);
    }

    // ===== CONTROL DEL CICLO DE VIDA =====

    private void start() {
        if (initialized) return;

        // Cargar datos existentes
        loadCooldowns();

        // Iniciar tareas programadas
        startTasks();

        initialized = true;
        DebugUtils.logInternalInfo("CooldownManager initialized with " + playerCooldowns.size() + " players");
    }

    private void startTasks() {
        // Tarea de guardado periódico
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::saveCooldowns, saveIntervalTicks, saveIntervalTicks);

        // Tarea de limpieza periódica
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::cleanupExpiredCooldowns, cleanupIntervalTicks, cleanupIntervalTicks);
    }

    /**
     * Apaga el sistema de cooldowns y guarda los datos
     */
    public static void shutdown() {
        if (instance != null && instance.initialized) {
            instance.stop();
        }
    }

    private void stop() {
        if (!initialized) return;

        // Cancelar tareas
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        // Guardar datos finales
        saveCooldowns();

        // Limpiar memoria
        playerCooldowns.clear();
        cooldownCallbacks.clear();

        initialized = false;
        logInternalDebug(debug(), "CooldownManager shutdown");
    }

    private Material getMaterialFromItemId(String itemId, Player player) {
        ItemConfiguration config = ItemManager.getItemConfiguration(itemId);
        if (config == null) {
            return Material.STONE;
        }

        String materialString = config.getMaterial();

        // Si tiene placeholders y tenemos un jugador, procesarlos
        if (player != null && materialString.contains("%")) {
            // Crear item temporal para procesar placeholders
            InteractiveItem tempItem = ItemManager.createItem(itemId, player);
            if (tempItem != null) {
                return tempItem.getItemStack().getType();
            }
        }

        // Procesar sin placeholders
        if (materialString.startsWith("headbase-") ||
                materialString.startsWith("headurl-") ||
                materialString.startsWith("playerhead-")) {
            return Material.PLAYER_HEAD;
        }

        try {
            return Material.valueOf(materialString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }
}
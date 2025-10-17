package net.exylia.commons.item.cooldown;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.item.ItemManager;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.utils.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;
import static net.exylia.commons.utils.DebugUtils.logInternalError;

public class CooldownManager {

    private static CooldownManager instance;
    private final JavaPlugin plugin;
    private final Gson gson;
    @Getter
    @Setter
    private CooldownConfiguration configuration;

    private final Map<UUID, Map<String, Long>> playerCooldowns;
    private final Map<UUID, Long> playerGlobalCooldowns;
    private final Map<String, Consumer<CooldownEvent>> cooldownCallbacks;
    private final File cooldownDataFile;

    private ScheduledTask saveTask;
    private ScheduledTask cleanupTask;
    private boolean initialized = false;

    private CooldownManager(JavaPlugin plugin, CooldownConfiguration configuration) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.configuration = configuration;
        this.playerCooldowns = new ConcurrentHashMap<>();
        this.playerGlobalCooldowns = new ConcurrentHashMap<>();
        this.cooldownCallbacks = new ConcurrentHashMap<>();
        this.cooldownDataFile = new File(plugin.getDataFolder(), configuration.getDataFileName());

        logInternalDebug("CooldownManager constructor initialized for plugin: " + plugin.getName());
    }

    public static void initialize(JavaPlugin plugin, CooldownConfiguration configuration) {
        if (instance != null && instance.initialized) {
            logInternalDebug("CooldownManager already initialized, skipping...");
            return;
        }

        logInternalDebug("Initializing CooldownManager with configuration: " + configuration);
        instance = new CooldownManager(plugin, configuration);
        instance.start();
    }

    public static CooldownManager getInstance() {
        if (instance == null || !instance.initialized) {
            throw new IllegalStateException("CooldownManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null && instance.initialized;
    }

    public void setCooldown(Player player, String itemId, double cooldownSeconds) {
        setCooldown(player.getUniqueId(), itemId, cooldownSeconds);
    }

    public void setCooldown(UUID playerId, String itemId, double cooldownSeconds) {
        logInternalDebug("Setting cooldown for player " + playerId + ", item " + itemId + ", duration " + cooldownSeconds + "s");

        if (cooldownSeconds <= 0) {
            logInternalDebug("Cooldown duration <= 0, removing cooldown instead");
            removeCooldown(playerId, itemId);
            return;
        }

        if (configuration.hasMaxItemsLimit()) {
            int currentItemsInCooldown = getCurrentItemsInCooldown(playerId);
            if (currentItemsInCooldown >= configuration.getMaxItemsInCooldown() && !hasCooldown(playerId, itemId)) {
                logInternalDebug("Max items limit reached (" + configuration.getMaxItemsInCooldown() + ") for player " + playerId + ", skipping cooldown");
                return;
            }
        }

        long expirationTime = System.currentTimeMillis() + (long)(cooldownSeconds * 1000.0);
        playerCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(itemId.toLowerCase(), expirationTime);

        if (configuration.hasGlobalCooldown()) {
            Player player = Bukkit.getPlayer(playerId);
            double effectiveGlobalCooldown = player != null ?
                configuration.getEffectiveGlobalCooldown(player) :
                configuration.getGlobalCooldownSeconds();

            if (effectiveGlobalCooldown > 0) {
                long globalExpirationTime = System.currentTimeMillis() + (long)(effectiveGlobalCooldown * 1000.0);
                playerGlobalCooldowns.put(playerId, globalExpirationTime);
                logInternalDebug("Global cooldown set for player " + playerId + " for " + effectiveGlobalCooldown + "s");
            }
        }

        triggerCooldownEvent(CooldownEventType.SET, playerId, itemId, cooldownSeconds);

        Schedulers.syncLater(() -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                Material material = getMaterialFromItemId(itemId, player);
                player.setCooldown(material, (int) cooldownSeconds * 20);
                logInternalDebug("Vanilla cooldown set for player " + playerId + ", material " + material + ", ticks " + ((int) cooldownSeconds * 20));
            }
        }, 1);
    }

    public boolean hasCooldown(Player player, String itemId) {
        return hasCooldown(player.getUniqueId(), itemId);
    }

    public boolean hasCooldown(UUID playerId, String itemId) {
        if (hasGlobalCooldown(playerId)) {
            logInternalDebug("Player " + playerId + " has global cooldown active, blocking item " + itemId);
            return true;
        }

        Map<String, Long> playerData = playerCooldowns.get(playerId);
        if (playerData == null) {
            return false;
        }

        Long expirationTime = playerData.get(itemId.toLowerCase());
        if (expirationTime == null) {
            return false;
        }

        if (System.currentTimeMillis() >= expirationTime) {
            logInternalDebug("Cooldown expired for player " + playerId + ", item " + itemId + ", removing automatically");
            playerData.remove(itemId.toLowerCase());
            triggerCooldownEvent(CooldownEventType.EXPIRE, playerId, itemId, 0.0);
            return false;
        }

        return true;
    }

    public double getRemainingCooldown(Player player, String itemId) {
        return getRemainingCooldown(player.getUniqueId(), itemId);
    }

    public double getRemainingCooldown(UUID playerId, String itemId) {
        double globalRemaining = getRemainingGlobalCooldown(playerId);
        if (globalRemaining > 0) {
            logInternalDebug("Player " + playerId + " has global cooldown remaining: " + globalRemaining + "s for item " + itemId);
            return globalRemaining;
        }

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
            logInternalDebug("Cooldown expired during check for player " + playerId + ", item " + itemId);
            playerData.remove(itemId.toLowerCase());
            triggerCooldownEvent(CooldownEventType.EXPIRE, playerId, itemId, 0.0);
            return 0.0;
        }

        double remainingSeconds = remainingMs / 1000.0;
        logInternalDebug("Remaining cooldown for player " + playerId + ", item " + itemId + ": " + remainingSeconds + "s");
        return remainingSeconds;
    }

    public Map<String, String> getPlayerCooldowns(Player player) {
        return getPlayerCooldowns(player.getUniqueId());
    }

    public Map<String, String> getPlayerCooldowns(UUID playerId) {
        Map<String, String> result = new ConcurrentHashMap<>();
        Map<String, Long> playerData = playerCooldowns.get(playerId);

        if (playerData != null) {
            long currentTime = System.currentTimeMillis();
            int expiredCount = 0;

            for (Map.Entry<String, Long> entry : new ConcurrentHashMap<>(playerData).entrySet()) {
                long remainingMs = entry.getValue() - currentTime;
                if (remainingMs <= 0) {
                    playerData.remove(entry.getKey());
                    triggerCooldownEvent(CooldownEventType.EXPIRE, playerId, entry.getKey(), 0.0);
                    expiredCount++;
                } else {
                    result.put(entry.getKey(), TimeFormatter.timeFormatter.format(remainingMs));
                }
            }

            if (expiredCount > 0) {
                logInternalDebug("Removed " + expiredCount + " expired cooldowns for player " + playerId);
            }
        }

        logInternalDebug("Retrieved " + result.size() + " active cooldowns for player " + playerId);
        return result;
    }

    public void removeCooldown(Player player, String itemId) {
        removeCooldown(player.getUniqueId(), itemId);
    }

    public void removeCooldown(UUID playerId, String itemId) {
        logInternalDebug("Removing cooldown for player " + playerId + ", item " + itemId);

        Map<String, Long> playerData = playerCooldowns.get(playerId);
        if (playerData != null) {
            Long removed = playerData.remove(itemId.toLowerCase());
            if (removed != null) {
                logInternalDebug("Successfully removed cooldown for player " + playerId + ", item " + itemId);
                triggerCooldownEvent(CooldownEventType.REMOVE, playerId, itemId, 0.0);
            }

            if (playerData.isEmpty()) {
                playerCooldowns.remove(playerId);
                logInternalDebug("Removed empty cooldown map for player " + playerId);
            }
        }
    }

    public void removeAllCooldowns(Player player) {
        removeAllCooldowns(player.getUniqueId());
    }

    public void removeAllCooldowns(UUID playerId) {
        logInternalDebug("Removing all cooldowns for player " + playerId);

        Map<String, Long> removed = playerCooldowns.remove(playerId);
        playerGlobalCooldowns.remove(playerId);

        if (removed != null && !removed.isEmpty()) {
            logInternalDebug("Removed " + removed.size() + " cooldowns for player " + playerId);
            triggerCooldownEvent(CooldownEventType.CLEAR_ALL, playerId, null, 0.0);
        }
    }

    public boolean hasGlobalCooldown(UUID playerId) {
        if (!configuration.hasGlobalCooldown()) {
            return false;
        }

        Long globalExpirationTime = playerGlobalCooldowns.get(playerId);
        if (globalExpirationTime == null) {
            return false;
        }

        if (System.currentTimeMillis() >= globalExpirationTime) {
            logInternalDebug("Global cooldown expired for player " + playerId + ", removing");
            playerGlobalCooldowns.remove(playerId);
            return false;
        }

        return true;
    }

    public double getRemainingGlobalCooldown(UUID playerId) {
        if (!configuration.hasGlobalCooldown()) {
            return 0.0;
        }

        Long globalExpirationTime = playerGlobalCooldowns.get(playerId);
        if (globalExpirationTime == null) {
            return 0.0;
        }

        long remainingMs = globalExpirationTime - System.currentTimeMillis();
        if (remainingMs <= 0) {
            logInternalDebug("Global cooldown expired during check for player " + playerId);
            playerGlobalCooldowns.remove(playerId);
            return 0.0;
        }

        return remainingMs / 1000.0;
    }

    public void removeGlobalCooldown(UUID playerId) {
        logInternalDebug("Removing global cooldown for player " + playerId);
        playerGlobalCooldowns.remove(playerId);
    }

    public void registerCooldownCallback(String itemId, Consumer<CooldownEvent> callback) {
        logInternalDebug("Registering cooldown callback for item " + itemId);
        cooldownCallbacks.put(itemId.toLowerCase(), callback);
    }

    public void unregisterCooldownCallback(String itemId) {
        logInternalDebug("Unregistering cooldown callback for item " + itemId);
        cooldownCallbacks.remove(itemId.toLowerCase());
    }

    private void triggerCooldownEvent(CooldownEventType type, UUID playerId, String itemId, double seconds) {
        logInternalDebug("Triggering cooldown event: " + type + " for player " + playerId + ", item " + itemId + ", seconds " + seconds);

        CooldownEvent event = new CooldownEvent(type, playerId, itemId, seconds);
        if (itemId != null) {
            Consumer<CooldownEvent> callback = cooldownCallbacks.get(itemId.toLowerCase());
            if (callback != null) {
                Schedulers.sync(() -> callback.accept(event));
            }
        }
        Schedulers.async(() -> Bukkit.getPluginManager().callEvent(event));
    }

    public void saveCooldowns() {
        logInternalDebug("Saving cooldowns to file: " + cooldownDataFile.getAbsolutePath());

        try {
            if (!cooldownDataFile.getParentFile().exists()) {
                boolean created = cooldownDataFile.getParentFile().mkdirs();
                logInternalDebug("Created parent directories: " + created);
            }

            cleanupExpiredCooldowns();

            Map<String, Map<String, Long>> saveData = new ConcurrentHashMap<>();
            playerCooldowns.forEach((uuid, cooldowns) -> {
                if (!cooldowns.isEmpty()) {
                    saveData.put(uuid.toString(), cooldowns);
                }
            });

            try (FileWriter writer = new FileWriter(cooldownDataFile)) {
                gson.toJson(saveData, writer);
            }

            logInternalDebug("Successfully saved " + saveData.size() + " player cooldowns");

        } catch (IOException e) {
            logInternalError("Error saving cooldowns: " + e.getMessage());
        }
    }

    public void loadCooldowns() {
        if (!cooldownDataFile.exists()) {
            logInternalDebug("Cooldown file does not exist, skipping load");
            return;
        }

        logInternalDebug("Loading cooldowns from file: " + cooldownDataFile.getAbsolutePath());

        try (FileReader reader = new FileReader(cooldownDataFile)) {
            Type type = new TypeToken<Map<String, Map<String, Long>>>(){}.getType();
            Map<String, Map<String, Long>> loadedData = gson.fromJson(reader, type);

            if (loadedData != null) {
                playerCooldowns.clear();
                int loadedPlayers = 0;
                int invalidUUIDs = 0;

                for (Map.Entry<String, Map<String, Long>> entry : loadedData.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        playerCooldowns.put(uuid, new ConcurrentHashMap<>(entry.getValue()));
                        loadedPlayers++;
                    } catch (IllegalArgumentException e) {
                        logInternalWarn("Invalid UUID in cooldown data: " + entry.getKey());
                        invalidUUIDs++;
                    }
                }

                cleanupExpiredCooldowns();
                logInternalDebug("Successfully loaded cooldowns for " + loadedPlayers + " players, invalid UUIDs: " + invalidUUIDs);
            }

        } catch (IOException e) {
            logInternalError("Error loading cooldowns: " + e.getMessage());
        }
    }

    public void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        int cleanedPlayers = 0;
        int cleanedItems = 0;

        for (Map.Entry<UUID, Map<String, Long>> playerEntry : new ConcurrentHashMap<>(playerCooldowns).entrySet()) {
            UUID playerId = playerEntry.getKey();
            Map<String, Long> playerData = playerEntry.getValue();
            int expiredItems = 0;

            for (Map.Entry<String, Long> itemEntry : new ConcurrentHashMap<>(playerData).entrySet()) {
                if (currentTime >= itemEntry.getValue()) {
                    playerData.remove(itemEntry.getKey());
                    triggerCooldownEvent(CooldownEventType.EXPIRE, playerId, itemEntry.getKey(), 0.0);
                    expiredItems++;
                }
            }

            cleanedItems += expiredItems;
            if (playerData.isEmpty()) {
                playerCooldowns.remove(playerId);
                cleanedPlayers++;
            }
        }

        if (cleanedPlayers > 0 || cleanedItems > 0) {
            logInternalDebug("Cleanup completed: removed " + cleanedItems + " expired items, " + cleanedPlayers + " empty players");
        }
    }

    public int getCurrentItemsInCooldown(UUID playerId) {
        Map<String, Long> playerData = playerCooldowns.get(playerId);
        if (playerData == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        int expiredCount = 0;

        for (Map.Entry<String, Long> entry : new ConcurrentHashMap<>(playerData).entrySet()) {
            if (currentTime >= entry.getValue()) {
                playerData.remove(entry.getKey());
                triggerCooldownEvent(CooldownEventType.EXPIRE, playerId, entry.getKey(), 0.0);
                expiredCount++;
            }
        }

        if (expiredCount > 0) {
            logInternalDebug("Removed " + expiredCount + " expired items during count for player " + playerId);
        }

        return playerData.size();
    }

    public boolean canAddMoreItemsToCooldown(UUID playerId) {
        if (!configuration.hasMaxItemsLimit()) {
            return true;
        }
        boolean canAdd = getCurrentItemsInCooldown(playerId) < configuration.getMaxItemsInCooldown();
        logInternalDebug("Player " + playerId + " can add more items to cooldown: " + canAdd);
        return canAdd;
    }

    public String getStats() {
        int totalPlayers = playerCooldowns.size();
        int totalCooldowns = playerCooldowns.values().stream()
                .mapToInt(Map::size)
                .sum();
        int globalCooldowns = playerGlobalCooldowns.size();

        String maxItemsInfo = configuration.hasMaxItemsLimit() ?
                ", Max items per player: " + configuration.getMaxItemsInCooldown() : "";
        String globalInfo = configuration.hasGlobalCooldown() ?
                ", Global cooldown configured, Active global: " + globalCooldowns : "";

        String stats = String.format("Players with cooldowns: %d, Total active cooldowns: %d%s%s",
                totalPlayers, totalCooldowns, maxItemsInfo, globalInfo);

        logInternalDebug("Stats requested: " + stats);
        return stats;
    }

    private void start() {
        if (initialized) return;

        logInternalDebug("Starting CooldownManager...");
        loadCooldowns();
        startTasks();

        initialized = true;
        DebugUtils.logInternalInfo("CooldownManager initialized with " + playerCooldowns.size() + " players");
    }

    private void startTasks() {
        logInternalDebug("Starting periodic tasks - save interval: " + configuration.getSaveIntervalTicks() +
                        " ticks, cleanup interval: " + configuration.getCleanupIntervalTicks() + " ticks");

        saveTask = Schedulers.asyncTimer(
                this::saveCooldowns, configuration.getSaveIntervalTicks(), configuration.getSaveIntervalTicks());

        cleanupTask = Schedulers.asyncTimer(
                this::cleanupExpiredCooldowns, configuration.getCleanupIntervalTicks(), configuration.getCleanupIntervalTicks());
    }

    public static void shutdown() {
        if (instance != null && instance.initialized) {
            logInternalDebug("Shutting down CooldownManager...");
            instance.stop();
        }
    }

    private void stop() {
        if (!initialized) return;

        if (saveTask != null) {
            saveTask.cancel();
            logInternalDebug("Save task cancelled");
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
            logInternalDebug("Cleanup task cancelled");
        }

        saveCooldowns();

        int totalPlayers = playerCooldowns.size();
        int totalGlobal = playerGlobalCooldowns.size();
        int totalCallbacks = cooldownCallbacks.size();

        playerCooldowns.clear();
        playerGlobalCooldowns.clear();
        cooldownCallbacks.clear();

        initialized = false;
        logInternalDebug("CooldownManager shutdown - cleared " + totalPlayers + " players, " +
                        totalGlobal + " global cooldowns, " + totalCallbacks + " callbacks");
    }

    private Material getMaterialFromItemId(String itemId, Player player) {
        ItemConfiguration config = ItemManager.getItemConfiguration(itemId);
        if (config == null) {
            logInternalWarn("No configuration found for item ID: " + itemId + ", using STONE");
            return Material.STONE;
        }

        String materialString = config.getMaterial();

        if (player != null && materialString.contains("%")) {
            InteractiveItem tempItem = ItemManager.createItem(itemId, player);
            if (tempItem != null) {
                Material material = tempItem.getItemStack().getType();
                logInternalDebug("Resolved material with placeholders for item " + itemId + ": " + material);
                return material;
            }
        }

        if (materialString.startsWith("headbase-") ||
                materialString.startsWith("headurl-") ||
                materialString.startsWith("playerhead-")) {
            logInternalDebug("Head item detected for " + itemId + ", using PLAYER_HEAD");
            return Material.PLAYER_HEAD;
        }

        try {
            Material material = Material.valueOf(materialString.toUpperCase());
            logInternalDebug("Resolved material for item " + itemId + ": " + material);
            return material;
        } catch (IllegalArgumentException e) {
            logInternalWarn("Invalid material '" + materialString + "' for item " + itemId + ", using STONE");
            return Material.STONE;
        }
    }
}

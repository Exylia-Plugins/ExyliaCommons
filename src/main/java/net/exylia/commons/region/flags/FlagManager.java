package net.exylia.commons.region.flags;

import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import net.exylia.commons.region.model.RegionFlagType;
import net.exylia.commons.region.RegionManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor central para la aplicación y validación de flags de región
 */
public class FlagManager {
    private static FlagManager instance;

    private final JavaPlugin plugin;
    private final Map<UUID, FlagState> playerStates; // Estado de flags por jugador
    private final Map<UUID, BukkitRunnable> activeEffects; // Efectos activos por jugador

    // Cache de validaciones
    private final Map<String, Boolean> validationCache;
    private final long cacheTimeout = 5000; // 5 segundos

    private FlagManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerStates = new ConcurrentHashMap<>();
        this.activeEffects = new ConcurrentHashMap<>();
        this.validationCache = new ConcurrentHashMap<>();

        // Iniciar tarea de limpieza de cache
        startCacheCleanupTask();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new FlagManager(plugin);
        }
    }

    public static FlagManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FlagManager no ha sido inicializado");
        }
        return instance;
    }

    // ===== VALIDACIÓN DE ACCIONES =====

    /**
     * Verifica si un jugador puede realizar una acción específica en su ubicación actual
     */
    public boolean canPlayerPerformAction(Player player, RegionFlag flag) {
        return canPlayerPerformActionAt(player, player.getLocation(), flag);
    }

    /**
     * Verifica si un jugador puede realizar una acción en una ubicación específica
     */
    public boolean canPlayerPerformActionAt(Player player, Location location, RegionFlag flag) {
        // Validar parámetros obligatorios
        if (location == null || flag == null) {
            throw new IllegalArgumentException("Location y flag no pueden ser null");
        }

        // Verificar cache primero
        String cacheKey = null;
        if (player != null) {
            cacheKey = getCacheKey(player, location, flag);
            Boolean cached = validationCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        // Verificar permisos administrativos
        if (player != null && hasAdminPermission(player, flag)) {
            cacheResult(cacheKey, true);
            return true;
        }

        // Obtener regiones en la ubicación
        List<Region> regions = RegionManager.getInstance().getRegionsAt(location);

        boolean result;
        if (regions.isEmpty()) {
            // No hay regiones, usar valor por defecto
            result = flag.isDefaultValue();
        } else {
            // Usar la región de mayor prioridad
            Region highestPriorityRegion = regions.get(0);
            result = evaluateFlagInRegion(player, highestPriorityRegion, flag);
        }

        // Cachear resultado solo si player no es null
        if (cacheKey != null) {
            cacheResult(cacheKey, result);
        }
        return result;
    }

    /**
     * Evalúa una flag en una región específica considerando permisos y configuración
     */
    private boolean evaluateFlagInRegion(Player player, Region region, RegionFlag flag) {
        // 1. Verificar permisos específicos del jugador en la región (solo si player no es null)
        if (player != null) {
            // Verificar si es owner o miembro
            UUID playerId = player.getUniqueId();
            if (region.isOwner(playerId)) {
                RegionFlagType flagType = region.getFlagType(flag);
                if (flagType == RegionFlagType.DENY) {
                    // Solo negar si está explícitamente denegado
                    return false;
                }
                return true;
            }

            if (region.isMember(playerId)) {
                // Los miembros pueden hacer cosas básicas
                if (flag.affectsBuilding() || flag == RegionFlag.INTERACT) {
                    RegionFlagType flagType = region.getFlagType(flag);
                    if (flagType == RegionFlagType.DENY) {
                        return false;
                    }
                    return true;
                }
            }

            // Verificar permisos específicos
            String flagPermission = flag.getRequiredPermission(region.getPluginName(), region.getId());
            if (player.hasPermission(flagPermission)) {
                return true;
            }
        }

        // 2. Obtener el valor de la flag en la región
        boolean flagValue = region.getFlagValue(flag);

        // 3. Verificar incompatibilidades de flags
        if (hasIncompatibleFlags(region, flag)) {
            return false;
        }

        return flagValue;
    }

    /**
     * Verifica si una región tiene flags incompatibles con la flag dada
     */
    private boolean hasIncompatibleFlags(Region region, RegionFlag flag) {
        return region.getConfiguredFlags().keySet().stream()
                .anyMatch(configuredFlag -> flag.isIncompatibleWith(configuredFlag) &&
                        region.getFlagValue(configuredFlag));
    }

    /**
     * Verifica si un jugador tiene permisos administrativos para una flag
     */
    private boolean hasAdminPermission(Player player, RegionFlag flag) {
        return player.hasPermission("exylia.region.admin") ||
                player.hasPermission("exylia.region.flag.admin") ||
                player.hasPermission("exylia.region.flag." + flag.getKey().replace("-", "_") + ".admin");
    }

    // ===== APLICACIÓN DE EFECTOS =====

    /**
     * Aplica los efectos de las flags de una región a un jugador cuando entra
     */
    public void applyRegionEffects(Player player, Region region) {
        FlagState state = getOrCreatePlayerState(player);

        for (Map.Entry<RegionFlag, RegionFlagType> entry : region.getConfiguredFlags().entrySet()) {
            RegionFlag flag = entry.getKey();
            RegionFlagType type = entry.getValue();

            // Solo aplicar efectos para flags que están ALLOW
            if (type == RegionFlagType.ALLOW || (type == RegionFlagType.DEFAULT && flag.isDefaultValue())) {
                applyFlagEffect(player, region, flag, state);
            }
        }

        // Guardar región activa
        state.setActiveRegion(region);
    }

    /**
     * Remueve los efectos de las flags de una región cuando el jugador sale
     */
    public void removeRegionEffects(Player player, Region region) {
        FlagState state = playerStates.get(player.getUniqueId());
        if (state == null) return;

        for (RegionFlag flag : region.getConfiguredFlags().keySet()) {
            removeFlagEffect(player, region, flag, state);
        }

        // Limpiar región activa si coincide
        if (region.equals(state.getActiveRegion())) {
            state.setActiveRegion(null);
        }
    }

    /**
     * Aplica el efecto de una flag específica
     */
    private void applyFlagEffect(Player player, Region region, RegionFlag flag, FlagState state) {
        switch (flag) {
            case FLIGHT:
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                    state.addAppliedFlag(flag);
                    player.sendMessage("§a¡Ahora puedes volar en esta región!");
                }
                break;

            case INVINCIBLE:
                state.addAppliedFlag(flag);
                player.sendMessage("§6¡Eres invencible en esta región!");
                break;

            case HEAL:
                startHealEffect(player, region);
                state.addAppliedFlag(flag);
                break;

            case FEED:
                startFeedEffect(player, region);
                state.addAppliedFlag(flag);
                break;

            case FORCE_ADVENTURE:
                if (player.getGameMode() != GameMode.ADVENTURE) {
                    state.setPreviousGameMode(player.getGameMode());
                    player.setGameMode(GameMode.ADVENTURE);
                    state.addAppliedFlag(flag);
                    player.sendMessage("§e¡Modo aventura activado en esta región!");
                }
                break;

            case FORCE_SURVIVAL:
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    state.setPreviousGameMode(player.getGameMode());
                    player.setGameMode(GameMode.SURVIVAL);
                    state.addAppliedFlag(flag);
                    player.sendMessage("§e¡Modo supervivencia activado en esta región!");
                }
                break;

            case FORCE_CREATIVE:
                if (player.getGameMode() != GameMode.CREATIVE) {
                    state.setPreviousGameMode(player.getGameMode());
                    player.setGameMode(GameMode.CREATIVE);
                    state.addAppliedFlag(flag);
                    player.sendMessage("§e¡Modo creativo activado en esta región!");
                }
                break;
        }
    }

    /**
     * Remueve el efecto de una flag específica
     */
    private void removeFlagEffect(Player player, Region region, RegionFlag flag, FlagState state) {
        if (!state.hasAppliedFlag(flag)) return;

        switch (flag) {
            case FLIGHT:
                // Solo remover vuelo si no tiene permiso natural
                if (!player.hasPermission("exylia.flight") &&
                        player.getGameMode() != GameMode.CREATIVE &&
                        player.getGameMode() != GameMode.SPECTATOR) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.sendMessage("§c¡Ya no puedes volar fuera de la región!");
                }
                break;

            case INVINCIBLE:
                player.sendMessage("§c¡Ya no eres invencible fuera de la región!");
                break;

            case HEAL:
                stopEffect(player, "heal");
                break;

            case FEED:
                stopEffect(player, "feed");
                break;

            case FORCE_ADVENTURE:
            case FORCE_SURVIVAL:
            case FORCE_CREATIVE:
                GameMode previous = state.getPreviousGameMode();
                if (previous != null) {
                    player.setGameMode(previous);
                    player.sendMessage("§e¡Modo de juego restaurado!");
                }
                break;
        }

        state.removeAppliedFlag(flag);
    }

    // ===== EFECTOS ESPECIALES =====

    /**
     * Inicia efecto de curación automática
     */
    private void startHealEffect(Player player, Region region) {
        String effectKey = "heal";
        stopEffect(player, effectKey); // Detener efecto anterior si existe

        BukkitRunnable healTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !region.contains(player)) {
                    this.cancel();
                    return;
                }

                double health = player.getHealth();
                double maxHealth = player.getMaxHealth();

                if (health < maxHealth) {
                    player.setHealth(Math.min(maxHealth, health + 1.0));
                }
            }
        };

        healTask.runTaskTimer(plugin, 0L, 40L); // Cada 2 segundos
        activeEffects.put(getEffectKey(player, effectKey), healTask);
    }

    /**
     * Inicia efecto de alimentación automática
     */
    private void startFeedEffect(Player player, Region region) {
        String effectKey = "feed";
        stopEffect(player, effectKey);

        BukkitRunnable feedTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !region.contains(player)) {
                    this.cancel();
                    return;
                }

                if (player.getFoodLevel() < 20) {
                    player.setFoodLevel(20);
                    player.setSaturation(20.0f);
                }
            }
        };

        feedTask.runTaskTimer(plugin, 0L, 100L); // Cada 5 segundos
        activeEffects.put(getEffectKey(player, effectKey), feedTask);
    }

    /**
     * Detiene un efecto específico para un jugador
     */
    private void stopEffect(Player player, String effectName) {
        UUID effectKey = getEffectKey(player, effectName);
        BukkitRunnable task = activeEffects.remove(effectKey);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Genera clave única para efectos
     */
    private UUID getEffectKey(Player player, String effectName) {
        return UUID.nameUUIDFromBytes((player.getUniqueId().toString() + ":" + effectName).getBytes());
    }

    // ===== GESTIÓN DE ESTADOS =====

    /**
     * Obtiene o crea el estado de flags para un jugador
     */
    private FlagState getOrCreatePlayerState(Player player) {
        return playerStates.computeIfAbsent(player.getUniqueId(),
                k -> new FlagState(player.getUniqueId()));
    }

    /**
     * Limpia el estado de un jugador
     */
    public void cleanupPlayerState(Player player) {
        FlagState state = playerStates.remove(player.getUniqueId());
        if (state != null) {
            // Detener todos los efectos activos
            for (RegionFlag flag : state.getAppliedFlags()) {
                removeFlagEffect(player, state.getActiveRegion(), flag, state);
            }
        }

        // Detener efectos activos
        activeEffects.entrySet().removeIf(entry -> {
            if (entry.getKey().toString().startsWith(player.getUniqueId().toString())) {
                entry.getValue().cancel();
                return true;
            }
            return false;
        });
    }

    // ===== CACHE =====

    /**
     * Genera clave de cache para validaciones
     */
    private String getCacheKey(Player player, Location location, RegionFlag flag) {
        return String.format("%s:%d:%d:%d:%s:%d",
                player.getUniqueId().toString(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                flag.getKey(),
                System.currentTimeMillis() / cacheTimeout);
    }

    /**
     * Guarda resultado en cache
     */
    private void cacheResult(String key, boolean result) {
        validationCache.put(key, result);
    }

    /**
     * Inicia tarea de limpieza de cache
     */
    private void startCacheCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                validationCache.entrySet().removeIf(entry -> {
                    String[] parts = entry.getKey().split(":");
                    if (parts.length >= 6) {
                        long timestamp = Long.parseLong(parts[5]) * cacheTimeout;
                        return currentTime - timestamp > cacheTimeout;
                    }
                    return true;
                });
            }
        }.runTaskTimerAsynchronously(plugin, 200L, 200L); // Cada 10 segundos
    }

    // ===== VERIFICACIONES ESPECIALES =====

    /**
     * Verifica si un jugador es invencible en su ubicación actual
     */
    public boolean isPlayerInvincible(Player player) {
        FlagState state = playerStates.get(player.getUniqueId());
        return state != null && state.hasAppliedFlag(RegionFlag.INVINCIBLE);
    }

    /**
     * Verifica si el PvP está permitido entre dos jugadores
     */
    public boolean isPvpAllowed(Player attacker, Player target) {
        return canPlayerPerformAction(attacker, RegionFlag.PVP) &&
                canPlayerPerformAction(target, RegionFlag.PVP) &&
                !isPlayerInvincible(target);
    }

    /**
     * Invalida el cache para una flag específica
     */
    public void invalidateFlagCache(RegionFlag flag) {
        String flagKey = ":" + flag.getKey() + ":";
        validationCache.entrySet().removeIf(entry ->
                entry.getKey().contains(flagKey));

        if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
            plugin.getLogger().fine("Cache invalidado para flag: " + flag.getKey());
        }
    }

    /**
     * Invalida el cache para un jugador específico
     */
    public void invalidatePlayerCache(Player player) {
        String playerPrefix = player.getUniqueId().toString() + ":";
        int removed = 0;

        Iterator<Map.Entry<String, Boolean>> iterator = validationCache.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getKey().startsWith(playerPrefix)) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0 && plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
            plugin.getLogger().fine("Cache invalidado para jugador " + player.getName() +
                    ": " + removed + " entradas removidas");
        }
    }

    /**
     * Invalida el cache para una región específica
     */
    public void invalidateRegionCache(Region region) {
        // Invalidar para todas las ubicaciones de la región
        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    String locationKey = ":" + x + ":" + y + ":" + z + ":";
                    validationCache.entrySet().removeIf(entry ->
                            entry.getKey().contains(locationKey));
                }
            }
        }
    }

    /**
     * Invalida el cache para una flag en una región específica (más eficiente)
     */
    public void invalidateRegionFlagCache(Region region, RegionFlag flag) {
        String flagKey = ":" + flag.getKey() + ":";
        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();

        validationCache.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            if (!key.contains(flagKey)) return false;

            // Extraer coordenadas del cache key
            String[] parts = key.split(":");
            if (parts.length >= 4) {
                try {
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);

                    return x >= min.getBlockX() && x <= max.getBlockX() &&
                            y >= min.getBlockY() && y <= max.getBlockY() &&
                            z >= min.getBlockZ() && z <= max.getBlockZ();
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return false;
        });
    }
}
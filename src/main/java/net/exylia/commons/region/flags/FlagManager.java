package net.exylia.commons.region.flags;

import lombok.Getter;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import net.exylia.commons.region.model.RegionFlagType;
import net.exylia.commons.region.RegionManager;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static net.exylia.commons.config.base.MainConfigBase.debug;

/**
 * Gestor central OPTIMIZADO para la aplicación y validación de flags de región
 * Incluye cache mejorado y optimizaciones de rendimiento
 */
public class FlagManager {
    private static FlagManager instance;

    private final JavaPlugin plugin;
    private final Map<UUID, FlagState> playerStates; // Estado de flags por jugador
    private final Map<UUID, BukkitRunnable> activeEffects; // Efectos activos por jugador

    // ===== CACHE OPTIMIZADO =====
    private final OptimizedFlagCache optimizedCache;

    // ===== ESTADÍSTICAS =====
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong validationCalls = new AtomicLong();

    private FlagManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerStates = new ConcurrentHashMap<>();
        this.activeEffects = new ConcurrentHashMap<>();
        this.optimizedCache = new OptimizedFlagCache();

        // Iniciar tarea de limpieza optimizada
        startOptimizedCleanupTask();
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

    // ===== VALIDACIÓN DE ACCIONES OPTIMIZADA =====

    /**
     * Verifica si un jugador puede realizar una acción específica en su ubicación actual
     */
    public boolean canPlayerPerformAction(Player player, RegionFlag flag) {
        return canPlayerPerformActionAt(player, player.getLocation(), flag);
    }

    /**
     * OPTIMIZADO: Verifica si un jugador puede realizar una acción en una ubicación específica
     * Usa cache optimizado y spatial index para máximo rendimiento
     */
    public boolean canPlayerPerformActionAt(Player player, Location location, RegionFlag flag) {
        validationCalls.incrementAndGet();

        // Validar parámetros obligatorios
        if (location == null || flag == null) {
            throw new IllegalArgumentException("Location y flag no pueden ser null");
        }

        // OPTIMIZACIÓN 1: Cache optimizado primero
        if (player != null) {
            Boolean cached = optimizedCache.getCachedResult(player,
                    location.getBlockX(), location.getBlockY(), location.getBlockZ(), flag);
            if (cached != null) {
                cacheHits.incrementAndGet();
                return cached;
            }
        }

        cacheMisses.incrementAndGet();

        // OPTIMIZACIÓN 2: Verificar permisos administrativos temprano
        if (player != null && hasAdminPermission(player, flag)) {
            cacheResult(player, location, flag, true);
            return true;
        }

        // OPTIMIZACIÓN 3: Usar spatial index del RegionManager para búsqueda O(1)
        List<Region> regions = RegionManager.getInstance().getRegionsAt(location);

        boolean result;
        if (regions.isEmpty()) {
            // No hay regiones, usar valor por defecto
            result = flag.isDefaultValue();
        } else {
            // OPTIMIZACIÓN 4: Usar la región de mayor prioridad directamente
            Region highestPriorityRegion = regions.get(0); // Ya viene ordenado por prioridad
            result = evaluateFlagInRegionOptimized(player, highestPriorityRegion, flag, location);
        }

        // Cachear resultado
        cacheResult(player, location, flag, result);
        return result;
    }

    /**
     * OPTIMIZADO: Evalúa una flag en una región específica con optimizaciones
     */
    private boolean evaluateFlagInRegionOptimized(Player player, Region region, RegionFlag flag, Location actionLocation) {
        // OPTIMIZACIÓN 1: Early exit para flags críticas
        if (player != null && flag == RegionFlag.REGION_MEMBERS_ONLY && region.getFlagValue(RegionFlag.REGION_MEMBERS_ONLY)) {
            if (flag.isAffectedByRegionMembersOnly()) {
                boolean playerInRegion = region.contains(player.getLocation());
                if (!playerInRegion) {
                    return false; // Denegación rápida
                }
            }
        }

        // OPTIMIZACIÓN 2: Cache de membresía del jugador
        if (player != null) {
            UUID playerId = player.getUniqueId();

            // Owners tienen acceso completo (excepto DENY explícito)
            if (region.isOwner(playerId)) {
                RegionFlagType flagType = region.getFlagType(flag);
                return flagType != RegionFlagType.DENY;
            }

            // Members tienen acceso a flags básicas
            if (region.isMember(playerId)) {
                if (flag.affectsBuilding() || flag == RegionFlag.INTERACT) {
                    RegionFlagType flagType = region.getFlagType(flag);
                    return flagType != RegionFlagType.DENY;
                }
            }

            // OPTIMIZACIÓN 3: Cache de permisos (evitar lookup repetido)
            String flagPermission = flag.getRequiredPermission(region.getId());
            if (player.hasPermission(flagPermission)) {
                return true;
            }
        }

        // OPTIMIZACIÓN 4: Obtener valor de flag directamente
        boolean flagValue = region.getFlagValue(flag);

        // OPTIMIZACIÓN 5: Skip verificación de incompatibilidades para flags comunes
        if (!flag.requiresSpecialPermission()) {
            return flagValue;
        }

        // Verificar incompatibilidades solo para flags especiales
        if (hasIncompatibleFlags(region, flag)) {
            return false;
        }

        return flagValue;
    }

    /**
     * OPTIMIZADO: Verificación especial para PvP con cache
     */
    public boolean isPvpAllowed(Player attacker, Player target) {
        // OPTIMIZACIÓN 1: Verificar invencibilidad primero (más rápido)
        if (isPlayerInvincible(target)) {
            return false;
        }

        // OPTIMIZACIÓN 2: Cache de PvP por par de jugadores
        String pvpCacheKey = getPvpCacheKey(attacker, target);
        Boolean cachedPvp = optimizedCache.getCachedPvpResult(pvpCacheKey);
        if (cachedPvp != null) {
            return cachedPvp;
        }

        // Obtener ubicaciones
        Location attackerLoc = attacker.getLocation();
        Location targetLoc = target.getLocation();

        // Obtener regiones usando spatial index optimizado
        List<Region> attackerRegions = RegionManager.getInstance().getRegionsAt(attackerLoc);
        List<Region> targetRegions = RegionManager.getInstance().getRegionsAt(targetLoc);

        // Si ninguno está en regiones, usar valor por defecto
        if (attackerRegions.isEmpty() && targetRegions.isEmpty()) {
            boolean result = RegionFlag.PVP.isDefaultValue();
            optimizedCache.cachePvpResult(pvpCacheKey, result);
            return result;
        }

        // OPTIMIZACIÓN 3: Verificar solo la región de mayor prioridad de cada jugador
        boolean pvpAllowed = true;

        // Verificar región del atacante
        if (!attackerRegions.isEmpty()) {
            Region attackerRegion = attackerRegions.get(0);
            if (!checkPvpInRegion(attackerRegion, attacker, target, attackerLoc, targetLoc)) {
                pvpAllowed = false;
            }
        }

        // Verificar región del objetivo (si es diferente)
        if (pvpAllowed && !targetRegions.isEmpty()) {
            Region targetRegion = targetRegions.get(0);
            if (!attackerRegions.contains(targetRegion)) { // Evitar verificación duplicada
                if (!checkPvpInRegion(targetRegion, attacker, target, attackerLoc, targetLoc)) {
                    pvpAllowed = false;
                }
            }
        }

        optimizedCache.cachePvpResult(pvpCacheKey, pvpAllowed);
        return pvpAllowed;
    }

    /**
     * Verifica PvP en una región específica
     */
    private boolean checkPvpInRegion(Region region, Player attacker, Player target, Location attackerLoc, Location targetLoc) {
        // Si la región tiene REGION_MEMBERS_ONLY activo
        if (region.getFlagValue(RegionFlag.REGION_MEMBERS_ONLY)) {
            boolean attackerInRegion = region.contains(attackerLoc);
            boolean targetInRegion = region.contains(targetLoc);

            if (!attackerInRegion || !targetInRegion) {
                return false;
            }
        }

        // Verificar si PvP está permitido en la región
        return region.getFlagValue(RegionFlag.PVP);
    }

    /**
     * Genera clave de cache para PvP
     */
    private String getPvpCacheKey(Player attacker, Player target) {
        UUID attackerId = attacker.getUniqueId();
        UUID targetId = target.getUniqueId();

        // Ordenar UUIDs para cache bidireccional
        if (attackerId.compareTo(targetId) < 0) {
            return attackerId + ":" + targetId;
        } else {
            return targetId + ":" + attackerId;
        }
    }

    /**
     * Verifica incompatibilidades con cache
     */
    private boolean hasIncompatibleFlags(Region region, RegionFlag flag) {
        // Cache de incompatibilidades por región
        String incompatibleKey = region.getId() + ":" + flag.name();
        Boolean cachedIncompatible = optimizedCache.getCachedIncompatibility(incompatibleKey);
        if (cachedIncompatible != null) {
            return cachedIncompatible;
        }

        boolean hasIncompatible = region.getConfiguredFlags().keySet().stream()
                .anyMatch(configuredFlag -> flag.isIncompatibleWith(configuredFlag) &&
                        region.getFlagValue(configuredFlag));

        optimizedCache.cacheIncompatibility(incompatibleKey, hasIncompatible);
        return hasIncompatible;
    }

    /**
     * Verifica permisos administrativos con cache
     */
    private boolean hasAdminPermission(Player player, RegionFlag flag) {
        String adminCacheKey = player.getUniqueId() + ":admin:" + flag.name();
        Boolean cachedAdmin = optimizedCache.getCachedAdminPermission(adminCacheKey);
        if (cachedAdmin != null) {
            return cachedAdmin;
        }

        boolean hasAdmin = player.hasPermission("exylia.region.admin") ||
                player.hasPermission("exylia.region.flag.admin") ||
                player.hasPermission("exylia.region.flag." + flag.getKey().replace("-", "_") + ".admin");

        optimizedCache.cacheAdminPermission(adminCacheKey, hasAdmin);
        return hasAdmin;
    }

    /**
     * Cachea resultado de validación
     */
    private void cacheResult(Player player, Location location, RegionFlag flag, boolean result) {
        if (player != null) {
            optimizedCache.cacheResult(player, location.getBlockX(), location.getBlockY(),
                    location.getBlockZ(), flag, result);
        }
    }

    // ===== APLICACIÓN DE EFECTOS (sin cambios significativos) =====

    public void applyRegionEffects(Player player, Region region) {
        FlagState state = getOrCreatePlayerState(player);

        for (Map.Entry<RegionFlag, RegionFlagType> entry : region.getConfiguredFlags().entrySet()) {
            RegionFlag flag = entry.getKey();
            RegionFlagType type = entry.getValue();

            if (type == RegionFlagType.ALLOW || (type == RegionFlagType.DEFAULT && flag.isDefaultValue())) {
                applyFlagEffect(player, region, flag, state);
            }
        }

        state.setActiveRegion(region);
    }

    public void removeRegionEffects(Player player, Region region) {
        FlagState state = playerStates.get(player.getUniqueId());
        if (state == null) return;

        for (RegionFlag flag : region.getConfiguredFlags().keySet()) {
            removeFlagEffect(player, region, flag, state);
        }

        if (region.equals(state.getActiveRegion())) {
            state.setActiveRegion(null);
        }
    }

    private void applyFlagEffect(Player player, Region region, RegionFlag flag, FlagState state) {
        switch (flag) {
            case FLIGHT:
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                    state.addAppliedFlag(flag);
                }
                break;

            case INVINCIBLE:
                state.addAppliedFlag(flag);
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
                }
                break;

            case FORCE_SURVIVAL:
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    state.setPreviousGameMode(player.getGameMode());
                    player.setGameMode(GameMode.SURVIVAL);
                    state.addAppliedFlag(flag);
                }
                break;

            case FORCE_CREATIVE:
                if (player.getGameMode() != GameMode.CREATIVE) {
                    state.setPreviousGameMode(player.getGameMode());
                    player.setGameMode(GameMode.CREATIVE);
                    state.addAppliedFlag(flag);
                }
                break;

            case REGION_MEMBERS_ONLY:
                state.addAppliedFlag(flag);
                break;
        }
    }

    private void removeFlagEffect(Player player, Region region, RegionFlag flag, FlagState state) {
        if (!state.hasAppliedFlag(flag)) return;

        switch (flag) {
            case FLIGHT:
                if (!player.hasPermission("exylia.flight") &&
                        player.getGameMode() != GameMode.CREATIVE &&
                        player.getGameMode() != GameMode.SPECTATOR) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
                break;

            case INVINCIBLE:
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
                }
                break;

            case REGION_MEMBERS_ONLY:
                break;
        }

        state.removeAppliedFlag(flag);
    }

    // ===== EFECTOS ESPECIALES =====

    private void startHealEffect(Player player, Region region) {
        String effectKey = "heal";
        stopEffect(player, effectKey);

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

        healTask.runTaskTimer(plugin, 0L, 40L);
        activeEffects.put(getEffectKey(player, effectKey), healTask);
    }

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

        feedTask.runTaskTimer(plugin, 0L, 100L);
        activeEffects.put(getEffectKey(player, effectKey), feedTask);
    }

    private void stopEffect(Player player, String effectName) {
        UUID effectKey = getEffectKey(player, effectName);
        BukkitRunnable task = activeEffects.remove(effectKey);
        if (task != null) {
            task.cancel();
        }
    }

    private UUID getEffectKey(Player player, String effectName) {
        return UUID.nameUUIDFromBytes((player.getUniqueId().toString() + ":" + effectName).getBytes());
    }

    // ===== GESTIÓN DE ESTADOS =====

    private FlagState getOrCreatePlayerState(Player player) {
        return playerStates.computeIfAbsent(player.getUniqueId(),
                k -> new FlagState(player.getUniqueId()));
    }

    public void cleanupPlayerState(Player player) {
        FlagState state = playerStates.remove(player.getUniqueId());
        if (state != null) {
            for (RegionFlag flag : state.getAppliedFlags()) {
                removeFlagEffect(player, state.getActiveRegion(), flag, state);
            }
        }

        activeEffects.entrySet().removeIf(entry -> {
            if (entry.getKey().toString().startsWith(player.getUniqueId().toString())) {
                entry.getValue().cancel();
                return true;
            }
            return false;
        });

        // Limpiar del cache optimizado
        optimizedCache.invalidatePlayer(player);
    }

    // ===== CACHE MANAGEMENT =====

    public void invalidateFlagCache(RegionFlag flag) {
        optimizedCache.invalidateFlag(flag);
    }

    public void invalidatePlayerCache(Player player) {
        optimizedCache.invalidatePlayer(player);
    }

    public void invalidateRegionCache(Region region) {
        optimizedCache.invalidateRegion(region);
    }

    public void invalidateRegionFlagCache(Region region, RegionFlag flag) {
        optimizedCache.invalidateRegionFlag(region, flag);
    }

    // ===== VERIFICACIONES ESPECIALES =====

    public boolean isPlayerInvincible(Player player) {
        FlagState state = playerStates.get(player.getUniqueId());
        return state != null && state.hasAppliedFlag(RegionFlag.INVINCIBLE);
    }

    // ===== TAREAS OPTIMIZADAS =====

    private void startOptimizedCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                optimizedCache.performMaintenance();
            }
        }.runTaskTimerAsynchronously(plugin, 100L, 100L); // Cada 5 segundos
    }

    /**
     * Obtiene estadísticas del sistema de flags
     */
    public FlagManagerStats getStats() {
        return new FlagManagerStats(
                playerStates.size(),
                activeEffects.size(),
                cacheHits.get(),
                cacheMisses.get(),
                validationCalls.get(),
                optimizedCache.getStats()
        );
    }

    /**
     * Reinicia estadísticas
     */
    public void resetStats() {
        cacheHits.set(0);
        cacheMisses.set(0);
        validationCalls.set(0);
    }

    /**
     * Estadísticas del FlagManager
     */
    @Getter
    public static class FlagManagerStats {
        private final int activePlayerStates;
        private final int activeEffects;
        private final long cacheHits;
        private final long cacheMisses;
        private final long validationCalls;
        private final OptimizedFlagCache.CacheStats cacheStats;

        public FlagManagerStats(int activePlayerStates, int activeEffects, long cacheHits,
                                long cacheMisses, long validationCalls, OptimizedFlagCache.CacheStats cacheStats) {
            this.activePlayerStates = activePlayerStates;
            this.activeEffects = activeEffects;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.validationCalls = validationCalls;
            this.cacheStats = cacheStats;
        }

        public double getCacheHitRatio() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "FlagManagerStats{states=%d, effects=%d, cache_hits=%d, cache_misses=%d, hit_ratio=%.2f%%, validations=%d}",
                    activePlayerStates, activeEffects, cacheHits, cacheMisses,
                    getCacheHitRatio() * 100, validationCalls
            );
        }
    }
}
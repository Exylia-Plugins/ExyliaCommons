package net.exylia.commons.region.model;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.region.RegionManager;
import net.exylia.commons.region.blocks.PlayerBlockTracker;
import net.exylia.commons.region.flags.FlagManager;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import net.exylia.commons.region.regeneration.RegionRegenerationManager;
import java.util.concurrent.CompletableFuture;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa una región en el mundo con sistema de flags mejorado
 */
@Getter
public class Region {
    private final String id;
    private final String pluginName;
    private final Selection selection;
    private final long createdAt;

    @Setter
    private String displayName;
    @Setter
    private String description;
    @Setter
    private RegionPriority priority;
    @Setter
    private Map<String, Object> metadata;

    // Nuevo sistema de flags: flag -> tipo (ALLOW/DENY/DEFAULT)
    private final Map<RegionFlag, RegionFlagType> flagStates;

    // Lista de miembros y owners
    @Setter
    private Set<UUID> owners;
    @Setter
    private Set<UUID> members;

    // Jugadores actualmente en la región
    private final Set<UUID> playersInside;

    // Callbacks personalizados
    @Setter
    private RegionCallback onEnter;
    @Setter
    private RegionCallback onExit;
    @Setter
    private RegionCallback onMove;

    public Region(String id, String pluginName, Selection selection) {
        this.id = id;
        this.pluginName = pluginName;
        this.selection = selection;
        this.createdAt = System.currentTimeMillis();
        this.displayName = id;
        this.description = "";
        this.priority = RegionPriority.NORMAL;
        this.metadata = new ConcurrentHashMap<>();
        this.flagStates = new ConcurrentHashMap<>();
        this.owners = ConcurrentHashMap.newKeySet();
        this.members = ConcurrentHashMap.newKeySet();
        this.playersInside = ConcurrentHashMap.newKeySet();
    }

    // ===== GESTIÓN DE FLAGS MEJORADA =====

    /**
     * Establece el estado de una flag
     */
    public void setFlag(RegionFlag flag, RegionFlagType type) {
        RegionFlagType previousType = flagStates.get(flag);

        if (type == RegionFlagType.DEFAULT) {
            flagStates.remove(flag);
        } else {
            flagStates.put(flag, type);
        }

        // Auto-invalidar cache si el valor cambió
        if (previousType != type) {
            invalidateCacheForFlag(flag);
        }
    }

    /**
     * Invalida el cache para esta región y flag específica
     */
    private void invalidateCacheForFlag(RegionFlag flag) {
        try {
            FlagManager flagManager = FlagManager.getInstance();
            flagManager.invalidateRegionFlagCache(this, flag);

            // Para flags críticas, también invalidar jugadores en la región
            if (isCriticalFlag(flag)) {
                for (Player player : getPlayersInside()) {
                    RegionManager.getInstance().refreshPlayerFlags(player);
                }
            }
        } catch (Exception e) {
            // FlagManager podría no estar inicializado durante tests
        }
    }

    /**
     * Verifica si una flag es crítica y requiere aplicación inmediata
     */
    private boolean isCriticalFlag(RegionFlag flag) {
        return flag == RegionFlag.PVP ||
                flag == RegionFlag.INVINCIBLE ||
                flag == RegionFlag.BUILD ||
                flag == RegionFlag.BREAK ||
                flag == RegionFlag.INTERACT ||
                flag == RegionFlag.FLIGHT ||
                flag.affectsCombat() ||
                flag.affectsMovement();
    }

    /**
     * Obtiene el tipo de una flag (ALLOW/DENY/DEFAULT)
     */
    public RegionFlagType getFlagType(RegionFlag flag) {
        return flagStates.getOrDefault(flag, RegionFlagType.DEFAULT);
    }

    /**
     * Obtiene el valor efectivo de una flag considerando defaults
     */
    public boolean getFlagValue(RegionFlag flag) {
        RegionFlagType type = getFlagType(flag);
        return type.getEffectiveValue(flag);
    }

    /**
     * Verifica si una flag está explícitamente configurada (no DEFAULT)
     */
    public boolean isFlagSet(RegionFlag flag) {
        return flagStates.containsKey(flag);
    }

    /**
     * Verifica si una flag está permitida (ALLOW o DEFAULT=true)
     */
    public boolean isFlagAllowed(RegionFlag flag) {
        return getFlagValue(flag);
    }

    /**
     * Verifica si una flag está denegada (DENY o DEFAULT=false)
     */
    public boolean isFlagDenied(RegionFlag flag) {
        return !getFlagValue(flag);
    }

    /**
     * Remueve una flag (vuelve a DEFAULT)
     */
    public void removeFlag(RegionFlag flag) {
        flagStates.remove(flag);
    }

    /**
     * Obtiene todas las flags configuradas (no DEFAULT)
     */
    public Map<RegionFlag, RegionFlagType> getConfiguredFlags() {
        return new HashMap<>(flagStates);
    }

    /**
     * Obtiene todas las flags con sus valores efectivos
     */
    public Map<RegionFlag, Boolean> getAllFlagValues() {
        Map<RegionFlag, Boolean> allFlags = new EnumMap<>(RegionFlag.class);

        // Añadir todas las flags con sus valores default
        for (RegionFlag flag : RegionFlag.values()) {
            allFlags.put(flag, flag.isDefaultValue());
        }

        // Sobrescribir con flags configuradas
        for (Map.Entry<RegionFlag, RegionFlagType> entry : flagStates.entrySet()) {
            allFlags.put(entry.getKey(), entry.getValue().getEffectiveValue(entry.getKey()));
        }

        return allFlags;
    }

    /**
     * Limpia todas las flags configuradas
     */
    public void clearFlags() {
        flagStates.clear();
    }

    /**
     * Aplica configuración de flags desde un mapa
     */
    public void setFlags(Map<RegionFlag, RegionFlagType> flags) {
        flagStates.clear();
        flagStates.putAll(flags);
    }

    // ===== GESTIÓN DE MIEMBROS =====

    /**
     * Verifica si un jugador es owner de la región
     */
    public boolean isOwner(UUID playerId) {
        return owners.contains(playerId);
    }

    /**
     * Verifica si un jugador es miembro de la región
     */
    public boolean isMember(UUID playerId) {
        return members.contains(playerId) || isOwner(playerId);
    }

    /**
     * Añade un owner a la región
     */
    public void addOwner(UUID playerId) {
        owners.add(playerId);
        // Los owners son también miembros
        members.add(playerId);
    }

    /**
     * Añade un miembro a la región
     */
    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    /**
     * Remueve un owner (pero mantiene como miembro si estaba)
     */
    public void removeOwner(UUID playerId) {
        owners.remove(playerId);
    }

    /**
     * Remueve un miembro completamente
     */
    public void removeMember(UUID playerId) {
        members.remove(playerId);
        owners.remove(playerId); // También remover de owners si estaba
    }

    // ===== RESTO DE MÉTODOS ORIGINALES =====

    /**
     * Verifica si una ubicación está dentro de la región
     */
    public boolean contains(Location location) {
        return selection.contains(location);
    }

    /**
     * Verifica si un jugador está dentro de la región
     */
    public boolean contains(Player player) {
        return contains(player.getLocation());
    }

    /**
     * Verifica si un jugador está registrado como dentro de la región
     */
    public boolean isPlayerInside(Player player) {
        return playersInside.contains(player.getUniqueId());
    }

    /**
     * Registra que un jugador entró a la región
     */
    public void addPlayer(Player player) {
        playersInside.add(player.getUniqueId());
    }

    /**
     * Registra que un jugador salió de la región
     */
    public void removePlayer(Player player) {
        playersInside.remove(player.getUniqueId());
    }

    /**
     * Obtiene todos los jugadores dentro de la región
     */
    public Set<Player> getPlayersInside() {
        Set<Player> players = new HashSet<>();
        for (UUID uuid : playersInside) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    /**
     * Limpia jugadores desconectados
     */
    public void cleanupOfflinePlayers() {
        playersInside.removeIf(uuid -> {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            return player == null || !player.isOnline();
        });
    }

    /**
     * Obtiene el mundo de la región
     */
    public World getWorld() {
        return selection.getPos1().getWorld();
    }

    /**
     * Obtiene el punto mínimo de la región
     */
    public Location getMinimumPoint() {
        return selection.getMinimumPoint();
    }

    /**
     * Obtiene el punto máximo de la región
     */
    public Location getMaximumPoint() {
        return selection.getMaximumPoint();
    }

    /**
     * Obtiene el volumen de la región
     */
    public long getVolume() {
        return selection.getVolume();
    }

    /**
     * Obtiene un valor de metadata
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Establece un valor de metadata
     */
    public void setMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    /**
     * Verifica si la región es válida
     */
    public boolean isValid() {
        return selection.isComplete() && id != null && !id.isEmpty();
    }

    /**
     * Obtiene la información de la región en formato legible
     */
    public String getInfo() {
        Location min = getMinimumPoint();
        Location max = getMaximumPoint();

        return String.format(
                "Region [%s] - Plugin: %s, World: %s, " +
                        "Min: %d,%d,%d, Max: %d,%d,%d, " +
                        "Volume: %d, Players: %d, Priority: %s, Flags: %d",
                id, pluginName, getWorld().getName(),
                min.getBlockX(), min.getBlockY(), min.getBlockZ(),
                max.getBlockX(), max.getBlockY(), max.getBlockZ(),
                getVolume(), playersInside.size(), priority, flagStates.size()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Region region = (Region) obj;
        return Objects.equals(id, region.id) && Objects.equals(pluginName, region.pluginName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pluginName);
    }

    @Override
    public String toString() {
        return String.format("Region{id='%s', plugin='%s', priority=%s, flags=%d}",
                id, pluginName, priority, flagStates.size());
    }

    /**
     * Interface para callbacks de región
     */
    @FunctionalInterface
    public interface RegionCallback {
        void execute(Player player, Region region);
    }

    // ===== MÉTODOS DE REGENERACIÓN =====

    /**
     * Guarda el estado actual de la región como schematic para regeneración
     */
    public CompletableFuture<Boolean> saveSchematic() {
        return RegionRegenerationManager.getInstance().saveRegionSchematic(this);
    }

    /**
     * Regenera la región usando su schematic guardado
     */
    public CompletableFuture<Boolean> regenerate() {
        return RegionRegenerationManager.getInstance().regenerateRegion(this);
    }

    /**
     * Limpia solo las entidades de la región sin regenerar bloques
     */
    public CompletableFuture<Integer> cleanEntities() {
        return RegionRegenerationManager.getInstance().cleanRegionEntities(this);
    }

    /**
     * Verifica si la región tiene un schematic guardado
     */
    public boolean hasSchematic() {
        return RegionRegenerationManager.getInstance().hasSchematic(this);
    }

    /**
     * Verifica si la región está siendo regenerada actualmente
     */
    public boolean isRegenerating() {
        return RegionRegenerationManager.getInstance().isRegenerating(this);
    }

    /**
     * Elimina el schematic guardado de la región
     */
    public CompletableFuture<Boolean> deleteSchematic() {
        return RegionRegenerationManager.getInstance().deleteRegionSchematic(this);
    }

    /**
     * Limpia solo los bloques colocados por jugadores en esta región
     */
    public CompletableFuture<Integer> clearPlayerBlocks() {
        return RegionRegenerationManager.getInstance().clearRegionPlayerBlocks(this);
    }

    // ===== MÉTODOS DE RASTREO DE BLOQUES =====

    /**
     * Verifica si un bloque en una ubicación específica fue colocado por un jugador
     */
    public boolean isPlayerPlacedBlock(Location location) {
        String regionKey = pluginName + ":" + id;
        return PlayerBlockTracker.getInstance().isPlayerPlacedBlock(regionKey, location);
    }

    /**
     * Obtiene todos los bloques colocados por jugadores en esta región
     */
    public Set<PlayerBlockTracker.BlockPosition> getPlayerBlocks() {
        String regionKey = pluginName + ":" + id;
        return PlayerBlockTracker.getInstance().getPlayerBlocks(regionKey);
    }

    /**
     * Obtiene el número de bloques colocados por jugadores en esta región
     */
    public int getPlayerBlockCount() {
        return getPlayerBlocks().size();
    }

    /**
     * Habilita el rastreo de bloques de jugador en esta región
     */
    public void enablePlayerBlockTracking() {
        setFlag(RegionFlag.TRACK_PLAYER_BLOCKS, RegionFlagType.ALLOW);
    }

    /**
     * Habilita la construcción protegida (solo bloques de jugador pueden ser destruidos)
     */
    public void enableProtectedBuilding() {
        setFlag(RegionFlag.PLAYER_BUILD_ONLY, RegionFlagType.ALLOW);
        setFlag(RegionFlag.TRACK_PLAYER_BLOCKS, RegionFlagType.ALLOW); // Necesario para saber qué bloques son de jugador
    }

    /**
     * Deshabilita el rastreo de bloques de jugador en esta región
     */
    public void disablePlayerBlockTracking() {
        setFlag(RegionFlag.TRACK_PLAYER_BLOCKS, RegionFlagType.DEFAULT);
        setFlag(RegionFlag.PLAYER_BUILD_ONLY, RegionFlagType.DEFAULT);

        // Limpiar datos existentes
        clearPlayerBlocks();
    }

    /**
     * Verifica si esta región tiene rastreo de bloques de jugador habilitado
     */
    public boolean hasPlayerBlockTracking() {
        return getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS);
    }

    /**
     * Verifica si esta región tiene construcción protegida habilitada
     */
    public boolean hasProtectedBuilding() {
        return getFlagValue(RegionFlag.PLAYER_BUILD_ONLY);
    }

    /**
     * Formatea tiempo en formato legible
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
}
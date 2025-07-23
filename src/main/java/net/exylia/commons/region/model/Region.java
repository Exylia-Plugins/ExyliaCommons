package net.exylia.commons.region.model;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.region.RegionManager;
import net.exylia.commons.region.blocks.AllowedBlocksManager;
import net.exylia.commons.region.blocks.PlayerBlockTracker;
import net.exylia.commons.region.blocks.TemporaryBlocksManager;
import net.exylia.commons.region.flags.FlagManager;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
@Setter
public class Region {
    private final String id;
    private Selection selection;
    private final long createdAt;

    private String displayName;
    private String description;
    private RegionPriority priority;
    private Map<String, Object> metadata;

    private final Map<RegionFlag, RegionFlagType> flagStates;

    private Set<UUID> owners;
    private Set<UUID> members;

    private final Set<UUID> playersInside;

    private RegionCallback onEnter;
    private RegionCallback onExit;
    private RegionCallback onMove;

    public Region(String id, Selection selection) {
        this.id = id;
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

    // ===== GESTIÓN DE FLAGS =====

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

    private void invalidateCacheForFlag(RegionFlag flag) {
        try {
            FlagManager flagManager = FlagManager.getInstance();
            flagManager.invalidateRegionFlagCache(this, flag);

            if (isCriticalFlag(flag)) {
                for (Player player : getPlayersInside()) {
                    RegionManager.getInstance().refreshPlayerFlags(player);
                }
            }
        } catch (Exception ignored) {
        }
    }

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

    public RegionFlagType getFlagType(RegionFlag flag) {
        return flagStates.getOrDefault(flag, RegionFlagType.DEFAULT);
    }

    public boolean getFlagValue(RegionFlag flag) {
        RegionFlagType type = getFlagType(flag);
        return type.getEffectiveValue(flag);
    }

    public boolean isFlagSet(RegionFlag flag) {
        return flagStates.containsKey(flag);
    }
    public boolean isFlagAllowed(RegionFlag flag) {
        return getFlagValue(flag);
    }
    public boolean isFlagDenied(RegionFlag flag) {
        return !getFlagValue(flag);
    }
    public void removeFlag(RegionFlag flag) {
        flagStates.remove(flag);
    }
    public Map<RegionFlag, RegionFlagType> getConfiguredFlags() {
        return new HashMap<>(flagStates);
    }

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

    public void clearFlags() {
        flagStates.clear();
    }
    public void setFlags(Map<RegionFlag, RegionFlagType> flags) {
        flagStates.clear();
        flagStates.putAll(flags);
    }

    // ===== GESTIÓN DE MIEMBROS =====

    public boolean isOwner(UUID playerId) {
        return owners.contains(playerId);
    }
    public boolean isMember(UUID playerId) {
        return members.contains(playerId) || isOwner(playerId);
    }
    public void addOwner(UUID playerId) {
        owners.add(playerId);
        members.add(playerId);
    }
    public void addMember(UUID playerId) {
        members.add(playerId);
    }
    public void removeOwner(UUID playerId) {
        owners.remove(playerId);
    }
    public void removeMember(UUID playerId) {
        members.remove(playerId);
        owners.remove(playerId); // También remover de owners si estaba
    }

    // ===== MÉTODOS =====

    public boolean contains(Location location) {
        return selection.contains(location);
    }
    public boolean contains(Player player) {
        return contains(player.getLocation());
    }
    public boolean isPlayerInside(Player player) {
        return playersInside.contains(player.getUniqueId());
    }
    public void addPlayer(Player player) {
        playersInside.add(player.getUniqueId());
    }
    public void removePlayer(Player player) {
        playersInside.remove(player.getUniqueId());
    }
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
    public void cleanupOfflinePlayers() {
        playersInside.removeIf(uuid -> {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            return player == null || !player.isOnline();
        });
    }
    public World getWorld() {
        return selection.getPos1().getWorld();
    }
    public Location getMinimumPoint() {
        return selection.getMinimumPoint();
    }
    public Location getMaximumPoint() {
        return selection.getMaximumPoint();
    }
    public long getVolume() {
        return selection.getVolume();
    }
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    public void setMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new ConcurrentHashMap<>();
        }
        this.metadata.put(key, value);
    }
    public void setMetadata(Map<String, Object> metadata) {
        if (this.metadata == null) {
            this.metadata = new ConcurrentHashMap<>();
        } else {
            this.metadata.clear();
        }
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
    }
    public boolean hasMetadata() {
        return metadata != null && !metadata.isEmpty();
    }
    public boolean hasMetadata(String key) {
        return metadata != null && metadata.containsKey(key);
    }
    public void removeMetadata(String key) {
        if (metadata != null) {
            metadata.remove(key);
        }
    }
    public void clearMetadata() {
        if (metadata != null) {
            metadata.clear();
        }
    }
    public Map<String, Object> getMetadataCopy() {
        if (metadata == null) return new HashMap<>();
        return new HashMap<>(metadata);
    }
    public boolean isValid() {
        return selection.isComplete() && id != null && !id.isEmpty();
    }
    public String getInfo() {
        Location min = getMinimumPoint();
        Location max = getMaximumPoint();

        return String.format(
                "Region [%s] - World: %s, " +
                        "Min: %d,%d,%d, Max: %d,%d,%d, " +
                        "Volume: %d, Players: %d, Priority: %s, Flags: %d",
                id, getWorld().getName(),
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
        return Objects.equals(id, region.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Region{id='%s', priority=%s, flags=%d}",
                id, priority, flagStates.size());
    }

    @FunctionalInterface
    public interface RegionCallback {
        void execute(Player player, Region region);
    }

    // ===== MÉTODOS DE REGENERACIÓN =====

    public CompletableFuture<Boolean> saveSchematic() {
        return RegionRegenerationManager.getInstance().saveRegionSchematic(this);
    }
    public CompletableFuture<Boolean> regenerate() {
        return RegionRegenerationManager.getInstance().regenerateRegion(this);
    }
    public CompletableFuture<Integer> cleanEntities() {
        return RegionRegenerationManager.getInstance().cleanRegionEntities(this);
    }
    public boolean hasSchematic() {
        return RegionRegenerationManager.getInstance().hasSchematic(this);
    }
    public boolean isRegenerating() {
        return RegionRegenerationManager.getInstance().isRegenerating(this);
    }
    public CompletableFuture<Boolean> deleteSchematic() {
        return RegionRegenerationManager.getInstance().deleteRegionSchematic(this);
    }
    public CompletableFuture<Integer> clearPlayerBlocks() {
        return RegionRegenerationManager.getInstance().clearRegionPlayerBlocks(this);
    }

    // ===== MÉTODOS DE RASTREO DE BLOQUES =====

    public boolean isPlayerPlacedBlock(Location location) {
        return PlayerBlockTracker.getInstance().isPlayerPlacedBlock(id, location);
    }
    public Set<PlayerBlockTracker.BlockPosition> getPlayerBlocks() {
        return PlayerBlockTracker.getInstance().getPlayerBlocks(id);
    }
    public int getPlayerBlockCount() {
        return getPlayerBlocks().size();
    }
    public void enablePlayerBlockTracking() {
        setFlag(RegionFlag.TRACK_PLAYER_BLOCKS, RegionFlagType.ALLOW);
    }
    public void enableProtectedBuilding() {
        setFlag(RegionFlag.PLAYER_BUILD_ONLY, RegionFlagType.ALLOW);
        setFlag(RegionFlag.TRACK_PLAYER_BLOCKS, RegionFlagType.ALLOW);
    }
    public void disablePlayerBlockTracking() {
        setFlag(RegionFlag.TRACK_PLAYER_BLOCKS, RegionFlagType.DEFAULT);
        setFlag(RegionFlag.PLAYER_BUILD_ONLY, RegionFlagType.DEFAULT);

        // Limpiar datos existentes
        clearPlayerBlocks();
    }
    public boolean hasPlayerBlockTracking() {
        return getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS);
    }
    public boolean hasProtectedBuilding() {
        return getFlagValue(RegionFlag.PLAYER_BUILD_ONLY);
    }

    // ===== MÉTODOS PARA REGION_MEMBERS_ONLY =====

    public void enableRegionMembersOnly() {
        setFlag(RegionFlag.REGION_MEMBERS_ONLY, RegionFlagType.ALLOW);
    }
    public void disableRegionMembersOnly() {
        setFlag(RegionFlag.REGION_MEMBERS_ONLY, RegionFlagType.DEFAULT);
    }
    public boolean hasRegionMembersOnly() {
        return getFlagValue(RegionFlag.REGION_MEMBERS_ONLY);
    }
    public boolean canPlayerAffectRegion(Player player, RegionFlag action) {
        if (!hasRegionMembersOnly()) {
            // Si REGION_MEMBERS_ONLY no está activo, usar lógica normal
            return getFlagValue(action);
        }

        // Si REGION_MEMBERS_ONLY está activo y la acción es afectada por él
        if (action.isAffectedByRegionMembersOnly()) {
            // El jugador debe estar dentro de la región
            boolean playerInside = contains(player.getLocation());
            if (!playerInside) {
                return false;
            }
        }

        // Si el jugador está dentro o la acción no es afectada, usar valor normal de la flag
        return getFlagValue(action);
    }

    // ===== MÉTODOS PARA BLOQUES PERMITIDOS =====

    public void enableAllowedBlocksOnly() {
        setFlag(RegionFlag.ALLOWED_BLOCKS_ONLY, RegionFlagType.ALLOW);
    }
    public void disableAllowedBlocksOnly() {
        setFlag(RegionFlag.ALLOWED_BLOCKS_ONLY, RegionFlagType.DEFAULT);
    }
    public boolean hasAllowedBlocksOnly() {
        return getFlagValue(RegionFlag.ALLOWED_BLOCKS_ONLY);
    }
    public void setAllowedBlocks(Set<Material> materials) {
        AllowedBlocksManager.getInstance().setAllowedBlocks(this, materials);
    }
    public void addAllowedMaterials(Set<Material> materials) {
        AllowedBlocksManager.getInstance().addAllowedMaterials(this, materials);
    }
    public void removeAllowedMaterials(Set<Material> materials) {
        AllowedBlocksManager.getInstance().removeAllowedMaterials(this, materials);
    }
    public Set<Material> getAllowedBlocks() {
        return AllowedBlocksManager.getInstance().getAllowedBlocks(this);
    }
    public boolean isMaterialAllowed(Material material) {
        return AllowedBlocksManager.getInstance().isMaterialAllowed(this, material);
    }


    // ===== MÉTODOS PARA BLOQUES TEMPORALES =====

    public void enableTemporaryBlocks() {
        setFlag(RegionFlag.TEMPORARY_BLOCKS, RegionFlagType.ALLOW);
    }
    public void enableTemporaryBlocks(int seconds) {
        setFlag(RegionFlag.TEMPORARY_BLOCKS, RegionFlagType.ALLOW);
        setTemporaryBlocksTime(seconds);
    }
    public void disableTemporaryBlocks() {
        setFlag(RegionFlag.TEMPORARY_BLOCKS, RegionFlagType.DEFAULT);
        removeMetadata("temporary-blocks-seconds");
    }
    public boolean hasTemporaryBlocks() {
        return getFlagValue(RegionFlag.TEMPORARY_BLOCKS);
    }
    public void setTemporaryBlocksTime(int seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("El tiempo debe ser mayor a 0 segundos");
        }
        setMetadata("temporary-blocks-seconds", seconds);
    }
    public int getTemporaryBlocksTime() {
        Integer time = getMetadata("temporary-blocks-seconds", Integer.class);
        return time != null ? time : 30;
    }
    public boolean hasTemporaryBlockAt(Location location) {
        return TemporaryBlocksManager.getInstance().isTemporaryBlock(location);
    }
    public TemporaryBlocksManager.TemporaryBlock getTemporaryBlockAt(Location location) {
        return TemporaryBlocksManager.getInstance().getTemporaryBlock(location);
    }
}
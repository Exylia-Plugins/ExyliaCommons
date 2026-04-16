package net.exylia.commons.v2.region.model;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.region.selection.Selection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class Region {
    private final String id;
    private final Selection selection;
    private final long createdAt;

    private String displayName;
    private String description;
    private RegionPriority priority;
    private final Map<String, Object> metadata;
    private final Map<RegionFlag, RegionFlagState> flags;
    private final Set<UUID> owners;
    private final Set<UUID> members;
    private final Set<UUID> playersInside;

    private RegionCallback onEnter;
    private RegionCallback onExit;

    private Set<Material> allowedBlocks;
    private Set<Material> breakableBlocks;
    private volatile int temporaryBlocksSeconds = 30;

    public Region(String id, Selection selection) {
        this.id = id;
        this.selection = selection;
        this.createdAt = System.currentTimeMillis();
        this.displayName = id;
        this.description = "";
        this.priority = RegionPriority.NORMAL;
        this.metadata = new ConcurrentHashMap<>();
        this.flags = new ConcurrentHashMap<>();
        this.owners = ConcurrentHashMap.newKeySet();
        this.members = ConcurrentHashMap.newKeySet();
        this.playersInside = ConcurrentHashMap.newKeySet();
        this.allowedBlocks = ConcurrentHashMap.newKeySet();
        this.breakableBlocks = ConcurrentHashMap.newKeySet();
    }

    public boolean isValid() {
        return id != null && !id.isEmpty() && selection != null && selection.isComplete();
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

    public Location getCenter() {
        return selection.getCenter();
    }

    public long getVolume() {
        return selection.getVolume();
    }

    public boolean contains(Location location) {
        return selection.contains(location);
    }

    public boolean contains(Player player) {
        return contains(player.getLocation());
    }

    public void setFlag(RegionFlag flag, RegionFlagState state) {
        DebugAPI.logLibDebug(DebugCategory.REGION, "Flag " + flag.name() + " changed to: " +state.name());
        if (state == RegionFlagState.DEFAULT) {
            flags.remove(flag);
        } else {
            flags.put(flag, state);
        }
    }

    public void setFlag(RegionFlag flag, boolean value) {
        setFlag(flag, RegionFlagState.fromBoolean(value));
    }

    public RegionFlagState getFlagState(RegionFlag flag) {
        return flags.getOrDefault(flag, RegionFlagState.DEFAULT);
    }

    public boolean getFlagValue(RegionFlag flag) {
        return getFlagState(flag).getEffectiveValue(flag);
    }

    public boolean isFlagSet(RegionFlag flag) {
        return flags.containsKey(flag);
    }

    public void removeFlag(RegionFlag flag) {
        flags.remove(flag);
    }

    public void clearFlags() {
        flags.clear();
    }

    public Map<RegionFlag, RegionFlagState> getConfiguredFlags() {
        if (flags.isEmpty()) {
            return new EnumMap<>(RegionFlag.class);
        }
        return new EnumMap<>(flags);
    }

    public Map<RegionFlag, Boolean> getAllFlagValues() {
        Map<RegionFlag, Boolean> allFlags = new EnumMap<>(RegionFlag.class);
        for (RegionFlag flag : RegionFlag.values()) {
            allFlags.put(flag, getFlagValue(flag));
        }
        return allFlags;
    }

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
        owners.remove(playerId);
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

    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public void removeMetadata(String key) {
        metadata.remove(key);
    }

    public void clearMetadata() {
        metadata.clear();
    }

    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    public Map<String, Object> getMetadataCopy() {
        return new HashMap<>(metadata);
    }

    public void setAllowedBlocks(Set<Material> materials) {
        this.allowedBlocks = ConcurrentHashMap.newKeySet();
        if (materials != null) {
            this.allowedBlocks.addAll(materials);
        }
    }

    public void addAllowedMaterial(Material material) {
        allowedBlocks.add(material);
    }

    public void removeAllowedMaterial(Material material) {
        allowedBlocks.remove(material);
    }

    public boolean isMaterialAllowed(Material material) {
        return allowedBlocks.contains(material);
    }

    public Set<Material> getAllowedBlocks() {
        return new HashSet<>(allowedBlocks);
    }

    public void setBreakableBlocks(Set<Material> materials) {
        this.breakableBlocks = ConcurrentHashMap.newKeySet();
        if (materials != null) {
            this.breakableBlocks.addAll(materials);
        }
    }

    public void addBreakableMaterial(Material material) {
        breakableBlocks.add(material);
    }

    public void removeBreakableMaterial(Material material) {
        breakableBlocks.remove(material);
    }

    public boolean isBreakableMaterial(Material material) {
        return breakableBlocks.contains(material);
    }

    public Set<Material> getBreakableBlocks() {
        return new HashSet<>(breakableBlocks);
    }

    public CompletableFuture<Boolean> saveSchematic() {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().saveSchematic(this);
    }

    public CompletableFuture<Boolean> saveSchematic(String schematicName) {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().saveSchematic(this, schematicName);
    }

    public CompletableFuture<Boolean> saveSchematic(net.exylia.commons.v2.region.schematic.SchematicManager.SchematicType type) {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().saveSchematic(this, this.id, type);
    }

    public CompletableFuture<Boolean> saveSchematic(String schematicName, net.exylia.commons.v2.region.schematic.SchematicManager.SchematicType type) {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().saveSchematic(this, schematicName, type);
    }

    public CompletableFuture<Boolean> regenerate() {
        return net.exylia.commons.v2.region.RegionManager.getInstance().restoreRegion(this);
    }

    public CompletableFuture<Boolean> regenerate(String schematicName) {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().regenerateRegion(this, schematicName);
    }

    public CompletableFuture<Boolean> regenerate(net.exylia.commons.v2.region.schematic.SchematicManager.SchematicType type) {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().regenerateRegion(this, this.id, type);
    }

    public CompletableFuture<Boolean> regenerate(String schematicName, net.exylia.commons.v2.region.schematic.SchematicManager.SchematicType type) {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().regenerateRegion(this, schematicName, type);
    }

    public CompletableFuture<Boolean> regenerate(boolean teleportToAir) {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().regenerateRegion(this, this.id, null, teleportToAir);
    }

    public CompletableFuture<Boolean> regenerate(String schematicName, boolean teleportToAir) {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().regenerateRegion(this, schematicName, null, teleportToAir);
    }

    public CompletableFuture<Boolean> regenerate(String schematicName, net.exylia.commons.v2.region.schematic.SchematicManager.SchematicType type, boolean teleportToAir) {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().regenerateRegion(this, schematicName, type, teleportToAir);
    }

    public boolean hasSchematic() {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().schematicExists(this.id);
    }

    public boolean hasSchematic(String schematicName) {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().schematicExists(schematicName);
    }

    public boolean deleteSchematic() {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().deleteSchematic(this.id);
    }

    public boolean deleteSchematic(String schematicName) {
        return net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().deleteSchematic(schematicName);
    }

    public CompletableFuture<Boolean> deleteSchematicAsync() {
        return CompletableFuture.supplyAsync(() ->
            net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().deleteSchematic(this.id)
        );
    }

    public CompletableFuture<Boolean> deleteSchematicAsync(String schematicName) {
        return CompletableFuture.supplyAsync(() ->
            net.exylia.commons.v2.region.schematic.SchematicManager.getInstance().deleteSchematic(schematicName)
        );
    }

    public CompletableFuture<Region> cloneTo(Location targetCenter) {
        return CompletableFuture.supplyAsync(() -> {
            Location offset = getCenter().clone().subtract(targetCenter);
            Location newPos1 = getMinimumPoint().clone().subtract(offset);
            Location newPos2 = getMaximumPoint().clone().subtract(offset);

            Selection newSelection = Selection.of(newPos1, newPos2);
            Region clonedRegion = new Region(id + "_clone_" + System.currentTimeMillis(), newSelection);

            clonedRegion.setDisplayName(displayName + " (Clone)");
            clonedRegion.setDescription(description);
            clonedRegion.setPriority(priority);
            clonedRegion.setTemporaryBlocksSeconds(temporaryBlocksSeconds);

            flags.forEach(clonedRegion::setFlag);
            owners.forEach(clonedRegion::addOwner);
            members.forEach(clonedRegion::addMember);
            allowedBlocks.forEach(clonedRegion::addAllowedMaterial);
            breakableBlocks.forEach(clonedRegion::addBreakableMaterial);
            metadata.forEach(clonedRegion::setMetadata);

            return clonedRegion;
        });
    }

    public String getInfo() {
        Location min = getMinimumPoint();
        Location max = getMaximumPoint();
        return String.format(
            "RegionV2[%s] - World: %s, Min: %d,%d,%d, Max: %d,%d,%d, Volume: %d, Players: %d, Priority: %s, Flags: %d",
            id, getWorld().getName(),
            min.getBlockX(), min.getBlockY(), min.getBlockZ(),
            max.getBlockX(), max.getBlockY(), max.getBlockZ(),
            getVolume(), playersInside.size(), priority, flags.size()
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
        return String.format("RegionV2{id='%s', priority=%s, flags=%d}", id, priority, flags.size());
    }
}

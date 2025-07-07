package net.exylia.commons.region.model;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa una región en el mundo
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
    @Setter
    private Set<RegionFlag> flags;

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
        this.flags = EnumSet.noneOf(RegionFlag.class);
        this.playersInside = ConcurrentHashMap.newKeySet();
    }

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
     * Verifica si la región tiene una flag específica
     */
    public boolean hasFlag(RegionFlag flag) {
        return flags.contains(flag);
    }

    /**
     * Añade una flag a la región
     */
    public void addFlag(RegionFlag flag) {
        this.flags.add(flag);
    }

    /**
     * Remueve una flag de la región
     */
    public void removeFlag(RegionFlag flag) {
        this.flags.remove(flag);
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
                        "Volume: %d, Players: %d, Priority: %s",
                id, pluginName, getWorld().getName(),
                min.getBlockX(), min.getBlockY(), min.getBlockZ(),
                max.getBlockX(), max.getBlockY(), max.getBlockZ(),
                getVolume(), playersInside.size(), priority
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
        return String.format("Region{id='%s', plugin='%s', priority=%s}", id, pluginName, priority);
    }

    /**
     * Interface para callbacks de región
     */
    @FunctionalInterface
    public interface RegionCallback {
        void execute(Player player, Region region);
    }
}
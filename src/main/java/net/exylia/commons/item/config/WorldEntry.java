package net.exylia.commons.item.config;

import org.bukkit.World;

import java.util.Objects;

/**
 * Representa una entrada de mundo para configuraciones de items
 * Formato: "worldName"
 */
public class WorldEntry {

    private final String worldName;

    /**
     * Constructor privado
     */
    private WorldEntry(String worldName) {
        this.worldName = worldName;
    }

    /**
     * Crea una WorldEntry desde un string
     * @param entry String con el nombre del mundo
     * @return WorldEntry parseada
     */
    public static WorldEntry parse(String entry) {
        if (entry == null || entry.trim().isEmpty()) {
            throw new IllegalArgumentException("World entry cannot be null or empty");
        }

        String trimmed = entry.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("World name cannot be empty: " + entry);
        }

        return new WorldEntry(trimmed);
    }

    /**
     * Verifica si esta entrada coincide con un mundo específico
     * @param world Mundo a verificar
     * @return true si coincide
     */
    public boolean matches(World world) {
        return world != null && this.worldName.equalsIgnoreCase(world.getName());
    }

    /**
     * Verifica si esta entrada coincide con el nombre de mundo
     * @param worldName Nombre del mundo a verificar
     * @return true si el nombre del mundo coincide
     */
    public boolean matchesWorldName(String worldName) {
        return this.worldName.equalsIgnoreCase(worldName);
    }

    // ===== GETTERS =====

    /**
     * Obtiene el nombre del mundo
     * @return Nombre del mundo
     */
    public String getWorldName() {
        return worldName;
    }

    // ===== MÉTODOS DE UTILIDAD =====

    /**
     * Convierte la entrada de vuelta a string
     * @return String en formato original
     */
    public String toConfigString() {
        return worldName;
    }

    @Override
    public String toString() {
        return toConfigString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        WorldEntry that = (WorldEntry) obj;
        return Objects.equals(worldName, that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName);
    }

    // ===== MÉTODOS DE CONVENIENCIA ESTÁTICOS =====

    /**
     * Crea una entrada para un mundo específico
     * @param worldName Nombre del mundo
     * @return WorldEntry para el mundo especificado
     */
    public static WorldEntry forWorld(String worldName) {
        return new WorldEntry(worldName);
    }
}
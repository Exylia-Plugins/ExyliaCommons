package net.exylia.commons.item.config;

import org.bukkit.World;

import java.util.Objects;

/**
 * Representa una entrada de región con soporte para mundo específico
 * Formato: "regionName" o "regionName|worldName"
 */
public class RegionEntry {

    private final String regionName;
    private final String worldName;
    private final boolean worldSpecific;

    /**
     * Constructor privado
     */
    private RegionEntry(String regionName, String worldName, boolean worldSpecific) {
        this.regionName = regionName;
        this.worldName = worldName;
        this.worldSpecific = worldSpecific;
    }

    /**
     * Crea una RegionEntry desde un string
     * @param entry String en formato "regionName" o "regionName|worldName"
     * @return RegionEntry parseada
     */
    public static RegionEntry parse(String entry) {
        if (entry == null || entry.trim().isEmpty()) {
            throw new IllegalArgumentException("Region entry cannot be null or empty");
        }

        String trimmed = entry.trim();

        if (trimmed.contains("|")) {
            String[] parts = trimmed.split("\\|", 2);
            String regionName = parts[0].trim();
            String worldName = parts[1].trim();

            if (regionName.isEmpty()) {
                throw new IllegalArgumentException("Region name cannot be empty: " + entry);
            }
            if (worldName.isEmpty()) {
                throw new IllegalArgumentException("World name cannot be empty: " + entry);
            }

            return new RegionEntry(regionName, worldName, true);
        } else {
            return new RegionEntry(trimmed, null, false);
        }
    }

    /**
     * Verifica si esta entrada coincide con una región en un mundo específico
     * @param regionName Nombre de la región a verificar
     * @param world Mundo donde está la región
     * @return true si coincide
     */
    public boolean matches(String regionName, World world) {
        if (!this.regionName.equalsIgnoreCase(regionName)) {
            return false;
        }

        // Si no es específico de mundo, coincide en cualquier mundo
        if (!worldSpecific) {
            return true;
        }

        // Si es específico de mundo, verificar que el mundo coincida
        return world != null && this.worldName.equalsIgnoreCase(world.getName());
    }

    /**
     * Verifica si esta entrada coincide solo con el nombre de región (sin verificar mundo)
     * @param regionName Nombre de la región a verificar
     * @return true si el nombre de región coincide
     */
    public boolean matchesRegionName(String regionName) {
        return this.regionName.equalsIgnoreCase(regionName);
    }

    // ===== GETTERS =====

    /**
     * Obtiene el nombre de la región
     * @return Nombre de la región
     */
    public String getRegionName() {
        return regionName;
    }

    /**
     * Obtiene el nombre del mundo (puede ser null si no es específico de mundo)
     * @return Nombre del mundo o null
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * Verifica si esta entrada es específica de un mundo
     * @return true si es específica de mundo
     */
    public boolean isWorldSpecific() {
        return worldSpecific;
    }

    // ===== MÉTODOS DE UTILIDAD =====

    /**
     * Convierte la entrada de vuelta a string
     * @return String en formato original
     */
    public String toConfigString() {
        if (worldSpecific) {
            return regionName + "|" + worldName;
        } else {
            return regionName;
        }
    }

    @Override
    public String toString() {
        return toConfigString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        RegionEntry that = (RegionEntry) obj;
        return worldSpecific == that.worldSpecific &&
                Objects.equals(regionName, that.regionName) &&
                Objects.equals(worldName, that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(regionName, worldName, worldSpecific);
    }

    // ===== MÉTODOS DE CONVENIENCIA ESTÁTICOS =====

    /**
     * Crea una entrada para cualquier mundo
     * @param regionName Nombre de la región
     * @return RegionEntry para cualquier mundo
     */
    public static RegionEntry forAnyWorld(String regionName) {
        return new RegionEntry(regionName, null, false);
    }

    /**
     * Crea una entrada para un mundo específico
     * @param regionName Nombre de la región
     * @param worldName Nombre del mundo
     * @return RegionEntry específica de mundo
     */
    public static RegionEntry forWorld(String regionName, String worldName) {
        return new RegionEntry(regionName, worldName, true);
    }
}
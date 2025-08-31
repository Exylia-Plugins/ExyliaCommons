package net.exylia.commons.region.blocks;

import lombok.Getter;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manager para manejar listas de bloques permitidos en regiones específicas
 */
public class AllowedBlocksManager {
    private static AllowedBlocksManager instance;

    private final JavaPlugin plugin;

    private AllowedBlocksManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new AllowedBlocksManager(plugin);
        }
    }

    public static AllowedBlocksManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AllowedBlocksManager no ha sido inicializado");
        }
        return instance;
    }

    /**
     * Verifica si un material está permitido in una región
     */
    public boolean isMaterialAllowed(Region region, Material material) {
        if (!region.getFlagValue(RegionFlag.ALLOWED_BLOCKS_ONLY)) {
            return true; // Si la flag no está activa, todos los bloques están permitidos
        }

        Set<Material> allowedBlocks = getAllowedBlocks(region);
        return allowedBlocks.contains(material);
    }

    /**
     * Obtiene la lista de bloques permitidos para una región
     */
    public Set<Material> getAllowedBlocks(Region region) {
        // Intentar obtener lista personalizada desde metadata
        @SuppressWarnings("unchecked")
        Set<String> customBlocks = region.getMetadata("allowed-blocks", Set.class);

        if (customBlocks != null) {
            return customBlocks.stream()
                    .map(this::parseMaterial)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    /**
     * Establece una lista personalizada de bloques permitidos
     */
    public void setAllowedBlocks(Region region, Set<Material> materials) {
        Set<String> materialNames = materials.stream()
                .map(Material::name)
                .collect(Collectors.toSet());

        region.setMetadata("allowed-blocks", materialNames);

        DebugUtils.logInternalInfo(String.format(
                "Lista personalizada de bloques establecida para región %s: %d materiales",
                region.getId(), materials.size()
        ));
    }

    /**
     * Añade materiales adicionales a la lista permitida
     */
    public void addAllowedMaterials(Region region, Set<Material> materialsToAdd) {
        Set<Material> currentAllowed = getAllowedBlocks(region);
        currentAllowed.addAll(materialsToAdd);
        setAllowedBlocks(region, currentAllowed);

        DebugUtils.logInternalInfo(String.format(
                "Añadidos %d materiales a la región %s",
                materialsToAdd.size(), region.getId()
        ));
    }

    /**
     * Remueve materiales de la lista permitida
     */
    public void removeAllowedMaterials(Region region, Set<Material> materialsToRemove) {
        Set<Material> currentAllowed = getAllowedBlocks(region);
        currentAllowed.removeAll(materialsToRemove);
        setAllowedBlocks(region, currentAllowed);

        DebugUtils.logInternalInfo(String.format(
                "Removidos %d materiales de la región %s",
                materialsToRemove.size(), region.getId()
        ));
    }


    /**
     * Parsea un string a Material de forma segura
     */
    private Material parseMaterial(String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Material desconocido: " + materialName);
            return null;
        }
    }

    /**
     * Verifica si un material es un bloque válido para colocar
     */
    public boolean isValidPlaceableBlock(Material material) {
        return material.isBlock() &&
                !material.name().contains("LEGACY") &&
                material != Material.AIR &&
                material != Material.VOID_AIR &&
                material != Material.CAVE_AIR;
    }

    /**
     * Clase para información de bloques permitidos
     */
    @Getter
    public static class AllowedBlocksInfo {
        private final boolean enabled;
        private final String configuration;
        private final Set<Material> allowedBlocks;
        private final int totalAllowed;

        public AllowedBlocksInfo(boolean enabled, String configuration, Set<Material> allowedBlocks) {
            this.enabled = enabled;
            this.configuration = configuration;
            this.allowedBlocks = new HashSet<>(allowedBlocks);
            this.totalAllowed = allowedBlocks.size();
        }

        @Override
        public String toString() {
            return String.format("AllowedBlocksInfo{enabled=%b, config='%s', total=%d}",
                    enabled, configuration, totalAllowed);
        }
    }
}
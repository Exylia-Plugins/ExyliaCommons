package net.exylia.commons.region.schematic;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class MaterialRegistry {
    private static final Map<Material, Short> MATERIAL_TO_ID = new HashMap<>();
    private static final Map<Short, Material> ID_TO_MATERIAL = new HashMap<>();

    static {
        short id = 0;
        for (Material material : Material.values()) {
            if (!material.isLegacy()) {
                MATERIAL_TO_ID.put(material, id);
                ID_TO_MATERIAL.put(id, material);
                id++;
            }
        }
    }

    public static short getId(Material material) {
        return MATERIAL_TO_ID.getOrDefault(material, (short) 0);
    }

    public static Material getMaterial(short id) {
        return ID_TO_MATERIAL.getOrDefault(id, Material.AIR);
    }
}

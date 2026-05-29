package net.exylia.commons.v2.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

public final class BiomeCompat {

    private BiomeCompat() {}

    public static String getBiomeName(Block block) {
        try {
            Object biome = block.getClass().getMethod("getBiome").invoke(block);
            if (biome == null) return "";
            Object nsKey = biome.getClass().getMethod("getKey").invoke(biome);
            return (String) nsKey.getClass().getMethod("getKey").invoke(nsKey);
        } catch (Exception ignored) {
            return "";
        }
    }

    public static Biome getByName(String name) {
        if (name == null || name.isBlank()) return fallback();
        String key = name.contains(":") ? name.split(":")[1].toLowerCase() : name.toLowerCase();
        return resolve(key);
    }

    public static Biome getByKey(String namespacedKey) {
        if (namespacedKey == null || namespacedKey.isBlank()) return fallback();
        String key = namespacedKey.toLowerCase();
        if (key.contains(":")) key = key.split(":")[1];
        return resolve(key);
    }

    private static Biome resolve(String key) {
        try {
            Biome biome = Registry.BIOME.get(NamespacedKey.minecraft(key));
            if (biome != null) return biome;
        } catch (Exception ignored) {}

        try {
            for (Biome biome : Registry.BIOME) {
                if (biome.getKey().getKey().equals(key)) return biome;
            }
        } catch (Exception ignored) {}

        return fallback();
    }

    private static Biome fallback() {
        Biome plains = Registry.BIOME.get(NamespacedKey.minecraft("plains"));
        return plains != null ? plains : Registry.BIOME.iterator().next();
    }
}

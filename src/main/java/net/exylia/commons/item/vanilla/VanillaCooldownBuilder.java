package net.exylia.commons.item.vanilla;

import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Builder para configurar cooldowns de items vanilla de forma fácil
 * ACTUALIZADO: Soporte para display-name
 */
public class VanillaCooldownBuilder {

    private final VanillaItemCooldownManager manager;

    public VanillaCooldownBuilder() {
        this.manager = VanillaItemCooldownManager.getInstance();
    }

    // ===== MÉTODOS FLUIDOS =====

    /**
     * Configura un item individual
     */
    public VanillaCooldownBuilder addItem(Material material, double cooldownSeconds) {
        manager.registerCooldown(material, cooldownSeconds);
        return this;
    }

    /**
     * Configura un item con trigger específico
     */
    public VanillaCooldownBuilder addItem(Material material, double cooldownSeconds, VanillaTriggerType trigger) {
        manager.registerCooldown(material, cooldownSeconds, trigger);
        return this;
    }

    /**
     * NUEVO: Configura un item con display name personalizado
     */
    public VanillaCooldownBuilder addItem(Material material, double cooldownSeconds, String displayName) {
        manager.registerCooldown(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, displayName);
        return this;
    }

    /**
     * NUEVO: Configura un item con trigger y display name
     */
    public VanillaCooldownBuilder addItem(Material material, double cooldownSeconds, VanillaTriggerType trigger, String displayName) {
        manager.registerCooldown(material, cooldownSeconds, trigger, displayName);
        return this;
    }

    /**
     * Configura múltiples items con el mismo cooldown
     */
    public VanillaCooldownBuilder addItems(List<Material> materials, double cooldownSeconds) {
        manager.registerCooldowns(materials, cooldownSeconds);
        return this;
    }

    /**
     * Configura múltiples items con el mismo cooldown y trigger
     */
    public VanillaCooldownBuilder addItems(List<Material> materials, double cooldownSeconds, VanillaTriggerType trigger) {
        manager.registerCooldowns(materials, cooldownSeconds, trigger);
        return this;
    }

    /**
     * NUEVO: Configura múltiples items con cooldown y display name
     */
    public VanillaCooldownBuilder addItems(List<Material> materials, double cooldownSeconds, String displayName) {
        manager.registerCooldowns(materials, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, displayName);
        return this;
    }

    /**
     * NUEVO: Configura múltiples items con cooldown, trigger y display name
     */
    public VanillaCooldownBuilder addItems(List<Material> materials, double cooldownSeconds, VanillaTriggerType trigger, String displayName) {
        manager.registerCooldowns(materials, cooldownSeconds, trigger, displayName);
        return this;
    }

    /**
     * Configura desde un Map
     */
    public VanillaCooldownBuilder fromMap(Map<Material, Double> cooldowns) {
        manager.registerCooldowns(cooldowns);
        return this;
    }

    // ===== CONFIGURACIÓN DESDE ARCHIVO =====

    /**
     * Carga configuraciones desde ConfigurationSection
     * ACTUALIZADO: Soporte para display-name
     */
    public VanillaCooldownBuilder fromConfig(ConfigurationSection config) {
        if (config == null) return this;

        for (String materialName : config.getKeys(false)) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());

                if (config.isConfigurationSection(materialName)) {
                    // Formato detallado
                    ConfigurationSection itemConfig = config.getConfigurationSection(materialName);
                    double cooldown = itemConfig.getDouble("cooldown", 0.0);
                    String triggerString = itemConfig.getString("trigger", "auto-detect");
                    String displayName = itemConfig.getString("display-name"); // NUEVO
                    VanillaTriggerType trigger = VanillaTriggerType.fromString(triggerString);

                    if (cooldown > 0) {
                        addItem(material, cooldown, trigger, displayName);
                    }
                } else {
                    // Formato simple: material: cooldown
                    double cooldown = config.getDouble(materialName, 0.0);
                    if (cooldown > 0) {
                        addItem(material, cooldown);
                    }
                }
            } catch (IllegalArgumentException e) {
                DebugUtils.logError("Invalid material in vanilla cooldown config: " + materialName);
            }
        }

        return this;
    }

    // ===== MÉTODOS DE CONFIGURACIÓN AVANZADA =====

    /**
     * Configura todos los items de un tipo específico
     */
    public VanillaCooldownBuilder itemsByType(ItemType type, double cooldownSeconds) {
        List<Material> materials = getItemsByType(type);
        return addItems(materials, cooldownSeconds);
    }

    /**
     * NUEVO: Configura todos los items de un tipo con display name
     */
    public VanillaCooldownBuilder itemsByType(ItemType type, double cooldownSeconds, String displayName) {
        List<Material> materials = getItemsByType(type);
        return addItems(materials, cooldownSeconds, displayName);
    }

    /**
     * Remueve la configuración de un item
     */
    public VanillaCooldownBuilder remove(Material material) {
        manager.unregisterCooldown(material);
        return this;
    }

    /**
     * Remueve múltiples configuraciones
     */
    public VanillaCooldownBuilder removeItems(List<Material> materials) {
        materials.forEach(manager::unregisterCooldown);
        return this;
    }

    /**
     * Limpia todas las configuraciones
     */
    public VanillaCooldownBuilder clear() {
        manager.clearAllCooldowns();
        return this;
    }

    // ===== ENUMS Y UTILIDADES =====

    public enum ItemType {
        FOOD,
        WEAPONS,
        TOOLS,
        POTIONS,
        PROJECTILES,
        ARMOR,
        BLOCKS
    }

    /**
     * Obtiene materials por tipo
     */
    private List<Material> getItemsByType(ItemType type) {
        return switch (type) {
            case FOOD -> Arrays.asList(
                    Material.APPLE, Material.BREAD, Material.CARROT, Material.POTATO,
                    Material.BAKED_POTATO, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE,
                    Material.GOLDEN_CARROT, Material.MELON_SLICE, Material.SWEET_BERRIES,
                    Material.CHORUS_FRUIT, Material.DRIED_KELP, Material.COOKED_BEEF,
                    Material.COOKED_CHICKEN, Material.COOKED_COD, Material.COOKED_MUTTON,
                    Material.COOKED_PORKCHOP, Material.COOKED_RABBIT, Material.COOKED_SALMON,
                    Material.BEEF, Material.CHICKEN, Material.COD, Material.MUTTON,
                    Material.PORKCHOP, Material.RABBIT, Material.SALMON, Material.TROPICAL_FISH,
                    Material.PUFFERFISH, Material.SPIDER_EYE, Material.ROTTEN_FLESH,
                    Material.MUSHROOM_STEW, Material.RABBIT_STEW, Material.BEETROOT_SOUP,
                    Material.SUSPICIOUS_STEW, Material.HONEY_BOTTLE, Material.CAKE
            );

            case WEAPONS -> Arrays.asList(
                    Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                    Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
                    Material.BOW, Material.CROSSBOW, Material.TRIDENT
            );

            case TOOLS -> Arrays.asList(
                    Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                    Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                    Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                    Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                    Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                    Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
                    Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
                    Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE,
                    Material.SHEARS, Material.FISHING_ROD, Material.FLINT_AND_STEEL
            );

            case POTIONS -> Arrays.asList(
                    Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION,
                    Material.MILK_BUCKET, Material.HONEY_BOTTLE
            );

            case PROJECTILES -> Arrays.asList(
                    Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW,
                    Material.SNOWBALL, Material.EGG, Material.ENDER_PEARL,
                    Material.FIREWORK_ROCKET, Material.FIRE_CHARGE
            );

            case ARMOR -> Arrays.asList(
                    Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
                    Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
                    Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE,
                    Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
                    Material.IRON_HELMET, Material.IRON_CHESTPLATE,
                    Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                    Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE,
                    Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
                    Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                    Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                    Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                    Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                    Material.TURTLE_HELMET, Material.SHIELD, Material.TOTEM_OF_UNDYING
            );

            case BLOCKS -> Arrays.asList(
                    Material.STONE, Material.DIRT, Material.GRASS_BLOCK, Material.COBBLESTONE,
                    Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
                    Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
                    Material.CRIMSON_PLANKS, Material.WARPED_PLANKS, Material.TNT
            );
        };
    }

    // ===== MÉTODOS ESTÁTICOS DE CONVENIENCIA =====

    /**
     * Crea un nuevo builder
     */
    public static VanillaCooldownBuilder create() {
        return new VanillaCooldownBuilder();
    }

    /**
     * Configura rápidamente un item
     */
    public static VanillaCooldownBuilder quickSetup(Material material, double cooldown) {
        return new VanillaCooldownBuilder().addItem(material, cooldown);
    }

    /**
     * Configura rápidamente un item con trigger
     */
    public static VanillaCooldownBuilder quickSetup(Material material, double cooldown, VanillaTriggerType trigger) {
        return new VanillaCooldownBuilder().addItem(material, cooldown, trigger);
    }

    /**
     * NUEVO: Configura rápidamente un item con display name
     */
    public static VanillaCooldownBuilder quickSetup(Material material, double cooldown, String displayName) {
        return new VanillaCooldownBuilder().addItem(material, cooldown, displayName);
    }

    /**
     * NUEVO: Configura rápidamente un item con trigger y display name
     */
    public static VanillaCooldownBuilder quickSetup(Material material, double cooldown, VanillaTriggerType trigger, String displayName) {
        return new VanillaCooldownBuilder().addItem(material, cooldown, trigger, displayName);
    }

    /**
     * Carga desde configuración
     */
    public static VanillaCooldownBuilder loadFromConfig(ConfigurationSection config) {
        return new VanillaCooldownBuilder().fromConfig(config);
    }
}
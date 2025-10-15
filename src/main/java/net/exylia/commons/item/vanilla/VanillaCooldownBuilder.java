package net.exylia.commons.item.vanilla;

import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class VanillaCooldownBuilder {

    private final VanillaItemCooldownManager manager;

    public VanillaCooldownBuilder() {
        this.manager = VanillaItemCooldownManager.getInstance();
    }

    public VanillaCooldownBuilder addItem(Material material, double cooldownSeconds) {
        manager.registerCooldown(material, cooldownSeconds);
        return this;
    }

    public VanillaCooldownBuilder addItem(Material material, double cooldownSeconds, VanillaTriggerType trigger) {
        manager.registerCooldown(material, cooldownSeconds, trigger);
        return this;
    }

    public VanillaCooldownBuilder addItem(Material material, double cooldownSeconds, String displayName) {
        manager.registerCooldown(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, displayName);
        return this;
    }

    public VanillaCooldownBuilder addItem(Material material, double cooldownSeconds, VanillaTriggerType trigger, String displayName) {
        manager.registerCooldown(material, cooldownSeconds, trigger, displayName);
        return this;
    }

    public VanillaCooldownBuilder addItem(Material material, double cooldownSeconds, Integer maxUsesPerRegion) {
        manager.registerCooldown(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, null, maxUsesPerRegion);
        return this;
    }

    public VanillaCooldownBuilder addItem(Material material, double cooldownSeconds, VanillaTriggerType trigger, String displayName, Integer maxUsesPerRegion) {
        manager.registerCooldown(material, cooldownSeconds, trigger, displayName, maxUsesPerRegion);
        return this;
    }

    public VanillaCooldownBuilder addItems(List<Material> materials, double cooldownSeconds) {
        manager.registerCooldowns(materials, cooldownSeconds);
        return this;
    }

    public VanillaCooldownBuilder addItems(List<Material> materials, double cooldownSeconds, VanillaTriggerType trigger) {
        manager.registerCooldowns(materials, cooldownSeconds, trigger);
        return this;
    }

    public VanillaCooldownBuilder addItems(List<Material> materials, double cooldownSeconds, String displayName) {
        manager.registerCooldowns(materials, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, displayName);
        return this;
    }

    public VanillaCooldownBuilder addItems(List<Material> materials, double cooldownSeconds, VanillaTriggerType trigger, String displayName) {
        manager.registerCooldowns(materials, cooldownSeconds, trigger, displayName);
        return this;
    }

    public VanillaCooldownBuilder fromMap(Map<Material, Double> cooldowns) {
        manager.registerCooldowns(cooldowns);
        return this;
    }

    public VanillaCooldownBuilder fromConfig(ConfigurationSection config) {
        if (config == null) return this;

        for (String materialName : config.getKeys(false)) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());

                if (config.isConfigurationSection(materialName)) {
                     
                    ConfigurationSection itemConfig = config.getConfigurationSection(materialName);
                    double cooldown = itemConfig.getDouble("cooldown", 0.0);
                    String triggerString = itemConfig.getString("trigger", "auto-detect");
                    String displayName = itemConfig.getString("display-name");
                    Integer regionLimit = itemConfig.isSet("region-limit") ? itemConfig.getInt("region-limit") : null;
                    VanillaTriggerType trigger = VanillaTriggerType.fromString(triggerString);

                    Map<String, VanillaRegionConfig> regionConfigs = new HashMap<>();
                    if (itemConfig.isConfigurationSection("regions")) {
                        ConfigurationSection regionsSection = itemConfig.getConfigurationSection("regions");
                        for (String regionName : regionsSection.getKeys(false)) {
                            ConfigurationSection regionSection = regionsSection.getConfigurationSection(regionName);
                            if (regionSection != null) {
                                double regionCooldown = regionSection.getDouble("cooldown", cooldown);
                                Integer maxUses = regionSection.isSet("max-uses") ? regionSection.getInt("max-uses") : null;
                                regionConfigs.put(regionName, new VanillaRegionConfig(regionCooldown, maxUses));
                                DebugUtils.logInternalDebug("Loaded region cooldown: " + regionName + " -> " + regionCooldown + "s for " + materialName);
                            }
                        }
                    }

                    Integer worldLimit = itemConfig.isSet("world-limit") ? itemConfig.getInt("world-limit") : null;
                    Map<String, VanillaWorldConfig> worldConfigs = new HashMap<>();
                    
                    ConfigurationSection worldSection = null;
                    if (itemConfig.isConfigurationSection("world")) {
                        worldSection = itemConfig.getConfigurationSection("world");
                    } else if (itemConfig.isConfigurationSection("worlds")) {
                        worldSection = itemConfig.getConfigurationSection("worlds");
                    }
                    
                    if (worldSection != null) {
                        
                        if (worldSection.isConfigurationSection("cooldowns")) {
                            ConfigurationSection cooldownsSection = worldSection.getConfigurationSection("cooldowns");
                            for (String worldName : cooldownsSection.getKeys(false)) {
                                double worldCooldown = cooldownsSection.getDouble(worldName, cooldown);
                                worldConfigs.put(worldName, new VanillaWorldConfig(worldCooldown, null));
                                DebugUtils.logInternalDebug("Loaded world cooldown: " + worldName + " -> " + worldCooldown + "s for " + materialName);
                            }
                        }
                        
                        if (worldSection.isConfigurationSection("limits")) {
                            ConfigurationSection limitsSection = worldSection.getConfigurationSection("limits");
                            for (String worldName : limitsSection.getKeys(false)) {
                                Integer maxUses = limitsSection.getInt(worldName);
                                VanillaWorldConfig existingConfig = worldConfigs.get(worldName);
                                if (existingConfig != null) {
                                    worldConfigs.put(worldName, new VanillaWorldConfig(existingConfig.getCooldown(), maxUses));
                                } else {
                                    worldConfigs.put(worldName, new VanillaWorldConfig(cooldown, maxUses));
                                }
                            }
                        }
                        
                        for (String key : worldSection.getKeys(false)) {
                            if (!key.equals("cooldowns") && !key.equals("limits") && worldSection.isConfigurationSection(key)) {
                                ConfigurationSection specificWorldSection = worldSection.getConfigurationSection(key);
                                double worldCooldown = specificWorldSection.getDouble("cooldown", cooldown);
                                Integer maxUses = specificWorldSection.isSet("max-uses") ? specificWorldSection.getInt("max-uses") : null;
                                worldConfigs.put(key, new VanillaWorldConfig(worldCooldown, maxUses));
                            }
                        }
                    }

                    VanillaItemConfig vanillaConfig = new VanillaItemConfig(material, cooldown, trigger, displayName, regionLimit, worldLimit, regionConfigs, worldConfigs);
                    VanillaItemCooldownManager.getInstance().registerConfig(vanillaConfig);
                } else {
                     
                    double cooldown = config.getDouble(materialName, 0.0);
                    if (cooldown > 0) {
                        addItem(material, cooldown);
                    }
                }
            } catch (IllegalArgumentException e) {
                DebugUtils.logInternalError("Invalid material in vanilla cooldown config: " + materialName);
            }
        }

        return this;
    }

    public VanillaCooldownBuilder itemsByType(ItemType type, double cooldownSeconds) {
        List<Material> materials = getItemsByType(type);
        return addItems(materials, cooldownSeconds);
    }

    public VanillaCooldownBuilder itemsByType(ItemType type, double cooldownSeconds, String displayName) {
        List<Material> materials = getItemsByType(type);
        return addItems(materials, cooldownSeconds, displayName);
    }

    public VanillaCooldownBuilder remove(Material material) {
        manager.unregisterCooldown(material);
        return this;
    }

    public VanillaCooldownBuilder removeItems(List<Material> materials) {
        materials.forEach(manager::unregisterCooldown);
        return this;
    }

    public VanillaCooldownBuilder clear() {
        manager.clearAllCooldowns();
        return this;
    }

    public enum ItemType {
        FOOD,
        WEAPONS,
        TOOLS,
        POTIONS,
        PROJECTILES,
        ARMOR,
        BLOCKS
    }

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
                    Material.TURTLE_HELMET, Material.SHIELD
            );

            case BLOCKS -> Arrays.asList(
                    Material.STONE, Material.DIRT, Material.GRASS_BLOCK, Material.COBBLESTONE,
                    Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
                    Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
                    Material.CRIMSON_PLANKS, Material.WARPED_PLANKS, Material.TNT
            );
        };
    }

    public static VanillaCooldownBuilder create() {
        return new VanillaCooldownBuilder();
    }

    public static VanillaCooldownBuilder quickSetup(Material material, double cooldown) {
        return new VanillaCooldownBuilder().addItem(material, cooldown);
    }

    public static VanillaCooldownBuilder quickSetup(Material material, double cooldown, VanillaTriggerType trigger) {
        return new VanillaCooldownBuilder().addItem(material, cooldown, trigger);
    }

    public static VanillaCooldownBuilder quickSetup(Material material, double cooldown, String displayName) {
        return new VanillaCooldownBuilder().addItem(material, cooldown, displayName);
    }

    public static VanillaCooldownBuilder quickSetup(Material material, double cooldown, VanillaTriggerType trigger, String displayName) {
        return new VanillaCooldownBuilder().addItem(material, cooldown, trigger, displayName);
    }

    public static VanillaCooldownBuilder loadFromConfig(ConfigurationSection config) {
        return new VanillaCooldownBuilder().fromConfig(config);
    }
}

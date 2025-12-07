package net.exylia.commons.v2.items.processor;

import net.exylia.commons.v2.items.config.ArmorTrimConfig;
import net.exylia.commons.v2.items.config.LeatherArmorConfig;
import net.exylia.commons.v2.items.config.PotionConfig;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.items.utils.PlaceholderDetector;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationParser {

    public static ItemData parseFromConfig(ConfigurationSection config) {
        if (config == null) {
            return ItemData.builder().build();
        }

        ItemData.ItemDataBuilder builder = ItemData.builder();

        parseMaterial(config, builder);
        parseName(config, builder);
        parseLore(config, builder);
        parseAmount(config, builder);
        parseGlowing(config, builder);
        parseHideAttributes(config, builder);
        parseEnchantments(config, builder);
        parsePotionConfig(config, builder);
        parseArmorTrimConfig(config, builder);
        parseLeatherArmorConfig(config, builder);
        parseClickSounds(config, builder);
        parseItemModel(config, builder);
        parseDynamicUpdate(config, builder);
        parseAttributes(config, builder);
        parseCustomNBT(config, builder);
        parseUnbreakable(config, builder);
        parseMaxStackSize(config, builder);

        return builder.build();
    }

    private static void parseMaterial(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("material")) {
            builder.rawMaterial(config.getString("material", "STONE"));
        }
    }

    private static void parseName(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("name")) {
            builder.rawName(config.getString("name"));
        }
        if (config.contains("display-name")) {
            builder.rawDisplayName(config.getString("display-name"));
        }
    }

    private static void parseLore(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("lore")) {
            List<String> lore = new ArrayList<>();
            if (config.isList("lore")) {
                lore = config.getStringList("lore");
            } else {
                String loreSingle = config.getString("lore");
                if (loreSingle != null && !loreSingle.isEmpty()) {
                    lore.add(loreSingle);
                }
            }
            if (!lore.isEmpty()) {
                builder.rawLore(lore);
            }
        }
    }

    private static void parseAmount(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("amount")) {
            if (config.isInt("amount")) {
                builder.rawAmount(String.valueOf(config.getInt("amount")));
            } else {
                builder.rawAmount(config.getString("amount"));
            }
        }
    }

    private static void parseGlowing(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        boolean glowing = config.getBoolean("glow", false) || config.getBoolean("glowing", false);
        builder.glowing(glowing);
    }

    private static void parseHideAttributes(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        boolean hideAttributes = config.getBoolean("hide-attributes", false) ||
                                 config.getBoolean("hide_attributes", false);
        builder.hideAttributes(hideAttributes);
    }

    private static void parseEnchantments(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("enchantments")) {
            ConfigurationSection enchantmentsSection = config.getConfigurationSection("enchantments");
            if (enchantmentsSection != null) {
                Map<String, Integer> enchantments = new HashMap<>();
                for (String enchantName : enchantmentsSection.getKeys(false)) {
                    try {
                        Object level = enchantmentsSection.get(enchantName);
                        if (level instanceof Number) {
                            enchantments.put(enchantName, ((Number) level).intValue());
                        } else if (level instanceof String) {
                            try {
                                enchantments.put(enchantName, Integer.parseInt((String) level));
                            } catch (NumberFormatException ignored) {
                                DebugUtils.logInternalWarn("Invalid enchantment level for " + enchantName + ": " + level);
                            }
                        }
                    } catch (Exception e) {
                        DebugUtils.logInternalWarn("Error parsing enchantment '" + enchantName + "': " + e.getMessage());
                    }
                }
                if (!enchantments.isEmpty()) {
                    builder.rawEnchantments(enchantments);
                }
            }
        }
    }

    private static void parsePotionConfig(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("potion")) {
            ConfigurationSection potionSection = config.getConfigurationSection("potion");
            if (potionSection != null) {
                builder.potionConfig(PotionConfig.fromConfig(potionSection));
            }
        } else if (config.contains("potion_effects") || config.contains("base_potion_type") ||
                   config.contains("potion_color")) {
            PotionConfig potionConfig = new PotionConfig();

            if (config.contains("potion_effects")) {
                List<?> effectsList = config.getList("potion_effects");
                if (effectsList != null) {
                    for (Object effectObj : effectsList) {
                        if (effectObj instanceof Map<?, ?> effectMap) {
                            String type = String.valueOf(effectMap.get("type"));
                            Object amplifier = effectMap.get("amplifier");
                            Object duration = effectMap.get("duration");

                            String amplifierStr = amplifier != null ? String.valueOf(amplifier) : "0";
                            String durationStr = duration != null ? String.valueOf(duration) : "600";

                            potionConfig.addCustomEffect(type, amplifierStr, durationStr);
                        }
                    }
                }
            }

            if (config.contains("base_potion_type")) {
                potionConfig.setBasePotionType(config.getString("base_potion_type"));
            }

            if (config.contains("potion_color")) {
                potionConfig.setPotionColor(config.getString("potion_color"));
            }

            builder.potionConfig(potionConfig);
        }
    }

    private static void parseArmorTrimConfig(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("armor_trim")) {
            ConfigurationSection trimSection = config.getConfigurationSection("armor_trim");
            if (trimSection != null) {
                ArmorTrimConfig trimConfig = ArmorTrimConfig.fromConfig(trimSection);
                if (trimConfig != null) {
                    builder.armorTrimConfig(trimConfig);
                }
            }
        }
    }

    private static void parseLeatherArmorConfig(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("leather_color")) {
            ConfigurationSection leatherSection = config.getConfigurationSection("leather_color");
            if (leatherSection != null) {
                LeatherArmorConfig leatherConfig = LeatherArmorConfig.fromConfig(leatherSection);
                if (leatherConfig != null) {
                    builder.leatherArmorConfig(leatherConfig);
                }
            } else {
                String colorString = config.getString("leather_color");
                if (colorString != null && !colorString.isEmpty()) {
                    LeatherArmorConfig leatherConfig = new LeatherArmorConfig();
                    leatherConfig.setColor(colorString);
                    builder.leatherArmorConfig(leatherConfig);
                }
            }
        }
    }

    private static void parseClickSounds(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("click_sounds")) {
            List<String> sounds = new ArrayList<>();
            if (config.isList("click_sounds")) {
                sounds = config.getStringList("click_sounds");
            } else {
                String soundSingle = config.getString("click_sounds");
                if (soundSingle != null && !soundSingle.isEmpty()) {
                    sounds.add(soundSingle);
                }
            }
            if (!sounds.isEmpty()) {
                builder.clickSounds(sounds);
            }
        }
    }

    private static void parseItemModel(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("item_model")) {
            String itemModel = config.getString("item_model");
            if (itemModel != null && !itemModel.isEmpty()) {
                builder.rawItemModel(itemModel);
            }
        }
    }

    private static void parseDynamicUpdate(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.getBoolean("dynamic_update", false)) {
            builder.dynamicUpdate(true);
            builder.updateInterval(config.getLong("update_interval", 20L));
        }
    }

    public static boolean detectPlaceholders(String text) {
        return PlaceholderDetector.contains(text);
    }

    public static boolean detectPlaceholdersInItemData(ItemData itemData) {
        if (itemData.getRawName() != null && detectPlaceholders(itemData.getRawName())) {
            return true;
        }

        if (itemData.getRawDisplayName() != null && detectPlaceholders(itemData.getRawDisplayName())) {
            return true;
        }

        if (itemData.getRawLore() != null) {
            for (String line : itemData.getRawLore()) {
                if (detectPlaceholders(line)) {
                    return true;
                }
            }
        }

        if (itemData.getRawAmount() != null && detectPlaceholders(itemData.getRawAmount())) {
            return true;
        }

        if (itemData.getRawMaterial() != null && detectPlaceholders(itemData.getRawMaterial())) {
            return true;
        }

        return false;
    }

    private static void parseAttributes(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("attributes")) {
            List<String> attributes = new ArrayList<>();
            if (config.isList("attributes")) {
                attributes = config.getStringList("attributes");
            } else {
                String attributeSingle = config.getString("attributes");
                if (attributeSingle != null && !attributeSingle.isEmpty()) {
                    attributes.add(attributeSingle);
                }
            }
            if (!attributes.isEmpty()) {
                builder.rawAttributes(attributes);
            }
        }
    }

    private static void parseCustomNBT(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("nbt")) {
            Map<String, String> nbtData = new HashMap<>();
            if (config.isConfigurationSection("nbt")) {
                ConfigurationSection nbtSection = config.getConfigurationSection("nbt");
                if (nbtSection != null) {
                    for (String key : nbtSection.getKeys(false)) {
                        Object value = nbtSection.get(key);
                        nbtData.put(key, value != null ? value.toString() : "");
                    }
                }
            } else if (config.isList("nbt")) {
                List<String> nbtList = config.getStringList("nbt");
                for (String nbtEntry : nbtList) {
                    String[] parts = nbtEntry.split(":", 2);
                    if (parts.length == 2) {
                        nbtData.put(parts[0], parts[1]);
                    }
                }
            }
            if (!nbtData.isEmpty()) {
                builder.customNBT(nbtData);
            }
        }
    }

    private static void parseUnbreakable(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        boolean unbreakable = config.getBoolean("unbreakable", false);
        builder.unbreakable(unbreakable);
    }

    private static void parseMaxStackSize(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        int maxStackSize = config.getInt("max_stack_size", config.getInt("maxStackSize", -1));
        if (config.contains("max_stack_size") || config.contains("maxStackSize")) {
            if (maxStackSize > 0) {
                builder.maxStackSize(maxStackSize);
            }
        }
    }
}

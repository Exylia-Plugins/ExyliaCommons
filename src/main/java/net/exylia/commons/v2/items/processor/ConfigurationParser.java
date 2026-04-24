package net.exylia.commons.v2.items.processor;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.items.config.ArmorTrimConfig;
import net.exylia.commons.v2.items.config.BannerConfig;
import net.exylia.commons.v2.items.config.LeatherArmorConfig;
import net.exylia.commons.v2.items.config.PotionConfig;
import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickCommand;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.items.utils.PlaceholderDetector;
import net.exylia.commons.v2.debug.api.DebugAPI;
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
        parseHideTooltip(config, builder);
        parseEnchantments(config, builder);
        parsePotionConfig(config, builder);
        parseArmorTrimConfig(config, builder);
        parseLeatherArmorConfig(config, builder);
        parseBannerConfig(config, builder);
        parseClickSounds(config, builder);
        parseItemModel(config, builder);
        parseTooltipStyle(config, builder);
        parseDynamicUpdate(config, builder);
        parseAttributes(config, builder);
        parseCustomNBT(config, builder);
        parseUnbreakable(config, builder);
        parseMaxStackSize(config, builder);
        parseSlots(config, builder);
        parseActions(config, builder);
        parseCommands(config, builder);
        parseRequiresTarget(config, builder);
        parseCondition(config, builder);

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
                for (String line : config.getStringList("lore")) {
                    if (line.contains("<nl>")) {
                        for (String part : line.split("<nl>", -1)) {
                            lore.add(part);
                        }
                    } else {
                        lore.add(line);
                    }
                }
            } else {
                String loreSingle = config.getString("lore");
                if (loreSingle != null && !loreSingle.isEmpty()) {
                    if (loreSingle.contains("<nl>")) {
                        for (String part : loreSingle.split("<nl>", -1)) {
                            lore.add(part);
                        }
                    } else {
                        lore.add(loreSingle);
                    }
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
        boolean hideAttributes = config.getBoolean("hide-attributes", true) ||
                                 config.getBoolean("hide_attributes", true);
        builder.hideAttributes(hideAttributes);
    }

    private static void parseHideTooltip(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        boolean hideTooltip = config.getBoolean("hide-tooltip", false) ||
                              config.getBoolean("hide_tooltip", false);
        builder.hideTooltip(hideTooltip);
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
                                DebugAPI.logLibWarn("Invalid enchantment level for " + enchantName + ": " + level);
                            }
                        }
                    } catch (Exception e) {
                        DebugAPI.logLibWarn("Error parsing enchantment '" + enchantName + "': " + e.getMessage());
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

    private static void parseBannerConfig(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("banner_design")) {
            String base64 = config.getString("banner_design");
            if (base64 != null && !base64.isEmpty()) {
                if (PlaceholderDetector.contains(base64)) {
                    builder.rawBannerDesign(base64);
                    return;
                }
                BannerConfig bannerConfig = BannerConfig.fromBase64(base64);
                if (bannerConfig != null) {
                    builder.bannerConfig(bannerConfig);
                    return;
                }
                DebugAPI.logLibWarn("Invalid banner_design base64 string in config");
            }
        } else if (config.contains("banner_patterns")) {
            ConfigurationSection bannerSection = config.getConfigurationSection("banner_patterns");
            if (bannerSection != null) {
                BannerConfig bannerConfig = BannerConfig.fromConfig(bannerSection);
                if (bannerConfig != null) {
                    builder.bannerConfig(bannerConfig);
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
        String itemModel = config.getString("item_model", config.getString("item-model"));
        if (itemModel != null && !itemModel.isEmpty()) {
            builder.rawItemModel(itemModel);
        }
    }

    private static void parseTooltipStyle(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        String tooltipStyle = config.getString("tooltip_style", config.getString("tooltip-style"));
        if (tooltipStyle != null && !tooltipStyle.isEmpty()) {
            builder.rawTooltipStyle(tooltipStyle);
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

    private static void parseSlots(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("slot") && config.contains("slots")) {
            throw new IllegalArgumentException("Cannot specify both 'slot' and 'slots' in configuration");
        }

        if (config.contains("slot")) {
            if (config.isInt("slot")) {
                builder.slotConfig(SlotConfig.single(config.getInt("slot")));
            } else {
                String rawSlot = config.getString("slot");
                builder.slotConfig(SlotConfig.singleRaw(rawSlot));
            }
        } else if (config.contains("slots")) {
            if (config.isList("slots")) {
                List<?> slotsList = config.getList("slots");
                List<Integer> intSlots = new ArrayList<>();
                List<String> rawSlots = new ArrayList<>();
                boolean hasPlaceholders = false;

                for (Object slotObj : slotsList) {
                    if (slotObj instanceof Integer) {
                        intSlots.add((Integer) slotObj);
                    } else if (slotObj instanceof String) {
                        String slotStr = (String) slotObj;
                        if (PlaceholderDetector.contains(slotStr)) {
                            hasPlaceholders = true;
                            rawSlots.add(slotStr);
                        } else {
                            try {
                                intSlots.add(Integer.parseInt(slotStr));
                            } catch (NumberFormatException e) {
                                rawSlots.add(slotStr);
                                hasPlaceholders = true;
                            }
                        }
                    }
                }

                if (hasPlaceholders && !rawSlots.isEmpty()) {
                    builder.slotConfig(SlotConfig.multipleRaw(rawSlots));
                } else if (!intSlots.isEmpty()) {
                    builder.slotConfig(SlotConfig.multiple(intSlots));
                }
            } else {
                String slotsString = config.getString("slots");
                if (slotsString != null && !slotsString.isEmpty()) {
                    List<Integer> parsedSlots = parseSlotRanges(slotsString);
                    if (!parsedSlots.isEmpty()) {
                        builder.slotConfig(SlotConfig.multiple(parsedSlots));
                    }
                }
            }
        }
    }

    private static List<Integer> parseSlotRanges(String slotsString) {
        List<Integer> slots = new ArrayList<>();
        String[] parts = slotsString.split(",");

        for (String part : parts) {
            part = part.trim();

            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());

                        for (int i = start; i <= end; i++) {
                            slots.add(i);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else {
                try {
                    slots.add(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return slots;
    }

    private static void parseActions(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("actions")) {
            List<String> actionStrings = new ArrayList<>();
            if (config.isList("actions")) {
                actionStrings = config.getStringList("actions");
            } else {
                String actionSingle = config.getString("actions");
                if (actionSingle != null && !actionSingle.isEmpty()) {
                    actionStrings.add(actionSingle);
                }
            }

            if (!actionStrings.isEmpty()) {
                List<ClickAction> actions = ClickActionParser.parseActions(actionStrings);
                builder.actions(actions);
                DebugAPI.logLibDebug(DebugCategory.ITEMS,
                    "Parsed " + actions.size() + " click actions from config");
            }
        }
    }

    private static void parseCommands(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        if (config.contains("commands")) {
            List<String> commandStrings = new ArrayList<>();
            if (config.isList("commands")) {
                commandStrings = config.getStringList("commands");
            } else {
                String commandSingle = config.getString("commands");
                if (commandSingle != null && !commandSingle.isEmpty()) {
                    commandStrings.add(commandSingle);
                }
            }

            if (!commandStrings.isEmpty()) {
                List<ClickCommand> commands = ClickActionParser.parseCommands(commandStrings);
                builder.commands(commands);
                DebugAPI.logLibDebug(DebugCategory.ITEMS,
                    "Parsed " + commands.size() + " click commands from config");
            }
        }
    }

    private static void parseRequiresTarget(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        boolean requiresTarget = config.getBoolean("requires-target", config.getBoolean("requiresTarget", false));
        builder.requiresTarget(requiresTarget);
    }

    private static void parseCondition(ConfigurationSection config, ItemData.ItemDataBuilder builder) {
        String condition = config.getString("condition");
        if (condition != null && !condition.isBlank()) {
            builder.condition(condition);
        }
    }
}

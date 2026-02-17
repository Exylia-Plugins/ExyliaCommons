package net.exylia.commons.v2.ui.config;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.items.api.ItemsAPI;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.ui.animation.AnimationSettings;
import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.exception.InvalidMenuConfigException;
import net.exylia.commons.v2.ui.model.FillerData;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.model.NavigationData;
import net.exylia.commons.v2.ui.model.SectionData;
import net.exylia.commons.v2.ui.refresh.RefreshMode;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class MenuParser {

    public static MenuData parse(ConfigurationSection config) {
        if (config == null) {
            throw new InvalidMenuConfigException("Configuration section cannot be null");
        }

        DebugAPI.logLibDebug(DebugCategory.UI, "Parsing menu configuration");

        MenuData.MenuDataBuilder builder = MenuData.builder();

        parseBasicProperties(config, builder);
        parseRefreshSettings(config, builder);
        parseAnimationSettings(config, builder);
        parseFillers(config, builder);
        parseItems(config, builder);
        parsePagination(config, builder);
        parseSections(config, builder);
        parseSnapshotSettings(config, builder);
        parsePlayerInventorySettings(config, builder);
        parseContext(config, builder);
        parseSounds(config, builder);

        MenuData menuData = builder.build();
        DebugAPI.logLibDebug(DebugCategory.UI, "Menu configuration parsed: type=" + menuData.getType() + ", size=" + menuData.getSize() + ", title=" + menuData.getTitle());

        return menuData;
    }

    private static void parseBasicProperties(ConfigurationSection config, MenuData.MenuDataBuilder builder) {
        String title = config.getString("title", "Menu");
        builder.title(title);

        String typeString = config.getString("type", "SIMPLE");
        MenuType type = MenuType.fromString(typeString);
        builder.type(type);

        int size = config.getInt("size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) {
            DebugAPI.logLibError(DebugCategory.UI, "Invalid menu size: " + size);
            throw new InvalidMenuConfigException("Invalid menu size: " + size + ". Must be a multiple of 9 between 9 and 54");
        }
        builder.size(size);

        DebugAPI.logLibDebug(DebugCategory.UI, "Parsed basic properties: title=\"" + title + "\", type=" + type + ", size=" + size);
    }

    private static void parseRefreshSettings(ConfigurationSection config, MenuData.MenuDataBuilder builder) {
        if (config.contains("refresh")) {
            ConfigurationSection refreshSection = config.getConfigurationSection("refresh");
            if (refreshSection != null) {
                String mode = refreshSection.getString("mode", "DISABLED");
                builder.refreshMode(RefreshMode.fromString(mode));

                long interval = refreshSection.getLong("interval", 20L);
                builder.refreshInterval(interval);

                long clickDelay = refreshSection.getLong("click_delay", 1L);
                builder.clickRefreshDelay(clickDelay);
            }
        }
    }

    private static void parseAnimationSettings(ConfigurationSection config, MenuData.MenuDataBuilder builder) {
        AnimationSettings.AnimationSettingsBuilder animBuilder = AnimationSettings.builder();

        if (config.contains("animation")) {
            Object animValue = config.get("animation");

            if (animValue instanceof String) {
                AnimationType type = AnimationType.fromString((String) animValue);
                animBuilder.openAnimation(type);
                DebugAPI.logLibDebug(DebugCategory.UI, "Parsed simple animation: " + type);
            } else if (config.isConfigurationSection("animation")) {
                ConfigurationSection animSection = config.getConfigurationSection("animation");
                if (animSection != null) {
                    if (animSection.contains("open")) {
                        animBuilder.openAnimation(AnimationType.fromString(animSection.getString("open")));
                    }
                    if (animSection.contains("page")) {
                        animBuilder.pageAnimation(AnimationType.fromString(animSection.getString("page")));
                    }
                    if (animSection.contains("speed")) {
                        animBuilder.speed(animSection.getInt("speed", 1));
                    }
                    DebugAPI.logLibDebug(DebugCategory.UI, "Parsed animation settings: open=" +
                            animBuilder.build().getOpenAnimation() + ", page=" + animBuilder.build().getPageAnimation());
                }
            }
        }

        builder.animationSettings(animBuilder.build());
    }

    private static void parseFillers(ConfigurationSection config, MenuData.MenuDataBuilder builder) {
        if (config.contains("filler")) {
            ConfigurationSection fillerSection = config.getConfigurationSection("filler");
            if (fillerSection != null) {
                if (fillerSection.contains("global")) {
                    ConfigurationSection globalSection = fillerSection.getConfigurationSection("global");
                    if (globalSection != null) {
                        ItemData globalFiller = ItemsAPI.parseFromConfig(globalSection);
                        builder.globalFiller(globalFiller);
                    }
                }

                if (fillerSection.contains("border")) {
                    ConfigurationSection borderSection = fillerSection.getConfigurationSection("border");
                    if (borderSection != null) {
                        ItemData borderFiller = ItemsAPI.parseFromConfig(borderSection);
                        builder.borderFiller(borderFiller);
                    }
                }

                if (fillerSection.contains("pagination")) {
                    ConfigurationSection paginationSection = fillerSection.getConfigurationSection("pagination");
                    if (paginationSection != null) {
                        ItemData paginationFiller = ItemsAPI.parseFromConfig(paginationSection);
                        builder.paginationFiller(paginationFiller);
                    }
                }

                if (fillerSection.contains("custom")) {
                    ConfigurationSection customSection = fillerSection.getConfigurationSection("custom");
                    if (customSection != null) {
                        List<FillerData> customFillers = parseCustomFillers(customSection);
                        if (!customFillers.isEmpty()) {
                            builder.customFillers(customFillers);
                            DebugAPI.logLibDebug(DebugCategory.UI, "Parsed " + customFillers.size() + " custom fillers");
                        }
                    }
                }
            }
        }
    }

    private static List<FillerData> parseCustomFillers(ConfigurationSection customSection) {
        List<FillerData> customFillers = new ArrayList<>();

        for (String fillerKey : customSection.getKeys(false)) {
            ConfigurationSection fillerConfig = customSection.getConfigurationSection(fillerKey);
            if (fillerConfig != null) {
                ItemData itemData = ItemsAPI.parseFromConfig(fillerConfig);
                List<Integer> slots = parseSlots(fillerConfig, "slots");

                if (!slots.isEmpty()) {
                    FillerData fillerData = FillerData.builder()
                            .name(fillerKey)
                            .itemData(itemData)
                            .slots(slots)
                            .build();
                    customFillers.add(fillerData);
                    DebugAPI.logLibDebug(DebugCategory.UI, "Parsed custom filler '" + fillerKey + "' with " + slots.size() + " slots");
                }
            }
        }

        return customFillers;
    }

    private static void parseItems(ConfigurationSection config, MenuData.MenuDataBuilder builder) {
        if (config.contains("items")) {
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            if (itemsSection != null) {
                Map<String, ItemData> items = new LinkedHashMap<>();

                for (String itemKey : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                    if (itemSection != null) {
                        ItemData itemData = ItemsAPI.parseFromConfig(itemSection);
                        items.put(itemKey, itemData);
                    }
                }

                builder.items(items);
            }
        }
    }

    private static void parsePagination(ConfigurationSection config, MenuData.MenuDataBuilder builder) {
        if (config.contains("pagination")) {
            ConfigurationSection paginationSection = config.getConfigurationSection("pagination");
            if (paginationSection != null) {
                if (paginationSection.contains("slots")) {
                    List<Integer> slots = parseSlots(paginationSection, "slots");
                    builder.paginationSlots(slots);
                }

                if (paginationSection.contains("items")) {
                    List<ItemData> paginationItems = new ArrayList<>();
                    if (paginationSection.isList("items")) {
                        List<?> itemsList = paginationSection.getList("items");
                        if (itemsList != null) {
                            for (Object itemObj : itemsList) {
                                if (itemObj instanceof Map) {
                                    ConfigurationSection itemSection = paginationSection.getRoot().createSection("temp", (Map<?, ?>) itemObj);
                                    ItemData itemData = ItemsAPI.parseFromConfig(itemSection);
                                    paginationItems.add(itemData);
                                }
                            }
                        }
                    }
                    builder.paginationItems(paginationItems);
                }

                if (paginationSection.contains("item_template")) {
                    ConfigurationSection templateSection = paginationSection.getConfigurationSection("item_template");
                    if (templateSection != null) {
                        ItemData template = ItemsAPI.parseFromConfig(templateSection);
                        builder.paginationItemTemplate(template);
                        DebugAPI.logLibDebug(DebugCategory.UI, "Parsed pagination item template");
                    }
                }

                if (paginationSection.contains("navigation")) {
                    NavigationData navigationData = parseNavigationData(paginationSection.getConfigurationSection("navigation"));
                    builder.paginationNavigation(navigationData);
                }
            }
        }
    }

    private static void parseSections(ConfigurationSection config, MenuData.MenuDataBuilder builder) {
        if (config.contains("sections")) {
            ConfigurationSection sectionsSection = config.getConfigurationSection("sections");
            if (sectionsSection != null) {
                List<SectionData> sections = new ArrayList<>();

                for (String sectionKey : sectionsSection.getKeys(false)) {
                    ConfigurationSection sectionConfig = sectionsSection.getConfigurationSection(sectionKey);
                    if (sectionConfig != null) {
                        SectionData sectionData = parseSectionData(sectionKey, sectionConfig);
                        sections.add(sectionData);
                    }
                }

                builder.sections(sections);
            }
        }
    }

    private static SectionData parseSectionData(String name, ConfigurationSection config) {
        SectionData.SectionDataBuilder builder = SectionData.builder();
        builder.name(name);

        if (config.contains("slots")) {
            List<Integer> slots = parseSlots(config, "slots");
            builder.slots(slots);
        }

        if (config.contains("items")) {
            List<ItemData> items = new ArrayList<>();
            if (config.isList("items")) {
                List<?> itemsList = config.getList("items");
                if (itemsList != null) {
                    for (Object itemObj : itemsList) {
                        if (itemObj instanceof Map) {
                            ConfigurationSection itemSection = config.getRoot().createSection("temp", (Map<?, ?>) itemObj);
                            ItemData itemData = ItemsAPI.parseFromConfig(itemSection);
                            items.add(itemData);
                        }
                    }
                }
            }
            builder.items(items);
        }

        if (config.contains("navigation")) {
            NavigationData navigationData = parseNavigationData(config.getConfigurationSection("navigation"));
            builder.navigation(navigationData);
        }

        if (config.contains("filler")) {
            ConfigurationSection fillerSection = config.getConfigurationSection("filler");
            if (fillerSection != null) {
                ItemData fillerItem = ItemsAPI.parseFromConfig(fillerSection);
                builder.fillerItem(fillerItem);
            }
        }

        if (config.contains("selected_template")) {
            ConfigurationSection templateSection = config.getConfigurationSection("selected_template");
            if (templateSection != null) {
                ItemData template = ItemsAPI.parseFromConfig(templateSection);
                builder.selectedItemTemplate(template);
            }
        }

        Set<String> reservedKeys = Set.of("slots", "items", "navigation", "filler", "selected_template");
        Map<String, ItemData> templates = new HashMap<>();
        for (String key : config.getKeys(false)) {
            if (key.endsWith("_template") && !reservedKeys.contains(key)) {
                ConfigurationSection templateSection = config.getConfigurationSection(key);
                if (templateSection != null) {
                    String templateName = key.substring(0, key.length() - "_template".length());
                    templates.put(templateName, ItemsAPI.parseFromConfig(templateSection));
                    DebugAPI.logLibDebug(DebugCategory.UI, "Parsed section template: " + templateName);
                }
            }
        }
        if (!templates.isEmpty()) {
            builder.templates(templates);
        }

        return builder.build();
    }

    private static NavigationData parseNavigationData(ConfigurationSection config) {
        if (config == null) {
            return null;
        }

        NavigationData.NavigationDataBuilder builder = NavigationData.builder();

        if (config.contains("previous")) {
            ConfigurationSection previousSection = config.getConfigurationSection("previous");
            if (previousSection != null) {
                ItemData previousButton = ItemsAPI.parseFromConfig(previousSection);
                int slot = previousSection.getInt("slot", -1);
                builder.previousButton(previousButton);
                builder.previousButtonSlot(slot);
            }
        }

        if (config.contains("next")) {
            ConfigurationSection nextSection = config.getConfigurationSection("next");
            if (nextSection != null) {
                ItemData nextButton = ItemsAPI.parseFromConfig(nextSection);
                int slot = nextSection.getInt("slot", -1);
                builder.nextButton(nextButton);
                builder.nextButtonSlot(slot);
            }
        }

        if (config.contains("info")) {
            ConfigurationSection infoSection = config.getConfigurationSection("info");
            if (infoSection != null) {
                ItemData infoItem = ItemsAPI.parseFromConfig(infoSection);
                int slot = infoSection.getInt("slot", -1);
                builder.infoItem(infoItem);
                builder.infoItemSlot(slot);
            }
        }

        return builder.build();
    }

    private static void parseSnapshotSettings(ConfigurationSection config, MenuData.MenuDataBuilder builder) {
        if (config.contains("snapshot")) {
            ConfigurationSection snapshotSection = config.getConfigurationSection("snapshot");
            if (snapshotSection != null) {
                boolean enabled = snapshotSection.getBoolean("enabled", false);
                builder.snapshotEnabled(enabled);

                if (snapshotSection.contains("snapshot_id")) {
                    builder.snapshotId(snapshotSection.getString("snapshot_id"));
                }

                boolean restoreOnClose = snapshotSection.getBoolean("restore_on_close", true);
                builder.restoreOnClose(restoreOnClose);
            }
        }
    }

    private static void parsePlayerInventorySettings(ConfigurationSection config, MenuData.MenuDataBuilder builder) {
        if (config.contains("player_inventory")) {
            ConfigurationSection playerInvSection = config.getConfigurationSection("player_inventory");
            if (playerInvSection != null) {
                boolean enabled = playerInvSection.getBoolean("enabled", false);
                builder.playerInventoryEnabled(enabled);

                if (playerInvSection.contains("allowed_slots")) {
                    List<Integer> allowedSlots = parseSlots(playerInvSection, "allowed_slots");
                    builder.allowedPlayerSlots(allowedSlots);
                }
            }
        }
    }

    private static void parseContext(ConfigurationSection config, MenuData.MenuDataBuilder builder) {
        builder.context(PlaceholderContext.create());
    }

    private static void parseSounds(ConfigurationSection config, MenuData.MenuDataBuilder builder) {
        if (config.contains("open_sounds")) {
            List<String> openSounds = parseSoundList(config, "open_sounds");
            if (!openSounds.isEmpty()) {
                builder.openSounds(openSounds);
            }
        }

        if (config.contains("close_sounds")) {
            List<String> closeSounds = parseSoundList(config, "close_sounds");
            if (!closeSounds.isEmpty()) {
                builder.closeSounds(closeSounds);
            }
        }

        if (config.contains("click_sounds")) {
            List<String> clickSounds = parseSoundList(config, "click_sounds");
            if (!clickSounds.isEmpty()) {
                builder.clickSounds(clickSounds);
            }
        }
    }

    private static List<String> parseSoundList(ConfigurationSection config, String key) {
        List<String> sounds = new ArrayList<>();

        if (config.isList(key)) {
            sounds = config.getStringList(key);
        } else if (config.isString(key)) {
            String soundSingle = config.getString(key);
            if (soundSingle != null && !soundSingle.isEmpty()) {
                sounds.add(soundSingle);
            }
        }

        return sounds;
    }

    public static List<Integer> parseSlots(ConfigurationSection config, String key) {
        List<Integer> slots = new ArrayList<>();

        if (!config.contains(key)) {
            return slots;
        }

        if (config.isList(key)) {
            List<?> slotsList = config.getList(key);
            if (slotsList != null) {
                for (Object slotObj : slotsList) {
                    if (slotObj instanceof Integer) {
                        slots.add((Integer) slotObj);
                    } else if (slotObj instanceof String) {
                        slots.addAll(parseSlotRange((String) slotObj));
                    }
                }
            }
        } else if (config.isString(key)) {
            String slotsString = config.getString(key);
            if (slotsString != null) {
                slots.addAll(parseSlotRange(slotsString));
            }
        } else if (config.isInt(key)) {
            slots.add(config.getInt(key));
        }

        return slots;
    }

    public static List<Integer> parseSlotRange(String input) {
        List<Integer> slots = new ArrayList<>();

        if (input == null || input.trim().isEmpty()) {
            return slots;
        }

        String[] parts = input.split(",");

        for (String part : parts) {
            part = part.trim();

            if (part.contains("-")) {
                String[] range = part.split("-", 2);
                try {
                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());

                    for (int i = start; i <= end; i++) {
                        if (!slots.contains(i)) {
                            slots.add(i);
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new InvalidMenuConfigException("Invalid slot range: " + part);
                }
            } else {
                try {
                    int slot = Integer.parseInt(part);
                    if (!slots.contains(slot)) {
                        slots.add(slot);
                    }
                } catch (NumberFormatException e) {
                    throw new InvalidMenuConfigException("Invalid slot number: " + part);
                }
            }
        }

        return slots;
    }
}

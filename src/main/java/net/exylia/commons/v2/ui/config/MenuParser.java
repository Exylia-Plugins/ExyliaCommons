package net.exylia.commons.v2.ui.config;

import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.model.RefreshMode;
import net.exylia.commons.v2.ui.sound.SoundConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class MenuParser {

    public static MenuConfig parseMenuConfig(FileConfiguration config) {
        if (config == null) {
            return null;
        }

        String id = config.getString("id", UUID.randomUUID().toString());
        String title = config.getString("title", "Menu");
        int rows = config.getInt("rows", 3);
        MenuType type = MenuType.fromString(config.getString("type", "simple"));

        MenuConfig.MenuConfigBuilder builder = MenuConfig.builder()
            .id(id)
            .title(title)
            .rows(rows)
            .type(type)
            .dynamicUpdates(config.getBoolean("dynamic_updates", false))
            .updateInterval(config.getLong("update_interval", 20L))
            .refreshMode(RefreshMode.fromString(config.getString("refresh_mode", "smart")));

        ConfigurationSection soundsSection = config.getConfigurationSection("sounds");
        if (soundsSection != null) {
            builder.openSound(parseSoundConfig(soundsSection.getConfigurationSection("open")));
            builder.closeSound(parseSoundConfig(soundsSection.getConfigurationSection("close")));
            builder.clickSound(parseSoundConfig(soundsSection.getConfigurationSection("click")));
        }

        Map<Integer, MenuItemConfig> items = new HashMap<>();
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    MenuItemConfig itemConfig = parseMenuItemConfig(itemSection);

                    if (itemSection.contains("slot")) {
                        int slot = itemSection.getInt("slot");
                        items.put(slot, itemConfig);
                    } else if (itemSection.contains("slots")) {
                        List<Integer> slots = parseSlots(itemSection.getString("slots"));
                        for (int slot : slots) {
                            items.put(slot, itemConfig);
                        }
                    }
                }
            }
        }
        builder.items(items);

        ConfigurationSection globalFillerSection = config.getConfigurationSection("global_filler");
        if (globalFillerSection != null) {
            builder.globalFiller(parseMenuItemConfig(globalFillerSection));
        }

        ConfigurationSection borderFillerSection = config.getConfigurationSection("border_filler");
        if (borderFillerSection != null) {
            builder.borderFiller(parseMenuItemConfig(borderFillerSection));
        }

        switch (type) {
            case PAGINATION -> builder.paginationConfig(
                parsePaginationConfig(config.getConfigurationSection("pagination")));
            case EDITABLE -> builder.editableConfig(
                parseEditableConfig(config.getConfigurationSection("editable")));
            case CONFIRMATION -> builder.confirmationConfig(
                parseConfirmationConfig(config.getConfigurationSection("confirmation")));
            case SELECTION -> builder.selectionConfig(
                parseSelectionConfig(config.getConfigurationSection("selection")));
        }

        return builder.build();
    }

    public static MenuItemConfig parseMenuItemConfig(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        MenuItemConfig.MenuItemConfigBuilder builder = MenuItemConfig.builder()
            .id(section.getString("id", UUID.randomUUID().toString()))
            .rawMaterial(section.getString("material", "STONE"))
            .rawName(section.getString("name"))
            .rawAmount(section.getString("amount", "1"))
            .rawLore(section.getStringList("lore"))
            .glowing(section.getBoolean("glowing", false))
            .hideAttributes(section.getBoolean("hide_attributes", false))
            .dynamicUpdate(section.getBoolean("dynamic_update", false))
            .updateInterval(section.getLong("update_interval", 20L));

        ConfigurationSection clickActionsSection = section.getConfigurationSection("click_actions");
        if (clickActionsSection != null) {
            builder.clickActions(ActionParser.parseActions(clickActionsSection));
        }

        ConfigurationSection enchantmentsSection = section.getConfigurationSection("enchantments");
        if (enchantmentsSection != null) {
            Map<String, Integer> enchants = new HashMap<>();
            for (String key : enchantmentsSection.getKeys(false)) {
                enchants.put(key.toUpperCase(), enchantmentsSection.getInt(key));
            }
            builder.enchantments(enchants);
        }

        if (section.contains("click_sound")) {
            if (section.isConfigurationSection("click_sound")) {
                builder.clickSound(parseSoundConfig(section.getConfigurationSection("click_sound")));
            } else if (section.isString("click_sound")) {
                builder.clickSound(SoundConfig.fromString(section.getString("click_sound")));
            }
        }

        builder.customItemId(section.getString("custom_item_id"));
        builder.skullTexture(section.getString("skull_texture"));
        builder.skullOwner(section.getString("skull_owner"));

        if (section.contains("potion_type")) {
            builder.potionType(section.getString("potion_type"));
        }

        if (section.contains("armor_trim_pattern")) {
            builder.armorTrimPattern(section.getString("armor_trim_pattern"));
        }

        if (section.contains("armor_trim_material")) {
            builder.armorTrimMaterial(section.getString("armor_trim_material"));
        }

        if (section.contains("leather_armor_color")) {
            builder.leatherArmorColor(section.getString("leather_armor_color"));
        }

        if (section.contains("item_model")) {
            builder.itemModel(section.getString("item_model"));
        }

        if (section.contains("attributes")) {
            builder.attributes(section.getStringList("attributes"));
        }

        if (section.contains("nbt")) {
            ConfigurationSection nbtSection = section.getConfigurationSection("nbt");
            if (nbtSection != null) {
                Map<String, String> nbt = new HashMap<>();
                for (String key : nbtSection.getKeys(false)) {
                    nbt.put(key, nbtSection.getString(key));
                }
                builder.customNBT(nbt);
            }
        }

        if (section.contains("slot")) {
            builder.slot(section.getInt("slot"));
        }

        if (section.contains("slots")) {
            builder.slots(parseSlots(section.getString("slots")));
        }

        return builder.build();
    }

    private static SoundConfig parseSoundConfig(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String sound = section.getString("sound");
        if (sound == null) {
            return null;
        }

        return SoundConfig.builder()
            .soundKey(sound)
            .volume((float) section.getDouble("volume", 1.0))
            .pitch((float) section.getDouble("pitch", 1.0))
            .delay(section.getLong("delay", 0L))
            .async(section.getBoolean("async", false))
            .build();
    }

    private static MenuConfig.PaginationConfig parsePaginationConfig(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String itemSlotsStr = section.getString("item_slots", "10-16,19-25,28-34");
        int[] itemSlots = parseSlots(itemSlotsStr).stream()
            .mapToInt(Integer::intValue)
            .toArray();

        MenuConfig.PaginationConfig.PaginationConfigBuilder builder =
            MenuConfig.PaginationConfig.builder()
                .itemSlots(itemSlots)
                .previousButtonSlot(section.getInt("previous_button.slot", 48))
                .nextButtonSlot(section.getInt("next_button.slot", 50));

        ConfigurationSection prevButtonSection = section.getConfigurationSection("previous_button");
        if (prevButtonSection != null) {
            builder.previousButton(parseMenuItemConfig(prevButtonSection));
        }

        ConfigurationSection nextButtonSection = section.getConfigurationSection("next_button");
        if (nextButtonSection != null) {
            builder.nextButton(parseMenuItemConfig(nextButtonSection));
        }

        ConfigurationSection fillerSection = section.getConfigurationSection("item_slot_filler");
        if (fillerSection != null) {
            builder.itemSlotFiller(parseMenuItemConfig(fillerSection));
        }

        return builder.build();
    }

    private static MenuConfig.EditableConfig parseEditableConfig(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String editableSlotsStr = section.getString("editable_slots", "");
        int[] editableSlots = parseSlots(editableSlotsStr).stream()
            .mapToInt(Integer::intValue)
            .toArray();

        MenuConfig.EditableConfig.EditableConfigBuilder builder =
            MenuConfig.EditableConfig.builder()
                .editableSlots(editableSlots);

        ConfigurationSection fillerSection = section.getConfigurationSection("editable_slot_filler");
        if (fillerSection != null) {
            builder.editableSlotFiller(parseMenuItemConfig(fillerSection));
        }

        return builder.build();
    }

    private static MenuConfig.ConfirmationConfig parseConfirmationConfig(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        MenuConfig.ConfirmationConfig.ConfirmationConfigBuilder builder =
            MenuConfig.ConfirmationConfig.builder()
                .message(section.getString("message", ""))
                .confirmSlot(section.getInt("confirm_slot", 11))
                .cancelSlot(section.getInt("cancel_slot", 15))
                .infoSlot(section.getInt("info_slot", 13));

        ConfigurationSection confirmButtonSection = section.getConfigurationSection("confirm_button");
        if (confirmButtonSection != null) {
            builder.confirmButton(parseMenuItemConfig(confirmButtonSection));
        }

        ConfigurationSection cancelButtonSection = section.getConfigurationSection("cancel_button");
        if (cancelButtonSection != null) {
            builder.cancelButton(parseMenuItemConfig(cancelButtonSection));
        }

        ConfigurationSection infoItemSection = section.getConfigurationSection("info_item");
        if (infoItemSection != null) {
            builder.infoItem(parseMenuItemConfig(infoItemSection));
        }

        return builder.build();
    }

    private static MenuConfig.SelectionConfig parseSelectionConfig(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        return MenuConfig.SelectionConfig.builder()
            .optionMaterial(section.getString("option_material", "PAPER"))
            .startSlot(section.getInt("start_slot", 10))
            .build();
    }

    public static List<Integer> parseSlots(String slotsString) {
        List<Integer> slots = new ArrayList<>();

        if (slotsString == null || slotsString.isEmpty()) {
            return slots;
        }

        String[] parts = slotsString.split(",");
        for (String part : parts) {
            part = part.trim();

            if (part.contains("-")) {
                String[] range = part.split("-");
                try {
                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());

                    for (int i = start; i <= end; i++) {
                        slots.add(i);
                    }
                } catch (NumberFormatException ignored) {
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
}

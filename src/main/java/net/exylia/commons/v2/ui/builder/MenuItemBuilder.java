package net.exylia.commons.v2.ui.builder;

import net.exylia.commons.v2.ui.config.MenuItemConfig;
import net.exylia.commons.v2.ui.model.ClickAction;
import net.exylia.commons.v2.ui.model.MenuItemV2;
import net.exylia.commons.v2.ui.sound.SoundConfig;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;

import java.util.*;

public class MenuItemBuilder {
    private final MenuItemConfig.MenuItemConfigBuilder configBuilder;
    private final Map<ClickType, List<ClickAction>> clickActions = new HashMap<>();

    private MenuItemBuilder(String material) {
        this.configBuilder = MenuItemConfig.builder()
                .id(UUID.randomUUID().toString())
                .rawMaterial(material);
    }

    public static MenuItemBuilder create(Material material) {
        return new MenuItemBuilder(material.name());
    }

    public static MenuItemBuilder create(String materialName) {
        return new MenuItemBuilder(materialName);
    }

    public static MenuItemBuilder fromConfig(MenuItemConfig config) {
        MenuItemV2 item = MenuItemV2.fromConfig(config);
        return new MenuItemBuilder(config.getRawMaterial()).build(item);
    }

    private MenuItemBuilder build(MenuItemV2 existing) {
        return this;
    }

    public MenuItemBuilder material(Material material) {
        configBuilder.rawMaterial(material.name());
        return this;
    }

    public MenuItemBuilder material(String materialName) {
        configBuilder.rawMaterial(materialName);
        return this;
    }

    public MenuItemBuilder name(String name) {
        configBuilder.rawName(name);
        return this;
    }

    public MenuItemBuilder lore(String... lines) {
        configBuilder.rawLore(Arrays.asList(lines));
        return this;
    }

    public MenuItemBuilder lore(List<String> lines) {
        configBuilder.rawLore(new ArrayList<>(lines));
        return this;
    }

    public MenuItemBuilder amount(String amount) {
        configBuilder.rawAmount(amount);
        return this;
    }

    public MenuItemBuilder amount(int amount) {
        configBuilder.rawAmount(String.valueOf(amount));
        return this;
    }

    public MenuItemBuilder action(ClickType clickType, ClickAction action) {
        clickActions.computeIfAbsent(clickType, k -> new ArrayList<>()).add(action);
        return this;
    }

    public MenuItemBuilder action(ClickType clickType, String actionString) {
        ClickAction action = ClickAction.builder()
                .actionString(actionString)
                .async(true)
                .build();
        return action(clickType, action);
    }

    public MenuItemBuilder actions(Map<ClickType, List<ClickAction>> actions) {
        actions.forEach((clickType, actionList) -> {
            actionList.forEach(action -> action(clickType, action));
        });
        return this;
    }

    public MenuItemBuilder enchantment(String enchantmentName, int level) {
        Map<String, Integer> enchants = new HashMap<>();
        enchants.put(enchantmentName, level);
        configBuilder.enchantments(enchants);
        return this;
    }

    public MenuItemBuilder enchantments(Map<String, Integer> enchantments) {
        configBuilder.enchantments(new HashMap<>(enchantments));
        return this;
    }

    public MenuItemBuilder potionType(String potionType) {
        configBuilder.potionType(potionType);
        return this;
    }

    public MenuItemBuilder armorTrim(String pattern, String material) {
        configBuilder.armorTrimPattern(pattern);
        configBuilder.armorTrimMaterial(material);
        return this;
    }

    public MenuItemBuilder leatherColor(String color) {
        configBuilder.leatherArmorColor(color);
        return this;
    }

    public MenuItemBuilder itemModel(String model) {
        configBuilder.itemModel(model);
        return this;
    }

    public MenuItemBuilder attributes(List<String> attributes) {
        configBuilder.attributes(new ArrayList<>(attributes));
        return this;
    }

    public MenuItemBuilder nbt(Map<String, String> nbt) {
        configBuilder.customNBT(new HashMap<>(nbt));
        return this;
    }

    public MenuItemBuilder glowing(boolean glowing) {
        configBuilder.glowing(glowing);
        return this;
    }

    public MenuItemBuilder glowing() {
        return glowing(true);
    }

    public MenuItemBuilder hideAttributes(boolean hide) {
        configBuilder.hideAttributes(hide);
        return this;
    }

    public MenuItemBuilder hideAttributes() {
        return hideAttributes(true);
    }

    public MenuItemBuilder dynamicUpdate(boolean dynamic) {
        configBuilder.dynamicUpdate(dynamic);
        return this;
    }

    public MenuItemBuilder dynamicUpdate() {
        return dynamicUpdate(true);
    }

    public MenuItemBuilder updateInterval(long ticks) {
        configBuilder.updateInterval(ticks);
        return this;
    }

    public MenuItemBuilder clickSound(SoundConfig sound) {
        configBuilder.clickSound(sound);
        return this;
    }

    public MenuItemBuilder clickSound(String soundKey) {
        configBuilder.clickSound(SoundConfig.fromString(soundKey));
        return this;
    }

    public MenuItemBuilder customItem(String itemId) {
        configBuilder.customItemId(itemId);
        return this;
    }

    public MenuItemBuilder skullTexture(String texture) {
        configBuilder.skullTexture(texture);
        return this;
    }

    public MenuItemBuilder skullOwner(String owner) {
        configBuilder.skullOwner(owner);
        return this;
    }

    public MenuItemBuilder slot(int slot) {
        configBuilder.slot(slot);
        return this;
    }

    public MenuItemBuilder slots(List<Integer> slots) {
        configBuilder.slots(new ArrayList<>(slots));
        return this;
    }

    public MenuItemV2 build() {
        configBuilder.clickActions(clickActions);
        MenuItemConfig config = configBuilder.build();
        return MenuItemV2.fromConfig(config);
    }
}

package net.exylia.commons.ui.builders;

import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.function.Supplier;

public abstract class ItemBuilder<T extends ItemBuilder<T>> {

    protected MenuItem item;

    protected ItemBuilder(MenuItem item) {
        this.item = item;
    }

    protected ItemBuilder(Material material) {
        this.item = new MenuItem(material);
    }

    protected ItemBuilder(String materialString) {
        this.item = new MenuItem(materialString);
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T name(String name) {
        item.setName(name);
        return self();
    }

    public T lore(String... lore) {
        item.setLore(lore);
        return self();
    }

    public T lore(List<String> lore) {
        item.setLoreList(lore);
        return self();
    }

    public T lore(Supplier<List<String>> loreSupplier) {
        item.setLore(loreSupplier);
        return self();
    }

    public T amount(int amount) {
        item.setAmount(amount);
        return self();
    }

    public T amount(String amountString) {
        item.setAmount(amountString);
        return self();
    }

    public T glow(boolean glowing) {
        item.setGlowing(glowing);
        return self();
    }

    public T hideAttributes() {
        item.hideAllAttributes();
        return self();
    }

    public T dynamicUpdate(boolean dynamic) {
        item.setDynamicUpdate(dynamic);
        return self();
    }

    public T updateInterval(long interval) {
        item.setUpdateInterval(interval);
        return self();
    }

    public T enchant(Enchantment enchantment, int level) {
        item.addEnchantment(enchantment, level);
        return self();
    }

    public T enchant(String enchantmentName, int level) {
        item.addEnchantment(enchantmentName, level);
        return self();
    }

    public T enchant(String enchantmentName, String level) {
        item.addEnchantment(enchantmentName, level);
        return self();
    }

    public T removeEnchant(Enchantment enchantment) {
        item.removeEnchantment(enchantment);
        return self();
    }

    public T removeEnchant(String enchantmentName) {
        item.removeEnchantment(enchantmentName);
        return self();
    }

    public T clearEnchants() {
        item.clearEnchantments();
        return self();
    }

    public T potionEffect(PotionEffectType effectType, int amplifier, int duration) {
        item.addPotionEffect(effectType, amplifier, duration);
        return self();
    }

    public T potionEffect(String effectType, int amplifier, int duration) {
        item.addPotionEffect(effectType, amplifier, duration);
        return self();
    }

    public T potionEffect(String effectType, String amplifier, String duration) {
        item.addPotionEffect(effectType, amplifier, duration);
        return self();
    }

    public T clearPotionEffects() {
        item.clearPotionEffects();
        return self();
    }

    public T potionColor(Color color) {
        item.setPotionColor(color);
        return self();
    }

    public T potionColor(String colorString) {
        item.setPotionColor(colorString);
        return self();
    }

    public T potionColor(int r, int g, int b) {
        item.setPotionColor(Color.fromRGB(r, g, b));
        return self();
    }

    public T basePotionType(String potionType) {
        item.setBasePotionType(potionType);
        return self();
    }

    public MenuItem build() {
        return item;
    }
}
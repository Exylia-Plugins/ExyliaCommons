package net.exylia.commons.ui.items;

import lombok.Getter;
import net.exylia.commons.v2.items.config.ArmorTrimConfig;
import net.exylia.commons.v2.items.config.LeatherArmorConfig;
import net.exylia.commons.v2.items.config.PotionConfig;
import net.exylia.commons.v2.items.model.ExyliaItem;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.items.processor.ConfigurationParser;
import net.exylia.commons.v2.items.utils.ItemStackUtils;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.skull.SkullManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class MenuItem extends ExyliaItem {

    @Getter
    private final String id;

    private boolean awaitingPlayerSkull = false;
    private String pendingPlayerName;

    @Getter
    private Consumer<MenuClickEvent> clickHandler;

    public MenuItem(Material material) {
        this(material.name());
    }

    public MenuItem(String materialString) {
        super();
        this.id = UUID.randomUUID().toString();
        itemData.setRawMaterial(materialString);
        itemStack = ItemStackUtils.createFromString(materialString);
    }

    public MenuItem(ItemStack itemStack) {
        super();
        this.id = UUID.randomUUID().toString();
        this.itemStack = itemStack.clone();
        itemData.setRawMaterial(itemStack.getType().name());
    }

    public MenuItem(ItemStack itemStack, boolean extractData) {
        super();
        this.id = UUID.randomUUID().toString();
        this.itemStack = itemStack.clone();
        if (extractData) {
            this.itemData = net.exylia.commons.v2.items.utils.ItemStackExtractor.extractFromItemStack(itemStack);
        } else {
            itemData.setRawMaterial(itemStack.getType().name());
        }
    }

    public MenuItem setName(String name) {
        itemData.setRawName(name);
        return this;
    }

    public MenuItem setName(Component name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public MenuItem setLore(String... lore) {
        itemData.setRawLore(new ArrayList<>(java.util.Arrays.asList(lore)));
        itemData.setLoreDynamicSupplier(null);
        return this;
    }

    public MenuItem setLoreList(List<String> lore) {
        itemData.setRawLore(new ArrayList<>(lore));
        itemData.setLoreDynamicSupplier(null);
        return this;
    }

    public MenuItem setLore(java.util.function.Supplier<List<String>> loreSupplier) {
        itemData.setLoreDynamicSupplier(loreSupplier);
        itemData.setRawLore(null);
        return this;
    }

    public MenuItem setLore(List<Component> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.lore(lore);
            itemStack.setItemMeta(meta);
        }
        itemData.setRawLore(null);
        itemData.setLoreDynamicSupplier(null);
        return this;
    }

    public MenuItem setAmount(int amount) {
        itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public MenuItem setAmount(String amountString) {
        itemData.setRawAmount(amountString);
        return this;
    }

    public MenuItem setMaxStackSize(int maxStackSize) {
        itemData.setMaxStackSize(Math.max(1, maxStackSize));
        return this;
    }

    public int getMaxStackSize() {
        return itemData.getMaxStackSize();
    }

    public MenuItem setMaterial(Material material) {
        return setMaterial(material.name());
    }

    public MenuItem setMaterial(String materialString) {
        itemData.setRawMaterial(materialString);
        updateMaterial(materialString);
        return this;
    }

    public MenuItem setItemStack(ItemStack newItemStack) {
        if (newItemStack != null) {
            this.itemStack = newItemStack.clone();
            itemData.setRawMaterial(newItemStack.getType().name());
        }
        return this;
    }

    public MenuItem addEnchantment(org.bukkit.enchantments.Enchantment enchantment, int level) {
        return addEnchantment(enchantment.getKey().getKey(), level);
    }

    public MenuItem addEnchantment(String enchantmentName, String level) {
        try {
            itemData.getRawEnchantments().put(enchantmentName, Integer.parseInt(level));
        } catch (NumberFormatException ignored) {
        }
        return this;
    }

    public MenuItem addEnchantment(String enchantmentName, int level) {
        return addEnchantment(enchantmentName, String.valueOf(level));
    }

    public MenuItem removeEnchantment(org.bukkit.enchantments.Enchantment enchantment) {
        return removeEnchantment(enchantment.getKey().getKey());
    }

    public MenuItem removeEnchantment(String enchantmentName) {
        itemData.getRawEnchantments().remove(enchantmentName);
        return this;
    }

    public MenuItem clearEnchantments() {
        itemData.getRawEnchantments().clear();
        return this;
    }

    public MenuItem setPotionConfig(PotionConfig config) {
        itemData.setPotionConfig(config);
        return this;
    }

    public MenuItem configurePotionFromConfig(ConfigurationSection config) {
        if (config != null) {
            itemData.setPotionConfig(PotionConfig.fromConfig(config));
        }
        return this;
    }

    public MenuItem withContext(ExyliaContext context) {
        itemData.setContext(context != null ? context : ExyliaContext.create());
        return this;
    }

    public MenuItem addToContext(Object object) {
        itemData.getContext().add(object);
        return this;
    }

    public MenuItem addToContext(Object... objects) {
        itemData.getContext().addAll(objects);
        return this;
    }

    public MenuItem addToContext(String key, Object value) {
        itemData.getContext().put(key, value);
        return this;
    }

    public <T> MenuItem addToContext(Class<T> type, T object) {
        itemData.getContext().add(type, object);
        return this;
    }

    public MenuItem addDynamicToContext(String key, java.util.function.Supplier<Object> supplier) {
        itemData.getContext().putDynamic(key, supplier);
        return this;
    }

    public MenuItem clearContext() {
        itemData.setContext(ExyliaContext.create());
        return this;
    }

    public MenuItem mergeContext(ExyliaContext otherContext) {
        itemData.getContext().merge(otherContext);
        return this;
    }

    public MenuItem createChildContext() {
        itemData.setContext(itemData.getContext().createChild());
        return this;
    }

    public MenuItem setGlowing(boolean glowing) {
        itemData.setGlowing(glowing);
        return this;
    }

    public MenuItem addItemFlags(org.bukkit.inventory.ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public MenuItem hideAllAttributes() {
        itemData.setHideAttributes(true);
        return this;
    }

    public MenuItem setClickHandler(Consumer<MenuClickEvent> handler) {
        this.clickHandler = handler;
        return this;
    }

    public MenuItem setDynamicUpdate(boolean dynamic) {
        itemData.setDynamicUpdate(dynamic);
        return this;
    }

    public MenuItem setUpdateInterval(long interval) {
        itemData.setUpdateInterval(Math.max(1, interval));
        return this;
    }

    public MenuItem setClickSounds(List<String> sounds) {
        itemData.setClickSounds(sounds != null ? new ArrayList<>(sounds) : new ArrayList<>());
        return this;
    }

    public MenuItem addClickSound(String sound) {
        if (sound != null && !sound.trim().isEmpty()) {
            itemData.getClickSounds().add(sound);
        }
        return this;
    }

    public List<String> getClickSounds() {
        return new ArrayList<>(itemData.getClickSounds());
    }

    public boolean hasClickSounds() {
        return !itemData.getClickSounds().isEmpty();
    }

    public MenuItem setItemModel(String itemModel) {
        itemData.setRawItemModel(itemModel);
        return this;
    }

    public MenuItem setArmorTrim(ArmorTrimConfig config) {
        itemData.setArmorTrimConfig(config);
        return this;
    }

    public MenuItem setArmorTrim(String material, String pattern) {
        itemData.setArmorTrimConfig(new ArmorTrimConfig().setMaterial(material).setPattern(pattern));
        return this;
    }

    public MenuItem setLeatherArmorColor(LeatherArmorConfig config) {
        itemData.setLeatherArmorConfig(config);
        return this;
    }

    public MenuItem setLeatherArmorColor(String color) {
        LeatherArmorConfig config = itemData.getLeatherArmorConfig();
        if (config == null) {
            config = new LeatherArmorConfig();
            itemData.setLeatherArmorConfig(config);
        }
        config.setColor(color);
        return this;
    }

    public MenuItem setLeatherArmorColor(int r, int g, int b) {
        LeatherArmorConfig config = itemData.getLeatherArmorConfig();
        if (config == null) {
            config = new LeatherArmorConfig();
            itemData.setLeatherArmorConfig(config);
        }
        config.setColor(r, g, b);
        return this;
    }

    public MenuItem setLeatherArmorColor(org.bukkit.Color color) {
        LeatherArmorConfig config = itemData.getLeatherArmorConfig();
        if (config == null) {
            config = new LeatherArmorConfig();
            itemData.setLeatherArmorConfig(config);
        }
        config.setColor(color);
        return this;
    }

    public ArmorTrimConfig getArmorTrimConfig() {
        return itemData.getArmorTrimConfig();
    }

    public LeatherArmorConfig getLeatherArmorConfig() {
        return itemData.getLeatherArmorConfig();
    }

    public void playClickSounds(Player player) {
        if (player != null && !itemData.getClickSounds().isEmpty()) {
            for (String sound : itemData.getClickSounds()) {
                net.exylia.commons.utils.effects.SoundUtils.playSound(player, sound);
            }
        }
    }

    public void process(Player player) {
        if (awaitingPlayerSkull && pendingPlayerName != null) {
            checkPlayerSkullUpdate();
        }

        processItem(player);
    }

    public void process() {
        processItem();
    }

    @Override
    public ItemStack buildProcessed(Player player) {
        process(player);
        return itemStack.clone();
    }

    @Override
    public ItemStack build() {
        return itemStack.clone();
    }

    public void handleClick(MenuClickEvent event) {
        if (clickHandler != null) {
            clickHandler.accept(event);
        }
    }

    public MenuItem clone() {
        ItemStack clonedStack = (this.itemStack == null) ? new ItemStack(Material.AIR) : this.itemStack.clone();
        MenuItem clone = new MenuItem(clonedStack);
        try {
            clone.itemData = (this.itemData == null)
                    ? net.exylia.commons.v2.items.utils.ItemStackExtractor.extractFromItemStack(clonedStack)
                    : this.itemData.copy();
        } catch (Exception ignored) {
            clone.itemData = net.exylia.commons.v2.items.utils.ItemStackExtractor.extractFromItemStack(clonedStack);
        }
        clone.clickHandler = this.clickHandler;
        clone.awaitingPlayerSkull = this.awaitingPlayerSkull;
        clone.pendingPlayerName = this.pendingPlayerName;
        return clone;
    }


    public MenuItem setNBT(JavaPlugin plugin, String key, String value) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            org.bukkit.NamespacedKey namespacedKey = new org.bukkit.NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public String getNBT(JavaPlugin plugin, String key) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            org.bukkit.NamespacedKey namespacedKey = new org.bukkit.NamespacedKey(plugin, key);
            return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
        }
        return null;
    }

    public static MenuItem fromConfig(ConfigurationSection config, Player player, ExyliaContext context) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration section cannot be null");
        }

        ItemData itemData = ConfigurationParser.parseFromConfig(config);
        MenuItem item = new MenuItem(itemData.getRawMaterial());
        item.itemData = itemData;

        if (context != null) {
            item.withContext(context);
        }

        item.processItem(player);

        return item;
    }

    public static MenuItem create(Material material) {
        return new MenuItem(material);
    }

    public static MenuItem create(Material material, String name, ExyliaContext context) {
        return new MenuItem(material)
                .setName(name)
                .withContext(context);
    }

    public static MenuItem create(Material material, String name, List<String> lore, ExyliaContext context) {
        return new MenuItem(material)
                .setName(name)
                .setLoreList(lore)
                .withContext(context);
    }

    public static MenuItem create(Material material, String name, java.util.function.Supplier<List<String>> loreSupplier, ExyliaContext context) {
        return new MenuItem(material)
                .setName(name)
                .setLore(loreSupplier)
                .withContext(context);
    }

    public boolean needsDynamicUpdate() {
        return itemData.isDynamicUpdate() || awaitingPlayerSkull || hasDynamicContent();
    }

    public List<String> getRawLore() {
        List<String> currentLore = getCurrentLore();
        return currentLore != null ? new ArrayList<>(currentLore) : new ArrayList<>();
    }

    public boolean hasDynamicLore() {
        return itemData.getLoreDynamicSupplier() != null;
    }

    public java.util.Map<String, Integer> getEnchantments() {
        return new java.util.HashMap<>(itemData.getRawEnchantments());
    }

    public PotionConfig getPotionConfig() {
        return itemData.getPotionConfig();
    }

    public boolean hasPotionConfig() {
        return itemData.getPotionConfig() != null && itemData.getPotionConfig().hasConfiguration();
    }

    private boolean isRealPlayerSkull(ItemStack skull, String expectedPlayerName) {
        if (skull.getType() != Material.PLAYER_HEAD) {
            DebugUtils.logInternalDebug("isRealPlayerSkull: Not a player head");
            return false;
        }

        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) {
            DebugUtils.logInternalDebug("isRealPlayerSkull: No skull meta");
            return false;
        }

        try {
            return isPlayerSkullCached(expectedPlayerName);
        } catch (Exception e) {
            DebugUtils.logInternalDebug("isRealPlayerSkull: Exception - " + e.getMessage());
            return false;
        }
    }

    private boolean isPlayerSkullCached(String playerName) {
        try {
            return SkullManager.getInstance().isPlayerCached(playerName);
        } catch (Exception e) {
            DebugUtils.logInternalDebug("isPlayerSkullCached: Exception - " + e.getMessage());
            return false;
        }
    }

    private void loadPlayerSkullAsync(String playerName) {
        net.exylia.commons.utils.skull.SkullUtils.acceptAsyncPlayerSkull(playerName, skull -> {
            refreshMenuIfDisplayed();
        });
    }

    private void refreshMenuIfDisplayed() {
    }

    private void checkPlayerSkullUpdate() {
        if (pendingPlayerName == null) return;
        ItemStack updatedSkull = net.exylia.commons.utils.skull.SkullUtils.createPlayerSkull(pendingPlayerName);
        if (isRealPlayerSkull(updatedSkull, pendingPlayerName)) {
            DebugUtils.logInternalDebug("checkPlayerSkullUpdate: Player skull now available for " + pendingPlayerName);
            this.itemStack = updatedSkull;
            this.awaitingPlayerSkull = false;
            this.pendingPlayerName = null;
            if (itemData.isDynamicUpdate() && !hasDynamicLore() && itemData.getRawName() == null && itemData.getRawAmount() == null) {
                itemData.setDynamicUpdate(false);
            }
        }
    }

    public String getRawMaterial() {
        return itemData.getRawMaterial();
    }

    public String getRawName() {
        return itemData.getRawName();
    }

    public String getRawAmount() {
        return itemData.getRawAmount();
    }

    public ExyliaContext getContext() {
        return itemData.getContext();
    }

    public long getUpdateInterval() {
        return itemData.getUpdateInterval();
    }

    public MenuItem addAttribute(String attribute) {
        if (attribute != null && !attribute.trim().isEmpty()) {
            itemData.getRawAttributes().add(attribute);
        }
        return this;
    }

    public MenuItem setAttributes(java.util.List<String> attributes) {
        itemData.setRawAttributes(attributes != null ? new java.util.ArrayList<>(attributes) : new java.util.ArrayList<>());
        return this;
    }

    public java.util.List<String> getAttributes() {
        return new java.util.ArrayList<>(itemData.getRawAttributes());
    }

    public MenuItem setNBT(String key, String value) {
        if (key != null && !key.trim().isEmpty()) {
            itemData.getCustomNBT().put(key, value != null ? value : "");
        }
        return this;
    }

    public MenuItem addNBT(java.util.Map<String, String> nbtMap) {
        if (nbtMap != null) {
            itemData.getCustomNBT().putAll(nbtMap);
        }
        return this;
    }

    public String getNBT(String key) {
        return itemData.getCustomNBT().get(key);
    }

    public java.util.Map<String, String> getAllNBT() {
        return new java.util.HashMap<>(itemData.getCustomNBT());
    }

    public static MenuItem fromItemStack(ItemStack itemStack) {
        return new MenuItem(itemStack, true);
    }

    public MenuItem extractAndLoad(ItemStack itemStack) {
        ItemData extractedData = net.exylia.commons.v2.items.utils.ItemStackExtractor.extractFromItemStack(itemStack);
        this.itemStack = itemStack.clone();
        this.itemData = extractedData;
        return this;
    }

    @Override
    protected void applyMaxStackSize() {
        if (itemData.getMaxStackSize() != -1) {
            super.applyMaxStackSize();
        } else {
            int amount = getAmount();
            if (amount > 1) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    meta.setMaxStackSize(amount);
                    itemStack.setItemMeta(meta);
                }
            }
        }
    }
}

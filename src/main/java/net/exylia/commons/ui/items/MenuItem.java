package net.exylia.commons.ui.items;

import lombok.Getter;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.provider.CustomItemManager;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.effects.SoundUtils;
import net.exylia.commons.utils.skull.SkullManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.exylia.commons.utils.skull.SkullUtils.*;

public class MenuItem {

    @Getter
    private final String id;

    private ItemStack itemStack;
    @Getter
    private String rawMaterial;
    @Getter
    private String rawName;
    private List<String> rawLore;
    private Supplier<List<String>> loreDynamicSupplier;
    @Getter
    private String rawAmount;

    @Getter
    private ExyliaContext context = ExyliaContext.create();

    private boolean dynamicUpdate = false;
    @Getter
    private long updateInterval = 20L;
    @Getter
    private Consumer<MenuClickEvent> clickHandler;

    private final Map<String, Integer> rawEnchantments = new HashMap<>();

    private boolean awaitingPlayerSkull = false;
    private String pendingPlayerName;

    private PotionConfig potionConfig;

    private List<String> clickSounds = new ArrayList<>();

    private boolean shouldHideAttributes = false;

    @Getter
    private String rawItemModel;

    private ArmorTrimConfig armorTrimConfig;
    private LeatherArmorConfig leatherArmorConfig;

    public MenuItem(Material material) {
        this(material.name());
    }

    public MenuItem(String materialString) {
        this.id = UUID.randomUUID().toString();
        this.rawMaterial = materialString;
        this.itemStack = createItemFromString(materialString);
    }

    public MenuItem(ItemStack itemStack) {
        this.id = UUID.randomUUID().toString();
        this.itemStack = itemStack.clone();
        this.rawMaterial = itemStack.getType().name();
    }

    public MenuItem setName(String name) {
        this.rawName = name;
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
        this.rawLore = Arrays.asList(lore);
        this.loreDynamicSupplier = null;
        return this;
    }

    public MenuItem setLoreList(List<String> lore) {
        this.rawLore = new ArrayList<>(lore);
        this.loreDynamicSupplier = null;
        return this;
    }

    public MenuItem setLore(Supplier<List<String>> loreSupplier) {
        this.loreDynamicSupplier = loreSupplier;
        this.rawLore = null;
        return this;
    }

    public MenuItem setLore(List<Component> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.lore(lore);
            itemStack.setItemMeta(meta);
        }
        this.rawLore = null;
        this.loreDynamicSupplier = null;
        return this;
    }

    public MenuItem setAmount(int amount) {
        itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public MenuItem setAmount(String amountString) {
        this.rawAmount = amountString;
        return this;
    }

    public MenuItem setMaterial(Material material) {
        return setMaterial(material.name());
    }

    public MenuItem setMaterial(String materialString) {
        this.rawMaterial = materialString;
        this.itemStack = createItemFromString(materialString);
        return this;
    }

    public MenuItem addEnchantment(Enchantment enchantment, int level) {
        return addEnchantment(enchantment.getKey().getKey(), String.valueOf(level));
    }

    public MenuItem addEnchantment(String enchantmentName, String level) {
        rawEnchantments.put(enchantmentName, Integer.parseInt(level));
        return this;
    }

    public MenuItem addEnchantment(String enchantmentName, int level) {
        return addEnchantment(enchantmentName, String.valueOf(level));
    }

    public MenuItem removeEnchantment(Enchantment enchantment) {
        return removeEnchantment(enchantment.getKey().getKey());
    }

    public MenuItem removeEnchantment(String enchantmentName) {
        rawEnchantments.remove(enchantmentName);
        return this;
    }

    public MenuItem clearEnchantments() {
        rawEnchantments.clear();
        return this;
    }

    public MenuItem setPotionConfig(PotionConfig config) {
        this.potionConfig = config;
        return this;
    }

    public MenuItem configurePotionFromConfig(org.bukkit.configuration.ConfigurationSection config) {
        if (config != null) {
            this.potionConfig = PotionConfig.fromConfig(config);
        }
        return this;
    }

    public MenuItem withContext(ExyliaContext context) {
        this.context = context != null ? context : ExyliaContext.create();
        return this;
    }

    public MenuItem addToContext(Object object) {
        this.context.add(object);
        return this;
    }

    public MenuItem addToContext(Object... objects) {
        this.context.addAll(objects);
        return this;
    }

    public MenuItem addToContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    public <T> MenuItem addToContext(Class<T> type, T object) {
        this.context.add(type, object);
        return this;
    }

    public MenuItem addDynamicToContext(String key, java.util.function.Supplier<Object> supplier) {
        this.context.putDynamic(key, supplier);
        return this;
    }

    public MenuItem clearContext() {
        this.context = ExyliaContext.create();
        return this;
    }

    public MenuItem mergeContext(ExyliaContext otherContext) {
        this.context.merge(otherContext);
        return this;
    }

    public MenuItem createChildContext() {
        this.context = this.context.createChild();
        return this;
    }

    public MenuItem setGlowing(boolean glowing) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (glowing) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.removeEnchant(Enchantment.UNBREAKING);
            }
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public MenuItem addItemFlags(ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public MenuItem hideAllAttributes() {
        this.shouldHideAttributes = true;
        applyHideAttributes();
        return this;
    }

    private void applyHideAttributes() {
        if (!shouldHideAttributes) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addAttributeModifier(
                    Attribute.LUCK,
                    new AttributeModifier(UUID.randomUUID(), "luck_boost", 0.0, AttributeModifier.Operation.ADD_NUMBER)
            );
            meta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(meta);
        }
    }

    public MenuItem setClickHandler(Consumer<MenuClickEvent> handler) {
        this.clickHandler = handler;
        return this;
    }

    public MenuItem setDynamicUpdate(boolean dynamic) {
        this.dynamicUpdate = dynamic;
        return this;
    }

    public MenuItem setUpdateInterval(long interval) {
        this.updateInterval = Math.max(1, interval);
        return this;
    }

    public MenuItem setClickSounds(List<String> sounds) {
        this.clickSounds = sounds != null ? new ArrayList<>(sounds) : new ArrayList<>();
        return this;
    }

    public MenuItem addClickSound(String sound) {
        if (sound != null && !sound.trim().isEmpty()) {
            this.clickSounds.add(sound);
        }
        return this;
    }

    public List<String> getClickSounds() {
        return new ArrayList<>(clickSounds);
    }

    public boolean hasClickSounds() {
        return !clickSounds.isEmpty();
    }

    public MenuItem setItemModel(String itemModel) {
        this.rawItemModel = itemModel;
        return this;
    }

    public MenuItem setArmorTrim(ArmorTrimConfig config) {
        this.armorTrimConfig = config;
        return this;
    }

    public MenuItem setArmorTrim(String material, String pattern) {
        this.armorTrimConfig = new ArmorTrimConfig().setMaterial(material).setPattern(pattern);
        return this;
    }

    public MenuItem setLeatherArmorColor(LeatherArmorConfig config) {
        this.leatherArmorConfig = config;
        return this;
    }

    public MenuItem setLeatherArmorColor(String color) {
        if (this.leatherArmorConfig == null) {
            this.leatherArmorConfig = new LeatherArmorConfig();
        }
        this.leatherArmorConfig.setColor(color);
        return this;
    }

    public MenuItem setLeatherArmorColor(int r, int g, int b) {
        if (this.leatherArmorConfig == null) {
            this.leatherArmorConfig = new LeatherArmorConfig();
        }
        this.leatherArmorConfig.setColor(r, g, b);
        return this;
    }

    public MenuItem setLeatherArmorColor(org.bukkit.Color color) {
        if (this.leatherArmorConfig == null) {
            this.leatherArmorConfig = new LeatherArmorConfig();
        }
        this.leatherArmorConfig.setColor(color);
        return this;
    }

    public ArmorTrimConfig getArmorTrimConfig() {
        return armorTrimConfig;
    }

    public LeatherArmorConfig getLeatherArmorConfig() {
        return leatherArmorConfig;
    }

    public void playClickSounds(Player player) {
        if (player != null && !clickSounds.isEmpty()) {
            for (String sound : clickSounds) {
                SoundUtils.playSound(player, sound);
            }
        }
    }

    public void process(Player player) {

        if (awaitingPlayerSkull && pendingPlayerName != null) {
            checkPlayerSkullUpdate();
        }

        if (rawMaterial != null) {
            String processedMaterial = context.processPlaceholders(rawMaterial, player);
            if (!processedMaterial.equals(rawMaterial)) {
                updateMaterial(processedMaterial);
            }
        }

        if (rawName != null) {
            String processedName = context.processPlaceholders(rawName, player);
            updateName(processedName);
        }

        List<String> currentLore = getCurrentLore();
        if (currentLore != null && !currentLore.isEmpty()) {
            List<Component> processedLore = new ArrayList<>();
            for (String line : currentLore) {
                String processedLine = context.processPlaceholders(line, player);
                processedLore.add(ColorUtils.parse(processedLine));
            }
            updateLore(processedLore);
        }

        if (rawAmount != null) {
            String processedAmount = context.processPlaceholders(rawAmount, player);
            updateAmount(processedAmount);
        }

        processEnchantments(player);
        processPotionConfig(player);
        applyItemModel(player);
        processArmorMeta(player);
        processLeatherArmorColor(player);

        applyHideAttributes();
    }

    public void process() {
        process(null);
    }

    public ItemStack buildProcessed(Player player) {
        process(player);
        return itemStack.clone();
    }

    public ItemStack build() {
        return itemStack.clone();
    }

    private void processEnchantments(Player player) {
        if (rawEnchantments.isEmpty()) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        for (Map.Entry<String, Integer> entry : rawEnchantments.entrySet()) {
            String enchantName = entry.getKey();
            String levelStr = String.valueOf(entry.getValue());

            if (player != null) {
                enchantName = context.processPlaceholders(enchantName, player);
                levelStr = context.processPlaceholders(levelStr, player);
            }

            try {
                Enchantment enchantment = getEnchantmentByName(enchantName);
                int level = Integer.parseInt(levelStr);

                if (enchantment != null) {
                    meta.addEnchant(enchantment, level, true);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        itemStack.setItemMeta(meta);
    }

    private void processPotionConfig(Player player) {
        if (potionConfig == null || !potionConfig.hasConfiguration()) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (!(meta instanceof PotionMeta potionMeta)) return;

        if (potionConfig.getBasePotionType() != null) {
            PotionData potionData = potionConfig.createPotionData();
            potionMeta.setBasePotionData(potionData);
        }

        List<PotionEffect> customEffects = potionConfig.createCustomEffects(player, context);
        for (PotionEffect effect : customEffects) {
            potionMeta.addCustomEffect(effect, true);
        }

        Color color = potionConfig.getProcessedColor(player, context);
        if (color != null) {
            potionMeta.setColor(color);
        }

        itemStack.setItemMeta(potionMeta);
    }

    private void applyItemModel(Player player) {
        if (rawItemModel == null || rawItemModel.isEmpty()) return;

        String processedModel = rawItemModel;
        if (player != null) {
            processedModel = context.processPlaceholders(rawItemModel, player);
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        try {
            String[] parts = processedModel.split(":", 2);
            if (parts.length == 2) {
                String namespace = parts[0];
                String key = parts[1];
                meta.setItemModel(new NamespacedKey(namespace, key));
                itemStack.setItemMeta(meta);
            }
        } catch (Exception e) {
            DebugUtils.logInternalWarn("MenuItem: Failed to apply item model: " + processedModel);
        }
    }

    private void processArmorMeta(Player player) {
        if (armorTrimConfig == null || !armorTrimConfig.hasConfiguration()) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (!(meta instanceof org.bukkit.inventory.meta.ArmorMeta armorMeta)) return;

        try {
            armorTrimConfig.applyTrim(armorMeta);
            itemStack.setItemMeta(meta);
        } catch (Exception e) {
            DebugUtils.logInternalWarn("MenuItem: Failed to apply armor trim");
        }
    }

    private void processLeatherArmorColor(Player player) {
        if (leatherArmorConfig == null || !leatherArmorConfig.hasConfiguration()) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (!(meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta leatherMeta)) return;

        try {
            leatherArmorConfig.applyColor(leatherMeta, player, context);
            itemStack.setItemMeta(meta);
        } catch (Exception e) {
            DebugUtils.logInternalWarn("MenuItem: Failed to apply leather armor color");
        }
    }

    private List<String> getCurrentLore() {
        if (loreDynamicSupplier != null) {
            try {
                return loreDynamicSupplier.get();
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return rawLore;
    }

    private void updateMaterial(String materialString) {
        ItemStack newStack = createItemFromString(materialString);
        ItemMeta currentMeta = itemStack.getItemMeta();

        if (currentMeta != null) {
            ItemMeta newMeta = newStack.getItemMeta();
            if (newMeta != null) {
                if (currentMeta.hasDisplayName()) {
                    newMeta.displayName(currentMeta.displayName());
                }
                if (currentMeta.hasLore()) {
                    newMeta.lore(currentMeta.lore());
                }

                newMeta.addItemFlags(currentMeta.getItemFlags().toArray(new ItemFlag[0]));
                currentMeta.getEnchants().forEach((enchant, level) ->
                        newMeta.addEnchant(enchant, level, true));

                newStack.setItemMeta(newMeta);
            }
        }

        newStack.setAmount(itemStack.getAmount());
        this.itemStack = newStack;
    }

    private void updateName(String name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.parse(name));
            itemStack.setItemMeta(meta);
        }
    }

    private void updateLore(List<Component> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.lore(lore);
            itemStack.setItemMeta(meta);
        }
    }

    private void updateAmount(String amountString) {
        try {
            int amount = Integer.parseInt(amountString.trim());
            itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        } catch (NumberFormatException e) {
        }
    }

    private ItemStack createItemFromString(String materialString) {
        if (materialString == null || materialString.isEmpty()) {
            return new ItemStack(Material.STONE);
        }

        if (materialString.startsWith("headbase-")) {
            String base64 = materialString.substring(9);
            return createSkullFromTexture(base64);
        }

        if (materialString.startsWith("headurl-")) {
            String url = materialString.substring(8);
            return createSkullFromUrl(url);
        }

        if (materialString.startsWith("playerhead-")) {
            String playerName = materialString.substring(11);
            ItemStack cachedSkull = createPlayerSkull(playerName);
            if (isRealPlayerSkull(cachedSkull, playerName)) {
                this.awaitingPlayerSkull = false;
                this.pendingPlayerName = null;
                return cachedSkull;
            }
            this.awaitingPlayerSkull = true;
            this.pendingPlayerName = playerName;
            this.dynamicUpdate = true;

            loadPlayerSkullAsync(playerName);
            return cachedSkull;
        }

        if (materialString.contains(":")) {
            DebugUtils.logInternalDebug("MenuItem: Attempting to load custom item: " + materialString);
            ItemStack customItem = CustomItemManager.getInstance().getCustomItem(materialString);
            if (customItem != null) {
                DebugUtils.logInternalDebug("MenuItem: Successfully loaded custom item: " + materialString);
                return customItem.clone();
            }
            DebugUtils.logInternalWarn("MenuItem: Custom item not found: " + materialString + ", falling back to STONE");
        }

        try {
            Material material = Material.valueOf(materialString.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            return new ItemStack(Material.STONE);
        }
    }

    private Enchantment getEnchantmentByName(String name) {
        try {
            return Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
        } catch (Exception e) {
            for (Enchantment enchant : Enchantment.values()) {
                if (enchant.getKey().getKey().equalsIgnoreCase(name) ||
                        enchant.toString().equalsIgnoreCase(name)) {
                    return enchant;
                }
            }
            return null;
        }
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
        acceptAsyncPlayerSkull(playerName, skull -> {
            refreshMenuIfDisplayed();
        });
    }

    private void refreshMenuIfDisplayed() {

    }

    private void checkPlayerSkullUpdate() {
        if (pendingPlayerName == null) return;
        ItemStack updatedSkull = createPlayerSkull(pendingPlayerName);
        if (isRealPlayerSkull(updatedSkull, pendingPlayerName)) {
            DebugUtils.logInternalDebug("checkPlayerSkullUpdate: Player skull now available for " + pendingPlayerName);
            this.itemStack = updatedSkull;
            this.awaitingPlayerSkull = false;
            this.pendingPlayerName = null;
            if (dynamicUpdate && !hasDynamicLore() && rawName == null && rawAmount == null) {
                this.dynamicUpdate = false;
            }
        }
    }

    public void handleClick(MenuClickEvent event) {
        if (clickHandler != null) {
            clickHandler.accept(event);
        }
    }

    public MenuItem clone() {
        MenuItem clone = new MenuItem(this.itemStack.clone());
        clone.rawMaterial = this.rawMaterial;
        clone.rawName = this.rawName;
        clone.rawAmount = this.rawAmount;
        clone.dynamicUpdate = this.dynamicUpdate;
        clone.updateInterval = this.updateInterval;
        clone.clickHandler = this.clickHandler;
        clone.context = this.context.copy();
        clone.loreDynamicSupplier = this.loreDynamicSupplier;
        clone.rawEnchantments.putAll(this.rawEnchantments);
        clone.potionConfig = this.potionConfig;
        clone.awaitingPlayerSkull = this.awaitingPlayerSkull;
        clone.pendingPlayerName = this.pendingPlayerName;
        clone.clickSounds = new ArrayList<>(this.clickSounds);
        clone.shouldHideAttributes = this.shouldHideAttributes;
        clone.rawItemModel = this.rawItemModel;
        clone.armorTrimConfig = this.armorTrimConfig;
        clone.leatherArmorConfig = this.leatherArmorConfig;

        if (this.rawLore != null) {
            clone.rawLore = new ArrayList<>(this.rawLore);
        }

        return clone;
    }

    public MenuItem setNBT(JavaPlugin plugin, String key, String value) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public String getNBT(JavaPlugin plugin, String key) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
        }
        return null;
    }

    public static MenuItem fromConfig(org.bukkit.configuration.ConfigurationSection config, Player player, ExyliaContext context) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration section cannot be null");
        }

        String material = config.getString("material", "STONE");
        MenuItem item = new MenuItem(material);

        if (config.contains("name")) {
            item.setName(config.getString("name"));
        }

        if (config.contains("lore")) {
            item.setLoreList(config.getStringList("lore"));
        }

        if (config.contains("amount")) {
            if (config.isInt("amount")) {
                item.setAmount(config.getInt("amount"));
            } else {
                item.setAmount(config.getString("amount"));
            }
        }

        if ((config.getBoolean("glow", false) || config.getBoolean("glowing", false))) {
            item.setGlowing(true);
        }

        boolean configHideAttributes = (config.getBoolean("hide_attributes", false)) || (config.getBoolean("hide-attributes", false));
        if (configHideAttributes) {
            item.shouldHideAttributes = true;
        }

        if (config.getBoolean("dynamic_update", false)) {
            item.setDynamicUpdate(true);
            item.setUpdateInterval(config.getLong("update_interval", 20L));
        }

        org.bukkit.configuration.ConfigurationSection enchantments = config.getConfigurationSection("enchantments");
        if (enchantments != null) {
            for (String enchantName : enchantments.getKeys(false)) {
                Object level = enchantments.get(enchantName);
                if (level instanceof Integer) {
                    item.addEnchantment(enchantName, (Integer) level);
                } else if (level instanceof String) {
                    item.addEnchantment(enchantName, (String) level);
                }
            }
        }

        if (config.contains("potion")) {
            org.bukkit.configuration.ConfigurationSection potionSection = config.getConfigurationSection("potion");
            item.configurePotionFromConfig(potionSection);
        }

        if (config.contains("potion_effects") || config.contains("base_potion_type") || config.contains("potion_color")) {
            if (item.potionConfig == null) {
                item.potionConfig = new PotionConfig();
            }

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

                            item.potionConfig.addCustomEffect(type, amplifierStr, durationStr);
                        }
                    }
                }
            }

            if (config.contains("base_potion_type")) {
                item.potionConfig.setBasePotionType(config.getString("base_potion_type"));
            }

            if (config.contains("potion_color")) {
                item.potionConfig.setPotionColor(config.getString("potion_color"));
            }
        }

        if (config.contains("click_sounds")) {
            if (config.isList("click_sounds")) {
                item.setClickSounds(config.getStringList("click_sounds"));
            } else {
                item.addClickSound(config.getString("click_sounds"));
            }
        }

        if (config.contains("item_model")) {
            item.setItemModel(config.getString("item_model"));
        }

        if (config.contains("armor_trim")) {
            org.bukkit.configuration.ConfigurationSection trimSection = config.getConfigurationSection("armor_trim");
            if (trimSection != null) {
                ArmorTrimConfig trimConfig = ArmorTrimConfig.fromConfig(trimSection);
                if (trimConfig != null) {
                    item.setArmorTrim(trimConfig);
                }
            }
        }

        if (config.contains("leather_color")) {
            org.bukkit.configuration.ConfigurationSection leatherSection = config.getConfigurationSection("leather_color");
            if (leatherSection != null) {
                LeatherArmorConfig leatherConfig = LeatherArmorConfig.fromConfig(leatherSection);
                if (leatherConfig != null) {
                    item.setLeatherArmorColor(leatherConfig);
                }
            } else {
                String colorString = config.getString("leather_color");
                if (colorString != null && !colorString.isEmpty()) {
                    item.setLeatherArmorColor(colorString);
                }
            }
        }

        if (context != null) {
            item.withContext(context);
            item.process(player);
        }

        if (configHideAttributes) {
            item.applyHideAttributes();
        }

        return item;
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

    public static MenuItem create(Material material, String name, Supplier<List<String>> loreSupplier, ExyliaContext context) {
        return new MenuItem(material)
                .setName(name)
                .setLore(loreSupplier)
                .withContext(context);
    }

    public ItemStack getItemStack() {
        return itemStack.clone();
    }

    public boolean needsDynamicUpdate() {
        return dynamicUpdate || awaitingPlayerSkull;
    }

    public List<String> getRawLore() {
        List<String> currentLore = getCurrentLore();
        return currentLore != null ? new ArrayList<>(currentLore) : new ArrayList<>();
    }

    public boolean hasDynamicLore() {
        return loreDynamicSupplier != null;
    }

    public Map<String, Integer> getEnchantments() {
        return new HashMap<>(rawEnchantments);
    }

    public PotionConfig getPotionConfig() {
        return potionConfig;
    }

    public boolean hasPotionConfig() {
        return potionConfig != null && potionConfig.hasConfiguration();
    }
}

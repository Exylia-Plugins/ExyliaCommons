package net.exylia.commons.ui.items;

import lombok.Getter;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.versions.ItemMetaAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.exylia.commons.utils.skull.SkullUtils.*;

public class MenuItem {

    @Getter
    private final String id;
    private final ItemMetaAdapter adapter = AdapterFactory.getItemMetaAdapter();

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
    private final List<PotionEffectData> rawPotionEffects = new ArrayList<>();
    private String rawPotionColor;
    private String rawBasePotionType;

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

    public static class PotionEffectData {
        public final String type;
        public final String amplifier;
        public final String duration;

        public PotionEffectData(String type, String amplifier, String duration) {
            this.type = type;
            this.amplifier = amplifier;
            this.duration = duration;
        }
    }

    private PotionType getPotionTypeByName(String name) {
        try {
            return PotionType.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return switch (name.toLowerCase()) {
                case "speed", "swiftness" -> PotionType.SPEED;
                case "slowness", "slow" -> PotionType.SLOWNESS;
                case "strength" -> PotionType.STRENGTH;
                case "instant_health", "healing", "heal" -> PotionType.INSTANT_HEAL;
                case "instant_damage", "harming", "harm" -> PotionType.INSTANT_DAMAGE;
                case "jump_boost", "jump" -> PotionType.JUMP;
                case "regeneration", "regen" -> PotionType.REGEN;
                case "fire_resistance", "fire_resist" -> PotionType.FIRE_RESISTANCE;
                case "water_breathing" -> PotionType.WATER_BREATHING;
                case "invisibility", "invis" -> PotionType.INVISIBILITY;
                case "night_vision" -> PotionType.NIGHT_VISION;
                case "weakness", "weak" -> PotionType.WEAKNESS;
                case "poison" -> PotionType.POISON;
                case "luck" -> PotionType.LUCK;
                case "turtle_master" -> PotionType.TURTLE_MASTER;
                case "slow_falling" -> PotionType.SLOW_FALLING;
                default -> null;
            };
        }
    }


    public MenuItem setName(String name) {
        this.rawName = name;
        return this;
    }

    public MenuItem setName(Component name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            adapter.setDisplayName(meta, name);
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
            adapter.setLore(meta, lore);
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

    public MenuItem addPotionEffect(PotionEffectType effectType, int amplifier, int duration) {
        return addPotionEffect(effectType.getName(), String.valueOf(amplifier), String.valueOf(duration));
    }

    public MenuItem addPotionEffect(String effectType, String amplifier, String duration) {
        rawPotionEffects.add(new PotionEffectData(effectType, amplifier, duration));
        return this;
    }

    public MenuItem addPotionEffect(String effectType, int amplifier, int duration) {
        return addPotionEffect(effectType, String.valueOf(amplifier), String.valueOf(duration));
    }

    public MenuItem clearPotionEffects() {
        rawPotionEffects.clear();
        return this;
    }

    public MenuItem setPotionColor(Color color) {
        return setPotionColor(String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
    }

    public MenuItem setPotionColor(String colorString) {
        this.rawPotionColor = colorString;
        return this;
    }

    public MenuItem setBasePotionType(String potionType) {
        this.rawBasePotionType = potionType;
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
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.removeEnchant(Enchantment.DURABILITY);
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
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(meta);
        }
        return this;
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

    public void process(Player player) {
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
        processPotionEffects(player);
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

    private void processPotionEffects(Player player) {
        if (rawPotionEffects.isEmpty() && rawPotionColor == null) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        // Manejar pociones normales y splash
        if (meta instanceof PotionMeta potionMeta) {
            processPotionMeta(potionMeta, player);
            itemStack.setItemMeta(potionMeta);
        }
        // Manejar flechas con poción (TippedArrow)
        else if (itemStack.getType() == Material.TIPPED_ARROW && meta instanceof PotionMeta potionMeta) {
            processPotionMeta(potionMeta, player);
            itemStack.setItemMeta(potionMeta);
        }
        // Intentar con cualquier ItemMeta que implemente PotionMeta (compatibilidad futura)
        else {
            try {
                if (meta instanceof PotionMeta potionMeta) {
                    processPotionMeta(potionMeta, player);
                    itemStack.setItemMeta(potionMeta);
                }
            } catch (Exception ignored) {
                // Si falla, continuar sin efectos de poción
            }
        }
    }

    private void processPotionMeta(PotionMeta potionMeta, Player player) {
        // Establecer tipo base de poción si está especificado
        if (rawBasePotionType != null) {
            String processedType = player != null ?
                    context.processPlaceholders(rawBasePotionType, player) : rawBasePotionType;

            try {
                PotionType potionType = PotionType.valueOf(processedType.toUpperCase());
                potionMeta.setBasePotionData(new PotionData(potionType));
            } catch (Exception ignored) {
                // Intentar con diferentes formatos
                try {
                    PotionType potionType = getPotionTypeByName(processedType);
                    if (potionType != null) {
                        potionMeta.setBasePotionData(new PotionData(potionType));
                    }
                } catch (Exception ignored2) {
                }
            }
        }

        // Agregar efectos custom
        if (!rawPotionEffects.isEmpty()) {
            for (PotionEffectData effectData : rawPotionEffects) {
                String effectType = effectData.type;
                String amplifierStr = effectData.amplifier;
                String durationStr = effectData.duration;

                if (player != null) {
                    effectType = context.processPlaceholders(effectType, player);
                    amplifierStr = context.processPlaceholders(amplifierStr, player);
                    durationStr = context.processPlaceholders(durationStr, player);
                }

                try {
                    PotionEffectType type = getPotionEffectByName(effectType);
                    int amplifier = Integer.parseInt(amplifierStr);
                    int duration = Integer.parseInt(durationStr);

                    if (type != null) {
                        PotionEffect effect = new PotionEffect(type, duration, amplifier);
                        potionMeta.addCustomEffect(effect, true);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Establecer color custom
        if (rawPotionColor != null) {
            String processedColor = player != null ?
                    context.processPlaceholders(rawPotionColor, player) : rawPotionColor;

            Color color = parseColor(processedColor);
            if (color != null) {
                potionMeta.setColor(color);
            }
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
                    adapter.setDisplayName(newMeta, adapter.getDisplayName(currentMeta));
                }
                if (currentMeta.hasLore()) {
                    adapter.setLore(newMeta, adapter.getLore(currentMeta));
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
            adapter.setDisplayName(meta, ColorUtils.parse(name));
            itemStack.setItemMeta(meta);
        }
    }

    private void updateLore(List<Component> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            adapter.setLore(meta, lore);
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
            return createPlayerSkull(playerName);
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

    private PotionEffectType getPotionEffectByName(String name) {
        try {
            return PotionEffectType.getByName(name.toUpperCase());
        } catch (Exception e) {
            for (PotionEffectType type : PotionEffectType.values()) {
                if (type != null && (type.getName().equalsIgnoreCase(name) ||
                        type.toString().equalsIgnoreCase(name))) {
                    return type;
                }
            }
            return null;
        }
    }

    private Color parseColor(String colorString) {
        try {
            if (colorString.startsWith("#")) {
                int rgb = Integer.parseInt(colorString.substring(1), 16);
                return Color.fromRGB(rgb);
            }

            String[] parts = colorString.split(",");
            if (parts.length == 3) {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return Color.fromRGB(r, g, b);
            }

            switch (colorString.toLowerCase()) {
                case "red": return Color.RED;
                case "blue": return Color.BLUE;
                case "green": return Color.GREEN;
                case "yellow": return Color.YELLOW;
                case "purple": return Color.PURPLE;
                case "orange": return Color.ORANGE;
                case "white": return Color.WHITE;
                case "black": return Color.BLACK;
                default: return null;
            }
        } catch (Exception e) {
            return null;
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
        clone.rawPotionEffects.addAll(this.rawPotionEffects);
        clone.rawPotionColor = this.rawPotionColor;
        clone.rawBasePotionType = this.rawBasePotionType;

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

        if (config.getBoolean("glowing", false)) {
            item.setGlowing(true);
        }

        if (config.getBoolean("hide_attributes", false)) {
            item.hideAllAttributes();
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

                        item.addPotionEffect(type, amplifierStr, durationStr);
                    }
                }
            }
        }

        if (config.contains("base_potion_type")) {
            item.setBasePotionType(config.getString("base_potion_type"));
        }

        if (config.contains("potion_color")) {
            item.setPotionColor(config.getString("potion_color"));
        }

        if (context != null) {
            item.withContext(context);
            item.process(player);
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
        return dynamicUpdate;
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

    public List<PotionEffectData> getPotionEffects() {
        return new ArrayList<>(rawPotionEffects);
    }

    public String getPotionColor() {
        return rawPotionColor;
    }
}
// ==================== MENU ITEMS SYSTEM ====================

package net.exylia.commons.ui.items;

import net.exylia.commons.ui.context.MenuContext;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.ItemMetaAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static net.exylia.commons.utils.SkullUtils.*;

/**
 * Enhanced menu item with context support and cleaner API
 */
public class MenuItem {

    private final String id;
    private final ItemMetaAdapter adapter = AdapterFactory.getItemMetaAdapter();

    // Core item properties
    private ItemStack itemStack;
    private String rawMaterial;
    private String rawName;
    private List<String> rawLore;
    private String rawAmount;

    // Behavior configuration
    private boolean dynamicUpdate = false;
    private long updateInterval = 20L;
    private Consumer<MenuClickEvent> clickHandler;

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

    // ==================== BASIC CONFIGURATION ====================

    /**
     * Sets the display name (supports placeholders)
     * @param name The name
     * @return This item for chaining
     */
    public MenuItem setName(String name) {
        this.rawName = name;
        return this;
    }

    /**
     * Sets the display name directly with a component
     * @param name The name component
     * @return This item for chaining
     */
    public MenuItem setName(Component name) {
        ItemMeta meta = itemStack.getItemMeta();
        adapter.setDisplayName(meta, name);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Sets the lore (supports placeholders)
     * @param lore The lore lines
     * @return This item for chaining
     */
    public MenuItem setLore(String... lore) {
        this.rawLore = Arrays.asList(lore);
        return this;
    }

    /**
     * Sets the lore from a list
     * @param lore The lore lines
     * @return This item for chaining
     */
    public MenuItem setLoreList(List<String> lore) {
        this.rawLore = new ArrayList<>(lore);
        return this;
    }

    /**
     * Sets the lore directly with components
     * @param lore The lore components
     * @return This item for chaining
     */
    public MenuItem setLore(List<Component> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        adapter.setLore(meta, lore);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Sets the amount (supports placeholders)
     * @param amount The amount
     * @return This item for chaining
     */
    public MenuItem setAmount(int amount) {
        itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    /**
     * Sets the amount with placeholder support
     * @param amountString The amount string (may contain placeholders)
     * @return This item for chaining
     */
    public MenuItem setAmount(String amountString) {
        this.rawAmount = amountString;
        return this;
    }

    /**
     * Sets the material
     * @param material The material
     * @return This item for chaining
     */
    public MenuItem setMaterial(Material material) {
        return setMaterial(material.name());
    }

    /**
     * Sets the material from string (supports heads)
     * @param materialString The material string
     * @return This item for chaining
     */
    public MenuItem setMaterial(String materialString) {
        this.rawMaterial = materialString;
        this.itemStack = createItemFromString(materialString);
        return this;
    }

    // ==================== VISUAL PROPERTIES ====================

    /**
     * Adds or removes glow effect
     * @param glowing Whether the item should glow
     * @return This item for chaining
     */
    public MenuItem setGlowing(boolean glowing) {
        ItemMeta meta = itemStack.getItemMeta();

        if (glowing) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.removeEnchant(Enchantment.DURABILITY);
        }

        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Adds item flags
     * @param flags The flags to add
     * @return This item for chaining
     */
    public MenuItem addItemFlags(ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addItemFlags(flags);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Hides all attributes
     * @return This item for chaining
     */
    public MenuItem hideAllAttributes() {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(meta);
        return this;
    }

    // ==================== BEHAVIOR CONFIGURATION ====================

    /**
     * Sets the click handler
     * @param handler The click handler
     * @return This item for chaining
     */
    public MenuItem setClickHandler(Consumer<MenuClickEvent> handler) {
        this.clickHandler = handler;
        return this;
    }

    /**
     * Enables or disables dynamic updates
     * @param dynamic Whether to enable dynamic updates
     * @return This item for chaining
     */
    public MenuItem setDynamicUpdate(boolean dynamic) {
        this.dynamicUpdate = dynamic;
        return this;
    }

    /**
     * Sets the update interval for dynamic updates
     * @param interval The interval in ticks
     * @return This item for chaining
     */
    public MenuItem setUpdateInterval(long interval) {
        this.updateInterval = Math.max(1, interval);
        return this;
    }

    // ==================== NBT DATA ====================

    /**
     * Sets NBT data
     * @param plugin The plugin
     * @param key The key
     * @param value The value
     * @return This item for chaining
     */
    public MenuItem setNBT(JavaPlugin plugin, String key, String value) {
        ItemMeta meta = itemStack.getItemMeta();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Gets NBT data
     * @param plugin The plugin
     * @param key The key
     * @return The value or null
     */
    public String getNBT(JavaPlugin plugin, String key) {
        ItemMeta meta = itemStack.getItemMeta();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
    }

    // ==================== CONTEXT PROCESSING ====================

    /**
     * Processes this item with context and player
     * @param context The menu context
     * @param player The player
     */
    public void processWithContext(MenuContext context, Player player) {
        // Process material with placeholders
        if (rawMaterial != null && context != null) {
            String processedMaterial = context.processPlaceholders(rawMaterial, player);
            if (!processedMaterial.equals(rawMaterial)) {
                updateMaterial(processedMaterial);
            }
        }

        // Process name with placeholders
        if (rawName != null && context != null) {
            String processedName = context.processPlaceholders(rawName, player);
            updateName(processedName);
        }

        // Process lore with placeholders
        if (rawLore != null && !rawLore.isEmpty() && context != null) {
            List<Component> processedLore = new ArrayList<>();
            for (String line : rawLore) {
                String processedLine = context.processPlaceholders(line, player);
                processedLore.add(ColorUtils.parse(processedLine));
            }
            updateLore(processedLore);
        }

        // Process amount with placeholders
        if (rawAmount != null && context != null) {
            String processedAmount = context.processPlaceholders(rawAmount, player);
            updateAmount(processedAmount);
        }
    }

    /**
     * Updates the material preserving metadata
     * @param materialString The new material string
     */
    private void updateMaterial(String materialString) {
        ItemStack newStack = createItemFromString(materialString);
        ItemMeta currentMeta = itemStack.getItemMeta();

        if (currentMeta != null) {
            ItemMeta newMeta = newStack.getItemMeta();
            if (newMeta != null) {
                // Copy important metadata
                if (currentMeta.hasDisplayName()) {
                    adapter.setDisplayName(newMeta, adapter.getDisplayName(currentMeta));
                }
                if (currentMeta.hasLore()) {
                    adapter.setLore(newMeta, adapter.getLore(currentMeta));
                }

                // Copy flags and enchantments
                newMeta.addItemFlags(currentMeta.getItemFlags().toArray(new ItemFlag[0]));
                currentMeta.getEnchants().forEach((enchant, level) ->
                        newMeta.addEnchant(enchant, level, true));

                newStack.setItemMeta(newMeta);
            }
        }

        newStack.setAmount(itemStack.getAmount());
        this.itemStack = newStack;
    }

    /**
     * Updates the display name
     * @param name The new name
     */
    private void updateName(String name) {
        ItemMeta meta = itemStack.getItemMeta();
        adapter.setDisplayName(meta, ColorUtils.parse(name));
        itemStack.setItemMeta(meta);
    }

    /**
     * Updates the lore
     * @param lore The new lore
     */
    private void updateLore(List<Component> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        adapter.setLore(meta, lore);
        itemStack.setItemMeta(meta);
    }

    /**
     * Updates the amount
     * @param amountString The amount string
     */
    private void updateAmount(String amountString) {
        try {
            int amount = Integer.parseInt(amountString.trim());
            itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        } catch (NumberFormatException e) {
            // Keep current amount if parsing fails
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Creates an ItemStack from a string
     * @param materialString The material string
     * @return The created ItemStack
     */
    private ItemStack createItemFromString(String materialString) {
        if (materialString == null || materialString.isEmpty()) {
            return new ItemStack(Material.STONE);
        }

        if (materialString.startsWith("headbase-")) {
            String base64 = materialString.substring(9);
            return createHeadFromBase64(base64);
        }

        if (materialString.startsWith("headurl-")) {
            String url = materialString.substring(8);
            return createHeadFromUrl(url);
        }

        if (materialString.startsWith("playerhead-")) {
            String playerName = materialString.substring(11);
            return createPlayerHead(playerName);
        }

        // Handle normal materials
        try {
            Material material = Material.valueOf(materialString.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            return new ItemStack(Material.STONE);
        }
    }

    /**
     * Builds the final ItemStack
     * @return The built ItemStack
     */
    public ItemStack build() {
        return itemStack.clone();
    }

    /**
     * Creates a copy of this item
     * @return A new MenuItem copy
     */
    public MenuItem clone() {
        MenuItem clone = new MenuItem(this.itemStack.clone());
        clone.rawMaterial = this.rawMaterial;
        clone.rawName = this.rawName;
        clone.rawAmount = this.rawAmount;
        clone.dynamicUpdate = this.dynamicUpdate;
        clone.updateInterval = this.updateInterval;
        clone.clickHandler = this.clickHandler;

        if (this.rawLore != null) {
            clone.rawLore = new ArrayList<>(this.rawLore);
        }

        return clone;
    }

    // ==================== EVENT HANDLING ====================

    /**
     * Handles a click event on this item
     * @param event The click event
     */
    public void handleClick(MenuClickEvent event) {
        if (clickHandler != null) {
            clickHandler.accept(event);
        }
    }

    // ==================== GETTERS ====================

    public String getId() { return id; }
    public ItemStack getItemStack() { return itemStack.clone(); }
    public boolean needsDynamicUpdate() { return dynamicUpdate; }
    public long getUpdateInterval() { return updateInterval; }
    public Consumer<MenuClickEvent> getClickHandler() { return clickHandler; }
}
package net.exylia.commons.item;

import lombok.Getter;
import net.exylia.commons.actions.ActionContext;
import net.exylia.commons.actions.GlobalActionManager;
import net.exylia.commons.command.CommandExecutor;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.item.utils.ItemNBTUtils;
import net.exylia.commons.item.utils.ItemPlaceholderUtils;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.versions.ItemMetaAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;
import static net.exylia.commons.utils.skull.SkullUtils.*;

public class InteractiveItem {

    private static final String NBT_ITEM_ID = "interactive_item_id";
    private static final String NBT_CURRENT_USES = "current_uses";
    private static final String NBT_UNIQUE_ID = "unique_id";

    private final PlaceholderSystemManager placeholderManager = PlaceholderSystemManager.getInstance();
    private final ItemMetaAdapter adapter = AdapterFactory.getItemMetaAdapter();
    private final ItemStack itemStack;

    private String configId;
    private ItemConfiguration config;

    private Player placeholderPlayer;

    @Getter
    private Consumer<ItemClickInfo> clickHandler;

    public InteractiveItem(String configId, ItemConfiguration config) {
        this.configId = configId;
        this.config = config;
        this.itemStack = createItemFromConfig(config);

        String effectiveId = config.hasForceId() ? config.getForceId() : configId;
        setItemId(effectiveId);

        initializeUses();

        if (config.getAmount() > 1) {
            this.itemStack.setAmount(config.getAmount());
        }
    }

    public InteractiveItem(String configId, ItemConfiguration config, Player player) {
        this.configId = configId;
        this.config = config;
        this.placeholderPlayer = player;
        this.itemStack = createItemFromConfig(config, player);

        String effectiveId = config.hasForceId() ? config.getForceId() : configId;
        setItemId(effectiveId);

        initializeUses();

        if (config.getAmount() > 1) {
            this.itemStack.setAmount(config.getAmount());
        }
    }

    private InteractiveItem(ItemStack itemStack, String configId, ItemConfiguration config) {
        this.itemStack = itemStack.clone();
        this.configId = configId;
        this.config = config;
    }

    public static InteractiveItem fromItemStack(ItemStack itemStack) {
        String itemId = getItemIdFromStack(itemStack);
        if (itemId == null) return null;

        ItemConfiguration config = findConfigurationForItemId(itemId);
        if (config == null) {
            logInternalWarn("No se encontró configuración para item ID: " + itemId);
            return null;
        }

        String originalConfigId = findOriginalConfigId(itemId, config);
        return new InteractiveItem(itemStack, originalConfigId, config);
    }

    private static ItemConfiguration findConfigurationForItemId(String itemId) {
        ItemConfiguration config = ItemManager.getItemConfiguration(itemId);
        if (config != null) {
            return config;
        }

        for (var entry : ItemManager.getAllConfigurations().entrySet()) {
            ItemConfiguration itemConfig = entry.getValue();
            if (itemConfig.hasForceId() && itemConfig.getForceId().equals(itemId)) {
                return itemConfig;
            }
        }

        return null;
    }

    private static String findOriginalConfigId(String itemId, ItemConfiguration config) {
        if (config.hasForceId() && config.getForceId().equals(itemId)) {
            for (var entry : ItemManager.getAllConfigurations().entrySet()) {
                if (entry.getValue() == config) {
                    return entry.getKey();
                }
            }
        }

        return itemId;
    }

    @Getter
    private ExyliaContext context = ExyliaContext.create();

    public InteractiveItem withContext(ExyliaContext context) {
        this.context = context != null ? context : ExyliaContext.create();
        return this;
    }

    public InteractiveItem addToContext(Object object) {
        this.context.add(object);
        return this;
    }

    public InteractiveItem addToContext(Object... objects) {
        this.context.addAll(objects);
        return this;
    }

    public InteractiveItem addToContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    public InteractiveItem clearContext() {
        this.context = ExyliaContext.create();
        return this;
    }

    public InteractiveItem setClickHandler(Consumer<ItemClickInfo> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public InteractiveItem setPlaceholderPlayer(Player player) {
        this.placeholderPlayer = player;
        return this;
    }

    public String getId() {
        return configId;
    }

    public String getEffectiveId() {
        if (config.hasForceId()) {
            String forceId = config.getForceId();
            DebugUtils.logInternalDebug(debug(), "Using force-id: " + forceId + " for item: " + configId);
            return forceId;
        }

        String nbtId = getItemIdFromStack(itemStack);
        DebugUtils.logInternalDebug(debug(), "Using NBT ID: " + nbtId + " for item: " + configId);
        return nbtId != null ? nbtId : configId;
    }

    public String getForceId() {
        return config.hasForceId() ? config.getForceId() : null;
    }

    public boolean hasForceId() {
        return config.hasForceId();
    }

    public String getRawName() { return config.getName(); }
    public String getRawDisplayName() { return config.getDisplayName(); }
    public List<String> getRawLore() { return config.getLore(); }
    public String getRawMaterialString() { return config.getMaterial(); }
    public boolean usesPlaceholders() { return config.isUsePlaceholders(); }
    public List<String> getCommands() { return config.getCommands(); }
    public String getAction() { return config.getAction(); }
    public int getSlot() { return config.getSlot(); }
    public boolean shouldConsumeOnUse() { return config.isConsumeOnUse(); }
    public boolean shouldCancelEvent() { return config.isCancelEvent(); }
    public int getMaxUses() { return config.getMaxUses(); }
    public boolean isStackable() { return config.isStackable(); }
    public double getCooldownSeconds() { return config.getCooldownSeconds(); }

    public boolean hasDisplayName() {
        return config.hasDisplayName();
    }

    public int getCurrentUses() {
        return ItemNBTUtils.getNBTInt(itemStack, getPlugin(), NBT_CURRENT_USES, getMaxUses());
    }

    public InteractiveItem setCurrentUses(int uses) {
        ItemNBTUtils.setNBTInt(itemStack, getPlugin(), NBT_CURRENT_USES, uses);
        return this;
    }

    public boolean hasLimitedUses() {
        return getMaxUses() > 0;
    }

    public boolean hasUsesRemaining() {
        return getMaxUses() == -1 || getCurrentUses() > 0;
    }

    public boolean consumeUse() {
        int maxUses = getMaxUses();
        if (maxUses == -1) return true;

        int currentUses = getCurrentUses();
        if (currentUses > 0) {
            int newUses = currentUses - 1;
            setCurrentUses(newUses);
            return newUses > 0;
        }
        return false;
    }

    public void updatePlaceholders(Player player, EquipmentSlot hand) {
        if (!usesPlaceholders()) return;

        ItemConfiguration freshConfig = ItemManager.getItemConfiguration(configId);
        if (freshConfig != null) {
            this.config = freshConfig;
        }

        Player targetPlayer = (placeholderPlayer != null) ? placeholderPlayer : player;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        ExyliaContext fullContext = context.copy().add(this);

        String rawName = getRawName();
        if (rawName != null) {
            String processedName = fullContext.processPlaceholders(rawName, targetPlayer);
            processedName = ItemPlaceholderUtils.processAllItemPlaceholders(processedName, this, targetPlayer);
            adapter.setDisplayName(meta, ColorUtils.parse(processedName));
        }

        List<String> rawLore = getRawLore();
        if (!rawLore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : rawLore) {
                String processedLine = fullContext.processPlaceholders(line, targetPlayer);
                processedLine = ItemPlaceholderUtils.processAllItemPlaceholders(processedLine, this, targetPlayer);
                loreComponents.add(ColorUtils.parse(processedLine));
            }
            adapter.setLore(meta, loreComponents);
        }

        itemStack.setItemMeta(meta);

        if (hand != null) {
            PlayerInventory inventory = player.getInventory();
            if (hand == EquipmentSlot.HAND) {
                inventory.setItemInMainHand(itemStack);
            } else if (hand == EquipmentSlot.OFF_HAND) {
                inventory.setItemInOffHand(itemStack);
            }
        }
    }

    public boolean hasAction() {
        String action = getAction();
        return action != null && !action.trim().isEmpty();
    }

    public boolean executeAction(ItemClickInfo clickInfo) {
        if (hasAction()) {
            ExyliaContext actionContext = context.copy().add(this);
            ActionContext context = new ActionContext(clickInfo.getPlayer(), clickInfo.getSource())
                    .withData("clickType", clickInfo.getClickType())
                    .withData("slot", clickInfo.getSlot())
                    .withData("item", this)
                    .withData("itemStack", clickInfo.getItemStack())
                    .withData("itemConfiguration", this.getConfiguration())
                    .withData("contexts", actionContext.getAllObjects());

            // Añadir hitPlayer si está presente en clickInfo
            if (clickInfo.getData().containsKey("hitPlayer")) {
                context.withData("hitPlayer", clickInfo.getData("hitPlayer"));
            }

            return GlobalActionManager.executeAction(getAction(), context);
        }
        return false;
    }

    public void executeCommands(Player player) {
        ExyliaContext commandContext = context.copy().add(this);
        if (placeholderPlayer != null) commandContext.add(placeholderPlayer);

        CommandExecutor.builder(player)
                .withPlaceholderPlayer(placeholderPlayer)
                .withPlaceholderContext(commandContext.getAllObjects())
                .execute(getCommands());
    }

    private void initializeUses() {
        int maxUses = getMaxUses();
        if (maxUses > 0) {
            if (!ItemNBTUtils.hasNBTValue(itemStack, getPlugin(), NBT_CURRENT_USES, PersistentDataType.INTEGER)) {
                setCurrentUses(maxUses);
            }
        }
    }

    private ItemStack createItemFromConfig(ItemConfiguration config) {
        return createItemFromConfig(config, null);
    }

    private ItemStack createItemFromConfig(ItemConfiguration config, Player player) {
        String materialString = config.getMaterial();

        if (player != null && containsPlaceholders(materialString)) {
            ExyliaContext fullContext = context.copy().add(this);
            materialString = fullContext.processPlaceholders(materialString, player);
        }

        ItemStack item = createItemFromString(materialString);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (config.getName() != null) {
                String name = config.getName();
                if (player != null && config.isUsePlaceholders()) {
                    ExyliaContext fullContext = context.copy().add(this);
                    name = fullContext.processPlaceholders(name, player);
                    name = ItemPlaceholderUtils.processAllItemPlaceholders(name, this, player);
                }
                adapter.setDisplayName(meta, ColorUtils.parse(name));
            }

            if (!config.getLore().isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : config.getLore()) {
                    String processedLine = line;
                    if (player != null && config.isUsePlaceholders()) {
                        ExyliaContext fullContext = context.copy().add(this);
                        processedLine = fullContext.processPlaceholders(line, player);
                        processedLine = ItemPlaceholderUtils.processAllItemPlaceholders(processedLine, this, player);
                    }
                    loreComponents.add(ColorUtils.parse(processedLine));
                }
                adapter.setLore(meta, loreComponents);
            }

            item.setItemMeta(meta);
        }

        if (config.isGlowing()) {
            setGlowing(item, true);
        }

        if (config.isHideAttributes()) {
            hideAllAttributes(item);
        }

        if (!config.isStackable()) {
            makeUnique(item);
        }

        return item;
    }

    private boolean containsPlaceholders(String text) {
        return text != null && text.contains("%");
    }

    private ItemStack createItemFromString(String materialString) {
        if (materialString == null || materialString.isEmpty()) {
            logInternalWarn("Material string is null or empty, using STONE");
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
            logInternalWarn("Invalid material: " + materialString + ", using STONE");
            return new ItemStack(Material.STONE);
        }
    }

    private static String getItemIdFromStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        NamespacedKey key = new NamespacedKey(ItemManager.getPlugin(), NBT_ITEM_ID);
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private void setItemId(String id) {
        ItemNBTUtils.setNBTString(itemStack, getPlugin(), NBT_ITEM_ID, id);
    }

    public InteractiveItem setAmount(int amount) {
        itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public InteractiveItem setGlowing(boolean glowing) {
        setGlowing(itemStack, glowing);
        return this;
    }

    public InteractiveItem hideAllAttributes() {
        hideAllAttributes(itemStack);
        return this;
    }

    private void setGlowing(ItemStack item, boolean glowing) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (glowing) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.removeEnchant(Enchantment.DURABILITY);
        }

        item.setItemMeta(meta);
    }

    private void hideAllAttributes(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_POTION_EFFECTS,
                ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
    }

    private void makeUnique(ItemStack item) {
        ItemNBTUtils.setNBTString(item, getPlugin(), NBT_UNIQUE_ID, UUID.randomUUID().toString());
    }

    public ItemStack getItemStack() {
        return itemStack.clone();
    }

    public ItemConfiguration getConfiguration() {
        return config;
    }

    private JavaPlugin getPlugin() {
        return ItemManager.getPlugin() != null ? ItemManager.getPlugin() :
                JavaPlugin.getProvidingPlugin(InteractiveItem.class);
    }

    @Override
    public InteractiveItem clone() {
        InteractiveItem clone = new InteractiveItem(this.itemStack.clone(), this.configId, this.config);
        clone.clickHandler = this.clickHandler;
        clone.placeholderPlayer = this.placeholderPlayer;
        clone.context = this.context.copy();
        return clone;
    }
}
package net.exylia.commons.item;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
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
import net.exylia.commons.utils.TimeFormatter;
import net.exylia.commons.utils.skull.SkullManager;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static net.exylia.commons.utils.DebugUtils.logInternalWarn;
import static net.exylia.commons.utils.skull.SkullUtils.*;

@Getter
@Accessors(fluent = true)
public class InteractiveItem {

    private static final String NBT_ITEM_ID = "interactive_item_id";
    private static final String NBT_CURRENT_USES = "current_uses";
    private static final String NBT_UNIQUE_ID = "unique_id";
    private static final String NBT_EXPIRATION_TIME = "expiration_time";
    private static final String NBT_EXPIRATION_BEHAVIOR = "expiration_behavior";

    private final PlaceholderSystemManager placeholderManager = PlaceholderSystemManager.getInstance();
    private final ItemMetaAdapter adapter = AdapterFactory.getItemMetaAdapter();
    private ItemStack itemStack;

    private String configId;
    private ItemConfiguration config;

    @Setter
    private Player placeholderPlayer;

    @Setter
    private Consumer<ItemClickInfo> clickHandler;

    @Setter
    @Accessors(fluent = true)
    private ExyliaContext context = ExyliaContext.create();

    private boolean awaitingPlayerSkull = false;
    private String pendingPlayerName;
    private boolean dynamicSkullUpdate = false;

    public InteractiveItem(String configId, ItemConfiguration config) {
        this.configId = configId;
        this.config = config;
        this.itemStack = createItemFromConfig(config);

        String effectiveId = config.hasForceId() ? config.getForceId() : configId;
        setItemId(effectiveId);

        initializeUses();
        initializeExpiration();

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
        initializeExpiration();

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

        return ItemManager.getAllConfigurations().entrySet().stream()
                .map(entry -> entry.getValue())
                .filter(itemConfig -> itemConfig.hasForceId() && itemConfig.getForceId().equals(itemId))
                .findFirst()
                .orElse(null);
    }

    private static String findOriginalConfigId(String itemId, ItemConfiguration config) {
        if (config.hasForceId() && config.getForceId().equals(itemId)) {
            return ItemManager.getAllConfigurations().entrySet().stream()
                    .filter(entry -> entry.getValue() == config)
                    .map(entry -> entry.getKey())
                    .findFirst()
                    .orElse(itemId);
        }
        return itemId;
    }

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

    public String getId() {
        return configId;
    }

    public String getEffectiveId() {
        if (config.hasForceId()) {
            String forceId = config.getForceId();
            DebugUtils.logInternalDebug("Using force-id: " + forceId + " for item: " + configId);
            return forceId;
        }

        String nbtId = getItemIdFromStack(itemStack);
        DebugUtils.logInternalDebug("Using NBT ID: " + nbtId + " for item: " + configId);
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

    public boolean hasExpiration() {
        return getExpirationTime() > 0;
    }

    public boolean isExpired() {
        long expirationTime = getExpirationTime();
        return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
    }

    public long getExpirationTime() {
        return ItemNBTUtils.getNBTLong(itemStack, getPlugin(), NBT_EXPIRATION_TIME, 0L);
    }

    public InteractiveItem setExpirationTime(long expirationTimeMillis) {
        ItemNBTUtils.setNBTLong(itemStack, getPlugin(), NBT_EXPIRATION_TIME, expirationTimeMillis);
        return this;
    }

    public InteractiveItem setExpirationFromNow(long durationMillis) {
        long expirationTime = System.currentTimeMillis() + durationMillis;
        setExpirationTime(expirationTime);
        return this;
    }

    public InteractiveItem setExpirationDate(String dateString) {
        try {
            long timestamp = parseDateString(dateString);
            setExpirationTime(timestamp);
        } catch (DateTimeParseException e) {
            logInternalWarn("Invalid expiration date format: " + dateString + ". Use formats like '24/12/2025 15:00'");
        }
        return this;
    }

    public long getRemainingTime() {
        if (!hasExpiration()) return -1;
        return Math.max(0, getExpirationTime() - System.currentTimeMillis());
    }

    public String getFormattedRemainingTime() {
        long remainingTime = getRemainingTime();
        if (remainingTime <= 0) return "Expirado";
        return TimeFormatter.timeFormatter.format(remainingTime);
    }

    public String getFormattedExpirationDate() {
        if (!hasExpiration()) return "";
        return TimeFormatter.timeFormatter.format(getExpirationTime());
    }

    public ExpirationBehavior getExpirationBehavior() {
        if (!ItemNBTUtils.hasNBTValue(itemStack, getPlugin(), NBT_EXPIRATION_BEHAVIOR, PersistentDataType.STRING)) {
            return ExpirationBehavior.KEEP;
        }
        String behaviorStr = ItemNBTUtils.getNBTString(itemStack, getPlugin(), NBT_EXPIRATION_BEHAVIOR);
        return ExpirationBehavior.fromString(behaviorStr);
    }

    public InteractiveItem setExpirationBehavior(ExpirationBehavior behavior) {
        if (behavior == null) behavior = ExpirationBehavior.KEEP;
        ItemNBTUtils.setNBTString(itemStack, getPlugin(), NBT_EXPIRATION_BEHAVIOR, behavior.getConfigName());
        return this;
    }

    public InteractiveItem setExpirationWithBehavior(long expirationTimeMillis, ExpirationBehavior behavior) {
        setExpirationTime(expirationTimeMillis);
        setExpirationBehavior(behavior);
        return this;
    }

    public InteractiveItem setExpirationFromNowWithBehavior(long durationMillis, ExpirationBehavior behavior) {
        setExpirationFromNow(durationMillis);
        setExpirationBehavior(behavior);
        return this;
    }

    public InteractiveItem setExpirationDateWithBehavior(String dateString, ExpirationBehavior behavior) {
        setExpirationDate(dateString);
        setExpirationBehavior(behavior);
        return this;
    }

    public boolean shouldRemoveWhenExpired() {
        return getExpirationBehavior() == ExpirationBehavior.REMOVE;
    }

    public boolean shouldDisableWhenExpired() {
        return getExpirationBehavior() == ExpirationBehavior.DISABLE;
    }

    public boolean shouldTransformWhenExpired() {
        return getExpirationBehavior() == ExpirationBehavior.TRANSFORM;
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

            if (clickInfo.getLocation() != null) {
                context.withData("hitLocation", clickInfo.getLocation());
            }

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

    private void initializeExpiration() {
        if (config.hasExpiration()) {
            if (!ItemNBTUtils.hasNBTValue(itemStack, getPlugin(), NBT_EXPIRATION_TIME, PersistentDataType.LONG)) {
                setExpirationTime(config.getExpirationTimeMillis());
            }
            if (!ItemNBTUtils.hasNBTValue(itemStack, getPlugin(), NBT_EXPIRATION_BEHAVIOR, PersistentDataType.STRING)) {
                String behavior = config.getExpirationBehavior();
                if (behavior != null && !behavior.isEmpty()) {
                    ItemNBTUtils.setNBTString(itemStack, getPlugin(), NBT_EXPIRATION_BEHAVIOR, behavior);
                }
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
        
        if (config.hasEnchantments()) {
            applyEnchantments(item, config.getEnchantments());
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
            ItemStack cachedSkull = createPlayerSkull(playerName);
            if (isRealPlayerSkull(cachedSkull, playerName)) {
                this.awaitingPlayerSkull = false;
                this.pendingPlayerName = null;
                this.dynamicSkullUpdate = false;
                return cachedSkull;
            }
            this.awaitingPlayerSkull = true;
            this.pendingPlayerName = playerName;
            this.dynamicSkullUpdate = true;
            
            loadPlayerSkullAsync(playerName);
            return cachedSkull;
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
    
    public InteractiveItem addEnchantment(Enchantment enchantment, int level) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
            itemStack.setItemMeta(meta);
        }
        return this;
    }
    
    public InteractiveItem removeEnchantment(Enchantment enchantment) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.removeEnchant(enchantment);
            itemStack.setItemMeta(meta);
        }
        return this;
    }
    
    public InteractiveItem clearEnchantments() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            Map<Enchantment, Integer> enchants = meta.getEnchants();
            for (Enchantment enchant : enchants.keySet()) {
                meta.removeEnchant(enchant);
            }
            itemStack.setItemMeta(meta);
        }
        return this;
    }
    
    public boolean hasEnchantment(Enchantment enchantment) {
        return itemStack.containsEnchantment(enchantment);
    }
    
    public int getEnchantmentLevel(Enchantment enchantment) {
        return itemStack.getEnchantmentLevel(enchantment);
    }
    
    public Map<Enchantment, Integer> getEnchantments() {
        ItemMeta meta = itemStack.getItemMeta();
        return meta != null ? meta.getEnchants() : new HashMap<>();
    }

    private void setGlowing(ItemStack item, boolean glowing) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (glowing) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.removeEnchant(Enchantment.UNBREAKING);
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
                ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
    }

    private void makeUnique(ItemStack item) {
        ItemNBTUtils.setNBTString(item, getPlugin(), NBT_UNIQUE_ID, UUID.randomUUID().toString());
    }
    
    private void applyEnchantments(ItemStack item, Map<Enchantment, Integer> enchantments) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            
            if (level > 0) {
                meta.addEnchant(enchantment, level, true);
            }
        }
        
        item.setItemMeta(meta);
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

    private long parseDateString(String dateString) throws DateTimeParseException {
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new DateTimeParseException("Empty date string", dateString, 0);
        }
        
        dateString = dateString.trim();
        
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),       
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),    
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),       
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),    
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),       
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),    
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),             
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),             
            DateTimeFormatter.ofPattern("dd-MM-yyyy")              
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
                return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (DateTimeParseException e) {
                 
            }
        }
        
        throw new DateTimeParseException("Unable to parse date: " + dateString, dateString, 0);
    }

    public static ItemStack addExpirationToItemStack(ItemStack itemStack, long expirationTimeMillis) {
        if (itemStack == null) return null;
        
        ItemStack cloned = itemStack.clone();
        JavaPlugin plugin = ItemManager.getPlugin() != null ? ItemManager.getPlugin() :
                JavaPlugin.getProvidingPlugin(InteractiveItem.class);
        
        ItemNBTUtils.setNBTLong(cloned, plugin, NBT_EXPIRATION_TIME, expirationTimeMillis);
        return cloned;
    }

    public static ItemStack addExpirationFromNow(ItemStack itemStack, long durationMillis) {
        if (itemStack == null) return null;
        
        long expirationTime = System.currentTimeMillis() + durationMillis;
        return addExpirationToItemStack(itemStack, expirationTime);
    }

    public static ItemStack addExpirationDate(ItemStack itemStack, String dateString) {
        if (itemStack == null) return null;
        
        try {
            long timestamp = parseStaticDateString(dateString);
            return addExpirationToItemStack(itemStack, timestamp);
        } catch (DateTimeParseException e) {
            logInternalWarn("Invalid expiration date format: " + dateString + ". Use formats like '24/12/2025 15:00'");
            return itemStack;
        }
    }

    public static ItemStack removeExpiration(ItemStack itemStack) {
        if (itemStack == null) return null;
        
        ItemStack cloned = itemStack.clone();
        JavaPlugin plugin = ItemManager.getPlugin() != null ? ItemManager.getPlugin() :
                JavaPlugin.getProvidingPlugin(InteractiveItem.class);
        
        ItemNBTUtils.removeNBTValue(cloned, plugin, NBT_EXPIRATION_TIME);
        return cloned;
    }

    public static boolean hasExpirationTime(ItemStack itemStack) {
        if (itemStack == null) return false;
        
        JavaPlugin plugin = ItemManager.getPlugin() != null ? ItemManager.getPlugin() :
                JavaPlugin.getProvidingPlugin(InteractiveItem.class);
        
        return ItemNBTUtils.hasNBTValue(itemStack, plugin, NBT_EXPIRATION_TIME, PersistentDataType.LONG);
    }

    public static boolean isItemStackExpired(ItemStack itemStack) {
        if (!hasExpirationTime(itemStack)) return false;
        
        JavaPlugin plugin = ItemManager.getPlugin() != null ? ItemManager.getPlugin() :
                JavaPlugin.getProvidingPlugin(InteractiveItem.class);
        
        long expirationTime = ItemNBTUtils.getNBTLong(itemStack, plugin, NBT_EXPIRATION_TIME, 0L);
        return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
    }

    public static long getItemStackRemainingTime(ItemStack itemStack) {
        if (!hasExpirationTime(itemStack)) return -1;
        
        JavaPlugin plugin = ItemManager.getPlugin() != null ? ItemManager.getPlugin() :
                JavaPlugin.getProvidingPlugin(InteractiveItem.class);
        
        long expirationTime = ItemNBTUtils.getNBTLong(itemStack, plugin, NBT_EXPIRATION_TIME, 0L);
        return Math.max(0, expirationTime - System.currentTimeMillis());
    }

    public static String getItemStackFormattedRemainingTime(ItemStack itemStack) {
        long remainingTime = getItemStackRemainingTime(itemStack);
        if (remainingTime <= 0) return "Expirado";
        if (remainingTime == -1) return "Sin expiración";
        return TimeFormatter.timeFormatter.format(remainingTime);
    }

    public static ItemStack applyExpirationFromString(ItemStack itemStack, String expirationString) {
        if (itemStack == null || expirationString == null || expirationString.trim().isEmpty()) {
            return itemStack;
        }
        
        String trimmed = expirationString.trim();
        
        try {
             
            long duration = Long.parseLong(trimmed);
            return addExpirationFromNow(itemStack, duration);
        } catch (NumberFormatException e) {
             
            return addExpirationDate(itemStack, trimmed);
        }
    }

    public static ItemStack addExpirationWithBehavior(ItemStack itemStack, long expirationTimeMillis, ExpirationBehavior behavior) {
        if (itemStack == null) return null;
        
        ItemStack cloned = addExpirationToItemStack(itemStack, expirationTimeMillis);
        if (behavior != null) {
            JavaPlugin plugin = ItemManager.getPlugin() != null ? ItemManager.getPlugin() :
                    JavaPlugin.getProvidingPlugin(InteractiveItem.class);
            ItemNBTUtils.setNBTString(cloned, plugin, NBT_EXPIRATION_BEHAVIOR, behavior.getConfigName());
        }
        return cloned;
    }

    public static ItemStack addExpirationFromNowWithBehavior(ItemStack itemStack, long durationMillis, ExpirationBehavior behavior) {
        if (itemStack == null) return null;
        
        long expirationTime = System.currentTimeMillis() + durationMillis;
        return addExpirationWithBehavior(itemStack, expirationTime, behavior);
    }

    public static ItemStack addExpirationDateWithBehavior(ItemStack itemStack, String dateString, ExpirationBehavior behavior) {
        if (itemStack == null) return null;
        
        try {
            long timestamp = parseStaticDateString(dateString);
            return addExpirationWithBehavior(itemStack, timestamp, behavior);
        } catch (DateTimeParseException e) {
            logInternalWarn("Invalid expiration date format: " + dateString + ". Use formats like '24/12/2025 15:00'");
            return itemStack;
        }
    }

    public static ExpirationBehavior getItemStackExpirationBehavior(ItemStack itemStack) {
        if (itemStack == null) return ExpirationBehavior.KEEP;
        
        JavaPlugin plugin = ItemManager.getPlugin() != null ? ItemManager.getPlugin() :
                JavaPlugin.getProvidingPlugin(InteractiveItem.class);
        
        if (!ItemNBTUtils.hasNBTValue(itemStack, plugin, NBT_EXPIRATION_BEHAVIOR, PersistentDataType.STRING)) {
            return ExpirationBehavior.KEEP;
        }
        
        String behaviorStr = ItemNBTUtils.getNBTString(itemStack, plugin, NBT_EXPIRATION_BEHAVIOR);
        return ExpirationBehavior.fromString(behaviorStr);
    }

    public static boolean shouldRemoveExpiredItem(ItemStack itemStack) {
        return isItemStackExpired(itemStack) && getItemStackExpirationBehavior(itemStack) == ExpirationBehavior.REMOVE;
    }

    public static boolean shouldDisableExpiredItem(ItemStack itemStack) {
        return isItemStackExpired(itemStack) && getItemStackExpirationBehavior(itemStack) == ExpirationBehavior.DISABLE;
    }

    public static boolean canUseItem(ItemStack itemStack) {
        if (!hasExpirationTime(itemStack)) return true;
        if (!isItemStackExpired(itemStack)) return true;
        
        ExpirationBehavior behavior = getItemStackExpirationBehavior(itemStack);
        return behavior != ExpirationBehavior.DISABLE && behavior != ExpirationBehavior.REMOVE;
    }

    private static long parseStaticDateString(String dateString) throws DateTimeParseException {
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new DateTimeParseException("Empty date string", dateString, 0);
        }
        
        dateString = dateString.trim().replace("_", " ");
        
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),       
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),    
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),       
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),    
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),       
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),    
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),             
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),             
            DateTimeFormatter.ofPattern("dd-MM-yyyy")              
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
                return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (DateTimeParseException e) {
                 
            }
        }
        
        throw new DateTimeParseException("Unable to parse date: " + dateString, dateString, 0);
    }

    public static void processExpiredItemsForPlayer(org.bukkit.entity.Player player) {
        net.exylia.commons.item.expiration.ExpirationManager manager = 
            net.exylia.commons.item.expiration.ExpirationManager.getInstance();
        if (manager != null) {
            manager.checkPlayerInventoryForExpiredItems(player);
        }
    }

    public static ItemStack updateExpirationPlaceholders(ItemStack itemStack) {
        if (itemStack == null || !hasExpirationTime(itemStack)) {
            return itemStack;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return itemStack;
        }

        List<net.kyori.adventure.text.Component> originalLore = AdapterFactory.getItemMetaAdapter().getLore(meta);
        if (originalLore == null || originalLore.isEmpty()) {
            return itemStack;
        }

        boolean hasExpirationPlaceholders = false;
        List<net.kyori.adventure.text.Component> updatedLore = new ArrayList<>();

        for (net.kyori.adventure.text.Component component : originalLore) {
            String loreText = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component);
            
            if (loreText.contains("%expiration_") || loreText.contains("%is_expired%")) {
                hasExpirationPlaceholders = true;
                String updatedText = processExpirationPlaceholdersInText(loreText, itemStack);
                updatedLore.add(ColorUtils.parse(updatedText));
            } else {
                updatedLore.add(component);
            }
        }

        if (hasExpirationPlaceholders) {
            ItemStack cloned = itemStack.clone();
            ItemMeta clonedMeta = cloned.getItemMeta();
            if (clonedMeta != null) {
                AdapterFactory.getItemMetaAdapter().setLore(clonedMeta, updatedLore);
                cloned.setItemMeta(clonedMeta);
            }
            return cloned;
        }

        return itemStack;
    }

    private static String processExpirationPlaceholdersInText(String text, ItemStack itemStack) {
        if (!hasExpirationTime(itemStack)) {
            return text.replace("%expiration_remaining%", "Sin expiración")
                    .replace("%expiration_date%", "Sin expiración")
                    .replace("%is_expired%", "false");
        }

        boolean isExpired = isItemStackExpired(itemStack);
        long remainingTime = getItemStackRemainingTime(itemStack);
        String remainingTimeFormatted = remainingTime <= 0 ? "Expirado" : 
            (remainingTime == -1 ? "Sin expiración" : TimeFormatter.timeFormatter.format(remainingTime));

        JavaPlugin plugin = ItemManager.getPlugin() != null ? ItemManager.getPlugin() :
                JavaPlugin.getProvidingPlugin(InteractiveItem.class);
        long expirationTime = ItemNBTUtils.getNBTLong(itemStack, plugin, NBT_EXPIRATION_TIME, 0L);
        String expirationDateFormatted = expirationTime > 0 ? TimeFormatter.timeFormatter.format(expirationTime) : "Sin expiración";

        return text.replace("%expiration_remaining%", remainingTimeFormatted)
                .replace("%expiration_date%", expirationDateFormatted)
                .replace("%is_expired%", String.valueOf(isExpired));
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
            updateSkullIfNeeded();
        });
    }

    private void updateSkullIfNeeded() {
        if (pendingPlayerName == null || !awaitingPlayerSkull) return;
        
        ItemStack updatedSkull = createPlayerSkull(pendingPlayerName);
        if (isRealPlayerSkull(updatedSkull, pendingPlayerName)) {
            DebugUtils.logInternalDebug("updateSkullIfNeeded: Player skull now available for " + pendingPlayerName);
            this.itemStack = updatedSkull;
            this.awaitingPlayerSkull = false;
            this.pendingPlayerName = null;
             
            if (!usesPlaceholders()) {
                this.dynamicSkullUpdate = false;
            }
        }
    }

    public void updatePlaceholders(Player player, EquipmentSlot hand) {
         
        if (awaitingPlayerSkull && pendingPlayerName != null) {
            updateSkullIfNeeded();
        }
        
        if (!usesPlaceholders()) return;

        ItemConfiguration freshConfig = ItemManager.getItemConfiguration(configId);
        if (freshConfig != null) {
            this.config = freshConfig;
        }

        Player targetPlayer = placeholderPlayer != null ? placeholderPlayer : player;
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

    public boolean needsDynamicUpdate() {
        return dynamicSkullUpdate || awaitingPlayerSkull || usesPlaceholders();
    }

    @Override
    public InteractiveItem clone() {
        InteractiveItem clone = new InteractiveItem(this.itemStack.clone(), this.configId, this.config);
        clone.clickHandler = this.clickHandler;
        clone.placeholderPlayer = this.placeholderPlayer;
        clone.context = this.context.copy();
        clone.awaitingPlayerSkull = this.awaitingPlayerSkull;
        clone.pendingPlayerName = this.pendingPlayerName;
        clone.dynamicSkullUpdate = this.dynamicSkullUpdate;
        return clone;
    }
}

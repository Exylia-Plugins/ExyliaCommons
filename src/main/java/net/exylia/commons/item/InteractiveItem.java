// ==================== INTERACTIVE ITEM MODERNIZADO ====================

package net.exylia.commons.item;

import lombok.Getter;
import net.exylia.commons.actions.ActionContext;
import net.exylia.commons.actions.GlobalActionManager;
import net.exylia.commons.command.CommandExecutor;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static net.exylia.commons.utils.DebugUtils.logInternalWarn;
import static net.exylia.commons.utils.SkullUtils.*;

/**
 * InteractiveItem modernizado que usa el sistema unificado de placeholders
 * Elimina la dependencia de sistemas de placeholders internos
 */
public class InteractiveItem {

    // NBT Keys para persistencia
    private static final String NBT_ITEM_ID = "interactive_item_id";
    private static final String NBT_CURRENT_USES = "current_uses";
    private static final String NBT_UNIQUE_ID = "unique_id";
    private static final String NBT_CREATION_TIME = "creation_time";

    // Componentes del sistema
    private final PlaceholderSystemManager placeholderManager = PlaceholderSystemManager.getInstance();
    private final ItemMetaAdapter adapter = AdapterFactory.getItemMetaAdapter();
    private final ItemStack itemStack;

    // Configuración desde memoria
    private String configId;
    private ItemConfiguration config;

    // Contextos para placeholders
    private Player placeholderPlayer;

    // Handler temporal (no persistente)
    @Getter
    private Consumer<ItemClickInfo> clickHandler;

    // ==================== CONSTRUCTORES ====================

    /**
     * Constructor para crear desde configuración
     */
    public InteractiveItem(String configId, ItemConfiguration config) {
        this.configId = configId;
        this.config = config;
        this.itemStack = createItemFromConfig(config);
        setItemId(configId);
        setCreationTime(System.currentTimeMillis());
        initializeUses();

        if (config.getAmount() > 1) {
            this.itemStack.setAmount(config.getAmount());
        }
    }

    /**
     * Constructor para crear desde config con jugador para placeholders
     */
    public InteractiveItem(String configId, ItemConfiguration config, Player player) {
        this.configId = configId;
        this.config = config;
        this.placeholderPlayer = player;
        this.itemStack = createItemFromConfig(config, player);
        setItemId(configId);
        setCreationTime(System.currentTimeMillis());
        initializeUses();

        if (config.getAmount() > 1) {
            this.itemStack.setAmount(config.getAmount());
        }
    }

    /**
     * Constructor para reconstruir desde ItemStack existente
     */
    private InteractiveItem(ItemStack itemStack, String configId, ItemConfiguration config) {
        this.itemStack = itemStack.clone();
        this.configId = configId;
        this.config = config;
    }

    /**
     * Crea un InteractiveItem desde un ItemStack existente
     */
    public static InteractiveItem fromItemStack(ItemStack itemStack) {
        String itemId = getItemIdFromStack(itemStack);
        if (itemId == null) return null;

        ItemConfiguration config = ItemManager.getItemConfiguration(itemId);
        if (config == null) {
            logInternalWarn("No se encontró configuración para item ID: " + itemId);
            return null;
        }

        return new InteractiveItem(itemStack, itemId, config);
    }

// ==================== GESTIÓN DE CONTEXTOS CON EXYLIACONTEXT ====================

    @Getter
    private ExyliaContext context = ExyliaContext.create();

    /**
     * Establece el contexto completo
     */
    public InteractiveItem withContext(ExyliaContext context) {
        this.context = context != null ? context : ExyliaContext.create();
        return this;
    }

    /**
     * Añade un objeto al contexto
     */
    public InteractiveItem addToContext(Object object) {
        this.context.add(object);
        return this;
    }

    /**
     * Añade múltiples objetos al contexto
     */
    public InteractiveItem addToContext(Object... objects) {
        this.context.addAll(objects);
        return this;
    }

    /**
     * Añade datos con clave al contexto
     */
    public InteractiveItem addToContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    /**
     * Limpia el contexto
     */
    public InteractiveItem clearContext() {
        this.context = ExyliaContext.create();
        return this;
    }

    // ==================== CONFIGURACIÓN TEMPORAL ====================

    public InteractiveItem setClickHandler(Consumer<ItemClickInfo> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public InteractiveItem setPlaceholderPlayer(Player player) {
        this.placeholderPlayer = player;
        return this;
    }

    // ==================== GETTERS DE CONFIGURACIÓN ====================

    public String getId() { return configId; }
    public String getRawName() { return config.getName(); }
    public List<String> getRawLore() { return config.getLore(); }
    public String getRawMaterialString() { return config.getMaterial(); }
    public boolean usesPlaceholders() { return config.usesPlaceholders(); }
    public List<String> getCommands() { return config.getCommands(); }
    public String getAction() { return config.getAction(); }
    public boolean shouldConsumeOnUse() { return config.shouldConsumeOnUse(); }
    public boolean shouldCancelEvent() { return config.shouldCancelEvent(); }
    public int getMaxUses() { return config.getMaxUses(); }
    public boolean isStackable() { return config.isStackable(); }
    public String getUsesDisplayFormat() { return config.getUsesDisplayFormat(); }
    public boolean shouldShowUsesInLore() { return config.shouldShowUsesInLore(); }
    public boolean shouldShowUsesInName() { return config.shouldShowUsesInName(); }

    // ==================== GESTIÓN DE USOS ====================

    public int getCurrentUses() {
        return getNBTInt(NBT_CURRENT_USES, getMaxUses());
    }

    public InteractiveItem setCurrentUses(int uses) {
        setNBTInt(NBT_CURRENT_USES, uses);
        updateUsesDisplay();
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

    // ==================== PROCESAMIENTO CON SISTEMA UNIFICADO ====================

    /**
     * Actualiza placeholders usando el sistema unificado
     */
    public void updatePlaceholders(Player player) {
        if (!usesPlaceholders()) return;

        // Recargar configuración si es necesario
        ItemConfiguration freshConfig = ItemManager.getItemConfiguration(configId);
        if (freshConfig != null) {
            this.config = freshConfig;
        }

        Player targetPlayer = (placeholderPlayer != null) ? placeholderPlayer : player;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        // Preparar contexto completo añadiendo este item
        ExyliaContext fullContext = context.copy().add(this);

        // Actualizar nombre
        String rawName = getRawName();
        if (rawName != null) {
            String processedName = fullContext.processPlaceholders(rawName, targetPlayer);

            // Procesar placeholder de usos si es necesario
            if (hasLimitedUses() && processedName.contains("%uses%")) {
                String usesText = getUsesDisplayFormat()
                        .replace("%current%", String.valueOf(getCurrentUses()))
                        .replace("%max%", String.valueOf(getMaxUses()));
                processedName = processedName.replace("%uses%", usesText);
            }

            adapter.setDisplayName(meta, ColorUtils.parse(processedName));
        }

        // Actualizar lore
        List<String> rawLore = getRawLore();
        if (!rawLore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : rawLore) {
                String processedLine = fullContext.processPlaceholders(line, targetPlayer);

                // Procesar placeholder de usos
                if (hasLimitedUses() && processedLine.contains("%uses%")) {
                    String usesText = getUsesDisplayFormat()
                            .replace("%current%", String.valueOf(getCurrentUses()))
                            .replace("%max%", String.valueOf(getMaxUses()));
                    processedLine = processedLine.replace("%uses%", usesText);
                }

                loreComponents.add(ColorUtils.parse(processedLine));
            }

            // Añadir usos si está habilitado y no está en lore original
            if (shouldShowUsesInLore() && hasLimitedUses() &&
                    rawLore.stream().noneMatch(line -> line.contains("%uses%"))) {
                loreComponents.add(ColorUtils.parse(""));
                String usesText = getUsesDisplayFormat()
                        .replace("%current%", String.valueOf(getCurrentUses()))
                        .replace("%max%", String.valueOf(getMaxUses()));
                loreComponents.add(ColorUtils.parse(usesText));
            }

            adapter.setLore(meta, loreComponents);
        }

        itemStack.setItemMeta(meta);
    }

    // ==================== ACCIONES Y COMANDOS ====================

    public boolean hasAction() {
        String action = getAction();
        return action != null && !action.trim().isEmpty();
    }

    public boolean executeAction(ItemClickInfo clickInfo) {
        if (hasAction()) {
            // Preparar contexto para la acción
            ExyliaContext actionContext = context.copy().add(this);

            ActionContext context = new ActionContext(clickInfo.player(), clickInfo.source())
                    .withData("clickType", clickInfo.clickType())
                    .withData("slot", clickInfo.slot())
                    .withData("item", this)
                    .withData("itemStack", clickInfo.itemStack())
                    .withData("itemConfiguration", this.getConfiguration())
                    .withData("contexts", actionContext.getAllObjects());

            return GlobalActionManager.executeAction(getAction(), context);
        }
        return false;
    }

    public void executeCommands(Player player) {
        // Preparar contexto para comandos añadiendo este item
        ExyliaContext commandContext = context.copy().add(this);
        if (placeholderPlayer != null) commandContext.add(placeholderPlayer);

        CommandExecutor.builder(player)
                .withPlaceholderPlayer(placeholderPlayer)
                .withPlaceholderContext(commandContext.getAllObjects())
                .execute(getCommands());
    }

    // ==================== CREACIÓN Y CONFIGURACIÓN ====================

    /**
     * Inicializa los usos del item
     */
    private void initializeUses() {
        int maxUses = getMaxUses();
        if (maxUses > 0) {
            if (!hasNBTValue(NBT_CURRENT_USES)) {
                setCurrentUses(maxUses);
            }
            updateUsesDisplay();
        }
    }

    /**
     * Crea ItemStack desde configuración
     */
    private ItemStack createItemFromConfig(ItemConfiguration config) {
        return createItemFromConfig(config, null);
    }

    /**
     * Crea ItemStack desde configuración con jugador
     */
    private ItemStack createItemFromConfig(ItemConfiguration config, Player player) {
        String materialString = config.getMaterial();

        // Procesar placeholders en material si hay jugador
        if (player != null && containsPlaceholders(materialString)) {
            ExyliaContext fullContext = context.copy().add(this);
            materialString = fullContext.processPlaceholders(materialString, player);
        }

        ItemStack item = createItemFromString(materialString);

        // Aplicar propiedades básicas
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Nombre
            if (config.getName() != null) {
                String name = config.getName();
                if (player != null && config.usesPlaceholders()) {
                    ExyliaContext fullContext = context.copy().add(this);
                    name = fullContext.processPlaceholders(name, player);
                }
                adapter.setDisplayName(meta, ColorUtils.parse(name));
            }

            // Lore
            if (!config.getLore().isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : config.getLore()) {
                    String processedLine = line;
                    if (player != null && config.usesPlaceholders()) {
                        ExyliaContext fullContext = context.copy().add(this);
                        processedLine = fullContext.processPlaceholders(line, player);
                    }
                    loreComponents.add(ColorUtils.parse(processedLine));
                }
                adapter.setLore(meta, loreComponents);
            }

            item.setItemMeta(meta);
        }

        // Aplicar propiedades adicionales...
        if (config.isGlowing()) {
            setGlowing(item, true);
        }

        if (config.shouldHideAttributes()) {
            hideAllAttributes(item);
        }

        if (!config.isStackable()) {
            makeUnique(item);
        }

        return item;
    }

    /**
     * Actualiza el display de usos
     */
    private void updateUsesDisplay() {
        if (!hasLimitedUses()) return;

        String usesText = getUsesDisplayFormat()
                .replace("%current%", String.valueOf(getCurrentUses()))
                .replace("%max%", String.valueOf(getMaxUses()));

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        // Actualizar nombre si está habilitado
        if (shouldShowUsesInName()) {
            String rawName = getRawName();
            if (rawName != null) {
                String displayName = rawName.contains("%uses%")
                        ? rawName.replace("%uses%", usesText)
                        : rawName + " " + usesText;
                adapter.setDisplayName(meta, ColorUtils.parse(displayName));
            }
        }

        // Actualizar lore si está habilitado
        if (shouldShowUsesInLore()) {
            List<Component> loreComponents = new ArrayList<>();
            List<String> rawLore = getRawLore();

            for (String line : rawLore) {
                if (line.contains("%uses%")) {
                    line = line.replace("%uses%", usesText);
                }
                loreComponents.add(ColorUtils.parse(line));
            }

            // Añadir línea de usos si no está en el lore original
            if (rawLore.stream().noneMatch(line -> line.contains("%uses%"))) {
                if (!rawLore.isEmpty()) loreComponents.add(ColorUtils.parse(""));
                loreComponents.add(ColorUtils.parse(usesText));
            }

            adapter.setLore(meta, loreComponents);
        }

        itemStack.setItemMeta(meta);
    }

    // ==================== MÉTODOS AUXILIARES ====================

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

        try {
            Material material = Material.valueOf(materialString.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            logInternalWarn("Invalid material: " + materialString + ", using STONE");
            return new ItemStack(Material.STONE);
        }
    }

    // ==================== NBT HELPERS ====================

    private static String getItemIdFromStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        NamespacedKey key = new NamespacedKey(ItemManager.getPlugin(), NBT_ITEM_ID);
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private boolean hasNBTValue(String key) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        NamespacedKey namespacedKey = new NamespacedKey(getPlugin(), key);
        return meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.INTEGER);
    }

    private int getNBTInt(String key, int defaultValue) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return defaultValue;
        NamespacedKey namespacedKey = new NamespacedKey(getPlugin(), key);
        return meta.getPersistentDataContainer().getOrDefault(namespacedKey, PersistentDataType.INTEGER, defaultValue);
    }

    private void setNBTInt(String key, int value) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        NamespacedKey namespacedKey = new NamespacedKey(getPlugin(), key);
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.INTEGER, value);
        itemStack.setItemMeta(meta);
    }

    private void setItemId(String id) {
        setNBTString(NBT_ITEM_ID, id);
    }

    private void setNBTString(String key, String value) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        NamespacedKey namespacedKey = new NamespacedKey(getPlugin(), key);
        if (value != null) {
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
        } else {
            meta.getPersistentDataContainer().remove(namespacedKey);
        }
        itemStack.setItemMeta(meta);
    }

    private void setCreationTime(long time) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        NamespacedKey key = new NamespacedKey(getPlugin(), NBT_CREATION_TIME);
        meta.getPersistentDataContainer().set(key, PersistentDataType.LONG, time);
        itemStack.setItemMeta(meta);
    }

    public long getCreationTime() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return 0;
        NamespacedKey key = new NamespacedKey(getPlugin(), NBT_CREATION_TIME);
        return meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.LONG, 0L);
    }

    // ==================== UTILIDADES VISUALES ====================

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
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey uniqueKey = new NamespacedKey(getPlugin(), NBT_UNIQUE_ID);
        meta.getPersistentDataContainer().set(uniqueKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        item.setItemMeta(meta);
    }

    // ==================== GETTERS FINALES ====================

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
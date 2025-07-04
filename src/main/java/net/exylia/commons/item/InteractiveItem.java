package net.exylia.commons.item;

import net.exylia.commons.actions.ActionContext;
import net.exylia.commons.actions.GlobalActionManager;
import net.exylia.commons.command.CommandExecutor;
import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.ItemMetaAdapter;
import me.clip.placeholderapi.PlaceholderAPI;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static net.exylia.commons.ExyliaPlugin.isPlaceholderAPIEnabled;
import static net.exylia.commons.utils.DebugUtils.logWarn;
import static net.exylia.commons.utils.SkullUtils.*;

/**
 * InteractiveItem con enfoque híbrido:
 * - En NBT: Solo ID del item, usos actuales y datos críticos de persistencia
 * - En memoria: Configuración, comandos, acciones (desde config/registro)
 */
public class InteractiveItem {

    // SOLO estos datos van en NBT (datos que DEBEN persistir)
    private static final String NBT_ITEM_ID = "interactive_item_id";
    private static final String NBT_CURRENT_USES = "current_uses";
    private static final String NBT_UNIQUE_ID = "unique_id"; // Para items no-stackeable
    private static final String NBT_CREATION_TIME = "creation_time"; // Para tracking

    // Datos en memoria (se obtienen del registro/config)
    private final ItemStack itemStack;
    private final ItemMetaAdapter adapter = AdapterFactory.getItemMetaAdapter();

    // Configuración dinámica (NO se persiste en NBT)
    private String configId; // ID para buscar en configuración
    private ItemConfiguration config; // Configuración cargada desde memoria

    // Datos temporales (NO se persisten)
    private Consumer<ItemClickInfo> clickHandler;
    private Player placeholderPlayer;
    private Object placeholderContext;

    /**
     * Constructor para crear desde configuración
     */
    public InteractiveItem(String configId, ItemConfiguration config) {
        this.configId = configId;
        this.config = config;
        this.itemStack = createItemFromConfig(config);
        setItemId(configId);
        setCreationTime(System.currentTimeMillis());

        // ARREGLO: Inicializar usos correctamente al crear
        initializeUses();

        // Aplicar cantidad si está especificada
        if (config.getAmount() > 1) {
            this.itemStack.setAmount(config.getAmount());
        }
    }

    /**
     * Constructor para crear desde config con placeholders
     */
    public InteractiveItem(String configId, ItemConfiguration config, Player player) {
        this.configId = configId;
        this.config = config;
        this.placeholderPlayer = player;
        this.itemStack = createItemFromConfig(config, player);
        setItemId(configId);
        setCreationTime(System.currentTimeMillis());

        // ARREGLO: Inicializar usos correctamente al crear
        initializeUses();

        // Aplicar cantidad si está especificada
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
     * Busca la configuración en memoria usando el ID
     */
    public static InteractiveItem fromItemStack(ItemStack itemStack) {
        String itemId = getItemIdFromStack(itemStack);
        if (itemId == null) return null;

        // Buscar configuración en el registro de items
        ItemConfiguration config = ItemManager.getItemConfiguration(itemId);
        if (config == null) {
            logWarn("No se encontró configuración para item ID: " + itemId);
            return null;
        }

        return new InteractiveItem(itemStack, itemId, config);
    }

    /**
     * Obtiene el ID del item desde el NBT del ItemStack
     */
    private static String getItemIdFromStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        NamespacedKey key = new NamespacedKey(ItemManager.getPlugin(), NBT_ITEM_ID);
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    // ===== GETTERS QUE USAN CONFIGURACIÓN EN MEMORIA =====

    public String getId() {
        return configId;
    }

    public String getRawName() {
        return config.getName();
    }

    public List<String> getRawLore() {
        return config.getLore();
    }

    public String getRawMaterialString() {
        return config.getMaterial();
    }

    public boolean usesPlaceholders() {
        return config.usesPlaceholders();
    }

    public List<String> getCommands() {
        return config.getCommands();
    }

    public String getAction() {
        return config.getAction();
    }

    public boolean shouldConsumeOnUse() {
        return config.shouldConsumeOnUse();
    }

    public boolean shouldCancelEvent() {
        return config.shouldCancelEvent();
    }

    public int getMaxUses() {
        return config.getMaxUses();
    }

    public boolean isStackable() {
        return config.isStackable();
    }

    public String getUsesDisplayFormat() {
        return config.getUsesDisplayFormat();
    }

    public boolean shouldShowUsesInLore() {
        return config.shouldShowUsesInLore();
    }

    public boolean shouldShowUsesInName() {
        return config.shouldShowUsesInName();
    }

    // ===== MÉTODOS DE INICIALIZACIÓN =====

    /**
     * Inicializa correctamente los usos del item
     */
    private void initializeUses() {
        int maxUses = getMaxUses();
        if (maxUses > 0) {
            // Solo establecer usos actuales si no están ya establecidos
            if (!hasNBTValue(NBT_CURRENT_USES)) {
                setCurrentUses(maxUses);
            }
            // Actualizar display después de establecer usos
            updateUsesDisplay();
        }
    }

    /**
     * Verifica si existe un valor NBT específico
     */
    private boolean hasNBTValue(String key) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        NamespacedKey namespacedKey = new NamespacedKey(getPlugin(), key);
        return meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.INTEGER);
    }

    public int getCurrentUses() {
        return getNBTInt(NBT_CURRENT_USES, getMaxUses());
    }

    public InteractiveItem setCurrentUses(int uses) {
        setNBTInt(NBT_CURRENT_USES, uses);
        updateUsesDisplay();
        return this;
    }

    private void setItemId(String id) {
        setNBTString(NBT_ITEM_ID, id);
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

    // ===== MÉTODOS NBT HELPERS =====

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

    // ===== MÉTODOS DE CONFIGURACIÓN TEMPORAL (NO PERSISTENTES) =====

    public InteractiveItem setClickHandler(Consumer<ItemClickInfo> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public InteractiveItem setPlaceholderPlayer(Player player) {
        this.placeholderPlayer = player;
        return this;
    }

    public InteractiveItem setPlaceholderContext(Object context) {
        this.placeholderContext = context;
        return this;
    }

    public Consumer<ItemClickInfo> getClickHandler() {
        return clickHandler;
    }

    // ===== MÉTODOS PARA MANTENER COMPATIBILIDAD =====

    /**
     * Actualiza la cantidad del ItemStack
     * @param amount Nueva cantidad
     * @return Este item para encadenamiento
     */
    public InteractiveItem setAmount(int amount) {
        itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    /**
     * Establece si el item debe brillar
     * @param glowing true para brillar
     * @return Este item para encadenamiento
     */
    public InteractiveItem setGlowing(boolean glowing) {
        setGlowing(itemStack, glowing);
        return this;
    }

    /**
     * Oculta todos los atributos del item
     * @return Este item para encadenamiento
     */
    public InteractiveItem hideAllAttributes() {
        hideAllAttributes(itemStack);
        return this;
    }

    // ===== MÉTODOS DE NEGOCIO =====

    public boolean hasAction() {
        String action = getAction();
        return action != null && !action.trim().isEmpty();
    }

    public boolean hasLimitedUses() {
        return getMaxUses() > 0;
    }

    public boolean hasUsesRemaining() {
        return getMaxUses() == -1 || getCurrentUses() > 0;
    }

    public boolean consumeUse() {
        int maxUses = getMaxUses();
        if (maxUses == -1) return true; // Usos infinitos

        int currentUses = getCurrentUses();
        if (currentUses > 0) {
            int newUses = currentUses - 1;
            setCurrentUses(newUses);
            return newUses > 0; // ARREGLO: Retorna false cuando llega a 0 usos
        }
        return false; // Ya no tiene usos
    }

    public boolean executeAction(ItemClickInfo clickInfo) {
        if (hasAction()) {
            ActionContext context = new ActionContext(clickInfo.player(), clickInfo.source())
                    .withData("clickType", clickInfo.clickType())
                    .withData("slot", clickInfo.slot())
                    .withData("item", this)
                    .withData("itemStack", clickInfo.itemStack())
                    .withData("itemConfiguration", this.getConfiguration());

            return GlobalActionManager.executeAction(getAction(), context);
        }
        return false;
    }

    public void executeCommands(Player player) {
        CommandExecutor.builder(player)
                .withPlaceholderPlayer(placeholderPlayer)
                .withPlaceholderContext(placeholderContext)
                .execute(getCommands());
    }

    // ===== CREACIÓN Y ACTUALIZACIÓN DE VISUAL =====

    private ItemStack createItemFromConfig(ItemConfiguration config) {
        return createItemFromConfig(config, null);
    }

    private ItemStack createItemFromConfig(ItemConfiguration config, Player player) {
        String materialString = config.getMaterial();

        // Procesar placeholders en material si hay jugador
        if (player != null && containsPlaceholders(materialString)) {
            materialString = processPlaceholders(materialString, player);
        }

        ItemStack item = createItemFromString(materialString);

        // Aplicar propiedades básicas
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Nombre
            if (config.getName() != null) {
                String name = config.getName();
                if (player != null && config.usesPlaceholders()) {
                    name = processPlaceholders(name, player);
                }
                adapter.setDisplayName(meta, ColorUtils.parse(name));
            }

            // Lore
            if (!config.getLore().isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : config.getLore()) {
                    String processedLine = line;
                    if (player != null && config.usesPlaceholders()) {
                        processedLine = processPlaceholders(line, player);
                    }
                    loreComponents.add(ColorUtils.parse(processedLine));
                }
                adapter.setLore(meta, loreComponents);
            }

            item.setItemMeta(meta);
        }

        // Aplicar propiedades adicionales
        if (config.isGlowing()) {
            setGlowing(item, true);
        }

        if (config.shouldHideAttributes()) {
            hideAllAttributes(item);
        }

        // Hacer único si no es stackeable
        if (!config.isStackable()) {
            makeUnique(item);
        }

        // ARREGLO: No llamar updateUsesDisplay aquí ya que se llama en initializeUses()

        return item;
    }

    private ItemStack createItemFromString(String materialString) {
        if (materialString == null || materialString.isEmpty()) {
            logWarn("Material string is null or empty, using STONE");
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
            logWarn("Invalid material: " + materialString + ", using STONE");
            return new ItemStack(Material.STONE);
        }
    }

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

    public void updatePlaceholders(Player player) {
        if (!usesPlaceholders()) return;

        // Recargar configuración actualizada si es necesario
        ItemConfiguration freshConfig = ItemManager.getItemConfiguration(configId);
        if (freshConfig != null) {
            this.config = freshConfig;
        }

        Player targetPlayer = (placeholderPlayer != null) ? placeholderPlayer : player;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        // Actualizar nombre
        String rawName = getRawName();
        if (rawName != null) {
            String processedName = processPlaceholders(rawName, targetPlayer);

            // Procesar placeholder de usos
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
                String processedLine = processPlaceholders(line, targetPlayer);

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

    // ===== MÉTODOS AUXILIARES =====

    private boolean containsPlaceholders(String text) {
        return text != null && (text.contains("%") || text.contains("{") || text.contains("<"));
    }

    private String processPlaceholders(String text, Player player) {
        if (text == null) return null;

        String processed = text;

        if (placeholderContext != null) {
//            processed = CustomPlaceholderManager.process(processed, placeholderContext);
        }

        if (isPlaceholderAPIEnabled()) {
            processed = PlaceholderAPI.setPlaceholders(player, processed);
        }

        return processed;
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

    // ===== GETTERS FINALES =====

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
        clone.placeholderContext = this.placeholderContext;
        return clone;
    }
}
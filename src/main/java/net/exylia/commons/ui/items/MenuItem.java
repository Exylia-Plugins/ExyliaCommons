package net.exylia.commons.ui.items;

import lombok.Getter;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.versions.ItemMetaAdapter;
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
import java.util.function.Supplier;

import static net.exylia.commons.utils.skull.SkullUtils.*;

/**
 * MenuItem modernizado que usa ExyliaContext para el manejo de contextos
 */
public class MenuItem {

    @Getter
    private final String id;
    private final ItemMetaAdapter adapter = AdapterFactory.getItemMetaAdapter();

    // Propiedades del item (raw = sin procesar)
    private ItemStack itemStack;
    @Getter
    private String rawMaterial;
    @Getter
    private String rawName;
    private List<String> rawLore;
    private Supplier<List<String>> loreDynamicSupplier;
    @Getter
    private String rawAmount;

    // Contexto modernizado
    private ExyliaContext context = ExyliaContext.create();

    // Comportamiento
    private boolean dynamicUpdate = false;
    @Getter
    private long updateInterval = 20L;
    @Getter
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

    // ==================== CONFIGURACIÓN BÁSICA ====================

    /**
     * Establece el nombre (con soporte de placeholders)
     */
    public MenuItem setName(String name) {
        this.rawName = name;
        return this;
    }

    /**
     * Establece el nombre directamente como Component
     */
    public MenuItem setName(Component name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            adapter.setDisplayName(meta, name);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    /**
     * Establece el lore (con soporte de placeholders)
     */
    public MenuItem setLore(String... lore) {
        this.rawLore = Arrays.asList(lore);
        this.loreDynamicSupplier = null; // Limpiar supplier dinámico
        return this;
    }

    /**
     * Establece el lore desde lista
     */
    public MenuItem setLoreList(List<String> lore) {
        this.rawLore = new ArrayList<>(lore);
        this.loreDynamicSupplier = null; // Limpiar supplier dinámico
        return this;
    }

    /**
     * Establece el lore dinámico usando un Supplier
     */
    public MenuItem setLore(Supplier<List<String>> loreSupplier) {
        this.loreDynamicSupplier = loreSupplier;
        this.rawLore = null; // Limpiar lore estático
        return this;
    }

    /**
     * Establece el lore directamente como Components
     */
    public MenuItem setLore(List<Component> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            adapter.setLore(meta, lore);
            itemStack.setItemMeta(meta);
        }
        // Limpiar tanto lore estático como dinámico si se establece directamente
        this.rawLore = null;
        this.loreDynamicSupplier = null;
        return this;
    }

    /**
     * Establece la cantidad
     */
    public MenuItem setAmount(int amount) {
        itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    /**
     * Establece la cantidad con soporte de placeholders
     */
    public MenuItem setAmount(String amountString) {
        this.rawAmount = amountString;
        return this;
    }

    /**
     * Establece el material
     */
    public MenuItem setMaterial(Material material) {
        return setMaterial(material.name());
    }

    /**
     * Establece el material desde string (soporte para heads)
     */
    public MenuItem setMaterial(String materialString) {
        this.rawMaterial = materialString;
        this.itemStack = createItemFromString(materialString);
        return this;
    }

    // ==================== GESTIÓN DE CONTEXTOS CON EXYLIACONTEXT ====================

    /**
     * Establece el contexto completo
     */
    public MenuItem withContext(ExyliaContext context) {
        this.context = context != null ? context : ExyliaContext.create();
        return this;
    }

    /**
     * Añade un objeto al contexto
     */
    public MenuItem addToContext(Object object) {
        this.context.add(object);
        return this;
    }

    /**
     * Añade múltiples objetos al contexto
     */
    public MenuItem addToContext(Object... objects) {
        this.context.addAll(objects);
        return this;
    }

    /**
     * Añade datos con clave al contexto
     */
    public MenuItem addToContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    /**
     * Añade un objeto con tipo específico al contexto
     */
    public <T> MenuItem addToContext(Class<T> type, T object) {
        this.context.add(type, object);
        return this;
    }

    /**
     * Añade datos dinámicos al contexto
     */
    public MenuItem addDynamicToContext(String key, java.util.function.Supplier<Object> supplier) {
        this.context.putDynamic(key, supplier);
        return this;
    }

    /**
     * Limpia el contexto
     */
    public MenuItem clearContext() {
        this.context = ExyliaContext.create();
        return this;
    }

    /**
     * Fusiona otro contexto con el actual
     */
    public MenuItem mergeContext(ExyliaContext otherContext) {
        this.context.merge(otherContext);
        return this;
    }

    /**
     * Obtiene el contexto actual
     */
    public ExyliaContext getContext() {
        return context;
    }

    /**
     * Crea un contexto hijo
     */
    public MenuItem createChildContext() {
        this.context = this.context.createChild();
        return this;
    }

    // ==================== PROPIEDADES VISUALES ====================

    /**
     * Establece efecto de brillo
     */
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

    /**
     * Añade flags al item
     */
    public MenuItem addItemFlags(ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    /**
     * Oculta todos los atributos
     */
    public MenuItem hideAllAttributes() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    // ==================== COMPORTAMIENTO ====================

    /**
     * Establece el manejador de clicks
     */
    public MenuItem setClickHandler(Consumer<MenuClickEvent> handler) {
        this.clickHandler = handler;
        return this;
    }

    /**
     * Habilita/deshabilita actualización dinámica
     */
    public MenuItem setDynamicUpdate(boolean dynamic) {
        this.dynamicUpdate = dynamic;
        return this;
    }

    /**
     * Establece intervalo de actualización
     */
    public MenuItem setUpdateInterval(long interval) {
        this.updateInterval = Math.max(1, interval);
        return this;
    }

    // ==================== PROCESAMIENTO CON EXYLIACONTEXT ====================

    /**
     * Procesa el item con placeholders usando ExyliaContext
     */
    public void process(Player player) {
        // Procesar material con placeholders
        if (rawMaterial != null) {
            String processedMaterial = context.processPlaceholders(rawMaterial, player);
            if (!processedMaterial.equals(rawMaterial)) {
                updateMaterial(processedMaterial);
            }
        }

        // Procesar nombre con placeholders
        if (rawName != null) {
            String processedName = context.processPlaceholders(rawName, player);
            updateName(processedName);
        }

        // Procesar lore con placeholders
        List<String> currentLore = getCurrentLore();
        if (currentLore != null && !currentLore.isEmpty()) {
            List<Component> processedLore = new ArrayList<>();
            for (String line : currentLore) {
                String processedLine = context.processPlaceholders(line, player);
                processedLore.add(ColorUtils.parse(processedLine));
            }
            updateLore(processedLore);
        }

        // Procesar cantidad con placeholders
        if (rawAmount != null) {
            String processedAmount = context.processPlaceholders(rawAmount, player);
            updateAmount(processedAmount);
        }
    }

    /**
     * Procesa el item sin jugador específico
     */
    public void process() {
        process(null);
    }

    /**
     * Construye el ItemStack final con procesamiento
     */
    public ItemStack buildProcessed(Player player) {
        process(player);
        return itemStack.clone();
    }

    /**
     * Construye el ItemStack final sin procesamiento
     */
    public ItemStack build() {
        return itemStack.clone();
    }

    // ==================== MÉTODOS INTERNOS DE ACTUALIZACIÓN ====================

    /**
     * Obtiene el lore actual (dinámico o estático)
     */
    private List<String> getCurrentLore() {
        if (loreDynamicSupplier != null) {
            try {
                return loreDynamicSupplier.get();
            } catch (Exception e) {
                // Si hay error en el supplier, devolver lista vacía
                return new ArrayList<>();
            }
        }
        return rawLore;
    }

    /**
     * Actualiza el material preservando metadata
     */
    private void updateMaterial(String materialString) {
        ItemStack newStack = createItemFromString(materialString);
        ItemMeta currentMeta = itemStack.getItemMeta();

        if (currentMeta != null) {
            ItemMeta newMeta = newStack.getItemMeta();
            if (newMeta != null) {
                // Copiar metadata importante
                if (currentMeta.hasDisplayName()) {
                    adapter.setDisplayName(newMeta, adapter.getDisplayName(currentMeta));
                }
                if (currentMeta.hasLore()) {
                    adapter.setLore(newMeta, adapter.getLore(currentMeta));
                }

                // Copiar flags y encantamientos
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
     * Actualiza el nombre
     */
    private void updateName(String name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            adapter.setDisplayName(meta, ColorUtils.parse(name));
            itemStack.setItemMeta(meta);
        }
    }

    /**
     * Actualiza el lore
     */
    private void updateLore(List<Component> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            adapter.setLore(meta, lore);
            itemStack.setItemMeta(meta);
        }
    }

    /**
     * Actualiza la cantidad
     */
    private void updateAmount(String amountString) {
        try {
            int amount = Integer.parseInt(amountString.trim());
            itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        } catch (NumberFormatException e) {
            // Mantener cantidad actual si falla el parsing
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Crea ItemStack desde string
     */
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

    /**
     * Maneja eventos de click
     */
    public void handleClick(MenuClickEvent event) {
        if (clickHandler != null) {
            clickHandler.accept(event);
        }
    }

    /**
     * Clona el item
     */
    public MenuItem clone() {
        MenuItem clone = new MenuItem(this.itemStack.clone());
        clone.rawMaterial = this.rawMaterial;
        clone.rawName = this.rawName;
        clone.rawAmount = this.rawAmount;
        clone.dynamicUpdate = this.dynamicUpdate;
        clone.updateInterval = this.updateInterval;
        clone.clickHandler = this.clickHandler;
        clone.context = this.context.copy(); // Copiar el contexto
        clone.loreDynamicSupplier = this.loreDynamicSupplier; // Copiar supplier dinámico

        if (this.rawLore != null) {
            clone.rawLore = new ArrayList<>(this.rawLore);
        }

        return clone;
    }

    // ==================== NBT SUPPORT ====================

    /**
     * Establece datos NBT
     */
    public MenuItem setNBT(JavaPlugin plugin, String key, String value) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    /**
     * Obtiene datos NBT
     */
    public String getNBT(JavaPlugin plugin, String key) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
        }
        return null;
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Crea un MenuItem desde ConfigurationSection con ExyliaContext
     */
    public static MenuItem fromConfig(org.bukkit.configuration.ConfigurationSection config, Player player, ExyliaContext context) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration section cannot be null");
        }

        // Material (obligatorio)
        String material = config.getString("material", "STONE");
        MenuItem item = new MenuItem(material);

        // Configurar propiedades básicas
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

        // Configurar propiedades visuales
        if (config.getBoolean("glowing", false)) {
            item.setGlowing(true);
        }

        if (config.getBoolean("hide_attributes", false)) {
            item.hideAllAttributes();
        }

        // Configurar actualizaciones dinámicas
        if (config.getBoolean("dynamic_update", false)) {
            item.setDynamicUpdate(true);
            item.setUpdateInterval(config.getLong("update_interval", 20L));
        }

        // Establecer contexto y procesar si es necesario
        if (context != null) {
            item.withContext(context);

            boolean usePlaceholders = config.getBoolean("use_placeholders", false);
            if (usePlaceholders && player != null) {
                item.process(player);
            }
        }

        return item;
    }

    /**
     * Crea un MenuItem básico con contexto
     */
    public static MenuItem create(Material material, String name, ExyliaContext context) {
        return new MenuItem(material)
                .setName(name)
                .withContext(context);
    }

    /**
     * Crea un MenuItem con lore y contexto
     */
    public static MenuItem create(Material material, String name, List<String> lore, ExyliaContext context) {
        return new MenuItem(material)
                .setName(name)
                .setLoreList(lore)
                .withContext(context);
    }

    /**
     * Crea un MenuItem con lore dinámico y contexto
     */
    public static MenuItem create(Material material, String name, Supplier<List<String>> loreSupplier, ExyliaContext context) {
        return new MenuItem(material)
                .setName(name)
                .setLore(loreSupplier)
                .withContext(context);
    }

    // ==================== GETTERS ====================

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
}
package net.exylia.commons.menu;

import net.exylia.commons.command.CommandExecutor;
import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.ColorUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.exylia.commons.utils.ItemMetaAdapter;
import net.exylia.commons.placeholders.PlaceholderRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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

import static net.exylia.commons.ExyliaPlugin.isPlaceholderAPIEnabled;
import static net.exylia.commons.utils.DebugUtils.logWarn;
import static net.exylia.commons.utils.SkullUtils.*;

/**
 * Representa un ítem interactivo en un menú
 */
public class MenuItem {

    private ItemStack itemStack;
    private final ItemMetaAdapter adapter = AdapterFactory.getItemMetaAdapter();
    private Consumer<MenuClickInfo> clickHandler;
    private String menuItemId;
    private String rawName;
    private List<String> rawLore;
    private String rawMaterialString; // Para guardar el material original con placeholders
    private String rawAmountString; // Para guardar la cantidad original con placeholders
    private Player materialPlaceholderPlayer; // Jugador para procesar placeholders del material
    private boolean usePlaceholders = false;
    private boolean dynamicUpdate = false;
    private long updateInterval = 20L; // 1 segundo por defecto
    private Player placeholderPlayer = null; // Jugador específico para procesar los placeholders
    private List<String> commands = new ArrayList<>(); // Lista de comandos a ejecutar
    private Object placeholderContext = null; // Objeto de contexto para placeholders personalizados

    /**
     * Constructor del ítem de menú usando String
     * Soporta materiales normales y cabezas personalizadas:
     * - Material normal: "ENDER_PEARL"
     * - Cabeza base64: "headbase-eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUv..."
     * - Cabeza de jugador: "playerhead-Notch"
     * - Cabeza URL directa: "headurl-http://textures.minecraft.net/texture/dceb1708d5404ef326103e7b60559c9178f3dce729007ac9a0b498bdebe46107"
     * @param materialString String que representa el material o tipo de cabeza
     */
    public MenuItem(String materialString) {
        this.rawMaterialString = materialString;
        this.itemStack = createItemFromString(materialString);
        this.menuItemId = UUID.randomUUID().toString();
    }

    public MenuItem(String materialString, Player player, Object placeholderContext) {
        this.rawMaterialString = materialString;
        this.materialPlaceholderPlayer = player;
        this.placeholderContext = placeholderContext;
        this.itemStack = createItemFromString(processPlaceholdersInMaterial(materialString, player));
        this.menuItemId = UUID.randomUUID().toString();
    }

    /**
     * Constructor del ítem de menú usando String con jugador específico para placeholders
     * @param materialString String que representa el material o tipo de cabeza (puede contener placeholders)
     * @param player Jugador para procesar los placeholders del material
     */
    public MenuItem(String materialString, Player player) {
        this.rawMaterialString = materialString;
        this.materialPlaceholderPlayer = player;
        this.itemStack = createItemFromString(processPlaceholdersInMaterial(materialString, player));
        this.menuItemId = UUID.randomUUID().toString();
    }

    /**
     * Constructor del ítem de menú
     * @param itemStack ItemStack para usar directamente
     */
    public MenuItem(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.menuItemId = UUID.randomUUID().toString();
    }

    /**
     * Procesa los placeholders en el string del material
     * @param materialString String del material con placeholders
     * @param player Jugador para procesar los placeholders
     * @return String del material con placeholders procesados
     */
    private String processPlaceholdersInMaterial(String materialString, Player player) {
        if (materialString == null || player == null) {
            return materialString;
        }

        String processed = materialString;

        // Procesar placeholders personalizados usando el nuevo sistema
        processed = PlaceholderRegistry.process(processed, placeholderContext, player);

        // Procesar PlaceholderAPI si está disponible
        if (isPlaceholderAPIEnabled()) {
            processed = PlaceholderAPI.setPlaceholders(player, processed);
        }

        return processed;
    }

    /**
     * Procesa los placeholders en el string de cantidad
     * @param amountString String de cantidad con placeholders
     * @param player Jugador para procesar los placeholders
     * @return Cantidad procesada y limitada entre 1 y 64
     */
    private int processPlaceholdersInAmount(String amountString, Player player) {
        if (amountString == null || amountString.isEmpty()) {
            return 1;
        }

        String processed = amountString;
        Player targetPlayer = (placeholderPlayer != null) ? placeholderPlayer : player;

        // Procesar placeholders personalizados usando el nuevo sistema
        processed = PlaceholderRegistry.process(processed, placeholderContext, targetPlayer);

        // Procesar PlaceholderAPI si está disponible
        if (isPlaceholderAPIEnabled()) {
            processed = PlaceholderAPI.setPlaceholders(targetPlayer, processed);
        }

        // Intentar convertir a número
        try {
            int amount = Integer.parseInt(processed.trim());
            // Limitar entre 1 y 64
            return Math.max(1, Math.min(64, amount));
        } catch (NumberFormatException e) {
            // Si no se puede parsear, devolver 1
            logWarn("Invalid amount value: " + processed + ", using 1");
            return 1;
        }
    }

    /**
     * Crea un ItemStack basado en un string
     * @param materialString String que puede ser un material o cabeza personalizada
     * @return ItemStack creado
     */
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

    public MenuItem setMaterial(String materialString) {
        this.rawMaterialString = materialString;
        this.itemStack = createItemFromString(materialString);
        return this;
    }

    /**
     * Establece el nombre del ítem
     * @param name Nombre (admite códigos de color y placeholders)
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setName(String name) {
        this.rawName = name;
        ItemMeta meta = itemStack.getItemMeta();
        adapter.setDisplayName(meta, ColorUtils.parse(name));
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Establece el nombre del ítem directamente con un componente
     * @param name Componente de nombre ya formateado
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setName(Component name) {
        ItemMeta meta = itemStack.getItemMeta();
        adapter.setDisplayName(meta, name);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Establece la descripción del ítem
     * @param lore Líneas de descripción (admiten códigos de color y placeholders)
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setLore(String... lore) {
        this.rawLore = Arrays.asList(lore);

        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(ColorUtils.parse(line));
        }

        ItemMeta meta = itemStack.getItemMeta();
        adapter.setLore(meta, loreComponents);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Establece la descripción del ítem usando una lista de strings
     * @param lore Lista de líneas de descripción (admiten códigos de color y placeholders)
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setLoreFromList(List<String> lore) {
        this.rawLore = new ArrayList<>(lore);

        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(ColorUtils.parse(line));
        }

        ItemMeta meta = itemStack.getItemMeta();
        adapter.setLore(meta, loreComponents);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Establece la descripción del ítem directamente con componentes
     * @param lore Lista de componentes de descripción ya formateados
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setLore(List<Component> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        adapter.setLore(meta, lore);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Establece la acción al hacer clic en el ítem
     * @param clickHandler Manejador del clic
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setClickHandler(Consumer<MenuClickInfo> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    /**
     * Establece la cantidad del ítem
     * @param amount Cantidad (1-64)
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setAmount(int amount) {
        itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    /**
     * Establece la cantidad del ítem usando un string (soporta placeholders)
     * @param amountString String de cantidad (puede contener placeholders como %players_count%)
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setAmount(String amountString) {
        this.rawAmountString = amountString;
        if (materialPlaceholderPlayer != null) {
            int amount = processPlaceholdersInAmount(amountString, materialPlaceholderPlayer);
            itemStack.setAmount(amount);
        } else {
            try {
                int amount = Integer.parseInt(amountString.trim());
                itemStack.setAmount(Math.max(1, Math.min(64, amount)));
            } catch (NumberFormatException e) {
                itemStack.setAmount(1);
            }
        }

        return this;
    }

    /**
     * Añade un brillo al ítem (encantamiento oculto)
     * @return El mismo ítem (para encadenamiento)
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
     * Añade flags al ítem
     * @param flags Flags a añadir
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem addItemFlags(ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addItemFlags(flags);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Oculta todos los atributos del ítem
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem hideAllAttributes() {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_POTION_EFFECTS,
                ItemFlag.HIDE_UNBREAKABLE);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Añade un identificador personalizado al ítem
     * @param plugin Plugin para crear la clave
     * @param key Nombre de la clave
     * @param value Valor a guardar
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setNBTTag(JavaPlugin plugin, String key, String value) {
        ItemMeta meta = itemStack.getItemMeta();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * Obtiene un identificador personalizado del ítem
     * @param plugin Plugin para crear la clave
     * @param key Nombre de la clave
     * @return Valor guardado o null si no existe
     */
    public String getNBTTag(JavaPlugin plugin, String key) {
        ItemMeta meta = itemStack.getItemMeta();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
    }

    /**
     * Establece el ID único del ítem en el menú
     * @param id ID personalizado
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setId(String id) {
        this.menuItemId = id;
        return this;
    }

    /**
     * Obtiene el ID único del ítem en el menú
     * @return ID del ítem
     */
    public String getId() {
        return menuItemId;
    }

    /**
     * Obtiene el manejador de clics
     * @return Manejador de clics
     */
    public Consumer<MenuClickInfo> getClickHandler() {
        return clickHandler;
    }

    /**
     * Obtiene el ItemStack asociado
     * @return ItemStack del ítem
     */
    public ItemStack getItemStack() {
        return itemStack.clone();
    }

    private String action = null;

    /**
     * Establece una acción personalizada para ejecutar al hacer clic
     * @param action String de acción (ej: "show_help", "open_shop gold", "teleport spawn")
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setAction(String action) {
        this.action = action;
        return this;
    }

    /**
     * Obtiene la acción configurada
     * @return String de acción o null si no hay configurada
     */
    public String getAction() {
        return action;
    }

    /**
     * Verifica si el ítem tiene una acción configurada
     * @return true si tiene acción
     */
    public boolean hasAction() {
        return action != null && !action.trim().isEmpty();
    }

    /**
     * Ejecuta la acción configurada si existe
     * @param clickInfo Información del clic
     * @return true si se ejecutó una acción, false si no
     */
    public boolean executeAction(MenuClickInfo clickInfo) {
        if (hasAction()) {
            return MenuActionAdapter.executeMenuAction(action, clickInfo);
        }
        return false;
    }

    @Override
    public MenuItem clone() {
        MenuItem clone = new MenuItem(this.itemStack.clone());
        clone.clickHandler = this.clickHandler; // Copiamos la referencia directa
        clone.menuItemId = this.menuItemId + "_clone_" + System.currentTimeMillis(); // ID único para debug
        clone.rawName = this.rawName;
        clone.rawMaterialString = this.rawMaterialString;
        clone.rawAmountString = this.rawAmountString;
        clone.materialPlaceholderPlayer = this.materialPlaceholderPlayer;
        if (this.rawLore != null) {
            clone.rawLore = new ArrayList<>(this.rawLore);
        }
        clone.usePlaceholders = this.usePlaceholders;
        clone.dynamicUpdate = this.dynamicUpdate;
        clone.updateInterval = this.updateInterval;
        clone.placeholderPlayer = this.placeholderPlayer;
        clone.commands = new ArrayList<>(this.commands);
        clone.placeholderContext = this.placeholderContext;
        clone.action = this.action;
        return clone;
    }

    public void applySkullOwner(Player player) {
        if (this.itemStack.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) this.itemStack.getItemMeta();
            meta.setOwningPlayer(player);
            this.itemStack.setItemMeta(meta);
        }
    }

    /**
     * Activa el uso de placeholders en el nombre y lore del ítem
     * @param use true para activar placeholders, false para desactivarlos
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem usePlaceholders(boolean use) {
        this.usePlaceholders = use;
        return this;
    }

    /**
     * Activa la actualización dinámica del ítem
     * @param update true para activar actualización, false para desactivarla
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setDynamicUpdate(boolean update) {
        this.dynamicUpdate = update;
        return this;
    }

    /**
     * Establece el intervalo de actualización del ítem
     * @param ticks Intervalo en ticks
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setUpdateInterval(long ticks) {
        this.updateInterval = Math.max(1, ticks);
        return this;
    }

    /**
     * Establece un jugador específico para procesar los placeholders
     * @param player Jugador para procesar los placeholders (o null para usar el jugador que ve el menú)
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setPlaceholderPlayer(Player player) {
        this.placeholderPlayer = player;
        return this;
    }

    /**
     * Obtiene el jugador específico para procesar los placeholders
     * @return Jugador específico o null si no hay configurado
     */
    public Player getPlaceholderPlayer() {
        return placeholderPlayer;
    }

    /**
     * Establece un objeto de contexto para procesar placeholders personalizados
     * @param context Objeto de contexto (ej: Player target, Kit, KitCategory, etc.)
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setPlaceholderContext(Object context) {
        this.placeholderContext = context;
        return this;
    }

    /**
     * Obtiene el objeto de contexto para placeholders personalizados
     * @return Objeto de contexto o null si no hay configurado
     */
    public Object getPlaceholderContext() {
        return placeholderContext;
    }

    /**
     * Establece un jugador específico para procesar placeholders del material
     * @param player Jugador para procesar los placeholders del material
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setMaterialPlaceholderPlayer(Player player) {
        this.materialPlaceholderPlayer = player;
        return this;
    }

    /**
     * Añade un comando a ejecutar cuando se hace clic en el ítem
     * @param command Comando a ejecutar (formato: "player: /comando" o "console: /comando")
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem addCommand(String command) {
        this.commands.add(command);
        return this;
    }

    /**
     * Establece la lista de comandos a ejecutar
     * @param commands Lista de comandos (formato: "player: /comando" o "console: /comando")
     * @return El mismo ítem (para encadenamiento)
     */
    public MenuItem setCommands(List<String> commands) {
        this.commands = new ArrayList<>(commands);
        return this;
    }

    /**
     * Obtiene la lista de comandos a ejecutar
     * @return Lista de comandos
     */
    public List<String> getCommands() {
        return commands;
    }

    /**
     * Ejecuta los comandos configurados
     * @param player Jugador que hace clic en el ítem
     */
    public void executeCommands(Player player) {
        CommandExecutor.builder(player)
                .withPlaceholderPlayer(placeholderPlayer)
                .withPlaceholderContext(placeholderContext)
                .execute(commands);
    }

    /**
     * Actualiza el material del ítem procesando los placeholders
     * @param player Jugador para procesar los placeholders
     */
    public void updateMaterial(Player player) {
        if (rawMaterialString == null) return;

        Player targetPlayer = (materialPlaceholderPlayer != null) ? materialPlaceholderPlayer : player;
        String processedMaterial = processPlaceholdersInMaterial(rawMaterialString, targetPlayer);

        // Crear nuevo ItemStack con el material actualizado
        ItemStack newItemStack = createItemFromString(processedMaterial);

        // Conservar la metadata actual
        ItemMeta currentMeta = itemStack.getItemMeta();
        ItemMeta newMeta = newItemStack.getItemMeta();

        // Copiar propiedades importantes del meta actual al nuevo
        if (currentMeta != null && newMeta != null) {
            if (currentMeta.hasDisplayName()) {
                adapter.setDisplayName(newMeta, adapter.getDisplayName(currentMeta));
            }
            if (currentMeta.hasLore()) {
                adapter.setLore(newMeta, adapter.getLore(currentMeta));
            }

            // Copiar flags y enchantments
            newMeta.addItemFlags(currentMeta.getItemFlags().toArray(new ItemFlag[0]));
            currentMeta.getEnchants().forEach((enchant, level) -> {
                newMeta.addEnchant(enchant, level, true);
            });

            // Copiar datos persistentes
            currentMeta.getPersistentDataContainer().getKeys().forEach(key -> {
                Object value = currentMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (value != null) {
                    newMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, (String) value);
                }
            });

            newItemStack.setItemMeta(newMeta);
        }

        // Conservar la cantidad
        newItemStack.setAmount(itemStack.getAmount());

        // Reemplazar el ItemStack
        this.itemStack.setType(newItemStack.getType());
        this.itemStack.setItemMeta(newItemStack.getItemMeta());

        // Si es una cabeza de jugador, aplicar la textura
        if (newItemStack.getType() == Material.PLAYER_HEAD && newItemStack.getItemMeta() instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) newItemStack.getItemMeta();
            this.itemStack.setItemMeta(skullMeta);
        }
    }

    /**
     * Actualiza la cantidad del ítem procesando los placeholders
     * @param player Jugador para procesar los placeholders
     */
    public void updateAmount(Player player) {
        if (rawAmountString == null) return;

        Player targetPlayer = (placeholderPlayer != null) ? placeholderPlayer : player;
        int amount = processPlaceholdersInAmount(rawAmountString, targetPlayer);
        itemStack.setAmount(amount);
    }

    /**
     * Procesa placeholders en el material y actualiza el ItemStack
     * Útil para cuando se establece el contexto después de crear el item
     */
    public void processMaterialPlaceholders(Player player) {
        if (rawMaterialString == null) return;

        // Procesar placeholders en el material
        String processedMaterial = rawMaterialString;

        // Usar el nuevo sistema de placeholders
        processedMaterial = PlaceholderRegistry.process(processedMaterial, placeholderContext, player);

        // Procesar PlaceholderAPI si está disponible
        if (isPlaceholderAPIEnabled()) {
            processedMaterial = PlaceholderAPI.setPlaceholders(player, processedMaterial);
        }

        // Solo actualizar si el material cambió
        if (!processedMaterial.equals(rawMaterialString)) {
            // Crear nuevo ItemStack con el material procesado
            ItemStack newItemStack = createItemFromString(processedMaterial);

            // Conservar la metadata actual
            ItemMeta currentMeta = itemStack.getItemMeta();
            if (currentMeta != null) {
                newItemStack.setItemMeta(currentMeta);
            }

            // Conservar la cantidad
            newItemStack.setAmount(itemStack.getAmount());

            // Reemplazar el ItemStack
            this.itemStack.setType(newItemStack.getType());
            this.itemStack.setItemMeta(newItemStack.getItemMeta());
        }
    }

    /**
     * Actualiza los placeholders del ítem para un jugador específico
     * AHORA USA EL NUEVO SISTEMA DE PLACEHOLDERS
     *
     * @param player Jugador para procesar los placeholders (si no hay un placeholderPlayer configurado)
     */
    public void updatePlaceholders(Player player) {
        if (!usePlaceholders) return;

        Player targetPlayer = (placeholderPlayer != null) ? placeholderPlayer : player;

        ItemMeta meta = itemStack.getItemMeta();

        // Procesar nombre con el nuevo sistema
        if (rawName != null && !rawName.isEmpty()) {
            String processedName = rawName;

            // Usar el nuevo sistema de placeholders
            processedName = PlaceholderRegistry.process(processedName, placeholderContext, targetPlayer);

            // Procesar PlaceholderAPI si está disponible
            if (isPlaceholderAPIEnabled()) {
                processedName = PlaceholderAPI.setPlaceholders(targetPlayer, processedName);
            }

            adapter.setDisplayName(meta, ColorUtils.parse(processedName));
        }

        // Procesar lore con el nuevo sistema
        if (rawLore != null && !rawLore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : rawLore) {
                String processedLine = line;

                // Usar el nuevo sistema de placeholders
                processedLine = PlaceholderRegistry.process(processedLine, placeholderContext, targetPlayer);

                // Procesar PlaceholderAPI si está disponible
                if (isPlaceholderAPIEnabled()) {
                    processedLine = PlaceholderAPI.setPlaceholders(targetPlayer, processedLine);
                }

                loreComponents.add(ColorUtils.parse(processedLine));
            }
            adapter.setLore(meta, loreComponents);
        }

        itemStack.setItemMeta(meta);

        updateAmount(player);
    }

    /**
     * Actualiza tanto el material como los placeholders del ítem
     * @param player Jugador para procesar los placeholders
     */
    public void updateAll(Player player) {
        processMaterialPlaceholders(player);
        updatePlaceholders(player);
    }

    /**
     * Comprueba si el ítem usa placeholders
     * @return true si usa placeholders
     */
    public boolean usesPlaceholders() {
        return usePlaceholders;
    }

    /**
     * Comprueba si el ítem debe actualizarse dinámicamente
     * @return true si debe actualizarse
     */
    public boolean needsDynamicUpdate() {
        return dynamicUpdate;
    }

    /**
     * Obtiene el intervalo de actualización
     * @return Intervalo en ticks
     */
    public long getUpdateInterval() {
        return updateInterval;
    }
}
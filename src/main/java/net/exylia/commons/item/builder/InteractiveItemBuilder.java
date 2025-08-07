package net.exylia.commons.item.builder;

import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.item.ItemClickInfo;
import net.exylia.commons.item.ItemManager;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.placeholders.ExyliaContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * Builder para crear InteractiveItem desde ConfigurationSection
 * Actualizado para el sistema modularizado
 * NUEVO: Soporte para force-id
 */
public class InteractiveItemBuilder {

    private final ConfigurationSection config;
    private Player placeholderPlayer;
    private ExyliaContext placeholderContext;
    private Consumer<ItemClickInfo> clickHandler;
    private String customId; // ID personalizado opcional

    /**
     * Constructor del builder
     * @param config ConfigurationSection con la configuración del ítem
     */
    public InteractiveItemBuilder(ConfigurationSection config) {
        this.config = config;
    }

    /**
     * Establece el jugador para procesar placeholders
     * @param player Jugador para placeholders
     * @return Este builder para encadenamiento
     */
    public InteractiveItemBuilder withPlaceholderPlayer(Player player) {
        this.placeholderPlayer = player;
        return this;
    }

    /**
     * Establece el contexto para placeholders personalizados
     * @param context Contexto para placeholders
     * @return Este builder para encadenamiento
     */
    public InteractiveItemBuilder withPlaceholderContext(ExyliaContext context) {
        this.placeholderContext = context;
        return this;
    }

    /**
     * Establece un manejador de clics personalizado
     * @param clickHandler Manejador de clics
     * @return Este builder para encadenamiento
     */
    public InteractiveItemBuilder withClickHandler(Consumer<ItemClickInfo> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    /**
     * Establece un ID personalizado para el item
     * @param id ID personalizado
     * @return Este builder para encadenamiento
     */
    public InteractiveItemBuilder withCustomId(String id) {
        this.customId = id;
        return this;
    }

    /**
     * Construye el InteractiveItem basado en la configuración
     * @return InteractiveItem configurado
     */
    public InteractiveItem build() {
        // 1. Crear la configuración desde el ConfigurationSection
        ItemConfiguration itemConfig = ItemConfiguration.builder()
                .loadFromConfig(config)
                .build();

        // 2. Determinar el ID a usar
        String itemId = customId != null ? customId :
                config.getString("id", "temp_item_" + System.currentTimeMillis());

        // 3. Registrar temporalmente la configuración si no existe
        if (!ItemManager.hasItemConfiguration(itemId)) {
            ItemManager.registerItemConfiguration(itemId, itemConfig);
        }

        // 4. Crear el InteractiveItem usando el sistema modularizado
        InteractiveItem item;
        if (placeholderPlayer != null) {
            item = ItemManager.createItem(itemId, placeholderPlayer);
        } else {
            item = ItemManager.createItem(itemId);
        }

        if (item == null) {
            throw new IllegalStateException("No se pudo crear el item con ID: " + itemId);
        }

        // 5. Configurar propiedades temporales (no persistentes)
        configureTemporaryProperties(item);

        return item;
    }

    /**
     * Configura las propiedades temporales del ítem (que no se persisten)
     */
    private void configureTemporaryProperties(InteractiveItem item) {
        // Click handler personalizado (no persistente)
        if (clickHandler != null) {
            item.setClickHandler(clickHandler);
        }

        // Jugador para placeholders (no persistente)
        if (placeholderPlayer != null) {
            item.setPlaceholderPlayer(placeholderPlayer);
        }

        // Contexto para placeholders (no persistente)
        if (placeholderContext != null) {
            item.withContext(placeholderContext);
        }
    }

    /**
     * Construye y registra permanentemente el item en el sistema
     * @param itemId ID permanente para registrar
     * @return InteractiveItem registrado
     */
    public InteractiveItem buildAndRegister(String itemId) {
        // Crear configuración desde el config
        ItemConfiguration itemConfig = ItemConfiguration.builder()
                .loadFromConfig(config)
                .build();

        // Registrar permanentemente
        ItemManager.registerItemConfiguration(itemId, itemConfig);

        // Crear item
        InteractiveItem item;
        if (placeholderPlayer != null) {
            item = ItemManager.createItem(itemId, placeholderPlayer);
        } else {
            item = ItemManager.createItem(itemId);
        }

        if (item == null) {
            throw new IllegalStateException("No se pudo crear el item registrado con ID: " + itemId);
        }

        // Configurar propiedades temporales
        configureTemporaryProperties(item);

        return item;
    }

    /**
     * Construye un InteractiveItem listo para usar
     * @return ItemStack listo para dar a un jugador
     */
    public ItemStack buildAsItemStack() {
        InteractiveItem item = build();
        return ItemManager.prepareItem(item);
    }

    /**
     * Construye y registra permanentemente, luego devuelve el ItemStack
     * @param itemId ID permanente para registrar
     * @return ItemStack listo para usar
     */
    public ItemStack buildAsItemStackAndRegister(String itemId) {
        InteractiveItem item = buildAndRegister(itemId);
        return ItemManager.prepareItem(item);
    }

    // ===== MÉTODOS ESTÁTICOS DE CONVENIENCIA =====

    /**
     * Crea un nuevo builder desde ConfigurationSection
     * @param config ConfigurationSection con la configuración
     * @return Nuevo builder
     */
    public static InteractiveItemBuilder from(ConfigurationSection config) {
        return new InteractiveItemBuilder(config);
    }

    /**
     * Construye directamente desde ConfigurationSection (temporal)
     * @param config ConfigurationSection con la configuración
     * @return InteractiveItem configurado
     */
    public static InteractiveItem buildFrom(ConfigurationSection config) {
        return new InteractiveItemBuilder(config).build();
    }

    /**
     * Construye directamente con jugador para placeholders (temporal)
     * @param config ConfigurationSection con la configuración
     * @param player Jugador para placeholders
     * @return InteractiveItem configurado
     */
    public static InteractiveItem buildFrom(ConfigurationSection config, Player player) {
        return new InteractiveItemBuilder(config)
                .withPlaceholderPlayer(player)
                .build();
    }

    /**
     * Construye y registra permanentemente desde ConfigurationSection
     * @param config ConfigurationSection con la configuración
     * @param itemId ID permanente para registrar
     * @return InteractiveItem registrado
     */
    public static InteractiveItem buildFromAndRegister(ConfigurationSection config, String itemId) {
        return new InteractiveItemBuilder(config).buildAndRegister(itemId);
    }

    /**
     * Construye y registra con jugador para placeholders
     * @param config ConfigurationSection con la configuración
     * @param itemId ID permanente para registrar
     * @param player Jugador para placeholders
     * @return InteractiveItem registrado
     */
    public static InteractiveItem buildFromAndRegister(ConfigurationSection config, String itemId, Player player) {
        return new InteractiveItemBuilder(config)
                .withPlaceholderPlayer(player)
                .buildAndRegister(itemId);
    }

    /**
     * Construye ItemStack directamente (temporal)
     * @param config ConfigurationSection con la configuración
     * @return ItemStack listo para usar
     */
    public static ItemStack buildAsItemStackFrom(ConfigurationSection config) {
        return new InteractiveItemBuilder(config).buildAsItemStack();
    }

    /**
     * Construye ItemStack con jugador para placeholders (temporal)
     * @param config ConfigurationSection con la configuración
     * @param player Jugador para placeholders
     * @return ItemStack listo para usar
     */
    public static ItemStack buildAsItemStackFrom(ConfigurationSection config, Player player) {
        return new InteractiveItemBuilder(config)
                .withPlaceholderPlayer(player)
                .buildAsItemStack();
    }

    /**
     * Construye ItemStack y registra permanentemente
     * @param config ConfigurationSection con la configuración
     * @param itemId ID permanente para registrar
     * @return ItemStack listo para usar
     */
    public static ItemStack buildAsItemStackFromAndRegister(ConfigurationSection config, String itemId) {
        return new InteractiveItemBuilder(config).buildAsItemStackAndRegister(itemId);
    }

    /**
     * Construye ItemStack, registra con jugador para placeholders
     * @param config ConfigurationSection con la configuración
     * @param itemId ID permanente para registrar
     * @param player Jugador para placeholders
     * @return ItemStack listo para usar
     */
    public static ItemStack buildAsItemStackFromAndRegister(ConfigurationSection config, String itemId, Player player) {
        return new InteractiveItemBuilder(config)
                .withPlaceholderPlayer(player)
                .buildAsItemStackAndRegister(itemId);
    }

    // ===== MÉTODOS NUEVOS PARA FORCE-ID =====

    /**
     * NUEVO: Construye un item con force-id para sobrescribir cooldowns vanilla
     * @param config ConfigurationSection con la configuración
     * @param forceId ID forzado para cooldowns
     * @return InteractiveItem con force-id configurado
     */
    public static InteractiveItem buildWithForceId(ConfigurationSection config, String forceId) {
        // Crear una copia de la configuración y añadir el force-id
        ItemConfiguration itemConfig = ItemConfiguration.builder()
                .loadFromConfig(config)
                .forceId(forceId)
                .build();

        String itemId = config.getString("id", "temp_item_" + System.currentTimeMillis());

        // Registrar temporalmente
        ItemManager.registerItemConfiguration(itemId, itemConfig);

        return ItemManager.createItem(itemId);
    }

    /**
     * NUEVO: Construye un item con force-id y jugador para placeholders
     * @param config ConfigurationSection con la configuración
     * @param forceId ID forzado para cooldowns
     * @param player Jugador para placeholders
     * @return InteractiveItem con force-id configurado
     */
    public static InteractiveItem buildWithForceId(ConfigurationSection config, String forceId, Player player) {
        // Crear una copia de la configuración y añadir el force-id
        ItemConfiguration itemConfig = ItemConfiguration.builder()
                .loadFromConfig(config)
                .forceId(forceId)
                .build();

        String itemId = config.getString("id", "temp_item_" + System.currentTimeMillis());

        // Registrar temporalmente
        ItemManager.registerItemConfiguration(itemId, itemConfig);

        return ItemManager.createItem(itemId, player);
    }

    /**
     * NUEVO: Construye ItemStack con force-id
     * @param config ConfigurationSection con la configuración
     * @param forceId ID forzado para cooldowns
     * @return ItemStack listo para usar
     */
    public static ItemStack buildAsItemStackWithForceId(ConfigurationSection config, String forceId) {
        InteractiveItem item = buildWithForceId(config, forceId);
        return ItemManager.prepareItem(item);
    }

    /**
     * NUEVO: Construye ItemStack con force-id y jugador para placeholders
     * @param config ConfigurationSection con la configuración
     * @param forceId ID forzado para cooldowns
     * @param player Jugador para placeholders
     * @return ItemStack listo para usar
     */
    public static ItemStack buildAsItemStackWithForceId(ConfigurationSection config, String forceId, Player player) {
        InteractiveItem item = buildWithForceId(config, forceId, player);
        return ItemManager.prepareItem(item);
    }

    /**
     * NUEVO: Registra permanentemente un item con force-id
     * @param config ConfigurationSection con la configuración
     * @param itemId ID permanente para registrar
     * @param forceId ID forzado para cooldowns
     * @return InteractiveItem registrado
     */
    public static InteractiveItem buildAndRegisterWithForceId(ConfigurationSection config, String itemId, String forceId) {
        ItemConfiguration itemConfig = ItemConfiguration.builder()
                .loadFromConfig(config)
                .forceId(forceId)
                .build();

        ItemManager.registerItemConfiguration(itemId, itemConfig);
        return ItemManager.createItem(itemId);
    }

    /**
     * NUEVO: Registra permanentemente un item con force-id y jugador para placeholders
     * @param config ConfigurationSection con la configuración
     * @param itemId ID permanente para registrar
     * @param forceId ID forzado para cooldowns
     * @param player Jugador para placeholders
     * @return InteractiveItem registrado
     */
    public static InteractiveItem buildAndRegisterWithForceId(ConfigurationSection config, String itemId, String forceId, Player player) {
        ItemConfiguration itemConfig = ItemConfiguration.builder()
                .loadFromConfig(config)
                .forceId(forceId)
                .build();

        ItemManager.registerItemConfiguration(itemId, itemConfig);
        return ItemManager.createItem(itemId, player);
    }
}
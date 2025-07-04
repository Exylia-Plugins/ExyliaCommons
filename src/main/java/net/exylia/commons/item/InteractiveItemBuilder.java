package net.exylia.commons.item;

import net.exylia.commons.placeholders.ExyliaContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

/**
 * Builder para crear InteractiveItem desde ConfigurationSection
 * Versión actualizada para el sistema híbrido
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
        ItemConfiguration itemConfig = ItemConfiguration.fromConfig(config).build();

        // 2. Determinar el ID a usar
        String itemId = customId != null ? customId :
                config.getString("id", "temp_item_" + System.currentTimeMillis());

        // 3. Registrar temporalmente la configuración si no existe
        if (!ItemManager.hasItemConfiguration(itemId)) {
            ItemManager.registerItemConfiguration(itemId, itemConfig);
        }

        // 4. Crear el InteractiveItem usando el sistema híbrido
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
        ItemConfiguration itemConfig = ItemConfiguration.fromConfig(config).build();

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

    // ===== MÉTODOS PARA MIGRACIÓN DESDE SISTEMA ANTERIOR =====

    /**
     * Convierte configuraciones del sistema anterior al nuevo
     * Útil para migrar configs existentes
     * @param config ConfigurationSection del sistema anterior
     * @param itemId ID para el nuevo sistema
     */
    public static void migrateFromOldSystem(ConfigurationSection config, String itemId) {
        ItemConfiguration newConfig = ItemConfiguration.fromConfig(config).build();
        ItemManager.registerItemConfiguration(itemId, newConfig);
    }

    /**
     * Migra múltiples configuraciones de una vez
     * @param configSection Sección con múltiples items del sistema anterior
     */
    public static void migrateMultipleFromOldSystem(ConfigurationSection configSection) {
        for (String itemId : configSection.getKeys(false)) {
            ConfigurationSection itemConfig = configSection.getConfigurationSection(itemId);
            if (itemConfig != null) {
                migrateFromOldSystem(itemConfig, itemId);
            }
        }
    }
}
package net.exylia.commons.item.builder;

import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.item.ItemClickInfo;
import net.exylia.commons.item.ItemManager;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.placeholders.ExyliaContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class InteractiveItemBuilder {

    private final ConfigurationSection config;
    private Player placeholderPlayer;
    private ExyliaContext placeholderContext;
    private Consumer<ItemClickInfo> clickHandler;
    private String customId;

    public InteractiveItemBuilder withPlaceholderPlayer(Player player) {
        this.placeholderPlayer = player;
        return this;
    }

    public InteractiveItemBuilder withPlaceholderContext(ExyliaContext context) {
        this.placeholderContext = context;
        return this;
    }

    public InteractiveItemBuilder withClickHandler(Consumer<ItemClickInfo> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public InteractiveItemBuilder withCustomId(String id) {
        this.customId = id;
        return this;
    }

    public InteractiveItem build() {
        ItemConfiguration itemConfig = ItemConfiguration.builder()
                .loadFromConfig(config)
                .build();

        String itemId = customId != null ? customId :
                config.getString("id", "temp_item_" + System.currentTimeMillis());

        if (!ItemManager.hasItemConfiguration(itemId)) {
            ItemManager.registerItemConfiguration(itemId, itemConfig);
        }

        InteractiveItem item = placeholderPlayer != null
                ? ItemManager.createItem(itemId, placeholderPlayer)
                : ItemManager.createItem(itemId);

        if (item == null) {
            throw new IllegalStateException("No se pudo crear el item con ID: " + itemId);
        }

        configureTemporaryProperties(item);
        return item;
    }

    private void configureTemporaryProperties(InteractiveItem item) {
        if (clickHandler != null) {
            item.clickHandler(clickHandler);
        }

        if (placeholderPlayer != null) {
            item.placeholderPlayer(placeholderPlayer);
        }

        if (placeholderContext != null) {
            item.withContext(placeholderContext);
        }
    }

    public InteractiveItem buildAndRegister(String itemId) {
        ItemConfiguration itemConfig = ItemConfiguration.builder()
                .loadFromConfig(config)
                .build();

        ItemManager.registerItemConfiguration(itemId, itemConfig);

        InteractiveItem item = placeholderPlayer != null
                ? ItemManager.createItem(itemId, placeholderPlayer)
                : ItemManager.createItem(itemId);

        if (item == null) {
            throw new IllegalStateException("No se pudo crear el item registrado con ID: " + itemId);
        }

        configureTemporaryProperties(item);
        return item;
    }

    public ItemStack buildAsItemStack() {
        InteractiveItem item = build();
        return ItemManager.prepareItem(item);
    }

    public ItemStack buildAsItemStackAndRegister(String itemId) {
        InteractiveItem item = buildAndRegister(itemId);
        return ItemManager.prepareItem(item);
    }

    @UtilityClass
    public static class Factory {

        public static InteractiveItemBuilder from(ConfigurationSection config) {
            return new InteractiveItemBuilder(config);
        }

        public static InteractiveItem buildFrom(ConfigurationSection config) {
            return new InteractiveItemBuilder(config).build();
        }

        public static InteractiveItem buildFrom(ConfigurationSection config, Player player) {
            return new InteractiveItemBuilder(config)
                    .withPlaceholderPlayer(player)
                    .build();
        }

        public static InteractiveItem buildFromAndRegister(ConfigurationSection config, String itemId) {
            return new InteractiveItemBuilder(config).buildAndRegister(itemId);
        }

        public static InteractiveItem buildFromAndRegister(ConfigurationSection config, String itemId, Player player) {
            return new InteractiveItemBuilder(config)
                    .withPlaceholderPlayer(player)
                    .buildAndRegister(itemId);
        }

        public static ItemStack buildAsItemStackFrom(ConfigurationSection config) {
            return new InteractiveItemBuilder(config).buildAsItemStack();
        }

        public static ItemStack buildAsItemStackFrom(ConfigurationSection config, Player player) {
            return new InteractiveItemBuilder(config)
                    .withPlaceholderPlayer(player)
                    .buildAsItemStack();
        }

        public static ItemStack buildAsItemStackFromAndRegister(ConfigurationSection config, String itemId) {
            return new InteractiveItemBuilder(config).buildAsItemStackAndRegister(itemId);
        }

        public static ItemStack buildAsItemStackFromAndRegister(ConfigurationSection config, String itemId, Player player) {
            return new InteractiveItemBuilder(config)
                    .withPlaceholderPlayer(player)
                    .buildAsItemStackAndRegister(itemId);
        }

        public static InteractiveItem buildWithForceId(ConfigurationSection config, String forceId) {
            ItemConfiguration itemConfig = ItemConfiguration.builder()
                    .loadFromConfig(config)
                    .forceId(forceId)
                    .build();

            String itemId = config.getString("id", "temp_item_" + System.currentTimeMillis());
            ItemManager.registerItemConfiguration(itemId, itemConfig);
            return ItemManager.createItem(itemId);
        }

        public static InteractiveItem buildWithForceId(ConfigurationSection config, String forceId, Player player) {
            ItemConfiguration itemConfig = ItemConfiguration.builder()
                    .loadFromConfig(config)
                    .forceId(forceId)
                    .build();

            String itemId = config.getString("id", "temp_item_" + System.currentTimeMillis());
            ItemManager.registerItemConfiguration(itemId, itemConfig);
            return ItemManager.createItem(itemId, player);
        }

        public static ItemStack buildAsItemStackWithForceId(ConfigurationSection config, String forceId) {
            InteractiveItem item = buildWithForceId(config, forceId);
            return ItemManager.prepareItem(item);
        }

        public static ItemStack buildAsItemStackWithForceId(ConfigurationSection config, String forceId, Player player) {
            InteractiveItem item = buildWithForceId(config, forceId, player);
            return ItemManager.prepareItem(item);
        }

        public static InteractiveItem buildAndRegisterWithForceId(ConfigurationSection config, String itemId, String forceId) {
            ItemConfiguration itemConfig = ItemConfiguration.builder()
                    .loadFromConfig(config)
                    .forceId(forceId)
                    .build();

            ItemManager.registerItemConfiguration(itemId, itemConfig);
            return ItemManager.createItem(itemId);
        }

        public static InteractiveItem buildAndRegisterWithForceId(ConfigurationSection config, String itemId, String forceId, Player player) {
            ItemConfiguration itemConfig = ItemConfiguration.builder()
                    .loadFromConfig(config)
                    .forceId(forceId)
                    .build();

            ItemManager.registerItemConfiguration(itemId, itemConfig);
            return ItemManager.createItem(itemId, player);
        }
    }
}
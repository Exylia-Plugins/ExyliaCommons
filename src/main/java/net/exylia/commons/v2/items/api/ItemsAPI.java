package net.exylia.commons.v2.items.api;

import lombok.Getter;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.items.processor.ConfigurationParser;
import net.exylia.commons.v2.items.processor.ItemProcessor;
import net.exylia.commons.v2.items.utils.NBTManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

public final class ItemsAPI {

    @Getter
    private static boolean initialized = false;

    private ItemsAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        if (initialized) {
            return;
        }

        NBTManager.setPlugin(plugin);
        initialized = true;
    }

    public static ItemData parseFromConfig(ConfigurationSection config) {
        return ConfigurationParser.parseFromConfig(config);
    }

    public static ProcessedItem process(ItemData itemData, Player player) {
        return ItemProcessor.process(itemData, player);
    }

    public static ProcessedItem process(ItemData itemData, Player player, boolean validate) {
        return ItemProcessor.process(itemData, player, validate);
    }

    public static CompletableFuture<ProcessedItem> processAsync(ItemData itemData, Player player) {
        return ItemProcessor.processAsync(itemData, player);
    }

    public static CompletableFuture<ProcessedItem> processAsync(ItemData itemData, Player player, boolean validate) {
        return ItemProcessor.processAsync(itemData, player, validate);
    }

    public static ProcessedItem processFromConfig(ConfigurationSection config, Player player) {
        ItemData itemData = parseFromConfig(config);
        return process(itemData, player);
    }

    public static CompletableFuture<ProcessedItem> processFromConfigAsync(ConfigurationSection config, Player player) {
        ItemData itemData = parseFromConfig(config);
        return processAsync(itemData, player);
    }

}

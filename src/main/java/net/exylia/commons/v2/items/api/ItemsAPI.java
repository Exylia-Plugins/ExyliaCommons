package net.exylia.commons.v2.items.api;

import lombok.Getter;
import net.exylia.commons.v2.items.integration.ItemClickHandler;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.items.processor.ConfigurationParser;
import net.exylia.commons.v2.items.processor.ItemProcessor;
import net.exylia.commons.v2.items.utils.ClickTypeConverter;
import net.exylia.commons.v2.items.utils.NBTManager;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
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

    public static CompletableFuture<Void> handleClick(
            ProcessedItem item,
            Player player,
            ClickType clickType
    ) {
        return ItemClickHandler.handleClick(item, player, clickType);
    }

    public static CompletableFuture<Void> handleClick(
            ProcessedItem item,
            Player player,
            ClickType clickType,
            PlaceholderContext context
    ) {
        return ItemClickHandler.handleClick(item, player, clickType, context);
    }

    public static CompletableFuture<Void> handleInteract(
            ProcessedItem item,
            Player player,
            Action action
    ) {
        ClickType clickType = ClickTypeConverter.fromAction(action, player.isSneaking());
        return ItemClickHandler.handleClick(item, player, clickType);
    }

    public static CompletableFuture<Void> handleInteract(
            ProcessedItem item,
            Player player,
            Action action,
            PlaceholderContext context
    ) {
        ClickType clickType = ClickTypeConverter.fromAction(action, player.isSneaking());
        return ItemClickHandler.handleClick(item, player, clickType, context);
    }

    public static ClickType convertAction(Action action, boolean sneaking) {
        return ClickTypeConverter.fromAction(action, sneaking);
    }

    public static ClickType convertAction(Action action) {
        return ClickTypeConverter.fromAction(action);
    }

    public static CompletableFuture<Void> handleInteractEntity(
            ProcessedItem item,
            Player player,
            Entity target
    ) {
        ClickType clickType = player.isSneaking() ? ClickType.SHIFT_RIGHT : ClickType.RIGHT;

        PlaceholderContext context = PlaceholderContext.create()
            .put("target", target.getName())
            .put("target_uuid", target.getUniqueId().toString())
            .put("target_type", target.getType().name())
            .with(target);

        return ItemClickHandler.handleClick(item, player, clickType, context);
    }

    public static CompletableFuture<Void> handleInteractEntity(
            ProcessedItem item,
            Player player,
            Entity target,
            PlaceholderContext additionalContext
    ) {
        ClickType clickType = player.isSneaking() ? ClickType.SHIFT_RIGHT : ClickType.RIGHT;

        PlaceholderContext context = PlaceholderContext.create()
            .put("target", target.getName())
            .put("target_uuid", target.getUniqueId().toString())
            .put("target_type", target.getType().name())
            .with(target);

        if (additionalContext != null) {
            context.merge(additionalContext);
        }

        return ItemClickHandler.handleClick(item, player, clickType, context);
    }

    public static CompletableFuture<Void> handleAttackEntity(
            ProcessedItem item,
            Player player,
            Entity target
    ) {
        ClickType clickType = player.isSneaking() ? ClickType.SHIFT_LEFT : ClickType.LEFT;

        PlaceholderContext context = PlaceholderContext.create()
            .put("target", target.getName())
            .put("target_uuid", target.getUniqueId().toString())
            .put("target_type", target.getType().name())
            .with(target);

        return ItemClickHandler.handleClick(item, player, clickType, context);
    }

    public static CompletableFuture<Void> handleAttackEntity(
            ProcessedItem item,
            Player player,
            Entity target,
            PlaceholderContext additionalContext
    ) {
        ClickType clickType = player.isSneaking() ? ClickType.SHIFT_LEFT : ClickType.LEFT;

        PlaceholderContext context = PlaceholderContext.create()
            .put("target", target.getName())
            .put("target_uuid", target.getUniqueId().toString())
            .put("target_type", target.getType().name())
            .with(target);

        if (additionalContext != null) {
            context.merge(additionalContext);
        }

        return ItemClickHandler.handleClick(item, player, clickType, context);
    }

}

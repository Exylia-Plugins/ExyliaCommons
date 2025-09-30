package net.exylia.commons.item;

import lombok.Getter;
import net.exylia.commons.actions.ActionSource;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.item.config.TriggerType;
import net.exylia.commons.item.cooldown.CooldownConfiguration;
import net.exylia.commons.item.cooldown.CooldownManager;
import net.exylia.commons.item.handlers.ItemHoldHandler;
import net.exylia.commons.item.handlers.ItemInteractionHandler;
import net.exylia.commons.item.handlers.ItemInventoryHandler;
import net.exylia.commons.item.handlers.ItemRegionHandler;
import net.exylia.commons.item.registry.ItemRegistry;
import net.exylia.commons.item.registry.ItemRegistryImpl;
import net.exylia.commons.item.vanilla.VanillaItemCooldownManager;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class ItemManager implements Listener {

    @Getter
    private static JavaPlugin plugin;
    private static ItemRegistry registry;
    private static ItemInteractionHandler interactionHandler;
    private static ItemHoldHandler holdHandler;
    private static NamespacedKey itemIdKey;
    private static boolean initialized = false;

    private static final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastDropTime = new ConcurrentHashMap<>();
    private static final long DOUBLE_CLICK_PREVENTION_MS = 150;
    private static final long DROP_INTERACTION_PREVENTION_MS = 100;

    public static void initialize(JavaPlugin javaPlugin) {
        initialize(javaPlugin, CooldownConfiguration.getDefault());
    }

    public static void initialize(JavaPlugin javaPlugin, CooldownConfiguration cooldownConfiguration) {
        if (initialized) return;

        plugin = javaPlugin;
        registry = new ItemRegistryImpl();
        interactionHandler = new ItemInteractionHandler(plugin);
        holdHandler = new ItemHoldHandler(plugin);
        itemIdKey = new NamespacedKey(plugin, "interactive_item_id");

        Bukkit.getPluginManager().registerEvents(new ItemManager(), plugin);
        CooldownManager.initialize(plugin, cooldownConfiguration);
        startClickTimeCleanupTask();

        initialized = true;

        if (WorldGuardUtils.isWorldGuardAvailable()) {
            DebugUtils.logInternalInfo("WorldGuard detectado - Soporte de regiones habilitado para cooldowns");
        }
    }

    public static void initialize(JavaPlugin javaPlugin, org.bukkit.configuration.ConfigurationSection cooldownConfig) {
        CooldownConfiguration configuration = CooldownConfiguration.fromConfiguration(cooldownConfig);
        initialize(javaPlugin, configuration);
    }

    public static void registerItemConfiguration(String id, ItemConfiguration config) {
        ensureInitialized();
        registry.registerItemConfiguration(id, config);
    }

    public static void registerItemConfigurations(ConfigurationSection configSection) {
        ensureInitialized();
        registry.registerItemConfigurations(configSection);
    }

    @Nullable
    public static ItemConfiguration getItemConfiguration(String id) {
        ensureInitialized();
        return registry.getItemConfiguration(id);
    }

    public static boolean hasItemConfiguration(String id) {
        ensureInitialized();
        return registry.hasItemConfiguration(id);
    }

    public static void unregisterItemConfiguration(String id) {
        ensureInitialized();
        registry.unregisterItemConfiguration(id);
    }

    public static void reloadItemConfiguration(String id, ItemConfiguration config) {
        ensureInitialized();
        registry.reloadItemConfiguration(id, config);
    }

    public static void reloadAllConfigurations(ConfigurationSection configSection) {
        ensureInitialized();
        registry.reloadAllConfigurations(configSection);
    }

    public static Map<String, ItemConfiguration> getAllConfigurations() {
        ensureInitialized();
        return registry.getAllConfigurations();
    }

    @Nullable
    public static InteractiveItem createItem(String id) {
        ensureInitialized();
        ItemConfiguration config = registry.getItemConfiguration(id);
        if (config == null) {
            DebugUtils.logInternalWarn("No configuration found for item ID: " + id);
            return null;
        }
        return new InteractiveItem(id, config);
    }

    @Nullable
    public static InteractiveItem createItem(String id, Player player) {
        ensureInitialized();
        ItemConfiguration config = registry.getItemConfiguration(id);
        if (config == null) {
            DebugUtils.logInternalWarn("No configuration found for item ID: " + id);
            return null;
        }
        return new InteractiveItem(id, config, player);
    }

    @Nullable
    public static ItemStack createItemStack(String id) {
        InteractiveItem item = createItem(id);
        return item != null ? prepareItem(item) : null;
    }

    @Nullable
    public static ItemStack createItemStack(String id, Player player) {
        InteractiveItem item = createItem(id, player);
        return item != null ? prepareItem(item) : null;
    }

    public static ItemStack prepareItem(InteractiveItem item) {
        ItemStack itemStack = item.getItemStack().clone();
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, item.getId());
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    @Nullable
    public static InteractiveItem getItemFromStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        String itemId = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (itemId == null) return null;

        ItemConfiguration config = registry.getItemConfiguration(itemId);
        if (config == null) {
            DebugUtils.logInternalWarn("Configuration not found for item ID: " + itemId + ". Item may be outdated.");
            return null;
        }

        return InteractiveItem.fromItemStack(itemStack);
    }

    public static boolean isInteractiveItem(ItemStack itemStack) {
        return getItemFromStack(itemStack) != null;
    }

    public static boolean canPlayerUseItemInCurrentRegion(Player player, ItemConfiguration config) {
        ensureInitialized();
        return ItemRegionHandler.canPlayerUseItemInCurrentRegion(player, config);
    }

    public static double getCooldownForPlayerRegion(Player player, ItemConfiguration config) {
        ensureInitialized();
        return ItemRegionHandler.getCooldownForPlayerRegion(player, config);
    }

    public static boolean canPlayerUseItem(Player player, String itemId) {
        ensureInitialized();
        return ItemInteractionHandler.canPlayerUseItem(player, itemId);
    }

    public static void setCooldown(Player player, String itemId, double seconds) {
        ensureInitialized();
        ItemInteractionHandler.setCooldown(player, itemId, seconds);
    }

    public static double getRemainingCooldown(Player player, String itemId) {
        ensureInitialized();
        return ItemInteractionHandler.getRemainingCooldown(player, itemId);
    }

    public static void removeCooldown(Player player, String itemId) {
        ensureInitialized();
        ItemInteractionHandler.removeCooldown(player, itemId);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        if (itemStack == null) {
            return;
        }

        InteractiveItem interactiveItem = getItemFromStack(itemStack);
        if (interactiveItem == null) {
            return;
        }

        ItemConfiguration config = interactiveItem.getConfiguration();
        TriggerType triggerType = config.getTriggerType();

        if (triggerType == TriggerType.HOLD) {
            DebugUtils.logInternalDebug("HOLD trigger type detected for item: " + interactiveItem.getId());
            
            // Check if there's already an active session for this item
            if (holdHandler.hasActiveSession(player, event.getHand(), interactiveItem.getEffectiveId())) {
                DebugUtils.logInternalDebug("HOLD session already active, ignoring interaction for item: " + interactiveItem.getId());
                // Just cancel the event if needed, but don't interfere with the session
                if (interactiveItem.shouldCancelEvent()) {
                    event.setCancelled(true);
                }
                return;
            }
            
            // Start session only if none exists
            DebugUtils.logInternalDebug("Starting new HOLD session for item: " + interactiveItem.getId());
            holdHandler.startHoldSession(player, interactiveItem, event.getHand());
            if (interactiveItem.shouldCancelEvent()) {
                event.setCancelled(true);
            }
            return;
        }

        if (!canPlayerClick(player.getUniqueId())) {
            DebugUtils.logInternalDebug("Player " + player.getName() + " cannot click (spam protection or other restriction)");
            return;
        }

        if (isRecentlyDropped(player.getUniqueId())) {
            DebugUtils.logInternalDebug("Player " + player.getName() + " recently dropped an item, ignoring interaction to prevent consumption bug");
            return;
        }

//        DebugUtils.logInternalDebug("Player " + player.getName() + " passed click validation");

        if (triggerType == TriggerType.AFTER_CONSUME) {
            DebugUtils.logInternalDebug("Processing AFTER_CONSUME trigger for item: " + interactiveItem.getId());
            String effectiveId = interactiveItem.getEffectiveId();
            if (config.hasCooldown() && !canPlayerUseItem(player, effectiveId)) {
                DebugUtils.logInternalDebug("Item " + effectiveId + " is on cooldown for player: " + player.getName());
                event.setCancelled(true);
                double remainingSeconds = getRemainingCooldown(player, effectiveId);
                DebugUtils.logInternalDebug("Remaining cooldown: " + remainingSeconds + " seconds for player: " + player.getName());
                interactionHandler.handleCooldownMessage(player, remainingSeconds, interactiveItem);
                return;
            }
            DebugUtils.logInternalDebug("AFTER_CONSUME item passed cooldown check, allowing consumption for player: " + player.getName());
            return;
        }

        if (interactiveItem.shouldCancelEvent()) {
//            DebugUtils.logInternalDebug("Cancelling PlayerInteract event for item: " + interactiveItem.getId());
            event.setCancelled(triggerType != TriggerType.ON_PROJECTILE_LAUNCH && triggerType != TriggerType.ON_PROJECTILE_HIT);
        }

//        DebugUtils.logInternalDebug("Creating click info and processing interaction for item: " + interactiveItem.getId());
        ItemClickInfo clickInfo = createItemClickInfo(event, player, itemStack);
        interactionHandler.processItemInteractionWithHand(player, itemStack, interactiveItem, clickInfo, event.getHand());
//        DebugUtils.logInternalDebug("Completed PlayerInteract processing for item: " + interactiveItem.getId() + " and player: " + player.getName());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) {
            return;
        }

        InteractiveItem interactiveItem = getItemFromStack(clickedItem);
        if (interactiveItem == null) {
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE &&
                event.getClickedInventory() == player.getInventory()) {
            DebugUtils.logInternalDebug("Player " + player.getName() + " is in creative mode and clicking own inventory, allowing movement");
            return;
        }

        if (ItemInventoryHandler.isMovementClick(event, interactiveItem.getConfiguration())) {
            DebugUtils.logInternalDebug("Movement click detected for item: " + interactiveItem.getId() + ", allowing movement");
            return;
        }

        if (!canPlayerClick(player.getUniqueId())) {
            DebugUtils.logInternalDebug("Player " + player.getName() + " cannot click (spam protection or other restriction) in inventory");
            return;
        }

        DebugUtils.logInternalDebug("Player " + player.getName() + " passed inventory click validation");

        if (interactiveItem.shouldCancelEvent() &&
                interactiveItem.getConfiguration().getTriggerType() != TriggerType.AFTER_CONSUME) {
            DebugUtils.logInternalDebug("Cancelling InventoryClick event for item: " + interactiveItem.getId());
            event.setCancelled(true);
        }

        DebugUtils.logInternalDebug("Creating inventory click info and processing interaction for item: " + interactiveItem.getId());
        ItemClickInfo clickInfo = createInventoryClickInfo(event, player, clickedItem);
        interactionHandler.processItemInteractionFromInventory(player, event, interactiveItem, clickInfo);
        DebugUtils.logInternalDebug("Completed InventoryClick processing for item: " + interactiveItem.getId() + " and player: " + player.getName());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        InteractiveItem interactiveItem = getItemFromStack(itemStack);
        if (interactiveItem == null) {
            return;
        }

        if (interactiveItem.getConfiguration().getTriggerType() != TriggerType.AFTER_CONSUME) {
            DebugUtils.logInternalDebug("Item " + interactiveItem.getId() + " is not AFTER_CONSUME type, ignoring consumption");
            return;
        }

        DebugUtils.logInternalDebug("Cancelling consumption event to handle manually for item: " + interactiveItem.getId());
        event.setCancelled(true); // Siempre cancelar para manejar usos manualmente

        if (!canPlayerClick(player.getUniqueId())) {
            DebugUtils.logInternalDebug("Player " + player.getName() + " cannot click (spam protection or other restriction) during consumption");
            return;
        }

        DebugUtils.logInternalDebug("Player " + player.getName() + " passed consumption click validation");

        EquipmentSlot hand = player.getInventory().getItemInMainHand().equals(itemStack) ?
                EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;

        DebugUtils.logInternalDebug("Detected consumption hand: " + hand + " for player: " + player.getName());

        ItemClickInfo clickInfo = new ItemClickInfo(player,
                org.bukkit.event.inventory.ClickType.RIGHT,
                hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                itemStack,
                ActionSource.ITEM_USE);

        DebugUtils.logInternalDebug("Processing item consumption for item: " + interactiveItem.getId() + " and player: " + player.getName());
        interactionHandler.processItemConsumptionEvent(player, itemStack, interactiveItem, clickInfo, hand);
        DebugUtils.logInternalDebug("Completed PlayerItemConsume processing for item: " + interactiveItem.getId() + " and player: " + player.getName());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof Player hitPlayer)) {
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        InteractiveItem interactiveItem = null;
        EquipmentSlot hand = null;

        if (isInteractiveItem(mainHand)) {
            interactiveItem = getItemFromStack(mainHand);
            hand = EquipmentSlot.HAND;
        } else if (isInteractiveItem(offHand)) {
            interactiveItem = getItemFromStack(offHand);
            hand = EquipmentSlot.OFF_HAND;
        }

        if (interactiveItem == null) {
            return;
        }

        DebugUtils.logInternalDebug("Processing hit with interactive item: " + interactiveItem.getId() + " in " + hand + " hand");

        if (!canPlayerClick(player.getUniqueId())) {
            DebugUtils.logInternalDebug("Player " + player.getName() + " cannot click (spam protection or other restriction) during hit");
            return;
        }

        DebugUtils.logInternalDebug("Player " + player.getName() + " passed hit click validation");

        ItemClickInfo clickInfo = new ItemClickInfo(player,
                org.bukkit.event.inventory.ClickType.LEFT,
                hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                hand == EquipmentSlot.HAND ? mainHand : offHand,
                ActionSource.ITEM_CLICK);

        clickInfo.withData("hitPlayer", hitPlayer); // Añadir hitPlayer al contexto

        DebugUtils.logInternalDebug("Processing hit player interaction for item: " + interactiveItem.getId() +
                ", attacker: " + player.getName() + ", victim: " + hitPlayer.getName());
        interactionHandler.processHitPlayer(player, hitPlayer, hand == EquipmentSlot.HAND ? mainHand : offHand,
                interactiveItem, clickInfo, hand);
        DebugUtils.logInternalDebug("Completed EntityDamageByEntity processing for item: " + interactiveItem.getId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) {
            return;
        }


        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        InteractiveItem interactiveItem = null;
        EquipmentSlot hand = null;

        if (isInteractiveItem(mainHand)) {
            InteractiveItem mainItem = getItemFromStack(mainHand);
            if (mainItem != null) {
                TriggerType trigger = mainItem.getConfiguration().getTriggerType();
                DebugUtils.logInternalDebug("Main hand item " + mainItem.getId() + " has trigger type: " + trigger);
                if (trigger != TriggerType.ON_PROJECTILE_LAUNCH && trigger != TriggerType.ON_PROJECTILE_HIT) {
                    DebugUtils.logInternalDebug("Cancelling projectile launch - main hand item has incompatible trigger type: " + trigger);
                    event.setCancelled(true);
                    return;
                }
                interactiveItem = mainItem;
                hand = EquipmentSlot.HAND;
                DebugUtils.logInternalDebug("Selected main hand item for projectile launch: " + mainItem.getId());
            }
        }

        if (interactiveItem == null && isInteractiveItem(offHand)) {
            InteractiveItem offItem = getItemFromStack(offHand);
            if (offItem != null) {
                TriggerType trigger = offItem.getConfiguration().getTriggerType();
                DebugUtils.logInternalDebug("Off hand item " + offItem.getId() + " has trigger type: " + trigger);
                if (trigger != TriggerType.ON_PROJECTILE_LAUNCH && trigger != TriggerType.ON_PROJECTILE_HIT) {
                    DebugUtils.logInternalDebug("Cancelling projectile launch - off hand item has incompatible trigger type: " + trigger);
                    event.setCancelled(true);
                    return;
                }
                interactiveItem = offItem;
                hand = EquipmentSlot.OFF_HAND;
                DebugUtils.logInternalDebug("Selected off hand item for projectile launch: " + offItem.getId());
            }
        }

        if (interactiveItem == null) {
            return;
        }

        DebugUtils.logInternalDebug("Processing projectile launch with item: " + interactiveItem.getId() + " in " + hand + " hand");

        ItemConfiguration config = interactiveItem.getConfiguration();
        if (!ItemRegionHandler.canPlayerUseItemInCurrentRegion(player, config)) {
            DebugUtils.logInternalDebug("Player " + player.getName() + " cannot use item " + interactiveItem.getId() + " in current region");
            event.setCancelled(true);
            interactionHandler.handleRegionDeniedMessage(player, interactiveItem);
            return;
        }

        DebugUtils.logInternalDebug("Player " + player.getName() + " passed region check for item: " + interactiveItem.getId());

        String effectiveId = interactiveItem.getEffectiveId();
        if (config.hasCooldown() && !canPlayerUseItem(player, effectiveId)) {
            DebugUtils.logInternalDebug("Item " + effectiveId + " is on cooldown for player: " + player.getName());
            event.setCancelled(true);
            double remainingSeconds = getRemainingCooldown(player, effectiveId);
            DebugUtils.logInternalDebug("Remaining cooldown: " + remainingSeconds + " seconds for projectile launch");
            interactionHandler.handleCooldownMessage(player, remainingSeconds, interactiveItem);
            return;
        }

        DebugUtils.logInternalDebug("Item " + effectiveId + " passed cooldown check for projectile launch");

        if (!interactiveItem.hasUsesRemaining()) {
            DebugUtils.logInternalDebug("Item " + interactiveItem.getId() + " has no uses remaining, cancelling projectile launch");
            event.setCancelled(true);
            return;
        }

        DebugUtils.logInternalDebug("Item " + interactiveItem.getId() + " has uses remaining for projectile launch");

        TriggerType triggerType = config.getTriggerType();

        if (triggerType == TriggerType.ON_PROJECTILE_LAUNCH || triggerType == TriggerType.ON_PROJECTILE_HIT) {
            DebugUtils.logInternalDebug("Processing projectile trigger type: " + triggerType + " for item: " + interactiveItem.getId());

            // Cancel the vanilla event to prevent double consumption
            event.setCancelled(true);
            DebugUtils.logInternalDebug("Cancelled vanilla projectile launch event for item: " + interactiveItem.getId());

            ItemClickInfo clickInfo = new ItemClickInfo(player,
                    org.bukkit.event.inventory.ClickType.RIGHT,
                    hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                    hand == EquipmentSlot.HAND ? mainHand : offHand,
                    ActionSource.ITEM_USE);

            // Process the item interaction first
            interactionHandler.processProjectileLaunch(player, hand == EquipmentSlot.HAND ? mainHand : offHand,
                    interactiveItem, clickInfo, hand);

            // Create and launch the projectile manually
            final Material itemMaterial = (hand == EquipmentSlot.HAND ? mainHand : offHand).getType();
            final EquipmentSlot finalHand = hand;
            final String finalItemId = interactiveItem.getId();
            final String finalEffectiveId = effectiveId;
            final ItemStack finalItemClone = (hand == EquipmentSlot.HAND ? mainHand : offHand).clone();
            final InteractiveItem finalInteractiveItem = interactiveItem.clone();

            Bukkit.getScheduler().runTask(plugin, () -> {
                org.bukkit.Location eyeLocation = player.getEyeLocation();
                org.bukkit.util.Vector direction = eyeLocation.getDirection().multiply(1.5);

                // Spawn the projectile based on item material
                org.bukkit.entity.EntityType projectileType = getProjectileTypeFromMaterial(itemMaterial);
                org.bukkit.entity.Projectile customProjectile = (org.bukkit.entity.Projectile) player.getWorld().spawnEntity(
                        eyeLocation.add(direction.clone().multiply(0.5)),
                        projectileType
                );

                customProjectile.setShooter(player);
                customProjectile.setVelocity(direction);
                customProjectile.setMetadata("custom_projectile", new FixedMetadataValue(plugin, true));

                if (triggerType == TriggerType.ON_PROJECTILE_HIT) {
                    DebugUtils.logInternalDebug("Setting metadata for ON_PROJECTILE_HIT item: " + finalItemId);
                    customProjectile.setMetadata("interactive_item_id", new FixedMetadataValue(plugin, finalItemId));
                    customProjectile.setMetadata("interactive_item_effective_id", new FixedMetadataValue(plugin, finalEffectiveId));
                    customProjectile.setMetadata("interactive_item_hand", new FixedMetadataValue(plugin, finalHand.name()));
                    customProjectile.setMetadata("interactive_item_shooter", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
                    customProjectile.setMetadata("interactive_item_stack", new FixedMetadataValue(plugin, finalItemClone));
                    customProjectile.setMetadata("interactive_item_object", new FixedMetadataValue(plugin, finalInteractiveItem));
                    DebugUtils.logInternalDebug("Metadata set for projectile hit detection, item: " + finalItemId);
                }

                DebugUtils.logInternalDebug("Manually spawned projectile of type " + projectileType + " for item: " + finalItemId);
            });

            DebugUtils.logInternalDebug("Completed ProjectileLaunch processing for item: " + interactiveItem.getId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!projectile.hasMetadata("interactive_item_id")) {
            return;
        }

        DebugUtils.logInternalDebug("ProjectileHit event triggered for projectile: " + projectile.getType());

        String itemId = projectile.getMetadata("interactive_item_id").get(0).asString();
        String effectiveId = projectile.getMetadata("interactive_item_effective_id").get(0).asString();
        String handName = projectile.getMetadata("interactive_item_hand").get(0).asString();
        String shooterUUID = projectile.getMetadata("interactive_item_shooter").get(0).asString();

        DebugUtils.logInternalDebug("Projectile hit metadata - ItemId: " + itemId + ", EffectiveId: " + effectiveId +
                ", Hand: " + handName + ", Shooter: " + shooterUUID);

        ItemStack originalItemStack = null;
        InteractiveItem originalInteractiveItem = null;

        if (projectile.hasMetadata("interactive_item_stack")) {
            originalItemStack = (ItemStack) projectile.getMetadata("interactive_item_stack").get(0).value();
            DebugUtils.logInternalDebug("Retrieved original item stack from projectile metadata");
        }

        if (projectile.hasMetadata("interactive_item_object")) {
            originalInteractiveItem = (InteractiveItem) projectile.getMetadata("interactive_item_object").get(0).value();
            DebugUtils.logInternalDebug("Retrieved original interactive item from projectile metadata");
        }

        Player shooter;
        try {
            shooter = Bukkit.getPlayer(java.util.UUID.fromString(shooterUUID));
        } catch (Exception e) {
            DebugUtils.logInternalDebug("Failed to parse shooter UUID: " + shooterUUID + ", error: " + e.getMessage());
            return;
        }

        if (shooter == null || !shooter.isOnline()) {
            DebugUtils.logInternalDebug("Shooter is null or offline for projectile hit, UUID: " + shooterUUID);
            return;
        }

        DebugUtils.logInternalDebug("Shooter found and online: " + shooter.getName());

        InteractiveItem interactiveItem = originalInteractiveItem;
        if (interactiveItem == null) {
            DebugUtils.logInternalDebug("No original interactive item in metadata, searching in shooter's hands");
            EquipmentSlot hand = EquipmentSlot.valueOf(handName);
            ItemStack itemStack = hand == EquipmentSlot.HAND ?
                    shooter.getInventory().getItemInMainHand() :
                    shooter.getInventory().getItemInOffHand();

            interactiveItem = getItemFromStack(itemStack);
            if (interactiveItem == null || !interactiveItem.getId().equals(itemId)) {
                DebugUtils.logInternalDebug("Item in hand changed since launch, searching inventory for item: " + itemId);
                // El item cambió desde el lanzamiento, intentar buscar en inventario
                interactiveItem = findInteractiveItemInInventory(shooter, itemId);
                if (interactiveItem == null) {
                    DebugUtils.logInternalDebug("Item not found in inventory, creating fallback item for: " + itemId);
                    // FALLBACK: Crear un item temporal con la configuración
                    ItemConfiguration config = getItemConfiguration(itemId);
                    if (config != null) {
                        interactiveItem = new InteractiveItem(itemId, config, shooter);
                        // Si tenemos el itemStack original, usar sus datos NBT
                        if (originalItemStack != null) {
                            InteractiveItem tempFromStack = InteractiveItem.fromItemStack(originalItemStack);
                            if (tempFromStack != null) {
                                interactiveItem = tempFromStack;
                                DebugUtils.logInternalDebug("Created fallback item from original stack NBT data");
                            }
                        }
                        DebugUtils.logInternalDebug("Created fallback interactive item: " + itemId);
                    } else {
                        DebugUtils.logInternalDebug("No configuration found for item: " + itemId + ", cannot process projectile hit");
                        return;
                    }
                } else {
                    DebugUtils.logInternalDebug("Found item in inventory: " + interactiveItem.getId());
                }
            } else {
                DebugUtils.logInternalDebug("Item still in shooter's hand: " + interactiveItem.getId());
            }
        } else {
            DebugUtils.logInternalDebug("Using original interactive item from metadata: " + originalInteractiveItem.getId());
        }

        if (interactiveItem.getConfiguration().getTriggerType() != TriggerType.ON_PROJECTILE_HIT) {
            DebugUtils.logInternalDebug("Item " + interactiveItem.getId() + " is not ON_PROJECTILE_HIT type, ignoring hit");
            return;
        }

        DebugUtils.logInternalDebug("Processing ON_PROJECTILE_HIT for item: " + interactiveItem.getId());

        Player hitPlayer = event.getHitEntity() instanceof Player ? (Player) event.getHitEntity() : null;
        if (hitPlayer != null) {
            DebugUtils.logInternalDebug("Projectile hit player: " + hitPlayer.getName());
        } else {
            DebugUtils.logInternalDebug("Projectile hit non-player entity or block");
        }

        ItemStack itemStackForClick = originalItemStack != null ? originalItemStack :
                (EquipmentSlot.valueOf(handName) == EquipmentSlot.HAND ?
                        shooter.getInventory().getItemInMainHand() :
                        shooter.getInventory().getItemInOffHand());

        ItemClickInfo clickInfo = new ItemClickInfo(shooter,
                org.bukkit.event.inventory.ClickType.RIGHT,
                EquipmentSlot.valueOf(handName) == EquipmentSlot.HAND ? shooter.getInventory().getHeldItemSlot() : 40,
                itemStackForClick,
                ActionSource.ITEM_USE,
                projectile.getLocation());

        if (hitPlayer != null) {
            clickInfo.withData("hitPlayer", hitPlayer);
        }
        clickInfo.withData("effectiveId", effectiveId);

        DebugUtils.logInternalDebug("Processing projectile hit interaction for item: " + interactiveItem.getId() +
                ", shooter: " + shooter.getName() + (hitPlayer != null ? ", hit player: " + hitPlayer.getName() : ""));
        interactionHandler.processProjectileHit(shooter, hitPlayer, itemStackForClick,
                interactiveItem, clickInfo, EquipmentSlot.valueOf(handName));
        DebugUtils.logInternalDebug("Completed ProjectileHit processing for item: " + interactiveItem.getId());
    }

    private InteractiveItem findInteractiveItemInInventory(Player player, String itemId) {
        DebugUtils.logInternalDebug("Searching inventory for interactive item: " + itemId + " for player: " + player.getName());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                InteractiveItem interactiveItem = getItemFromStack(item);
                if (interactiveItem != null && interactiveItem.getId().equals(itemId)) {
                    return interactiveItem;
                }
            }
        }
        DebugUtils.logInternalDebug("Interactive item " + itemId + " not found in inventory for player: " + player.getName());
        return null;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            DebugUtils.logInternalDebug("InventoryDrag event not triggered by player, ignoring");
            return;
        }

        DebugUtils.logInternalDebug("InventoryDrag event triggered for player: " + player.getName());

        ItemStack draggedItem = event.getOldCursor();
        InteractiveItem interactiveItem = getItemFromStack(draggedItem);
        if (interactiveItem == null) {
            return;
        }

        ItemConfiguration config = interactiveItem.getConfiguration();

        if (player.getGameMode() == GameMode.CREATIVE) {
            DebugUtils.logInternalDebug("Player " + player.getName() + " is in creative mode, allowing drag for item: " + interactiveItem.getId());
            return;
        }

        if (!config.isAllowMovement()) {
            DebugUtils.logInternalDebug("Item " + interactiveItem.getId() + " does not allow movement, cancelling drag");
            event.setCancelled(true);
            return;
        }

        boolean involvesPlayerInventory = event.getRawSlots().stream()
                .anyMatch(slot -> slot >= event.getView().getTopInventory().getSize());

        DebugUtils.logInternalDebug("Drag involves player inventory: " + involvesPlayerInventory + " for item: " + interactiveItem.getId());

        if (involvesPlayerInventory && !config.isAllowMovement()) {
            DebugUtils.logInternalDebug("Cancelling drag involving player inventory for item: " + interactiveItem.getId());
            event.setCancelled(true);
        }

        DebugUtils.logInternalDebug("Completed InventoryDrag processing for item: " + interactiveItem.getId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        lastDropTime.put(player.getUniqueId(), System.currentTimeMillis());

        InteractiveItem interactiveItem = getItemFromStack(droppedItem);
        if (interactiveItem == null) {
            return;
        }

        ItemConfiguration config = interactiveItem.getConfiguration();

        if (player.getGameMode() == GameMode.CREATIVE) {
            DebugUtils.logInternalDebug("Player " + player.getName() + " is in creative mode, allowing drop for item: " + interactiveItem.getId());
            return;
        }

        if (!config.isAllowDrop()) {
            DebugUtils.logInternalDebug("Item " + interactiveItem.getId() + " does not allow dropping, cancelling drop by player: " + player.getName());
            event.setCancelled(true);
        } else {
            DebugUtils.logInternalDebug("Item " + interactiveItem.getId() + " allows dropping, drop successful for player: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();
        if (mainHand != null) {
            InteractiveItem mainInteractiveItem = getItemFromStack(mainHand);
            if (mainInteractiveItem != null) {
                ItemConfiguration config = mainInteractiveItem.getConfiguration();
                if (player.getGameMode() != GameMode.CREATIVE && !config.isAllowSwapToOffhand()) {
                    DebugUtils.logInternalDebug("Item " + mainInteractiveItem.getId() + " does not allow swap to offhand, cancelling swap");
                    event.setCancelled(true);
                    return;
                } else {
                    DebugUtils.logInternalDebug("Item " + mainInteractiveItem.getId() + " allows swap to offhand or player is in creative");
                }
            }
        }

        if (offHand != null) {
            InteractiveItem offInteractiveItem = getItemFromStack(offHand);
            if (offInteractiveItem != null) {
                ItemConfiguration config = offInteractiveItem.getConfiguration();
                if (player.getGameMode() != GameMode.CREATIVE && !config.isAllowSwapToOffhand()) {
                    DebugUtils.logInternalDebug("Item " + offInteractiveItem.getId() + " does not allow swap to offhand, cancelling swap");
                    event.setCancelled(true);
                } else {
                    DebugUtils.logInternalDebug("Item " + offInteractiveItem.getId() + " allows swap to offhand or player is in creative");
                }
            }
        }

        if (!event.isCancelled()) {
            holdHandler.stopAllSessionsForPlayer(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack newMainHand = player.getInventory().getItemInMainHand();
                ItemStack newOffHand = player.getInventory().getItemInOffHand();
                
                if (newMainHand != null && newMainHand.getType() != Material.AIR) {
                    InteractiveItem mainItem = getItemFromStack(newMainHand);
                    if (mainItem != null && mainItem.getConfiguration().getTriggerType() == TriggerType.HOLD) {
                        DebugUtils.logInternalDebug("Starting HOLD session for main hand after swap: " + mainItem.getId());
                        holdHandler.startHoldSession(player, mainItem, EquipmentSlot.HAND);
                    }
                }
                
                if (newOffHand != null && newOffHand.getType() != Material.AIR) {
                    InteractiveItem offItem = getItemFromStack(newOffHand);
                    if (offItem != null && offItem.getConfiguration().getTriggerType() == TriggerType.HOLD) {
                        DebugUtils.logInternalDebug("Starting HOLD session for off hand after swap: " + offItem.getId());
                        holdHandler.startHoldSession(player, offItem, EquipmentSlot.OFF_HAND);
                    }
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        holdHandler.stopAllSessionsForPlayer(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
            if (newItem != null && newItem.getType() != Material.AIR) {
                InteractiveItem interactiveItem = getItemFromStack(newItem);
                if (interactiveItem != null && interactiveItem.getConfiguration().getTriggerType() == TriggerType.HOLD) {
                    DebugUtils.logInternalDebug("Starting HOLD session for newly selected item: " + interactiveItem.getId());
                    holdHandler.startHoldSession(player, interactiveItem, EquipmentSlot.HAND);
                }
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR) 
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DebugUtils.logInternalDebug("PlayerQuit event triggered for player: " + player.getName());
        
        // Stop all HOLD sessions for the disconnecting player
        holdHandler.stopAllSessionsForPlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClickForHold(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Only check if the click affects the hotbar or hands
        if (event.getSlot() >= 0 && event.getSlot() <= 8) { // Hotbar slots
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkAndStartHoldSessions(player), 1L);
        } else if (event.getSlot() == 40) { // Off-hand slot
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkAndStartHoldSessions(player), 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        
        // Check if the picked up item could go to the hands
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkAndStartHoldSessions(player), 2L);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemStack = event.getItemInHand();
        InteractiveItem interactiveItem = getItemFromStack(itemStack);
        if (interactiveItem != null) {
            event.setCancelled(true);
        }
    }

    private static ItemClickInfo createItemClickInfo(PlayerInteractEvent event, Player player, ItemStack itemStack) {
        Action action = event.getAction();
        org.bukkit.event.inventory.ClickType clickType;

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            clickType = org.bukkit.event.inventory.ClickType.LEFT;
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            clickType = org.bukkit.event.inventory.ClickType.RIGHT;
        } else {
            clickType = org.bukkit.event.inventory.ClickType.UNKNOWN;
        }

        int slot = event.getHand() == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40;

        return new ItemClickInfo(player, clickType, slot, itemStack, ActionSource.ITEM_CLICK);
    }

    private static ItemClickInfo createInventoryClickInfo(InventoryClickEvent event, Player player, ItemStack itemStack) {
        return new ItemClickInfo(player, event.getClick(), event.getSlot(), itemStack, ActionSource.INVENTORY_CLICK);
    }

    private static boolean canPlayerClick(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastClickTime.get(playerId);

        if (lastTime != null && (currentTime - lastTime) < DOUBLE_CLICK_PREVENTION_MS) {
            return false;
        }

        lastClickTime.put(playerId, currentTime);
        return true;
    }

    private static boolean isRecentlyDropped(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastDropTime.get(playerId);

        return lastTime != null && (currentTime - lastTime) < DROP_INTERACTION_PREVENTION_MS;
    }

    private static void startClickTimeCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            lastClickTime.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > DOUBLE_CLICK_PREVENTION_MS);
            lastDropTime.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > DROP_INTERACTION_PREVENTION_MS);
        }, 20L * 60, 20L * 60);
    }

    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ItemManager has not been initialized. Call ItemManager.initialize(plugin) first.");
        }
    }

    // ===== MÉTODOS PARA DISPLAY NAME UNIFICADO =====

    public static String getEffectiveDisplayName(String itemId) {
        ensureInitialized();

        // Verificar si es un item interactivo
        ItemConfiguration interactiveConfig = getItemConfiguration(itemId);
        if (interactiveConfig != null) {
            if (interactiveConfig.hasDisplayName()) {
                return interactiveConfig.getDisplayName();
            } else if (interactiveConfig.getName() != null) {
                return interactiveConfig.getName();
            }
        }

        // Verificar si es un item vanilla con el formato "vanilla_material"
        if (itemId.startsWith("vanilla_")) {
            String materialName = itemId.substring(8).toUpperCase();
            try {
                Material material = Material.valueOf(materialName);
                VanillaItemCooldownManager vanillaManager = VanillaItemCooldownManager.getInstance();
                return vanillaManager.getEffectiveDisplayName(material);
            } catch (IllegalArgumentException e) {
                // Material no válido
            }
        }

        return null;
    }

    public static String getEffectiveDisplayName(String itemId, String fallback) {
        String displayName = getEffectiveDisplayName(itemId);
        return displayName != null ? displayName : fallback;
    }

    public static boolean hasCustomDisplayName(String itemId) {
        ensureInitialized();

        // Verificar item interactivo
        ItemConfiguration interactiveConfig = getItemConfiguration(itemId);
        if (interactiveConfig != null) {
            return interactiveConfig.hasDisplayName();
        }

        // Verificar item vanilla
        if (itemId.startsWith("vanilla_")) {
            String materialName = itemId.substring(8).toUpperCase();
            try {
                Material material = Material.valueOf(materialName);
                VanillaItemCooldownManager vanillaManager = VanillaItemCooldownManager.getInstance();
                return vanillaManager.hasCustomDisplayName(material);
            } catch (IllegalArgumentException e) {
                // Material no válido
            }
        }

        return false;
    }

    public static Material getMaterialFromVanillaId(String itemId) {
        if (!itemId.startsWith("vanilla_")) {
            return null;
        }

        String materialName = itemId.substring(8).toUpperCase();
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isVanillaItem(String itemId) {
        return itemId.startsWith("vanilla_") && getMaterialFromVanillaId(itemId) != null;
    }
    
    public static void shutdown() {
        if (holdHandler != null) {
            holdHandler.cleanup();
        }
    }
    
    // Utility method to check and start HOLD sessions for both hands
    private static void checkAndStartHoldSessions(Player player) {
        if (holdHandler == null) return;

        // Check main hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.getType() != Material.AIR) {
            InteractiveItem mainItem = getItemFromStack(mainHand);
            if (mainItem != null && mainItem.getConfiguration().getTriggerType() == TriggerType.HOLD) {
                DebugUtils.logInternalDebug("Auto-starting HOLD session for main hand: " + mainItem.getId());
                holdHandler.startHoldSession(player, mainItem, EquipmentSlot.HAND);
            }
        }

        // Check off hand
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && offHand.getType() != Material.AIR) {
            InteractiveItem offItem = getItemFromStack(offHand);
            if (offItem != null && offItem.getConfiguration().getTriggerType() == TriggerType.HOLD) {
                DebugUtils.logInternalDebug("Auto-starting HOLD session for off hand: " + offItem.getId());
                holdHandler.startHoldSession(player, offItem, EquipmentSlot.OFF_HAND);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null && isInteractiveItem(ingredient)) {
                DebugUtils.logInternalDebug("Interactive item found in crafting matrix, cancelling craft preparation");
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null && isInteractiveItem(ingredient)) {
                DebugUtils.logInternalDebug("Interactive item found in crafting matrix, cancelling craft event");
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);

        if ((first != null && isInteractiveItem(first)) || (second != null && isInteractiveItem(second))) {
            DebugUtils.logInternalDebug("Interactive item found in anvil, cancelling anvil preparation");
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        for (ItemStack ingredient : event.getInventory().getContents()) {
            if (ingredient != null && isInteractiveItem(ingredient)) {
                DebugUtils.logInternalDebug("Interactive item found in smithing table, cancelling smithing preparation");
                event.setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        if (isInteractiveItem(event.getItem())) {
            DebugUtils.logInternalDebug("Interactive item found in enchanting table, cancelling enchant preparation");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        if (isInteractiveItem(event.getItem())) {
            DebugUtils.logInternalDebug("Interactive item found in enchanting table, cancelling enchant event");
            event.setCancelled(true);
        }
    }

    private static org.bukkit.entity.EntityType getProjectileTypeFromMaterial(Material material) {
        return switch (material) {
            case EGG -> org.bukkit.entity.EntityType.EGG;
            case SNOWBALL -> org.bukkit.entity.EntityType.SNOWBALL;
            case ENDER_PEARL -> org.bukkit.entity.EntityType.ENDER_PEARL;
            case EXPERIENCE_BOTTLE -> org.bukkit.entity.EntityType.THROWN_EXP_BOTTLE;
            case SPLASH_POTION, LINGERING_POTION -> org.bukkit.entity.EntityType.SPLASH_POTION;
            case TRIDENT -> org.bukkit.entity.EntityType.TRIDENT;
            case BOW, CROSSBOW -> org.bukkit.entity.EntityType.ARROW;
            default -> org.bukkit.entity.EntityType.EGG; // Default to egg for other throwable items
        };
    }
}
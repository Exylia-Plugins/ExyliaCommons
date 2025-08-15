package net.exylia.commons.item;

import lombok.Getter;
import net.exylia.commons.actions.ActionSource;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.item.config.TriggerType;
import net.exylia.commons.item.cooldown.CooldownManager;
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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.exylia.commons.config.base.MainConfigBase.debug;

/**
 * Manager principal del sistema de items - MODULARIZADO
 * CORREGIDO: Problemas con items lanzables
 */
public class ItemManager implements Listener {

    @Getter
    private static JavaPlugin plugin;
    private static ItemRegistry registry;
    private static ItemInteractionHandler interactionHandler;
    private static NamespacedKey itemIdKey;
    private static boolean initialized = false;

    private static final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();
    private static final long DOUBLE_CLICK_PREVENTION_MS = 150;

    public static void initialize(JavaPlugin javaPlugin) {
        if (initialized) return;

        plugin = javaPlugin;
        registry = new ItemRegistryImpl();
        interactionHandler = new ItemInteractionHandler(plugin);
        itemIdKey = new NamespacedKey(plugin, "interactive_item_id");

        Bukkit.getPluginManager().registerEvents(new ItemManager(), plugin);
        CooldownManager.initialize(plugin);
        startClickTimeCleanupTask();

        initialized = true;

        if (WorldGuardUtils.isWorldGuardAvailable()) {
            DebugUtils.logInternalInfo("WorldGuard detectado - Soporte de regiones habilitado");
        }
    }

    // ... métodos de registro sin cambios ...
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        if (itemStack == null) {
            return;
        }

        InteractiveItem interactiveItem = getItemFromStack(itemStack);
        if (interactiveItem == null) {
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Found interactive item: " + interactiveItem.getId() + " for player: " + player.getName());

        ItemConfiguration config = interactiveItem.getConfiguration();
        TriggerType triggerType = config.getTriggerType();

        DebugUtils.logInternalDebug(debug(), "Item trigger type: " + triggerType + " for item: " + interactiveItem.getId());

        if (triggerType == TriggerType.ON_PROJECTILE_LAUNCH || triggerType == TriggerType.ON_PROJECTILE_HIT) {
            DebugUtils.logInternalDebug(debug(), "Projectile trigger type detected, skipping PlayerInteract processing for item: " + interactiveItem.getId());
            return;
        }

        if (!canPlayerClick(player.getUniqueId())) {
            DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " cannot click (spam protection or other restriction)");
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " passed click validation");

        if (triggerType == TriggerType.AFTER_CONSUME) {
            DebugUtils.logInternalDebug(debug(), "Processing AFTER_CONSUME trigger for item: " + interactiveItem.getId());
            String effectiveId = interactiveItem.getEffectiveId();
            if (config.hasCooldown() && !canPlayerUseItem(player, effectiveId)) {
                DebugUtils.logInternalDebug(debug(), "Item " + effectiveId + " is on cooldown for player: " + player.getName());
                event.setCancelled(true);
                double remainingSeconds = getRemainingCooldown(player, effectiveId);
                DebugUtils.logInternalDebug(debug(), "Remaining cooldown: " + remainingSeconds + " seconds for player: " + player.getName());
                interactionHandler.handleCooldownMessage(player, remainingSeconds);
                return;
            }
            DebugUtils.logInternalDebug(debug(), "AFTER_CONSUME item passed cooldown check, allowing consumption for player: " + player.getName());
            return;
        }

        if (interactiveItem.shouldCancelEvent()) {
            DebugUtils.logInternalDebug(debug(), "Cancelling PlayerInteract event for item: " + interactiveItem.getId());
            event.setCancelled(true);
        }

        DebugUtils.logInternalDebug(debug(), "Creating click info and processing interaction for item: " + interactiveItem.getId());
        ItemClickInfo clickInfo = createItemClickInfo(event, player, itemStack);
        interactionHandler.processItemInteractionWithHand(player, itemStack, interactiveItem, clickInfo, event.getHand());
        DebugUtils.logInternalDebug(debug(), "Completed PlayerInteract processing for item: " + interactiveItem.getId() + " and player: " + player.getName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
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

        DebugUtils.logInternalDebug(debug(), "Found interactive item: " + interactiveItem.getId() + " in inventory for player: " + player.getName());

        if (player.getGameMode() == GameMode.CREATIVE &&
                event.getClickedInventory() == player.getInventory()) {
            DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " is in creative mode and clicking own inventory, allowing movement");
            return;
        }

        if (ItemInventoryHandler.isMovementClick(event, interactiveItem.getConfiguration())) {
            DebugUtils.logInternalDebug(debug(), "Movement click detected for item: " + interactiveItem.getId() + ", allowing movement");
            return;
        }

        if (!canPlayerClick(player.getUniqueId())) {
            DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " cannot click (spam protection or other restriction) in inventory");
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " passed inventory click validation");

        if (interactiveItem.shouldCancelEvent() &&
                interactiveItem.getConfiguration().getTriggerType() != TriggerType.AFTER_CONSUME) {
            DebugUtils.logInternalDebug(debug(), "Cancelling InventoryClick event for item: " + interactiveItem.getId());
            event.setCancelled(true);
        }

        DebugUtils.logInternalDebug(debug(), "Creating inventory click info and processing interaction for item: " + interactiveItem.getId());
        ItemClickInfo clickInfo = createInventoryClickInfo(event, player, clickedItem);
        interactionHandler.processItemInteractionFromInventory(player, event, interactiveItem, clickInfo);
        DebugUtils.logInternalDebug(debug(), "Completed InventoryClick processing for item: " + interactiveItem.getId() + " and player: " + player.getName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        InteractiveItem interactiveItem = getItemFromStack(itemStack);
        if (interactiveItem == null) {
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Found interactive item: " + interactiveItem.getId() + " for consumption by player: " + player.getName());

        if (interactiveItem.getConfiguration().getTriggerType() != TriggerType.AFTER_CONSUME) {
            DebugUtils.logInternalDebug(debug(), "Item " + interactiveItem.getId() + " is not AFTER_CONSUME type, ignoring consumption");
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Cancelling consumption event to handle manually for item: " + interactiveItem.getId());
        event.setCancelled(true); // Siempre cancelar para manejar usos manualmente

        if (!canPlayerClick(player.getUniqueId())) {
            DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " cannot click (spam protection or other restriction) during consumption");
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " passed consumption click validation");

        EquipmentSlot hand = player.getInventory().getItemInMainHand().equals(itemStack) ?
                EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;

        DebugUtils.logInternalDebug(debug(), "Detected consumption hand: " + hand + " for player: " + player.getName());

        ItemClickInfo clickInfo = new ItemClickInfo(player,
                org.bukkit.event.inventory.ClickType.RIGHT,
                hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                itemStack,
                ActionSource.ITEM_USE);

        DebugUtils.logInternalDebug(debug(), "Processing item consumption for item: " + interactiveItem.getId() + " and player: " + player.getName());
        interactionHandler.processItemConsumptionEvent(player, itemStack, interactiveItem, clickInfo, hand);
        DebugUtils.logInternalDebug(debug(), "Completed PlayerItemConsume processing for item: " + interactiveItem.getId() + " and player: " + player.getName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
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
            DebugUtils.logInternalDebug(debug(), "Found interactive item in main hand for player: " + player.getName());
            interactiveItem = getItemFromStack(mainHand);
            hand = EquipmentSlot.HAND;
        } else if (isInteractiveItem(offHand)) {
            DebugUtils.logInternalDebug(debug(), "Found interactive item in off hand for player: " + player.getName());
            interactiveItem = getItemFromStack(offHand);
            hand = EquipmentSlot.OFF_HAND;
        }

        if (interactiveItem == null) {
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Processing hit with interactive item: " + interactiveItem.getId() + " in " + hand + " hand");

        if (!canPlayerClick(player.getUniqueId())) {
            DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " cannot click (spam protection or other restriction) during hit");
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " passed hit click validation");

        ItemClickInfo clickInfo = new ItemClickInfo(player,
                org.bukkit.event.inventory.ClickType.LEFT,
                hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                hand == EquipmentSlot.HAND ? mainHand : offHand,
                ActionSource.ITEM_CLICK);

        clickInfo.withData("hitPlayer", hitPlayer); // Añadir hitPlayer al contexto

        DebugUtils.logInternalDebug(debug(), "Processing hit player interaction for item: " + interactiveItem.getId() +
                ", attacker: " + player.getName() + ", victim: " + hitPlayer.getName());
        interactionHandler.processHitPlayer(player, hitPlayer, hand == EquipmentSlot.HAND ? mainHand : offHand,
                interactiveItem, clickInfo, hand);
        DebugUtils.logInternalDebug(debug(), "Completed EntityDamageByEntity processing for item: " + interactiveItem.getId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
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
                DebugUtils.logInternalDebug(debug(), "Main hand item " + mainItem.getId() + " has trigger type: " + trigger);
                if (trigger != TriggerType.ON_PROJECTILE_LAUNCH && trigger != TriggerType.ON_PROJECTILE_HIT) {
                    DebugUtils.logInternalDebug(debug(), "Cancelling projectile launch - main hand item has incompatible trigger type: " + trigger);
                    event.setCancelled(true);
                    return;
                }
                interactiveItem = mainItem;
                hand = EquipmentSlot.HAND;
                DebugUtils.logInternalDebug(debug(), "Selected main hand item for projectile launch: " + mainItem.getId());
            }
        }

        if (interactiveItem == null && isInteractiveItem(offHand)) {
            InteractiveItem offItem = getItemFromStack(offHand);
            if (offItem != null) {
                TriggerType trigger = offItem.getConfiguration().getTriggerType();
                DebugUtils.logInternalDebug(debug(), "Off hand item " + offItem.getId() + " has trigger type: " + trigger);
                if (trigger != TriggerType.ON_PROJECTILE_LAUNCH && trigger != TriggerType.ON_PROJECTILE_HIT) {
                    DebugUtils.logInternalDebug(debug(), "Cancelling projectile launch - off hand item has incompatible trigger type: " + trigger);
                    event.setCancelled(true);
                    return;
                }
                interactiveItem = offItem;
                hand = EquipmentSlot.OFF_HAND;
                DebugUtils.logInternalDebug(debug(), "Selected off hand item for projectile launch: " + offItem.getId());
            }
        }

        if (interactiveItem == null) {
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Processing projectile launch with item: " + interactiveItem.getId() + " in " + hand + " hand");

        ItemConfiguration config = interactiveItem.getConfiguration();
        if (!ItemRegionHandler.canPlayerUseItemInCurrentRegion(player, config)) {
            DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " cannot use item " + interactiveItem.getId() + " in current region");
            event.setCancelled(true);
            interactionHandler.handleRegionDeniedMessage(player);
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " passed region check for item: " + interactiveItem.getId());

        String effectiveId = interactiveItem.getEffectiveId();
        if (config.hasCooldown() && !canPlayerUseItem(player, effectiveId)) {
            DebugUtils.logInternalDebug(debug(), "Item " + effectiveId + " is on cooldown for player: " + player.getName());
            event.setCancelled(true);
            double remainingSeconds = getRemainingCooldown(player, effectiveId);
            DebugUtils.logInternalDebug(debug(), "Remaining cooldown: " + remainingSeconds + " seconds for projectile launch");
            interactionHandler.handleCooldownMessage(player, remainingSeconds);
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Item " + effectiveId + " passed cooldown check for projectile launch");

        if (!interactiveItem.hasUsesRemaining()) {
            DebugUtils.logInternalDebug(debug(), "Item " + interactiveItem.getId() + " has no uses remaining, cancelling projectile launch");
            event.setCancelled(true);
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Item " + interactiveItem.getId() + " has uses remaining for projectile launch");

        TriggerType triggerType = config.getTriggerType();

        if (triggerType == TriggerType.ON_PROJECTILE_LAUNCH || triggerType == TriggerType.ON_PROJECTILE_HIT) {
            DebugUtils.logInternalDebug(debug(), "Processing projectile trigger type: " + triggerType + " for item: " + interactiveItem.getId());

            ItemClickInfo clickInfo = new ItemClickInfo(player,
                    org.bukkit.event.inventory.ClickType.RIGHT,
                    hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                    hand == EquipmentSlot.HAND ? mainHand : offHand,
                    ActionSource.ITEM_USE);

            interactionHandler.processProjectileLaunch(player, hand == EquipmentSlot.HAND ? mainHand : offHand,
                    interactiveItem, clickInfo, hand);

            if (triggerType == TriggerType.ON_PROJECTILE_HIT) {
                DebugUtils.logInternalDebug(debug(), "Setting metadata for ON_PROJECTILE_HIT item: " + interactiveItem.getId());
                projectile.setMetadata("interactive_item_id", new org.bukkit.metadata.FixedMetadataValue(plugin, interactiveItem.getId()));
                projectile.setMetadata("interactive_item_effective_id", new org.bukkit.metadata.FixedMetadataValue(plugin, effectiveId));
                projectile.setMetadata("interactive_item_hand", new org.bukkit.metadata.FixedMetadataValue(plugin, hand.name()));
                projectile.setMetadata("interactive_item_shooter", new org.bukkit.metadata.FixedMetadataValue(plugin, player.getUniqueId().toString()));
                ItemStack itemClone = hand == EquipmentSlot.HAND ? mainHand.clone() : offHand.clone();
                projectile.setMetadata("interactive_item_stack", new org.bukkit.metadata.FixedMetadataValue(plugin, itemClone));
                projectile.setMetadata("interactive_item_object", new org.bukkit.metadata.FixedMetadataValue(plugin, interactiveItem.clone()));
                DebugUtils.logInternalDebug(debug(), "Metadata set for projectile hit detection, item: " + interactiveItem.getId());
            }

            DebugUtils.logInternalDebug(debug(), "Completed ProjectileLaunch processing for item: " + interactiveItem.getId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!projectile.hasMetadata("interactive_item_id")) {
            return;
        }

        DebugUtils.logInternalDebug(debug(), "ProjectileHit event triggered for projectile: " + projectile.getType());

        String itemId = projectile.getMetadata("interactive_item_id").get(0).asString();
        String effectiveId = projectile.getMetadata("interactive_item_effective_id").get(0).asString();
        String handName = projectile.getMetadata("interactive_item_hand").get(0).asString();
        String shooterUUID = projectile.getMetadata("interactive_item_shooter").get(0).asString();

        DebugUtils.logInternalDebug(debug(), "Projectile hit metadata - ItemId: " + itemId + ", EffectiveId: " + effectiveId +
                ", Hand: " + handName + ", Shooter: " + shooterUUID);

        ItemStack originalItemStack = null;
        InteractiveItem originalInteractiveItem = null;

        if (projectile.hasMetadata("interactive_item_stack")) {
            originalItemStack = (ItemStack) projectile.getMetadata("interactive_item_stack").get(0).value();
            DebugUtils.logInternalDebug(debug(), "Retrieved original item stack from projectile metadata");
        }

        if (projectile.hasMetadata("interactive_item_object")) {
            originalInteractiveItem = (InteractiveItem) projectile.getMetadata("interactive_item_object").get(0).value();
            DebugUtils.logInternalDebug(debug(), "Retrieved original interactive item from projectile metadata");
        }

        Player shooter;
        try {
            shooter = Bukkit.getPlayer(java.util.UUID.fromString(shooterUUID));
        } catch (Exception e) {
            DebugUtils.logInternalDebug(debug(), "Failed to parse shooter UUID: " + shooterUUID + ", error: " + e.getMessage());
            return;
        }

        if (shooter == null || !shooter.isOnline()) {
            DebugUtils.logInternalDebug(debug(), "Shooter is null or offline for projectile hit, UUID: " + shooterUUID);
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Shooter found and online: " + shooter.getName());

        InteractiveItem interactiveItem = originalInteractiveItem;
        if (interactiveItem == null) {
            DebugUtils.logInternalDebug(debug(), "No original interactive item in metadata, searching in shooter's hands");
            EquipmentSlot hand = EquipmentSlot.valueOf(handName);
            ItemStack itemStack = hand == EquipmentSlot.HAND ?
                    shooter.getInventory().getItemInMainHand() :
                    shooter.getInventory().getItemInOffHand();

            interactiveItem = getItemFromStack(itemStack);
            if (interactiveItem == null || !interactiveItem.getId().equals(itemId)) {
                DebugUtils.logInternalDebug(debug(), "Item in hand changed since launch, searching inventory for item: " + itemId);
                // El item cambió desde el lanzamiento, intentar buscar en inventario
                interactiveItem = findInteractiveItemInInventory(shooter, itemId);
                if (interactiveItem == null) {
                    DebugUtils.logInternalDebug(debug(), "Item not found in inventory, creating fallback item for: " + itemId);
                    // FALLBACK: Crear un item temporal con la configuración
                    ItemConfiguration config = getItemConfiguration(itemId);
                    if (config != null) {
                        interactiveItem = new InteractiveItem(itemId, config, shooter);
                        // Si tenemos el itemStack original, usar sus datos NBT
                        if (originalItemStack != null) {
                            InteractiveItem tempFromStack = InteractiveItem.fromItemStack(originalItemStack);
                            if (tempFromStack != null) {
                                interactiveItem = tempFromStack;
                                DebugUtils.logInternalDebug(debug(), "Created fallback item from original stack NBT data");
                            }
                        }
                        DebugUtils.logInternalDebug(debug(), "Created fallback interactive item: " + itemId);
                    } else {
                        DebugUtils.logInternalDebug(debug(), "No configuration found for item: " + itemId + ", cannot process projectile hit");
                        return;
                    }
                } else {
                    DebugUtils.logInternalDebug(debug(), "Found item in inventory: " + interactiveItem.getId());
                }
            } else {
                DebugUtils.logInternalDebug(debug(), "Item still in shooter's hand: " + interactiveItem.getId());
            }
        } else {
            DebugUtils.logInternalDebug(debug(), "Using original interactive item from metadata: " + originalInteractiveItem.getId());
        }

        if (interactiveItem.getConfiguration().getTriggerType() != TriggerType.ON_PROJECTILE_HIT) {
            DebugUtils.logInternalDebug(debug(), "Item " + interactiveItem.getId() + " is not ON_PROJECTILE_HIT type, ignoring hit");
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Processing ON_PROJECTILE_HIT for item: " + interactiveItem.getId());

        Player hitPlayer = event.getHitEntity() instanceof Player ? (Player) event.getHitEntity() : null;
        if (hitPlayer != null) {
            DebugUtils.logInternalDebug(debug(), "Projectile hit player: " + hitPlayer.getName());
        } else {
            DebugUtils.logInternalDebug(debug(), "Projectile hit non-player entity or block");
        }

        ItemStack itemStackForClick = originalItemStack != null ? originalItemStack :
                (EquipmentSlot.valueOf(handName) == EquipmentSlot.HAND ?
                        shooter.getInventory().getItemInMainHand() :
                        shooter.getInventory().getItemInOffHand());

        ItemClickInfo clickInfo = new ItemClickInfo(shooter,
                org.bukkit.event.inventory.ClickType.RIGHT,
                EquipmentSlot.valueOf(handName) == EquipmentSlot.HAND ? shooter.getInventory().getHeldItemSlot() : 40,
                itemStackForClick,
                ActionSource.ITEM_USE);

        if (hitPlayer != null) {
            clickInfo.withData("hitPlayer", hitPlayer);
        }
        clickInfo.withData("effectiveId", effectiveId);

        DebugUtils.logInternalDebug(debug(), "Processing projectile hit interaction for item: " + interactiveItem.getId() +
                ", shooter: " + shooter.getName() + (hitPlayer != null ? ", hit player: " + hitPlayer.getName() : ""));
        interactionHandler.processProjectileHit(shooter, hitPlayer, itemStackForClick,
                interactiveItem, clickInfo, EquipmentSlot.valueOf(handName));
        DebugUtils.logInternalDebug(debug(), "Completed ProjectileHit processing for item: " + interactiveItem.getId());
    }

    private InteractiveItem findInteractiveItemInInventory(Player player, String itemId) {
        DebugUtils.logInternalDebug(debug(), "Searching inventory for interactive item: " + itemId + " for player: " + player.getName());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                InteractiveItem interactiveItem = getItemFromStack(item);
                if (interactiveItem != null && interactiveItem.getId().equals(itemId)) {
                    DebugUtils.logInternalDebug(debug(), "Found interactive item " + itemId + " in inventory for player: " + player.getName());
                    return interactiveItem;
                }
            }
        }
        DebugUtils.logInternalDebug(debug(), "Interactive item " + itemId + " not found in inventory for player: " + player.getName());
        return null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            DebugUtils.logInternalDebug(debug(), "InventoryDrag event not triggered by player, ignoring");
            return;
        }

        DebugUtils.logInternalDebug(debug(), "InventoryDrag event triggered for player: " + player.getName());

        ItemStack draggedItem = event.getOldCursor();
        InteractiveItem interactiveItem = getItemFromStack(draggedItem);
        if (interactiveItem == null) {
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Found interactive item: " + interactiveItem.getId() + " being dragged by player: " + player.getName());

        ItemConfiguration config = interactiveItem.getConfiguration();

        if (player.getGameMode() == GameMode.CREATIVE) {
            DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " is in creative mode, allowing drag for item: " + interactiveItem.getId());
            return;
        }

        if (!config.isAllowMovement()) {
            DebugUtils.logInternalDebug(debug(), "Item " + interactiveItem.getId() + " does not allow movement, cancelling drag");
            event.setCancelled(true);
            return;
        }

        boolean involvesPlayerInventory = event.getRawSlots().stream()
                .anyMatch(slot -> slot >= event.getView().getTopInventory().getSize());

        DebugUtils.logInternalDebug(debug(), "Drag involves player inventory: " + involvesPlayerInventory + " for item: " + interactiveItem.getId());

        if (involvesPlayerInventory && !config.isAllowMovement()) {
            DebugUtils.logInternalDebug(debug(), "Cancelling drag involving player inventory for item: " + interactiveItem.getId());
            event.setCancelled(true);
        }

        DebugUtils.logInternalDebug(debug(), "Completed InventoryDrag processing for item: " + interactiveItem.getId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        DebugUtils.logInternalDebug(debug(), "PlayerDropItem event triggered for player: " + player.getName());
        DebugUtils.logInternalDebug(debug(), "Dropped item: " + droppedItem.getType() + " by player: " + player.getName());

        InteractiveItem interactiveItem = getItemFromStack(droppedItem);
        if (interactiveItem == null) {
            return;
        }

        DebugUtils.logInternalDebug(debug(), "Found interactive item: " + interactiveItem.getId() + " being dropped by player: " + player.getName());

        ItemConfiguration config = interactiveItem.getConfiguration();

        if (player.getGameMode() == GameMode.CREATIVE) {
            DebugUtils.logInternalDebug(debug(), "Player " + player.getName() + " is in creative mode, allowing drop for item: " + interactiveItem.getId());
            return;
        }

        if (!config.isAllowDrop()) {
            DebugUtils.logInternalDebug(debug(), "Item " + interactiveItem.getId() + " does not allow dropping, cancelling drop by player: " + player.getName());
            event.setCancelled(true);
        } else {
            DebugUtils.logInternalDebug(debug(), "Item " + interactiveItem.getId() + " allows dropping, drop successful for player: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();

        DebugUtils.logInternalDebug(debug(), "PlayerSwapHandItems event triggered for player: " + player.getName());
        DebugUtils.logInternalDebug(debug(), "Swapping - MainHand: " + (mainHand != null ? mainHand.getType() : "null") +
                ", OffHand: " + (offHand != null ? offHand.getType() : "null"));

        if (mainHand != null) {
            InteractiveItem mainInteractiveItem = getItemFromStack(mainHand);
            if (mainInteractiveItem != null) {
                DebugUtils.logInternalDebug(debug(), "Found interactive item in main hand: " + mainInteractiveItem.getId());
                ItemConfiguration config = mainInteractiveItem.getConfiguration();
                if (player.getGameMode() != GameMode.CREATIVE && !config.isAllowSwapToOffhand()) {
                    DebugUtils.logInternalDebug(debug(), "Item " + mainInteractiveItem.getId() + " does not allow swap to offhand, cancelling swap");
                    event.setCancelled(true);
                    return;
                } else {
                    DebugUtils.logInternalDebug(debug(), "Item " + mainInteractiveItem.getId() + " allows swap to offhand or player is in creative");
                }
            }
        }

        if (offHand != null) {
            InteractiveItem offInteractiveItem = getItemFromStack(offHand);
            if (offInteractiveItem != null) {
                DebugUtils.logInternalDebug(debug(), "Found interactive item in off hand: " + offInteractiveItem.getId());
                ItemConfiguration config = offInteractiveItem.getConfiguration();
                if (player.getGameMode() != GameMode.CREATIVE && !config.isAllowSwapToOffhand()) {
                    DebugUtils.logInternalDebug(debug(), "Item " + offInteractiveItem.getId() + " does not allow swap to offhand, cancelling swap");
                    event.setCancelled(true);
                } else {
                    DebugUtils.logInternalDebug(debug(), "Item " + offInteractiveItem.getId() + " allows swap to offhand or player is in creative");
                }
            }
        }

        DebugUtils.logInternalDebug(debug(), "Completed PlayerSwapHandItems processing for player: " + player.getName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemStack = event.getItemInHand();
        Player player = event.getPlayer();

        DebugUtils.logInternalDebug(debug(), "BlockPlace event triggered for player: " + player.getName());
        DebugUtils.logInternalDebug(debug(), "Placing with item: " + (itemStack != null ? itemStack.getType() : "null"));

        InteractiveItem interactiveItem = getItemFromStack(itemStack);
        if (interactiveItem != null) {
            DebugUtils.logInternalDebug(debug(), "Found interactive item: " + interactiveItem.getId() + " in block place event, cancelling placement");
            event.setCancelled(true);
        } else {
            DebugUtils.logInternalDebug(debug(), "No interactive item found, allowing block placement");
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

    private static void startClickTimeCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            lastClickTime.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > DOUBLE_CLICK_PREVENTION_MS);
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
}
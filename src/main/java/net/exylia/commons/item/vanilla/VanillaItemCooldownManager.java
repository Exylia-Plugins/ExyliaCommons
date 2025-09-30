package net.exylia.commons.item.vanilla;

import net.exylia.commons.config.base.MessagesBase;
import net.exylia.commons.item.ItemManager;
import net.exylia.commons.item.cooldown.CooldownManager;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.TimeFormatter;
import net.exylia.commons.utils.visuals.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registrador de cooldowns para items vanilla
 * Soporte para diferentes triggers según el tipo de item
 * ACTUALIZADO: Soporte para display-name
 */
public class VanillaItemCooldownManager implements Listener {

    private static VanillaItemCooldownManager instance;
    private static JavaPlugin plugin;
    private static boolean initialized = false;

    // Configuraciones de cooldown por material
    private final Map<Material, VanillaItemConfig> itemConfigs = new ConcurrentHashMap<>();

    // Control de doble clic
    private final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();
    private static final long DOUBLE_CLICK_PREVENTION_MS = 150;

    // Control de consumo múltiple - tracking de jugadores que acaban de consumir
    private final Map<UUID, Set<Material>> recentlyConsumed = new ConcurrentHashMap<>();
    private static final long CONSUME_PROTECTION_MS = 1000; // 1 segundo de protección

    private VanillaItemCooldownManager() {}

    /**
     * Inicializa el sistema de cooldowns para items vanilla
     */
    public static void initialize(JavaPlugin javaPlugin) {
        if (initialized) return;

        plugin = javaPlugin;
        instance = new VanillaItemCooldownManager();

        VanillaRegionLimitManager.initialize(plugin);

        Bukkit.getPluginManager().registerEvents(instance, plugin);

        instance.startCleanupTask();

        initialized = true;
        DebugUtils.logInternalInfo("VanillaItemCooldownManager initialized");
    }

    public static VanillaItemCooldownManager getInstance() {
        if (!initialized) {
            throw new IllegalStateException("VanillaItemCooldownManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    // ===== MÉTODOS DE CONFIGURACIÓN =====

    /**
     * Registra un cooldown para un material específico
     */
    public void registerCooldown(Material material, double cooldownSeconds) {
        registerCooldown(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT);
    }

    /**
     * Registra un cooldown con tipo de trigger específico
     */
    public void registerCooldown(Material material, double cooldownSeconds, VanillaTriggerType triggerType) {
        registerCooldown(material, cooldownSeconds, triggerType, null);
    }

    /**
     * NUEVO: Registra un cooldown con display name personalizado
     */
    public void registerCooldown(Material material, double cooldownSeconds, String displayName) {
        registerCooldown(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, displayName);
    }

    /**
     * NUEVO: Registra un cooldown con tipo de trigger y display name
     */
    public void registerCooldown(Material material, double cooldownSeconds, VanillaTriggerType triggerType, String displayName) {
        registerCooldown(material, cooldownSeconds, triggerType, displayName, null);
    }

    public void registerCooldown(Material material, double cooldownSeconds, VanillaTriggerType triggerType, String displayName, Integer maxUsesPerRegion) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }

        VanillaItemConfig config = new VanillaItemConfig(material, cooldownSeconds, triggerType, displayName, maxUsesPerRegion);
        itemConfigs.put(material, config);

        if (config.hasRegionLimit()) {
            VanillaRegionLimitManager.getInstance().registerRegionLimit(material, maxUsesPerRegion);
        }

        DebugUtils.logInternalDebug("Registered vanilla cooldown: " + material + " -> " + cooldownSeconds + "s (" + triggerType + ")" +
                (displayName != null ? " with display name: '" + displayName + "'" : "") +
                (maxUsesPerRegion != null ? " with region limit: " + maxUsesPerRegion : ""));
    }

    public void registerConfig(VanillaItemConfig config) {
        if (config == null || config.getMaterial() == null) {
            throw new IllegalArgumentException("Config and material cannot be null");
        }

        itemConfigs.put(config.getMaterial(), config);

        if (config.hasRegionLimit()) {
            VanillaRegionLimitManager.getInstance().registerRegionLimit(config.getMaterial(), config.getMaxUsesPerRegion());
        }

        for (String regionName : config.getRegionConfigs().keySet()) {
            VanillaRegionConfig regionConfig = config.getRegionConfigs().get(regionName);
            if (regionConfig.hasMaxUses()) {
                VanillaRegionLimitManager.getInstance().registerRegionLimit(config.getMaterial(), regionConfig.getMaxUses());
            }
        }

        DebugUtils.logInternalDebug("Registered vanilla config: " + config.getMaterial() + " with " + 
                config.getRegionConfigs().size() + " region overrides");
    }

    /**
     * Registra cooldowns para múltiples materiales
     */
    public void registerCooldowns(Map<Material, Double> cooldowns) {
        cooldowns.forEach(this::registerCooldown);
    }

    /**
     * Registra cooldowns para múltiples materiales con el mismo valor
     */
    public void registerCooldowns(List<Material> materials, double cooldownSeconds) {
        materials.forEach(material -> registerCooldown(material, cooldownSeconds));
    }

    /**
     * Registra cooldowns para múltiples materiales con trigger específico
     */
    public void registerCooldowns(List<Material> materials, double cooldownSeconds, VanillaTriggerType triggerType) {
        materials.forEach(material -> registerCooldown(material, cooldownSeconds, triggerType));
    }

    /**
     * NUEVO: Registra cooldowns para múltiples materiales con display name
     */
    public void registerCooldowns(List<Material> materials, double cooldownSeconds, VanillaTriggerType triggerType, String displayName) {
        materials.forEach(material -> registerCooldown(material, cooldownSeconds, triggerType, displayName));
    }

    /**
     * Remueve el cooldown de un material
     */
    public void unregisterCooldown(Material material) {
        itemConfigs.remove(material);
        DebugUtils.logInternalDebug("Unregistered vanilla cooldown: " + material);
    }

    /**
     * Limpia todas las configuraciones
     */
    public void clearAllCooldowns() {
        itemConfigs.clear();
        DebugUtils.logInternalInfo("Cleared all vanilla item cooldowns");
    }

    /**
     * Verifica si un material tiene cooldown configurado
     */
    public boolean hasCooldownConfig(Material material) {
        return itemConfigs.containsKey(material);
    }

    /**
     * Obtiene la configuración de cooldown de un material
     */
    public VanillaItemConfig getCooldownConfig(Material material) {
        return itemConfigs.get(material);
    }

    // ===== MÉTODOS DE COOLDOWN =====

    /**
     * Verifica si un jugador puede usar un item
     */
    public boolean canPlayerUseItem(Player player, Material material) {
        if (!hasCooldownConfig(material)) return true;
        if (player.getGameMode() == GameMode.CREATIVE) return true;

        VanillaItemConfig config = getCooldownConfig(material);
        String currentWorld = player.getWorld().getName();
        String currentRegion = VanillaRegionLimitManager.getInstance().getCurrentRegion(player);
        
        // Verificar restricciones por mundo primero
        if (config.hasWorldConfigs()) {
            if (config.isBlockedInWorld(currentWorld)) {
                return false;
            }
            
            int maxUsesInWorld = config.getMaxUsesForWorld(currentWorld);
            if (maxUsesInWorld > 0) {
                // TODO: Implementar sistema de tracking de usos por mundo similar al de regiones
                // Por ahora, permitir el uso pero esto debería ser implementado
            }
        } else if (config.hasWorldLimit()) {
            // TODO: Implementar verificación de límite mundial
        }
        
        // Verificar restricciones por región
        if (config.hasRegionConfigs() && currentRegion != null) {
            if (config.isBlockedInRegion(currentRegion)) {
                return false;
            }
            
            int maxUses = config.getMaxUsesForRegion(currentRegion);
            if (maxUses > 0) {
                int currentUsage = VanillaRegionLimitManager.getInstance().getCurrentUsage(player, currentRegion, material);
                if (currentUsage >= maxUses) {
                    return false;
                }
            }
        } else if (config.hasRegionLimit()) {
            if (!VanillaRegionLimitManager.getInstance().canPlayerUseInRegion(player, material)) {
                return false;
            }
        }

        String itemId = getItemId(material);
        return !CooldownManager.getInstance().hasCooldown(player, itemId);
    }

    /**
     * Verifica si un jugador acaba de consumir este item (protección anti-spam)
     */
    private boolean hasRecentlyConsumed(Player player, Material material) {
        Set<Material> playerConsumed = recentlyConsumed.get(player.getUniqueId());
        return playerConsumed != null && playerConsumed.contains(material);
    }

    /**
     * Marca un item como recientemente consumido
     */
    private void markAsRecentlyConsumed(Player player, Material material) {
        recentlyConsumed.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(material);

        // Remover después del tiempo de protección
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Set<Material> playerConsumed = recentlyConsumed.get(player.getUniqueId());
            if (playerConsumed != null) {
                playerConsumed.remove(material);
                if (playerConsumed.isEmpty()) {
                    recentlyConsumed.remove(player.getUniqueId());
                }
            }
        }, CONSUME_PROTECTION_MS / 50); // Convertir ms a ticks
    }

    /**
     * Establece un cooldown para un jugador
     */
    public void setCooldown(Player player, Material material) {
        VanillaItemConfig config = getCooldownConfig(material);
        if (config == null) return;

        String currentWorld = player.getWorld().getName();
        String currentRegion = VanillaRegionLimitManager.getInstance().getCurrentRegion(player);
        
        // Registrar uso en región si aplica
        if (config.hasRegionConfigs() && currentRegion != null) {
            int maxUses = config.getMaxUsesForRegion(currentRegion);
            if (maxUses > 0) {
                VanillaRegionLimitManager.getInstance().recordUsage(player, material);
            }
        } else if (config.hasRegionLimit()) {
            VanillaRegionLimitManager.getInstance().recordUsage(player, material);
        }

        // Calcular cooldown con prioridad: Región específica > Región __global__ > Mundo > Base
        double baseCooldown = config.getCooldownSeconds();
        double finalCooldown = baseCooldown;
        
        DebugUtils.logInternalDebug("=== Vanilla Cooldown Debug ===");
        DebugUtils.logInternalDebug("Material: " + material + ", Player: " + player.getName());
        DebugUtils.logInternalDebug("World: " + currentWorld + ", Region: " + currentRegion);
        DebugUtils.logInternalDebug("Base cooldown: " + baseCooldown);
        DebugUtils.logInternalDebug("Has world configs: " + config.hasWorldConfigs());
        DebugUtils.logInternalDebug("Has region configs: " + config.hasRegionConfigs());
        
        // Verificar cooldown específico de mundo (si no hay región específica)
        boolean hasSpecificWorldConfig = config.hasWorldConfigs() && config.getWorldConfigs().containsKey(currentWorld);
        if (hasSpecificWorldConfig) {
            finalCooldown = config.getCooldownForWorld(currentWorld);
            DebugUtils.logInternalDebug("Using world-specific cooldown for " + currentWorld + ": " + finalCooldown);
        } else {
            DebugUtils.logInternalDebug("No specific world config for " + currentWorld + ", using base: " + finalCooldown);
        }
        
        // Verificar cooldown específico de región (OVERRIDE mundo y base)
        boolean hasSpecificRegionConfig = config.hasRegionConfigs() && currentRegion != null && config.getRegionConfigs().containsKey(currentRegion);
        if (hasSpecificRegionConfig) {
            finalCooldown = config.getCooldownForRegion(currentRegion);
            DebugUtils.logInternalDebug("Using region-specific cooldown for " + currentRegion + ": " + finalCooldown + " (overrides world/base)");
        } else if (currentRegion == null && config.hasRegionConfigs() && config.getRegionConfigs().containsKey("__global__")) {
            // Jugador no está en ninguna región pero hay configuración __global__
            finalCooldown = config.getCooldownForRegion("__global__");
            DebugUtils.logInternalDebug("Player not in any region, using __global__ region cooldown: " + finalCooldown + " (overrides world/base)");
        } else if (currentRegion != null) {
            DebugUtils.logInternalDebug("No specific region config for " + currentRegion + ", keeping: " + finalCooldown);
        } else {
            DebugUtils.logInternalDebug("Player not in any region and no __global__ config, keeping: " + finalCooldown);
        }
        
        DebugUtils.logInternalDebug("Final cooldown: " + finalCooldown);
        DebugUtils.logInternalDebug("=== End Debug ===");

        final double cooldownSeconds = finalCooldown;
        if (cooldownSeconds > 0) {
            String itemId = getItemId(material);
            CooldownManager.getInstance().setCooldown(player, itemId, cooldownSeconds);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setCooldown(material, (int) (cooldownSeconds * 20));
            }, 1L);
        }
    }

    /**
     * Obtiene el cooldown restante de un jugador
     */
    public double getRemainingCooldown(Player player, Material material) {
        if (!hasCooldownConfig(material)) return 0.0;

        String itemId = getItemId(material);
        return CooldownManager.getInstance().getRemainingCooldown(player, itemId);
    }

    /**
     * Remueve el cooldown de un jugador
     */
    public void removeCooldown(Player player, Material material) {
        String itemId = getItemId(material);
        CooldownManager.getInstance().removeCooldown(player, itemId);
        player.setCooldown(material, 0);
    }

    // ===== EVENT HANDLERS =====

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        ItemStack item = event.getItem();

        if (item == null || item.getType().isAir()) return;

        // NUEVO: Verificar si es un item interactivo con force-id
        if (isInteractiveItemWithForceId(item)) {
            // Si es un item interactivo con force-id, no aplicar cooldown vanilla
            // El sistema de InteractiveItem se encargará del cooldown
            return;
        }

        Material material = item.getType();
        VanillaItemConfig config = getCooldownConfig(material);
        if (config == null) return;

        // Control de doble clic
        if (!canPlayerClick(player.getUniqueId())) {
            return;
        }

        VanillaTriggerType triggerType = config.getTriggerType();

        // Resto del código sin cambios...
        if (triggerType == VanillaTriggerType.INTERACT ||
                (triggerType == VanillaTriggerType.AUTO_DETECT && isInteractTrigger(material))) {

            if (!canPlayerUseItem(player, material)) {
                event.setCancelled(true);
                handleCooldownMessage(player, material);
                return;
            }

            setCooldown(player, material);
        }
        else if (triggerType == VanillaTriggerType.AFTER_CONSUME ||
                (triggerType == VanillaTriggerType.AUTO_DETECT && isConsumeTrigger(material))) {

            if (!canPlayerUseItem(player, material)) {
                event.setCancelled(true);
                handleCooldownMessage(player, material);
                return;
            }

            if (hasRecentlyConsumed(player, material)) {
                event.setCancelled(true);
                return;
            }
        }
        else if (triggerType == VanillaTriggerType.AFTER_PROJECTILE ||
                (triggerType == VanillaTriggerType.AUTO_DETECT && isProjectileTrigger(material))) {

            if (!canPlayerUseItem(player, material)) {
                event.setCancelled(true);
                handleCooldownMessage(player, material);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        // Detectar el item que lanzó el proyectil
        Material material = getProjectileSourceMaterial(player, projectile);
        if (material == null) return;

        // NUEVO: Verificar si el item en mano es un InteractiveItem con force-id
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if ((mainHand.getType() == material && isInteractiveItemWithForceId(mainHand)) ||
                (offHand.getType() == material && isInteractiveItemWithForceId(offHand))) {
            // Si es un item interactivo con force-id, no aplicar cooldown vanilla
            // El sistema de InteractiveItem ya se encargó del cooldown
            return;
        }

        VanillaItemConfig config = getCooldownConfig(material);
        if (config == null) return;

        VanillaTriggerType triggerType = config.getTriggerType();

        // Aplicar cooldown después de lanzar proyectil
        if (triggerType == VanillaTriggerType.AFTER_PROJECTILE ||
                (triggerType == VanillaTriggerType.AUTO_DETECT && isProjectileTrigger(material))) {

            setCooldown(player, material);
        }
    }

    private boolean isInteractiveItemWithForceId(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return false;

        try {
            // Verificar si es un item interactivo
            Object interactiveItem = ItemManager.getItemFromStack(itemStack);
            if (interactiveItem == null) return false;

            // Usar reflexión para verificar si tiene force-id
            java.lang.reflect.Method hasForceIdMethod = interactiveItem.getClass().getMethod("hasForceId");
            boolean hasForceId = (Boolean) hasForceIdMethod.invoke(interactiveItem);

            if (hasForceId) {
                java.lang.reflect.Method getForceIdMethod = interactiveItem.getClass().getMethod("getForceId");
                String forceId = (String) getForceIdMethod.invoke(interactiveItem);

                // Verificar si el force-id corresponde a un vanilla item que nosotros manejamos
                String expectedVanillaId = "vanilla_" + itemStack.getType().name().toLowerCase();
                return expectedVanillaId.equals(forceId);
            }

        } catch (Exception e) {
            // Si hay error en reflexión, asumir que no es InteractiveItem
            return false;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST) // Cambiar a LOWEST para verificar ANTES del consumo
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        ItemStack item = event.getItem();
        Material material = item.getType();

        VanillaItemConfig config = getCooldownConfig(material);
        if (config == null) return;

        VanillaTriggerType triggerType = config.getTriggerType();

        // Verificar si este item tiene cooldown de tipo AFTER_CONSUME
        if (triggerType == VanillaTriggerType.AFTER_CONSUME ||
                (triggerType == VanillaTriggerType.AUTO_DETECT && isConsumeTrigger(material))) {

            // Verificar cooldown antes de permitir el consumo
            if (!canPlayerUseItem(player, material)) {
                event.setCancelled(true);
                handleCooldownMessage(player, material);
                return;
            }

            // Verificar protección anti-spam
            if (hasRecentlyConsumed(player, material)) {
                event.setCancelled(true);
                return;
            }

            // Si el consumo es permitido, marcar como recientemente consumido
            // y aplicar el cooldown inmediatamente
            markAsRecentlyConsumed(player, material);
            setCooldown(player, material);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack bow = event.getBow();
        if (bow == null) return;

        Material material = bow.getType();
        VanillaItemConfig config = getCooldownConfig(material);
        if (config == null) return;

        VanillaTriggerType triggerType = config.getTriggerType();

        if (triggerType == VanillaTriggerType.AFTER_PROJECTILE ||
                (triggerType == VanillaTriggerType.AUTO_DETECT && isProjectileTrigger(material))) {

            setCooldown(player, material);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Material material = Material.TOTEM_OF_UNDYING;
        VanillaItemConfig config = getCooldownConfig(material);
        if (config == null) return;

        if (!canPlayerUseItem(player, material)) {
            event.setCancelled(true);
            handleCooldownMessage(player, material);
            return;
        }

        setCooldown(player, material);
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Detecta automáticamente el tipo de trigger basado en el material
     */
    private boolean isInteractTrigger(Material material) {
        return material == Material.SHIELD ||
                material == Material.FIREWORK_ROCKET ||
                material.name().contains("POTION") ||
                material.name().contains("BUCKET");
    }

    private boolean isConsumeTrigger(Material material) {
        return material.isEdible() ||
                material == Material.POTION ||
                material == Material.MILK_BUCKET;
    }

    private boolean isTotemTrigger(Material material) {
        return material == Material.TOTEM_OF_UNDYING;
    }

    private boolean isProjectileTrigger(Material material) {
        return material == Material.ENDER_PEARL ||
                material == Material.SNOWBALL ||
                material == Material.EGG ||
                material == Material.BOW ||
                material == Material.CROSSBOW ||
                material == Material.TRIDENT;
    }

    /**
     * Detecta el material que causó el lanzamiento del proyectil
     */
    private Material getProjectileSourceMaterial(Player player, Projectile projectile) {
        switch (projectile.getType()) {
            case ENDER_PEARL:
                return Material.ENDER_PEARL;
            case SNOWBALL:
                return Material.SNOWBALL;
            case EGG:
                return Material.EGG;
            case TRIDENT:
                return Material.TRIDENT;
            case ARROW:
            case SPECTRAL_ARROW:
                // Verificar si tiene arco o ballesta en mano
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                ItemStack offHand = player.getInventory().getItemInOffHand();

                if (mainHand.getType() == Material.BOW || mainHand.getType() == Material.CROSSBOW) {
                    return mainHand.getType();
                }
                if (offHand.getType() == Material.BOW || offHand.getType() == Material.CROSSBOW) {
                    return offHand.getType();
                }
                break;
        }
        return null;
    }

    /**
     * Genera un ID único para el item vanilla
     */
    private String getItemId(Material material) {
        return "vanilla_" + material.name().toLowerCase();
    }

    /**
     * Control de doble clic
     */
    private boolean canPlayerClick(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastClickTime.get(playerId);

        if (lastTime != null && (currentTime - lastTime) < DOUBLE_CLICK_PREVENTION_MS) {
            return false;
        }

        lastClickTime.put(playerId, currentTime);
        return true;
    }

    /**
     * Tarea de limpieza de clicks y consumos
     */
    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();

            // Limpiar clicks antiguos
            lastClickTime.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > DOUBLE_CLICK_PREVENTION_MS);

            // Limpiar jugadores offline de recentlyConsumed
            recentlyConsumed.entrySet().removeIf(entry -> {
                Player player = Bukkit.getPlayer(entry.getKey());
                return player == null || !player.isOnline();
            });
        }, 20L * 60, 20L * 60);
    }

    /**
     * Maneja el mensaje de cooldown
     * ACTUALIZADO: Usa display name efectivo
     */
    private void handleCooldownMessage(Player player, Material material) {
        VanillaItemConfig config = getCooldownConfig(material);
        String itemDisplayName = config != null ? config.getEffectiveDisplayName() : getItemDisplayName(material);
        String itemName = getItemName(material);

        String currentRegion = VanillaRegionLimitManager.getInstance().getCurrentRegion(player);

        if (config != null && config.hasRegionConfigs() && currentRegion != null) {
            if (config.isBlockedInRegion(currentRegion)) {
                MessageUtils.sendMessage(player,
                        MessagesBase.getWithContext("system.items.vanilla_region_blocked", ExyliaContext.create()
                                .put("item_display", itemDisplayName)
                                .put("item_name", itemName)
                                .put("item", itemDisplayName)
                                .put("region", currentRegion)));
                return;
            }

            int maxUses = config.getMaxUsesForRegion(currentRegion);
            if (maxUses > 0) {
                int currentUsage = VanillaRegionLimitManager.getInstance().getCurrentUsage(player, currentRegion, material);
                if (currentUsage >= maxUses) {
                    int remaining = maxUses - currentUsage;
                    MessageUtils.sendMessage(player,
                            MessagesBase.getWithContext("system.items.vanilla_region_limit", ExyliaContext.create()
                                    .put("item_display", itemDisplayName)
                                    .put("item_name", itemName)
                                    .put("item", itemDisplayName)
                                    .put("remaining", remaining)
                                    .put("region", currentRegion)));
                    return;
                }
            }
        } else if (config != null && config.hasRegionLimit()) {
            if (!VanillaRegionLimitManager.getInstance().canPlayerUseInRegion(player, material)) {
                int remaining = VanillaRegionLimitManager.getInstance().getRemainingUses(player, material);
                String region = VanillaRegionLimitManager.getInstance().getCurrentRegion(player);

                MessageUtils.sendMessage(player,
                        MessagesBase.getWithContext("system.items.vanilla_region_limit", ExyliaContext.create()
                                .put("item_display", itemDisplayName)
                                .put("item_name", itemName)
                                .put("item", itemDisplayName)
                                .put("remaining", remaining)
                                .put("region", region != null ? region : "unknown")));
                return;
            }
        }

        double remainingSeconds = getRemainingCooldown(player, material);
        String formattedTime = TimeFormatter.timeFormatter.format(remainingSeconds);

        MessageUtils.sendMessage(player,
                MessagesBase.getWithContext("system.items.vanilla_cooldown", ExyliaContext.create()
                                .put("item_display", itemDisplayName)
                                .put("item_name", itemName)
                                .put("item", itemDisplayName)
                                .put("cooldown_formatted", formattedTime)
                                .put("cooldown_seconds", String.valueOf(remainingSeconds))));
    }

    /**
     * Obtiene el nombre display del material (fallback)
     */
    private String getItemDisplayName(Material material) {
        return material.name().toLowerCase().replace("_", " ");
    }

    /**
     * Obtiene el nombre del material
     */
    private String getItemName(Material material) {
        return material.name().toLowerCase();
    }

    // ===== MÉTODOS PÚBLICOS PARA CONFIGURACIÓN =====

    /**
     * Obtiene todas las configuraciones registradas
     */
    public Map<Material, VanillaItemConfig> getAllConfigs() {
        return new HashMap<>(itemConfigs);
    }

    /**
     * NUEVO: Obtiene el display name efectivo de un material
     */
    public String getEffectiveDisplayName(Material material) {
        VanillaItemConfig config = getCooldownConfig(material);
        return config != null ? config.getEffectiveDisplayName() : getItemDisplayName(material);
    }

    /**
     * NUEVO: Verifica si un material tiene display name personalizado
     */
    public boolean hasCustomDisplayName(Material material) {
        VanillaItemConfig config = getCooldownConfig(material);
        return config != null && config.hasDisplayName();
    }

    /**
     * Obtiene estadísticas del sistema
     */
    public String getStats() {
        long interactCount = itemConfigs.values().stream()
                .mapToLong(config -> config.getTriggerType() == VanillaTriggerType.INTERACT ? 1 : 0)
                .sum();

        long consumeCount = itemConfigs.values().stream()
                .mapToLong(config -> config.getTriggerType() == VanillaTriggerType.AFTER_CONSUME ? 1 : 0)
                .sum();

        long projectileCount = itemConfigs.values().stream()
                .mapToLong(config -> config.getTriggerType() == VanillaTriggerType.AFTER_PROJECTILE ? 1 : 0)
                .sum();

        long customDisplayCount = itemConfigs.values().stream()
                .mapToLong(config -> config.hasDisplayName() ? 1 : 0)
                .sum();

        return String.format("Vanilla Item Cooldowns - Total: %d, Interact: %d, Consume: %d, Projectile: %d, Custom Display Names: %d",
                itemConfigs.size(), interactCount, consumeCount, projectileCount, customDisplayCount);
    }
}
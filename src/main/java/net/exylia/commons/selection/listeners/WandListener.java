package net.exylia.commons.selection.listeners;

import net.exylia.commons.selection.SelectionManager;
import net.exylia.commons.selection.events.WandUseEvent;
import net.exylia.commons.selection.model.Selection;
import net.exylia.commons.selection.wand.WandFactory;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Listener para el manejo de wands
 */
public class WandListener implements Listener {
    private final JavaPlugin plugin;
    private final WandFactory wandFactory;
    private final SelectionManager selectionManager;

    public WandListener(JavaPlugin plugin, WandFactory wandFactory) {
        this.plugin = plugin;
        this.wandFactory = wandFactory;
        this.selectionManager = SelectionManager.getInstance();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Verificar si es una wand
        if (!wandFactory.isWand(item)) {
            return;
        }

        event.setCancelled(true);

        // Verificar permisos
        if (!player.hasPermission("exylia.selection.use")) {
            return;
        }

        // Obtener información de la wand
        String selectionId = wandFactory.getSelectionId(item);
        if (selectionId == null) {
            return;
        }

        // Determinar acción
        WandUseEvent.WandAction action = getWandAction(event.getAction(), player.isSneaking());
        if (action == null) {
            return;
        }

        // Obtener o crear selección
        Selection selection = getOrCreateSelection(player, selectionId);
        if (selection == null) {
            return;
        }

        // Obtener ubicación del bloque
        Location location = getTargetLocation(event, player);
        if (location == null) {
            return;
        }

        // Disparar evento personalizado
        WandUseEvent wandEvent = new WandUseEvent(player, item, location, action, selection);
        Bukkit.getPluginManager().callEvent(wandEvent);

        if (wandEvent.isCancelled()) {
            return;
        }

        // Procesar acción
        handleWandAction(player, selection, location, action, item);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpiar selecciones del jugador cuando se desconecta
        selectionManager.clearSelections(event.getPlayer());
    }

    // ===== MÉTODOS PRIVADOS =====

    private WandUseEvent.WandAction getWandAction(Action action, boolean sneaking) {
        return switch (action) {
            case LEFT_CLICK_BLOCK ->
                    sneaking ? WandUseEvent.WandAction.SHIFT_LEFT_CLICK : WandUseEvent.WandAction.LEFT_CLICK_BLOCK;
            case RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR ->
                    sneaking ? WandUseEvent.WandAction.SHIFT_RIGHT_CLICK : WandUseEvent.WandAction.RIGHT_CLICK_BLOCK;
            default -> null;
        };
    }

    private Selection getOrCreateSelection(Player player, String selectionId) {
        Optional<Selection> existingSelection = selectionManager.getSelection(player, selectionId);

        return existingSelection.orElseGet(() -> selectionManager.createSelection(player, selectionId, wandFactory.getSelectionType(player.getInventory().getItemInMainHand())));
    }

    private Location getTargetLocation(PlayerInteractEvent event, Player player) {
        Block block = event.getClickedBlock();

        if (block != null) {
            return block.getLocation();
        }

        // Si no hay bloque, usar el bloque que está mirando
        Block targetBlock = player.getTargetBlockExact(100);
        return targetBlock != null ? targetBlock.getLocation() : null;
    }

    private void handleWandAction(Player player, Selection selection, Location location, WandUseEvent.WandAction action, ItemStack wand) {
        switch (action) {
            case LEFT_CLICK_BLOCK:
                handlePos1Selection(player, selection, location, wand);
                break;
            case RIGHT_CLICK_BLOCK:
                handlePos2Selection(player, selection, location, wand);
                break;
            case SHIFT_LEFT_CLICK:
                handleSelectionInfo(player, selection);
                break;
            case SHIFT_RIGHT_CLICK:
                handleSelectionClear(player, selection);
                break;
        }
    }

    private void handlePos1Selection(Player player, Selection selection, Location location, ItemStack wand) {
        selectionManager.setPos1(player, selection.getSelectionId(), location);

        MessageUtils.sendMessageAsync(player, "%prefix% {primary}Position 1 has been set to: {info}X: " + location.getBlockX() + ", Y: " + location.getBlockY() + ", Z: " + location.getBlockZ());

        if (selection.isComplete()) {
            String info = String.format("{success}✓ Selection completed with: §7%d blocks", selection.getVolume());
            wandFactory.updateWandLore(wand, info);
        }
    }

    private void handlePos2Selection(Player player, Selection selection, Location location, ItemStack wand) {
        selectionManager.setPos2(player, selection.getSelectionId(), location);

        MessageUtils.sendMessageAsync(player, "%prefix% {primary}Position 2 has been set to: {info}X: " + location.getBlockX() + ", Y: " + location.getBlockY() + ", Z: " + location.getBlockZ());

        // Actualizar wand si está completa
        if (selection.isComplete()) {
            String info = String.format("{success}✓ Selection completed with: §7%d blocks", selection.getVolume());
            wandFactory.updateWandLore(wand, info);
        }
    }

    private void handleSelectionInfo(Player player, Selection selection) {
        if (!selection.isComplete()) {
            MessageUtils.sendMessageAsync(player, "%prefix% {primary}Selection is incomplete.");
            MessageUtils.sendMessageAsync(player, "%prefix% {primary}Pos1: {info}" + (selection.getPos1() != null ? "✓" : "✗"));
            MessageUtils.sendMessageAsync(player, "%prefix% {primary}Pos2: {info}" + (selection.getPos2() != null ? "✓" : "✗"));
            return;
        }

        Location min = selection.getMinimumPoint();
        Location max = selection.getMaximumPoint();

        MessageUtils.sendMessageAsync(player, "{secondary}&m                                    ");
        MessageUtils.sendMessageAsync(player, "%prefix% {primary}Selection information:");
        MessageUtils.sendMessageAsync(player, "%prefix% {primary}ID: {info}" + selection.getSelectionId());
        MessageUtils.sendMessageAsync(player, "%prefix% {primary}Type: {info}" + selection.getType().getDisplayName());
        MessageUtils.sendMessageAsync(player, "%prefix% {primary}Min: {info}X: " + min.getBlockX() + ", Y: " + min.getBlockY() + ", Z: " + min.getBlockZ());
        MessageUtils.sendMessageAsync(player, "%prefix% {primary}Max: {info}X: " + max.getBlockX() + ", Y: " + max.getBlockY() + ", Z: " + max.getBlockZ());
        MessageUtils.sendMessageAsync(player, "%prefix% {primary}Volume: {info}" + selection.getVolume() + " blocks");
        MessageUtils.sendMessageAsync(player, "%prefix% {primary}World: {info}" + selection.getPos1().getWorld().getName());
        MessageUtils.sendMessageAsync(player, "{secondary}&m                                    ");
    }

    private void handleSelectionClear(Player player, Selection selection) {
        boolean cleared = selectionManager.clearSelection(player, selection.getSelectionId());

        if (cleared) {
            MessageUtils.sendMessageAsync(player, "%prefix% {primary}Selection has been cleared.");
        } else {
            MessageUtils.sendMessageAsync(player, "%prefix% {error}Error clearing selection.");
        }
    }
}
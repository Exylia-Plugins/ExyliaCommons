package net.exylia.commons.v2.chat.handler;

import net.exylia.commons.v2.chat.config.ChatInputDefaults;
import net.exylia.commons.v2.chat.core.ChatInputManager;
import net.exylia.commons.v2.chat.request.BooleanInputRequest;
import net.exylia.commons.v2.chat.request.ConfirmationRequest;
import net.exylia.commons.v2.chat.request.OptionEntry;
import net.exylia.commons.v2.chat.request.SingleOptionRequest;
import net.exylia.commons.v2.chat.session.InputSession;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InventoryInputHandler implements Listener {

    private final Map<UUID, Inventory> trackedInventories = new HashMap<>();
    private final Set<UUID> programmaticClose = new HashSet<>();
    private final Map<UUID, Map<Integer, Runnable>> slotCallbacks = new HashMap<>();

    public void show(InputSession session) {
        Player player = session.getPlayer();
        UUID uuid = player.getUniqueId();

        Inventory inventory;
        Map<Integer, Runnable> callbacks = new HashMap<>();

        if (session.getRequest() instanceof BooleanInputRequest boolReq) {
            inventory = buildBooleanInventory(player, boolReq, callbacks);
        } else if (session.getRequest() instanceof ConfirmationRequest confirmReq) {
            inventory = buildConfirmationInventory(player, confirmReq, callbacks);
        } else if (session.getRequest() instanceof SingleOptionRequest optionReq) {
            inventory = buildOptionInventory(player, optionReq, callbacks);
        } else {
            return;
        }

        trackedInventories.put(uuid, inventory);
        slotCallbacks.put(uuid, callbacks);
        player.openInventory(inventory);
    }

    public void close(InputSession session) {
        UUID uuid = session.getPlayer().getUniqueId();
        programmaticClose.add(uuid);
        session.getPlayer().closeInventory();
        trackedInventories.remove(uuid);
        slotCallbacks.remove(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        Inventory tracked = trackedInventories.get(uuid);
        if (tracked == null) return;
        if (event.getClickedInventory() == null || !tracked.equals(event.getClickedInventory())) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        int slot = event.getSlot();
        Map<Integer, Runnable> callbacks = slotCallbacks.get(uuid);
        if (callbacks == null) return;

        Runnable action = callbacks.get(slot);
        if (action != null) {
            action.run();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        if (programmaticClose.remove(uuid)) return;

        Inventory tracked = trackedInventories.remove(uuid);
        if (tracked == null) return;
        slotCallbacks.remove(uuid);

        InputSession session = ChatInputManager.getInstance().getActiveSession(uuid);
        if (session == null || session.getHandlerType() != InputSession.HandlerType.INVENTORY) return;

        ChatInputManager.getInstance().handleCancelFromHandler(player);
    }

    private Inventory buildBooleanInventory(Player player, BooleanInputRequest request, Map<Integer, Runnable> callbacks) {
        Inventory inv = Bukkit.createInventory(null, 27, ColorAPI.parse(request.getPrompt()));
        Material fillerMat = parseMaterial(ChatInputDefaults.ChatInput.UIFallback.FILLER_MATERIAL, Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = buildFiller(fillerMat);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        Material yesMat = parseMaterial(ChatInputDefaults.ChatInput.UIFallback.YES_MATERIAL, Material.LIME_CONCRETE);
        Material noMat = parseMaterial(ChatInputDefaults.ChatInput.UIFallback.NO_MATERIAL, Material.RED_CONCRETE);
        inv.setItem(11, buildItem(yesMat, ChatInputDefaults.ChatInput.UIFallback.YES_LABEL));
        inv.setItem(15, buildItem(noMat, ChatInputDefaults.ChatInput.UIFallback.NO_LABEL));

        UUID uuid = player.getUniqueId();
        callbacks.put(11, () -> {
            closeAndComplete(player, uuid, Boolean.TRUE);
        });
        callbacks.put(15, () -> {
            closeAndComplete(player, uuid, Boolean.FALSE);
        });

        return inv;
    }

    private Inventory buildConfirmationInventory(Player player, ConfirmationRequest request, Map<Integer, Runnable> callbacks) {
        Inventory inv = Bukkit.createInventory(null, 27, ColorAPI.parse(request.getPrompt()));
        Material fillerMat = parseMaterial(ChatInputDefaults.ChatInput.UIFallback.FILLER_MATERIAL, Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = buildFiller(fillerMat);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        Material yesMat = parseMaterial(ChatInputDefaults.ChatInput.UIFallback.YES_MATERIAL, Material.LIME_CONCRETE);
        Material noMat = parseMaterial(ChatInputDefaults.ChatInput.UIFallback.NO_MATERIAL, Material.RED_CONCRETE);
        inv.setItem(11, buildItem(yesMat, ChatInputDefaults.ChatInput.UIFallback.YES_LABEL));
        inv.setItem(15, buildItem(noMat, ChatInputDefaults.ChatInput.UIFallback.NO_LABEL));

        UUID uuid = player.getUniqueId();
        callbacks.put(11, () -> closeAndComplete(player, uuid, Boolean.TRUE));
        callbacks.put(15, () -> closeAndComplete(player, uuid, Boolean.FALSE));

        return inv;
    }

    private Inventory buildOptionInventory(Player player, SingleOptionRequest request, Map<Integer, Runnable> callbacks) {
        List<OptionEntry> options = request.getOptions();
        int totalItems = options.size() + 1;
        int size = totalItems <= 7 ? 27 : (totalItems <= 16 ? 45 : 54);

        Inventory inv = Bukkit.createInventory(null, size, ColorAPI.parse(request.getPrompt()));
        Material fillerMat = parseMaterial(ChatInputDefaults.ChatInput.UIFallback.FILLER_MATERIAL, Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = buildFiller(fillerMat);
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        int startSlot = 10;
        UUID uuid = player.getUniqueId();
        for (int i = 0; i < options.size(); i++) {
            OptionEntry entry = options.get(i);
            int slot = startSlot + i;
            if (slot >= size - 1) break;
            inv.setItem(slot, buildItem(Material.PAPER, entry.label()));
            final String key = entry.key();
            callbacks.put(slot, () -> closeAndComplete(player, uuid, key));
        }

        Material cancelMat = parseMaterial(ChatInputDefaults.ChatInput.UIFallback.CANCEL_MATERIAL, Material.RED_CONCRETE);
        int cancelSlot = size - 1;
        inv.setItem(cancelSlot, buildItem(cancelMat, ChatInputDefaults.ChatInput.UIFallback.CANCEL_LABEL));
        callbacks.put(cancelSlot, () -> {
            closeInventoryProgrammatically(uuid, player);
            ChatInputManager.getInstance().handleCancelFromHandler(player);
        });

        return inv;
    }

    private void closeAndComplete(Player player, UUID uuid, Object value) {
        closeInventoryProgrammatically(uuid, player);
        ChatInputManager.getInstance().handleCompleteFromHandler(uuid, value);
    }

    private void closeInventoryProgrammatically(UUID uuid, Player player) {
        programmaticClose.add(uuid);
        player.closeInventory();
        trackedInventories.remove(uuid);
        slotCallbacks.remove(uuid);
    }

    private static ItemStack buildItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorAPI.parse(name));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildFiller(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.space());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}

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

    /** Slots 0-44 hold options; the bottom row is reserved for navigation. */
    private static final int CONTENT_SLOTS = 45;
    private static final int PREV_SLOT = 45;
    private static final int PAGE_SLOT = 49;
    private static final int NEXT_SLOT = 53;

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

    /**
     * Inventory fallback for clients that cannot use dialogs. Options are paged across the top
     * 45 slots with navigation on the bottom row, so a large registry (particles, sounds, ...)
     * stays fully reachable instead of being silently truncated at 44 entries.
     */
    private Inventory buildOptionInventory(Player player, SingleOptionRequest request, Map<Integer, Runnable> callbacks) {
        // Reserve the bottom row for navigation whenever the list cannot fit in one screen.
        List<OptionEntry> all = request.getFilteredOptions();
        boolean needsPaging = all.size() > CONTENT_SLOTS;
        int size = needsPaging ? 54 : (all.size() + 1 <= 7 ? 27 : (all.size() + 1 <= 16 ? 45 : 54));
        int perPage = needsPaging ? CONTENT_SLOTS : size - 1;

        int totalPages = needsPaging ? Math.max(1, (all.size() + perPage - 1) / perPage) : 1;
        int page = Math.min(Math.max(0, request.getPage()), totalPages - 1);
        request.setPage(page);

        Inventory inv = Bukkit.createInventory(null, size, ColorAPI.parse(request.getPrompt()));
        Material fillerMat = parseMaterial(ChatInputDefaults.ChatInput.UIFallback.FILLER_MATERIAL, Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = buildFiller(fillerMat);
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        UUID uuid = player.getUniqueId();
        int from = page * perPage;
        int to = Math.min(from + perPage, all.size());

        for (int i = from; i < to; i++) {
            OptionEntry entry = all.get(i);
            int slot = needsPaging ? (i - from) : (10 + (i - from));
            if (slot >= size - 1) break;
            inv.setItem(slot, buildItem(Material.PAPER, entry.label()));
            final String key = entry.key();
            callbacks.put(slot, () -> closeAndComplete(player, uuid, key));
        }

        if (needsPaging && totalPages > 1) {
            if (page > 0) {
                inv.setItem(PREV_SLOT, buildItem(Material.ARROW, ChatInputDefaults.ChatInput.Dialog.PREVIOUS_BUTTON));
                callbacks.put(PREV_SLOT, () -> reopen(player, request, page - 1));
            }
            inv.setItem(PAGE_SLOT, buildItem(Material.PAPER, ChatInputDefaults.ChatInput.Dialog.PAGE_LINE
                    .replace("%page%", String.valueOf(page + 1))
                    .replace("%total%", String.valueOf(totalPages))));
            if (page < totalPages - 1) {
                inv.setItem(NEXT_SLOT, buildItem(Material.ARROW, ChatInputDefaults.ChatInput.Dialog.NEXT_BUTTON));
                callbacks.put(NEXT_SLOT, () -> reopen(player, request, page + 1));
            }
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

    /** Rebuilds the option inventory on a different page, keeping the session alive. */
    private void reopen(Player player, SingleOptionRequest request, int page) {
        request.setPage(page);
        UUID uuid = player.getUniqueId();
        Map<Integer, Runnable> callbacks = new HashMap<>();
        Inventory inv = buildOptionInventory(player, request, callbacks);

        // Suppress the close event for the swap, otherwise it would cancel the session.
        programmaticClose.add(uuid);
        trackedInventories.put(uuid, inv);
        slotCallbacks.put(uuid, callbacks);
        player.openInventory(inv);
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

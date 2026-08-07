package net.exylia.commons.v2.ui.selector.impl.loot.action;

import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.chat.api.ChatInputAPI;
import net.exylia.commons.v2.loot.model.LootEntry;
import net.exylia.commons.v2.ui.selector.impl.iconpicker.IconPickerAPI;
import net.exylia.commons.v2.ui.selector.impl.loot.LootClipboard;
import net.exylia.commons.v2.ui.selector.impl.loot.LootEditorRegistry;
import net.exylia.commons.v2.ui.selector.impl.loot.LootEditorSession;
import net.exylia.commons.v2.ui.selector.impl.loot.LootListClipboard;
import net.exylia.commons.v2.ui.selector.impl.loot.menu.LootEditMenu;
import net.exylia.commons.v2.ui.selector.impl.loot.menu.LootListMenu;
import net.exylia.commons.v2.ui.selector.impl.loot.menu.LootTypeSelectMenu;
import net.exylia.commons.v2.loot.model.LootEntryType;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class LootEditorActionRegistrar {

    private static final String NS = "commons";

    private LootEditorActionRegistrar() {}

    public static void register(JavaPlugin plugin) {
        if (ActionAPI.get(NS + ":loot_list").isPresent()) return;

        ActionAPI.create("loot_list", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;
                    LootListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("loot_add", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    if (session.isAllowCommands()) {
                        LootTypeSelectMenu.open(player);
                        return;
                    }

                    IconPickerAPI.open(
                            player,
                            () -> LootListMenu.open(player, session),
                            snapshot -> {
                                LootEntry entry = LootEntry.builder()
                                        .itemSnapshot(snapshot.serialize())
                                        .build();
                                session.addEntry(entry);
                                LootListMenu.open(player, session);
                            }
                    );
                })
                .build();

        ActionAPI.create("loot_add_type", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null || args.isEmpty()) return;

                    LootEntryType type;
                    try {
                        type = LootEntryType.valueOf(args.getString(0, "ITEM"));
                    } catch (IllegalArgumentException e) {
                        type = LootEntryType.ITEM;
                    }

                    if (type == LootEntryType.COMMAND) {
                        LootEntry entry = LootEntry.ofCommand("");
                        session.addEntry(entry);
                        player.closeInventory();
                        ChatInputAPI.text(player, "Enter the command to run (without /), e.g. give %player% diamond 1")
                                .onCancel(() -> {
                                    session.removeEntry(entry.getId());
                                    LootListMenu.open(player, session);
                                })
                                .onResponse(value -> {
                                    entry.setCommand(value.trim());
                                    session.replaceEntry(entry);
                                    LootEditMenu.open(player, entry);
                                })
                                .ask();
                        return;
                    }

                    IconPickerAPI.open(
                            player,
                            () -> LootListMenu.open(player, session),
                            snapshot -> {
                                LootEntry entry = LootEntry.builder()
                                        .type(LootEntryType.ITEM)
                                        .itemSnapshot(snapshot.serialize())
                                        .build();
                                session.addEntry(entry);
                                LootListMenu.open(player, session);
                            }
                    );
                })
                .build();

        ActionAPI.create("loot_edit", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    LootEntry entry = session.findById(id);
                    if (entry == null) return;

                    LootEditMenu.open(player, entry);
                })
                .build();

        ActionAPI.create("loot_delete", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    session.removeEntry(id);
                    LootListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("loot_copy", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    LootEntry entry = session.findById(id);
                    if (entry == null) return;

                    LootClipboard.copy(player, entry);
                    player.sendMessage(ColorAPI.parse("{success}Loot entry copied to clipboard."));
                    LootListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("loot_paste", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    LootEntry pasted = LootClipboard.paste(player);
                    if (pasted == null) {
                        player.sendMessage(ColorAPI.parse("{error}No loot entry in clipboard."));
                        return;
                    }
                    session.addEntry(pasted);
                    LootListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("loot_copy_all", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    LootListClipboard.copy(player, session.getEntries());
                    player.sendMessage(ColorAPI.parse("{success}Copied " + session.getEntries().size() + " loot entries to clipboard."));
                    LootListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("loot_paste_all", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    List<LootEntry> pasted = LootListClipboard.paste(player);
                    if (pasted == null) {
                        player.sendMessage(ColorAPI.parse("{error}No loot table in clipboard."));
                        return;
                    }
                    pasted.forEach(session::addEntry);
                    player.sendMessage(ColorAPI.parse("{success}Pasted " + pasted.size() + " loot entries."));
                    LootListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("loot_save", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    LootEditorRegistry.getInstance().remove(player);
                    if (session.getOnSave() != null) {
                        session.getOnSave().accept(player, List.copyOf(session.getEntries()));
                    }
                })
                .build();

        ActionAPI.create("loot_cancel", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    LootEditorRegistry.getInstance().remove(player);
                    if (session.getOnCancel() != null) {
                        session.getOnCancel().run();
                    }
                })
                .build();

        ActionAPI.create("loot_set_item", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    LootEntry entry = session.findById(id);
                    if (entry == null) return;

                    IconPickerAPI.open(
                            player,
                            () -> LootEditMenu.open(player, entry),
                            snapshot -> {
                                entry.setItemSnapshot(snapshot.serialize());
                                session.replaceEntry(entry);
                                LootEditMenu.open(player, entry);
                            }
                    );
                })
                .build();

        ActionAPI.create("loot_set_command", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    LootEntry entry = session.findById(id);
                    if (entry == null) return;

                    player.closeInventory();
                    ChatInputAPI.text(player, "Enter the command to run (without /), e.g. give %player% diamond 1")
                            .onCancel(() -> LootEditMenu.open(player, entry))
                            .onResponse(value -> {
                                entry.setCommand(value.trim());
                                session.replaceEntry(entry);
                                LootEditMenu.open(player, entry);
                            })
                            .ask();
                })
                .build();

        ActionAPI.create("loot_adjust_min", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    LootEntry entry = session.findById(id);
                    if (entry == null) return;

                    int delta = args.getInt(1, 0);
                    int updated = Math.max(1, Math.min(entry.getMaxAmount(), entry.getMinAmount() + delta));
                    entry.setMinAmount(updated);
                    session.replaceEntry(entry);
                    LootEditMenu.open(player, entry);
                })
                .build();

        ActionAPI.create("loot_adjust_max", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    LootEntry entry = session.findById(id);
                    if (entry == null) return;

                    int delta = args.getInt(1, 0);
                    int updated = Math.max(entry.getMinAmount(), Math.min(64, entry.getMaxAmount() + delta));
                    entry.setMaxAmount(updated);
                    session.replaceEntry(entry);
                    LootEditMenu.open(player, entry);
                })
                .build();

        ActionAPI.create("loot_adjust_weight", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    LootEntry entry = session.findById(id);
                    if (entry == null) return;

                    double delta = args.getInt(1, 0);
                    double updated = Math.max(0.1, Math.min(100.0, entry.getWeight() + delta));
                    entry.setWeight(Math.round(updated * 10.0) / 10.0);
                    session.replaceEntry(entry);
                    LootEditMenu.open(player, entry);
                })
                .build();

        ActionAPI.create("loot_set_tier", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    LootEntry entry = session.findById(id);
                    if (entry == null) return;

                    ChatInputAPI.text(player, "Enter tier name (or 'none' to clear)")
                            .onCancel(() -> LootEditMenu.open(player, entry))
                            .onResponse(value -> {
                                entry.setTier(value.equalsIgnoreCase("none") ? null : value.toUpperCase());
                                session.replaceEntry(entry);
                                LootEditMenu.open(player, entry);
                            })
                            .ask();
                })
                .build();

        ActionAPI.create("loot_clear_tier", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    LootEditorSession session = LootEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    LootEntry entry = session.findById(id);
                    if (entry == null) return;

                    entry.setTier(null);
                    session.replaceEntry(entry);
                    LootEditMenu.open(player, entry);
                })
                .build();
    }
}

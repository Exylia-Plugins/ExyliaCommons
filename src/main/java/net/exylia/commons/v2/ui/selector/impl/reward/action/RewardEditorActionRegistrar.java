package net.exylia.commons.v2.ui.selector.impl.reward.action;

import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.chat.api.ChatInputAPI;
import net.exylia.commons.v2.items.input.IconInputHelper;
import net.exylia.commons.v2.reward.model.RewardEntry;
import net.exylia.commons.v2.reward.model.RewardType;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.selector.impl.reward.RewardClipboard;
import net.exylia.commons.v2.ui.selector.impl.reward.RewardEditorRegistry;
import net.exylia.commons.v2.ui.selector.impl.reward.RewardEditorSession;
import net.exylia.commons.v2.ui.selector.impl.reward.menu.RewardEditMenu;
import net.exylia.commons.v2.ui.selector.impl.reward.menu.RewardListMenu;
import net.exylia.commons.v2.ui.selector.impl.reward.menu.RewardTypeSelectMenu;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class RewardEditorActionRegistrar {

    private static final String NS = "commons";

    private RewardEditorActionRegistrar() {}

    public static void register(JavaPlugin plugin) {
        if (ActionAPI.get(NS + ":reward_list").isPresent()) return;

        ActionAPI.create("reward_list", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;
                    RewardListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("reward_add", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;
                    RewardTypeSelectMenu.open(player, session);
                })
                .build();

        ActionAPI.create("reward_add_type", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String typeName = args.getString(0, "");
                    RewardType type;
                    try {
                        type = RewardType.valueOf(typeName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return;
                    }

                    handleAddType(player, session, type);
                })
                .build();

        ActionAPI.create("reward_edit", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    RewardEntry entry = session.findById(id);
                    if (entry == null) return;

                    RewardEditMenu.open(player, entry);
                })
                .build();

        ActionAPI.create("reward_delete", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    session.removeReward(id);
                    RewardListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("reward_copy", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    RewardEntry entry = session.findById(id);
                    if (entry == null) return;

                    RewardClipboard.copy(player, entry);
                    player.sendMessage("§aCopied reward to clipboard.");
                    RewardListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("reward_paste", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    RewardEntry pasted = RewardClipboard.paste(player);
                    if (pasted == null) {
                        player.sendMessage("§cNo reward in clipboard.");
                        return;
                    }
                    session.addReward(pasted);
                    RewardListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("reward_save", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    RewardEditorRegistry.getInstance().remove(player);
                    if (session.getOnSave() != null) {
                        session.getOnSave().accept(player, List.copyOf(session.getRewards()));
                    }
                })
                .build();

        ActionAPI.create("reward_cancel", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    RewardEditorRegistry.getInstance().remove(player);
                    if (session.getOnCancel() != null) {
                        session.getOnCancel().run();
                    }
                })
                .build();

        ActionAPI.create("reward_set_value", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    RewardEntry entry = session.findById(id);
                    if (entry == null) return;

                    handleSetValue(player, session, entry);
                })
                .build();

        ActionAPI.create("reward_set_chance", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    RewardEntry entry = session.findById(id);
                    if (entry == null) return;

                    ChatInputAPI.decimal(player, "Chance (0.01 - 100.0)")
                            .range(0.01, 100.0)
                            .onCancel(() -> RewardEditMenu.open(player, entry))
                            .onResponse(value -> {
                                entry.setChance(value.doubleValue());
                                session.replaceReward(entry);
                                RewardEditMenu.open(player, entry);
                            })
                            .ask();
                })
                .build();

        ActionAPI.create("reward_set_condition", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    RewardEntry entry = session.findById(id);
                    if (entry == null) return;

                    ChatInputAPI.text(player, "Enter condition expression (or 'none' to clear)")
                            .onCancel(() -> RewardEditMenu.open(player, entry))
                            .onResponse(value -> {
                                entry.setCondition(value.equalsIgnoreCase("none") ? null : value);
                                session.replaceReward(entry);
                                RewardEditMenu.open(player, entry);
                            })
                            .ask();
                })
                .build();

        ActionAPI.create("reward_clear_condition", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    RewardEntry entry = session.findById(id);
                    if (entry == null) return;

                    entry.setCondition(null);
                    session.replaceReward(entry);
                    RewardEditMenu.open(player, entry);
                })
                .build();

        ActionAPI.create("reward_set_priority", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    RewardEntry entry = session.findById(id);
                    if (entry == null) return;

                    ChatInputAPI.integer(player, "Priority (higher = first, default 0)")
                            .onCancel(() -> RewardEditMenu.open(player, entry))
                            .onResponse(value -> {
                                entry.setPriority(value.intValue());
                                session.replaceReward(entry);
                                RewardEditMenu.open(player, entry);
                            })
                            .ask();
                })
                .build();

        ActionAPI.create("reward_set_delivery_msg", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    RewardEntry entry = session.findById(id);
                    if (entry == null) return;

                    ChatInputAPI.text(player, "Delivery message (sent after reward, or 'none' to clear)")
                            .onCancel(() -> RewardEditMenu.open(player, entry))
                            .onResponse(value -> {
                                entry.setDeliveryMessage(value.equalsIgnoreCase("none") ? null : value);
                                session.replaceReward(entry);
                                RewardEditMenu.open(player, entry);
                            })
                            .ask();
                })
                .build();

        ActionAPI.create("reward_clear_delivery_msg", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    RewardEditorSession session = RewardEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    RewardEntry entry = session.findById(id);
                    if (entry == null) return;

                    entry.setDeliveryMessage(null);
                    session.replaceReward(entry);
                    RewardEditMenu.open(player, entry);
                })
                .build();
    }

    private static void handleAddType(Player player, RewardEditorSession session, RewardType type) {
        switch (type) {
            case COMMAND -> ChatInputAPI.text(player, "Enter command (use %player% for player name)")
                    .onCancel(() -> RewardTypeSelectMenu.open(player, session))
                    .onResponse(cmd -> {
                        session.addReward(RewardEntry.ofCommand(cmd));
                        RewardListMenu.open(player, session);
                    })
                    .ask();

            case MESSAGE -> ChatInputAPI.text(player, "Enter message (supports color codes)")
                    .onCancel(() -> RewardTypeSelectMenu.open(player, session))
                    .onResponse(msg -> {
                        session.addReward(RewardEntry.ofMessage(msg));
                        RewardListMenu.open(player, session);
                    })
                    .ask();

            case ITEM -> IconInputHelper.ask(
                    player,
                    () -> RewardTypeSelectMenu.open(player, session),
                    snapshot -> {
                        RewardEntry entry = RewardEntry.builder()
                                .type(RewardType.ITEM)
                                .itemSnapshot(snapshot.serialize())
                                .build();
                        session.addReward(entry);
                        RewardListMenu.open(player, session);
                    }
            );
        }
    }

    private static void handleSetValue(Player player, RewardEditorSession session, RewardEntry entry) {
        switch (entry.getType()) {
            case COMMAND -> ChatInputAPI.text(player, "Enter new command (use %player% for player name)")
                    .onCancel(() -> RewardEditMenu.open(player, entry))
                    .onResponse(cmd -> {
                        entry.setCommand(cmd);
                        session.replaceReward(entry);
                        RewardEditMenu.open(player, entry);
                    })
                    .ask();

            case MESSAGE -> ChatInputAPI.text(player, "Enter new message (supports color codes)")
                    .onCancel(() -> RewardEditMenu.open(player, entry))
                    .onResponse(msg -> {
                        entry.setMessage(msg);
                        session.replaceReward(entry);
                        RewardEditMenu.open(player, entry);
                    })
                    .ask();

            case ITEM -> IconInputHelper.ask(
                    player,
                    () -> RewardEditMenu.open(player, entry),
                    snapshot -> {
                        entry.setItemSnapshot(snapshot.serialize());
                        session.replaceReward(entry);
                        RewardEditMenu.open(player, entry);
                    }
            );
        }
    }
}

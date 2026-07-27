package net.exylia.commons.v2.ui.selector.impl.namedcommand.action;

import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.chat.api.ChatInputAPI;
import net.exylia.commons.v2.namedcommand.model.NamedCommandEntry;
import net.exylia.commons.v2.ui.selector.impl.namedcommand.NamedCommandClipboard;
import net.exylia.commons.v2.ui.selector.impl.namedcommand.NamedCommandEditorRegistry;
import net.exylia.commons.v2.ui.selector.impl.namedcommand.NamedCommandEditorSession;
import net.exylia.commons.v2.ui.selector.impl.namedcommand.menu.NamedCommandListMenu;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class NamedCommandEditorActionRegistrar {

    private static final String NS = "commons";

    private NamedCommandEditorActionRegistrar() {}

    public static void register(JavaPlugin plugin) {
        if (ActionAPI.get(NS + ":namedcmd_list").isPresent()) return;

        ActionAPI.create("namedcmd_list", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    NamedCommandEditorSession session = NamedCommandEditorRegistry.getInstance().get(player);
                    if (session == null) return;
                    NamedCommandListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("namedcmd_add", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    NamedCommandEditorSession session = NamedCommandEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    player.closeInventory();
                    ChatInputAPI.text(player, "Enter a name for this command")
                            .onCancel(() -> NamedCommandListMenu.open(player, session))
                            .onResponse(name -> ChatInputAPI.text(player, "Enter the command (use %player% for player name)")
                                    .onCancel(() -> NamedCommandListMenu.open(player, session))
                                    .onResponse(command -> {
                                        session.addCommand(NamedCommandEntry.of(name, command));
                                        NamedCommandListMenu.open(player, session);
                                    })
                                    .ask())
                            .ask();
                })
                .build();

        ActionAPI.create("namedcmd_edit_name", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    NamedCommandEditorSession session = NamedCommandEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    NamedCommandEntry entry = session.findById(id);
                    if (entry == null) return;

                    player.closeInventory();
                    ChatInputAPI.text(player, "Enter the new name")
                            .onCancel(() -> NamedCommandListMenu.open(player, session))
                            .onResponse(value -> {
                                entry.setName(value);
                                session.replaceCommand(entry);
                                NamedCommandListMenu.open(player, session);
                            })
                            .ask();
                })
                .build();

        ActionAPI.create("namedcmd_edit_command", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    NamedCommandEditorSession session = NamedCommandEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    NamedCommandEntry entry = session.findById(id);
                    if (entry == null) return;

                    player.closeInventory();
                    ChatInputAPI.text(player, "Enter the new command (use %player% for player name)")
                            .onCancel(() -> NamedCommandListMenu.open(player, session))
                            .onResponse(value -> {
                                entry.setCommand(value);
                                session.replaceCommand(entry);
                                NamedCommandListMenu.open(player, session);
                            })
                            .ask();
                })
                .build();

        ActionAPI.create("namedcmd_delete", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    NamedCommandEditorSession session = NamedCommandEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    session.removeCommand(id);
                    NamedCommandListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("namedcmd_copy", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    NamedCommandEditorSession session = NamedCommandEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    String id = args.getString(0, "");
                    NamedCommandEntry entry = session.findById(id);
                    if (entry == null) return;

                    NamedCommandClipboard.copy(player, entry);
                    player.sendMessage("§aCopied command to clipboard.");
                    NamedCommandListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("namedcmd_paste", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    NamedCommandEditorSession session = NamedCommandEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    NamedCommandEntry pasted = NamedCommandClipboard.paste(player);
                    if (pasted == null) {
                        player.sendMessage("§cNo command in clipboard.");
                        return;
                    }
                    session.addCommand(pasted);
                    NamedCommandListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("namedcmd_save", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    NamedCommandEditorSession session = NamedCommandEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    NamedCommandEditorRegistry.getInstance().remove(player);
                    if (session.getOnSave() != null) {
                        session.getOnSave().accept(player, List.copyOf(session.getCommands()));
                    }
                })
                .build();

        ActionAPI.create("namedcmd_cancel", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    NamedCommandEditorSession session = NamedCommandEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    NamedCommandEditorRegistry.getInstance().remove(player);
                    if (session.getOnCancel() != null) {
                        session.getOnCancel().run();
                    }
                })
                .build();
    }
}

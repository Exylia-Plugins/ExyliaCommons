package net.exylia.commons.v2.ui.selector.impl.iconpicker.action;

import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.chat.api.ChatInputAPI;
import net.exylia.commons.v2.items.snapshot.ItemSnapshot;
import net.exylia.commons.v2.items.utils.ItemStackUtils;
import net.exylia.commons.v2.ui.selector.impl.iconpicker.IconPickerRegistry;
import net.exylia.commons.v2.ui.selector.impl.iconpicker.IconPickerSession;
import net.exylia.commons.v2.ui.selector.impl.iconpicker.menu.IconPickerHeadMenu;
import net.exylia.commons.v2.ui.selector.impl.iconpicker.menu.IconPickerMenu;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class IconPickerActionRegistrar {

    private static final String NS = "commons";

    private IconPickerActionRegistrar() {}

    public static void register(JavaPlugin plugin) {
        if (ActionAPI.get(NS + ":iconpicker_open").isPresent()) return;

        ActionAPI.create("iconpicker_open", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    IconPickerSession session = IconPickerRegistry.getInstance().get(player);
                    if (session == null) return;
                    IconPickerMenu.open(player, session);
                })
                .build();

        ActionAPI.create("iconpicker_clear_slot", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    IconPickerSession session = IconPickerRegistry.getInstance().get(player);
                    if (session == null) return;
                    session.setCapturedItem(null);
                    IconPickerMenu.open(player, session);
                })
                .build();

        ActionAPI.create("iconpicker_confirm", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    IconPickerSession session = IconPickerRegistry.getInstance().get(player);
                    if (session == null) return;

                    if (!session.hasCapturedItem()) {
                        player.sendMessage(ColorAPI.parse("{error}Place an item in the slot first."));
                        return;
                    }
                    session.complete(ItemSnapshot.from(session.getCapturedItem()));
                })
                .build();

        ActionAPI.create("iconpicker_cancel", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    IconPickerSession session = IconPickerRegistry.getInstance().get(player);
                    if (session == null) return;
                    session.cancel();
                })
                .build();

        ActionAPI.create("iconpicker_set_id", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    IconPickerSession session = IconPickerRegistry.getInstance().get(player);
                    if (session == null) return;

                    player.closeInventory();
                    ChatInputAPI.text(player, "{warning}Type a material name or custom item ID:")
                            .onCancel(() -> IconPickerMenu.open(player, session))
                            .onResponse(input -> {
                                String trimmed = input.trim();
                                if (trimmed.isEmpty()) {
                                    IconPickerMenu.open(player, session);
                                    return;
                                }
                                if (!ItemStackUtils.isValidMaterial(trimmed)) {
                                    player.sendMessage(ColorAPI.parse("{error}Invalid material or item ID: " + trimmed));
                                    IconPickerMenu.open(player, session);
                                    return;
                                }
                                session.complete(ItemSnapshot.from(trimmed));
                            })
                            .ask();
                })
                .build();

        ActionAPI.create("iconpicker_open_heads", plugin).namespace(NS)
                .handler((ctx, args) -> IconPickerHeadMenu.open(ctx.getPlayer()))
                .build();

        ActionAPI.create("iconpicker_head_input", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    IconPickerSession session = IconPickerRegistry.getInstance().get(player);
                    if (session == null) return;

                    String mode = args.getString(0, "URL");
                    String prompt = switch (mode) {
                        case "BASE64" -> "{warning}Paste the base64 texture value:";
                        case "PLAYER" -> "{warning}Type the player name:";
                        default -> "{warning}Paste the skin texture URL:";
                    };

                    player.closeInventory();
                    ChatInputAPI.text(player, prompt)
                            .onCancel(() -> IconPickerHeadMenu.open(player))
                            .onResponse(input -> {
                                String trimmed = input.trim();
                                if (trimmed.isEmpty()) {
                                    IconPickerHeadMenu.open(player);
                                    return;
                                }
                                String snapshotValue = switch (mode) {
                                    case "BASE64" -> "basehead:" + trimmed;
                                    case "PLAYER" -> "playerhead:" + trimmed;
                                    default -> "urlhead:" + trimmed;
                                };
                                session.complete(ItemSnapshot.from(snapshotValue));
                            })
                            .ask();
                })
                .build();
    }
}

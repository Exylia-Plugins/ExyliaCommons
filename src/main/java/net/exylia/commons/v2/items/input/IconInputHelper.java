package net.exylia.commons.v2.items.input;

import net.exylia.commons.v2.chat.api.ChatInputAPI;
import net.exylia.commons.v2.chat.config.ChatInputConfig;
import net.exylia.commons.v2.items.skull.SkullParser;
import net.exylia.commons.v2.items.snapshot.ItemSnapshot;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public final class IconInputHelper {

    private static final ChatInputConfig BASE_CONFIG = ChatInputConfig.builder()
        .prompt("{warning}Hold item + type 'done' {muted}| Or: basehead-BASE64, urlhead-URL, playerhead-NAME")
        .titleText("{warning}&lSet Icon")
        .subtitleText("{info}Hold item and type 'done'")
        .build();

    private IconInputHelper() {}

    public static void ask(Player player, Runnable onCancel, Consumer<ItemSnapshot> callback) {
        ask(player, BASE_CONFIG.toBuilder().onCancel(onCancel).build(), callback);
    }

    public static void ask(Player player, ChatInputConfig config, Consumer<ItemSnapshot> callback) {
        ChatInputAPI.ask(player, config, input -> {
            if (input.equalsIgnoreCase("done")) {
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType() == Material.AIR) {
                    player.sendMessage("§cYou must hold an item!");
                    Runnable onCancel = config.getOnCancel();
                    if (onCancel != null) onCancel.run();
                    return;
                }
                callback.accept(ItemSnapshot.from(held));
                return;
            }

            if (SkullParser.isSkullString(input)
                    || input.toLowerCase().startsWith("headbase-")
                    || input.toLowerCase().startsWith("headurl-")
                    || input.toLowerCase().startsWith("playerhead-")) {
                callback.accept(ItemSnapshot.from(input));
                return;
            }

            try {
                Material.valueOf(input.toUpperCase());
                callback.accept(ItemSnapshot.from(input.toUpperCase()));
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cInvalid input! Hold item and type 'done', or provide a valid material/head string.");
                Runnable onCancel = config.getOnCancel();
                if (onCancel != null) onCancel.run();
            }
        });
    }
}

package net.exylia.commons.v2.items.input;

import net.exylia.commons.v2.chat.api.ChatInputAPI;
import net.exylia.commons.v2.items.skull.SkullParser;
import net.exylia.commons.v2.items.snapshot.ItemSnapshot;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public final class IconInputHelper {

    private static final String PROMPT =
        "{warning}Hold item + type 'done' {muted}| Or: basehead-BASE64, urlhead-URL, playerhead-NAME";

    private IconInputHelper() {}

    public static void ask(Player player, Runnable onCancel, Consumer<ItemSnapshot> callback) {
        ChatInputAPI.text(player, PROMPT)
            .forceChat()
            .forceTitle("{warning}Hold item + type 'done'")
            .onCancel(onCancel)
            .onResponse(input -> handleInput(player, input, onCancel, callback))
            .ask();
    }

    private static void handleInput(Player player, String input, Runnable onCancel, Consumer<ItemSnapshot> callback) {
        if (input.equalsIgnoreCase("done")) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType() == Material.AIR) {
                player.sendMessage("§cYou must hold an item!");
                ask(player, onCancel, callback);
                return;
            }
            callback.accept(ItemSnapshot.from(held));
            return;
        }

        String lower = input.toLowerCase();
        if (SkullParser.isSkullString(input)
                || lower.startsWith("headbase-")
                || lower.startsWith("headurl-")
                || lower.startsWith("playerhead-")) {
            callback.accept(ItemSnapshot.from(input));
            return;
        }

        try {
            Material.valueOf(input.toUpperCase());
            callback.accept(ItemSnapshot.from(input.toUpperCase()));
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid input! Hold item and type 'done', or provide a valid material/head string.");
            ask(player, onCancel, callback);
        }
    }
}

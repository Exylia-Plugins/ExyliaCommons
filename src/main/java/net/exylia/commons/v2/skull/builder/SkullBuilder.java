package net.exylia.commons.v2.skull.builder;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.skull.renderer.SkullRenderer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SkullBuilder {

    private final SkullRenderer renderer;
    private final SkullType type;
    private final String value;

    private String displayName;
    private List<String> lore;
    private int amount = 1;
    private boolean glow = false;
    private List<ItemFlag> flags = new ArrayList<>();

    public static SkullBuilder texture(SkullRenderer renderer, String base64) {
        return new SkullBuilder(renderer, SkullType.TEXTURE, base64);
    }

    public static SkullBuilder textureURL(SkullRenderer renderer, String url) {
        return new SkullBuilder(renderer, SkullType.TEXTURE_URL, url);
    }

    public static SkullBuilder player(SkullRenderer renderer, String playerName) {
        return new SkullBuilder(renderer, SkullType.PLAYER, playerName);
    }

    public SkullBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public SkullBuilder lore(String... lore) {
        this.lore = Arrays.asList(lore);
        return this;
    }

    public SkullBuilder lore(List<String> lore) {
        this.lore = new ArrayList<>(lore);
        return this;
    }

    public SkullBuilder amount(int amount) {
        this.amount = Math.max(1, Math.min(64, amount));
        return this;
    }

    public SkullBuilder glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public SkullBuilder flags(ItemFlag... flags) {
        this.flags.addAll(Arrays.asList(flags));
        return this;
    }

    public ItemStack build() {
        ItemStack skull = switch (type) {
            case TEXTURE -> renderer.renderTexture(value);
            case TEXTURE_URL -> renderer.renderTextureURL(value);
            case PLAYER -> renderer.renderPlayer(value);
        };

        applyMeta(skull);
        return skull;
    }

    public CompletableFuture<ItemStack> buildAsync() {
        CompletableFuture<ItemStack> future = switch (type) {
            case TEXTURE -> renderer.renderTextureAsync(value);
            case TEXTURE_URL -> renderer.renderTextureURLAsync(value);
            case PLAYER -> renderer.renderPlayerAsync(value);
        };

        return future.thenApply(skull -> {
            applyMeta(skull);
            return skull;
        });
    }

    public void buildAsync(Consumer<ItemStack> consumer) {
        buildAsync().thenAccept(skull -> Schedulers.sync(() -> consumer.accept(skull)));
    }

    private void applyMeta(ItemStack skull) {
        skull.setAmount(amount);

        ItemMeta meta = skull.getItemMeta();
        if (meta == null) {
            return;
        }

        if (displayName != null) {
            meta.setDisplayName(displayName);
        }

        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }

        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (!flags.isEmpty()) {
            meta.addItemFlags(flags.toArray(new ItemFlag[0]));
        }

        skull.setItemMeta(meta);
    }

    private enum SkullType {
        TEXTURE,
        TEXTURE_URL,
        PLAYER
    }
}

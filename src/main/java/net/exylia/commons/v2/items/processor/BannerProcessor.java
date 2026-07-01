package net.exylia.commons.v2.items.processor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.exylia.commons.v2.compat.ShieldMetaCompat;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.items.config.BannerConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BannerProcessor {

    public static void apply(ItemStack itemStack, BannerConfig bannerConfig, Player player, PlaceholderContext context) {
        if (bannerConfig == null || !bannerConfig.hasConfiguration()) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        DebugAPI.logPluginDebug("[BannerProcessor] item=" + itemStack.getType() + " metaClass=" + meta.getClass().getSimpleName()
                + " isBannerMeta=" + (meta instanceof BannerMeta)
                + " isShieldMeta=" + ShieldMetaCompat.isShieldMeta(meta)
                + " isBlockStateMeta=" + (meta instanceof BlockStateMeta));
        try {
            if (ShieldMetaCompat.isShieldMeta(meta)) {
                bannerConfig.applyToShieldMeta(meta, player, context);
                itemStack.setItemMeta(meta);
            } else if (meta instanceof BannerMeta bannerMeta) {
                bannerConfig.applyToBanner(bannerMeta, player, context);
                itemStack.setItemMeta(bannerMeta);
            } else if (meta instanceof BlockStateMeta blockStateMeta) {
                bannerConfig.applyToShield(blockStateMeta, player, context);
                itemStack.setItemMeta(blockStateMeta);
            } else {
                DebugAPI.logPluginDebug("[BannerProcessor] NO matching branch for " + meta.getClass().getName());
            }
        } catch (Exception e) {
            DebugAPI.logLibWarn("BannerProcessor: Failed to apply banner design - " + e.getMessage());
        }
    }

    public static void apply(ItemStack itemStack, BannerConfig bannerConfig) {
        apply(itemStack, bannerConfig, null, null);
    }
}

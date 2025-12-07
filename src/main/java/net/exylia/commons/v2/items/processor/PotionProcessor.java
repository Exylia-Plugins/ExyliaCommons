package net.exylia.commons.v2.items.processor;

import net.exylia.commons.v2.items.config.PotionConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.List;

public class PotionProcessor {

    public static void apply(ItemStack itemStack, PotionConfig potionConfig,
                           Player player, ExyliaContext context) {
        if (potionConfig == null || !potionConfig.hasConfiguration()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (!(meta instanceof PotionMeta potionMeta)) {
            return;
        }

        if (potionConfig.getBasePotionType() != null) {
            PotionType potionType = potionConfig.createPotionType();
            potionMeta.setBasePotionType(potionType);
        }

        List<PotionEffect> customEffects = potionConfig.createCustomEffects(player, context);
        for (PotionEffect effect : customEffects) {
            potionMeta.addCustomEffect(effect, true);
        }

        Color color = potionConfig.getProcessedColor(player, context);
        if (color != null) {
            potionMeta.setColor(color);
        }

        itemStack.setItemMeta(potionMeta);
    }

    public static void apply(ItemStack itemStack, PotionConfig potionConfig) {
        apply(itemStack, potionConfig, null, null);
    }
}

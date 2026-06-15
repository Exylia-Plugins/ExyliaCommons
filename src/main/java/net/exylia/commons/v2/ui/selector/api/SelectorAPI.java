package net.exylia.commons.v2.ui.selector.api;

import net.exylia.commons.v2.ui.selector.impl.color.BukkitColorSelector;
import net.exylia.commons.v2.ui.selector.impl.effect.PotionEffectEditorSelector;
import net.exylia.commons.v2.ui.selector.impl.effect.PotionEffectSelector;
import net.exylia.commons.v2.ui.selector.impl.reward.RewardEditorSelector;
import org.bukkit.entity.Player;

public final class SelectorAPI {

    private SelectorAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static PotionEffectSelector potionEffect(Player player) {
        return PotionEffectSelector.of(player);
    }

    public static PotionEffectEditorSelector potionEffectEditor(Player player) {
        return PotionEffectEditorSelector.of(player);
    }

    public static RewardEditorSelector rewardEditor(Player player) {
        return RewardEditorSelector.of(player);
    }

    public static BukkitColorSelector color(Player player) {
        return BukkitColorSelector.of(player);
    }
}

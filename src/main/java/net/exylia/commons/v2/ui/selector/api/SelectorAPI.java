package net.exylia.commons.v2.ui.selector.api;

import net.exylia.commons.v2.ui.selector.impl.color.BukkitColorSelector;
import net.exylia.commons.v2.ui.selector.impl.effect.EffectEditorSelector;
import net.exylia.commons.v2.ui.selector.impl.effect.PotionEffectEditorSelector;
import net.exylia.commons.v2.ui.selector.impl.effect.PotionEffectSelector;
import net.exylia.commons.v2.ui.selector.impl.loot.LootEditorSelector;
import net.exylia.commons.v2.ui.selector.impl.namedcommand.NamedCommandEditorSelector;
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

    /**
     * Opens the effect editor: an in-game UI for building a list of
     * {@link net.exylia.commons.v2.effect.model.EffectEntry} (particles, sounds, potions,
     * fireworks, titles, actionbars, messages, sequences) with chance, condition, permission,
     * priority, delay and scope.
     *
     * <p>Persist the saved list with
     * {@link net.exylia.commons.v2.effect.config.EffectSerializer#write}.
     */
    public static EffectEditorSelector effectEditor(Player player) {
        return EffectEditorSelector.of(player);
    }

    public static RewardEditorSelector rewardEditor(Player player) {
        return RewardEditorSelector.of(player);
    }

    public static NamedCommandEditorSelector namedCommandEditor(Player player) {
        return NamedCommandEditorSelector.of(player);
    }

    public static LootEditorSelector lootEditor(Player player) {
        return LootEditorSelector.of(player);
    }

    public static BukkitColorSelector color(Player player) {
        return BukkitColorSelector.of(player);
    }
}

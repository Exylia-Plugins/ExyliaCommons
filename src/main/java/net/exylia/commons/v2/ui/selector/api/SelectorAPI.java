package net.exylia.commons.v2.ui.selector.api;

import net.exylia.commons.v2.ui.selector.impl.color.BukkitColorSelector;
import net.exylia.commons.v2.ui.selector.impl.effect.EffectEditorSelector;
import net.exylia.commons.v2.ui.selector.impl.effect.PotionEffectEditorSelector;
import net.exylia.commons.v2.ui.selector.impl.effect.PotionEffectSelector;
import net.exylia.commons.v2.ui.selector.impl.loot.LootEditorSelector;
import net.exylia.commons.v2.ui.selector.impl.namedcommand.NamedCommandEditorSelector;
import net.exylia.commons.v2.ui.selector.impl.registry.RegistryPickerAPI;
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

    /**
     * Browsable pickers for Minecraft's registries (particles, sounds, potion effects, firework
     * shapes, materials), rendered as a paged and searchable native dialog.
     *
     * <pre>{@code
     * SelectorAPI.registry().particle(player).onPick(entry::setParticle).open();
     * }</pre>
     */
    public static RegistryPickers registry() {
        return RegistryPickers.INSTANCE;
    }

    /** Namespace holder so {@code SelectorAPI.registry().particle(player)} reads naturally. */
    public static final class RegistryPickers {

        private static final RegistryPickers INSTANCE = new RegistryPickers();

        private RegistryPickers() {}

        public RegistryPickerAPI.Picker particle(Player player) {
            return RegistryPickerAPI.particle(player);
        }

        public RegistryPickerAPI.Picker sound(Player player) {
            return RegistryPickerAPI.sound(player);
        }

        public RegistryPickerAPI.Picker potionEffect(Player player) {
            return RegistryPickerAPI.potionEffect(player);
        }

        public RegistryPickerAPI.Picker fireworkShape(Player player) {
            return RegistryPickerAPI.fireworkShape(player);
        }

        public RegistryPickerAPI.Picker material(Player player) {
            return RegistryPickerAPI.material(player);
        }

        public RegistryPickerAPI.Picker block(Player player) {
            return RegistryPickerAPI.block(player);
        }
    }
}

package net.exylia.commons.v2.ui.selector.impl.effect;

import net.exylia.commons.v2.ui.selector.core.AbstractSelector;
import net.exylia.commons.v2.ui.selector.impl.effect.menu.PotionEffectListMenu;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class PotionEffectEditorSelector extends AbstractSelector<List<PotionEffectResult>> {

    private List<PotionEffectResult> initialEffects = new ArrayList<>();

    private PotionEffectEditorSelector(Player player) {
        super(player);
        this.title = "Potion Effects";
    }

    public static PotionEffectEditorSelector of(Player player) {
        return new PotionEffectEditorSelector(player);
    }

    public PotionEffectEditorSelector title(String title) {
        this.title = title;
        return this;
    }

    public PotionEffectEditorSelector effects(List<PotionEffectResult> effects) {
        this.initialEffects = new ArrayList<>(effects);
        return this;
    }

    public PotionEffectEditorSelector onSave(BiConsumer<Player, List<PotionEffectResult>> callback) {
        this.onSelect = callback;
        return this;
    }

    public PotionEffectEditorSelector onCancel(Runnable callback) {
        this.onCancel = callback;
        return this;
    }

    @Override
    public void open() {
        PotionEffectEditorSession session = new PotionEffectEditorSession(
                player,
                initialEffects,
                title,
                onSelect,
                onCancel
        );
        PotionEffectEditorRegistry.getInstance().put(session);
        PotionEffectListMenu.open(player, session);
    }
}

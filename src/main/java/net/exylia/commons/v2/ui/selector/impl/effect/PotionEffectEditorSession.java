package net.exylia.commons.v2.ui.selector.impl.effect;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Getter
public class PotionEffectEditorSession {

    private final Player player;
    private final List<PotionEffectResult> effects;
    private final BiConsumer<Player, List<PotionEffectResult>> onSave;
    private final Runnable onCancel;

    @Setter
    private String title;

    public PotionEffectEditorSession(
            Player player,
            List<PotionEffectResult> initialEffects,
            String title,
            BiConsumer<Player, List<PotionEffectResult>> onSave,
            Runnable onCancel
    ) {
        this.player = player;
        this.effects = new ArrayList<>(initialEffects);
        this.title = title;
        this.onSave = onSave;
        this.onCancel = onCancel;
    }

    public void addEffect(PotionEffectResult effect) {
        effects.add(effect);
    }

    public void removeEffect(int index) {
        if (index >= 0 && index < effects.size()) {
            effects.remove(index);
        }
    }
}

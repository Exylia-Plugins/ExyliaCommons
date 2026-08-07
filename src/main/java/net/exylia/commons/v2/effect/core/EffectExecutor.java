package net.exylia.commons.v2.effect.core;

import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.effect.model.EffectType;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.api.ActionBarAPI;
import net.exylia.commons.v2.visual.api.EffectAPI;
import net.exylia.commons.v2.visual.api.FireworkAPI;
import net.exylia.commons.v2.visual.api.MessageAPI;
import net.exylia.commons.v2.visual.api.ParticleAPI;
import net.exylia.commons.v2.visual.api.SoundAPI;
import net.exylia.commons.v2.visual.api.TitleAPI;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class EffectExecutor {
    private EffectExecutor() {
    }

    public static void execute(Player player, EffectEntry entry) {
        execute(player, entry, null);
    }

    public static void execute(Player player, EffectEntry entry, PlaceholderContext context) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(entry, "entry");
        PlaceholderContext actualContext = context != null
                ? context.withPlayer(player)
                : PlaceholderContext.create().withPlayer(player);

        switch (entry.getType()) {
            case PARTICLE -> ParticleAPI.spawn(player, entry.toParticleConfig(player.getLocation()));
            case SOUND -> SoundAPI.play(player, entry.toSoundConfig(player.getLocation()));
            case POTION -> EffectAPI.apply(player, entry.toPotionConfig());
            case FIREWORK -> FireworkAPI.launch(player.getLocation(), entry.toFireworkConfig(player.getLocation()));
            case TITLE -> TitleAPI.send(player, entry.toTitleConfig(), actualContext);
            case ACTIONBAR -> ActionBarAPI.send(player, entry.toActionBarConfig(), actualContext);
            case MESSAGE -> {
                List<String> lines = entry.messageLines();
                if (entry.isCentered()) MessageAPI.sendCentered(player, lines, actualContext);
                else MessageAPI.send(player, lines, actualContext);
            }
        }
    }

    public static void execute(Player player, List<EffectEntry> entries) {
        execute(player, entries, null);
    }

    public static void execute(Player player, List<EffectEntry> entries, PlaceholderContext context) {
        if (entries == null) return;
        for (EffectEntry entry : entries) {
            if (entry != null) execute(player, entry, context);
        }
    }
}

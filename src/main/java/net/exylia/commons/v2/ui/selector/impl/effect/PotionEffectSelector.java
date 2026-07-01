package net.exylia.commons.v2.ui.selector.impl.effect;

import net.exylia.commons.v2.chat.api.ChatInputAPI;
import net.exylia.commons.v2.ui.selector.core.AbstractSelector;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.StreamSupport;

public final class PotionEffectSelector extends AbstractSelector<PotionEffectResult> {

    private static volatile List<EffectOption> cachedOptions;

    private String levelPrompt = "Level (1-255)";
    private String durationPrompt = "Duration in seconds (-1 = infinite)";

    private PotionEffectSelector(Player player) {
        super(player);
        this.title = "Select Potion Effect";
    }

    public static PotionEffectSelector of(Player player) {
        return new PotionEffectSelector(player);
    }

    public PotionEffectSelector title(String title) {
        this.title = title;
        return this;
    }

    public PotionEffectSelector levelPrompt(String levelPrompt) {
        this.levelPrompt = levelPrompt;
        return this;
    }

    public PotionEffectSelector durationPrompt(String durationPrompt) {
        this.durationPrompt = durationPrompt;
        return this;
    }

    public PotionEffectSelector onSelect(BiConsumer<Player, PotionEffectResult> callback) {
        this.onSelect = callback;
        return this;
    }

    public PotionEffectSelector onCancel(Runnable callback) {
        this.onCancel = callback;
        return this;
    }

    @Override
    public void open() {
        ChatInputAPI.OptionBuilder builder = ChatInputAPI.option(player, title);
        for (EffectOption opt : getEffectOptions()) {
            builder.option(opt.key(), "<color:" + opt.color() + ">" + opt.label());
        }
        builder
                .columns(3)
                .onCancel(onCancel)
                .onResponse(this::handleEffectSelected)
                .ask();
    }

    private void handleEffectSelected(String effectKey) {
        NamespacedKey nsKey = NamespacedKey.fromString(effectKey);
        PotionEffectType effectType = nsKey != null ? Registry.EFFECT.get(nsKey) : null;
        if (effectType == null) {
            if (onCancel != null) onCancel.run();
            return;
        }
        ChatInputAPI.numbers(player, title)
                .field("level", levelPrompt, 1L, 255L)
                .field("duration", durationPrompt, -1L, (long) Integer.MAX_VALUE)
                .onCancel(onCancel)
                .onResponse(values -> {
                    if (onSelect != null) {
                        int level = values.get("level").intValue();
                        int duration = values.get("duration").intValue();
                        onSelect.accept(player, new PotionEffectResult(effectType, level - 1, duration));
                    }
                })
                .ask();
    }

    private static List<EffectOption> getEffectOptions() {
        if (cachedOptions == null) {
            synchronized (PotionEffectSelector.class) {
                if (cachedOptions == null) {
                    cachedOptions = StreamSupport.stream(Registry.EFFECT.spliterator(), false)
                            .sorted(Comparator.comparing(e -> e.getKey().asString()))
                            .map(type -> new EffectOption(
                                    type.getKey().asString(),
                                    formatName(type),
                                    PotionEffectColors.colorFor(type)
                            ))
                            .toList();
                }
            }
        }
        return cachedOptions;
    }

    private static String formatName(PotionEffectType type) {
        String raw = type.getKey().getKey().replace('_', ' ');
        StringBuilder sb = new StringBuilder(raw.length());
        boolean capitalizeNext = true;
        for (char c : raw.toCharArray()) {
            if (c == ' ') {
                sb.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private record EffectOption(String key, String label, String color) {}
}

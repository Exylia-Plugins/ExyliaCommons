package net.exylia.commons.v2.effect.api;

import net.exylia.commons.v2.effect.core.EffectManager;
import net.exylia.commons.v2.effect.model.EffectContext;
import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.effect.model.EffectResult;
import net.exylia.commons.v2.effect.model.EffectScope;
import net.exylia.commons.v2.effect.model.EffectType;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Fluent builder for a single {@link EffectEntry}. Mirrors
 * {@link net.exylia.commons.v2.reward.api.RewardBuilder}.
 *
 * <pre>{@code
 * EffectAPI.builder()
 *     .sound("BLOCK_STONE_BREAK", 1.0f, 1.2f)
 *     .scope(EffectScope.RADIUS).radius(12)
 *     .chance(50)
 *     .play(player);
 * }</pre>
 */
public class EffectBuilder {

    private final EffectEntry entry = new EffectEntry();

    // ------------------------------------------------------------- payloads

    public EffectBuilder particle(String particle) {
        return particle(particle, 1);
    }

    public EffectBuilder particle(String particle, int count) {
        entry.setType(EffectType.PARTICLE);
        entry.setParticle(particle);
        entry.setParticleCount(count);
        return this;
    }

    public EffectBuilder offset(double x, double y, double z) {
        entry.setOffsetX(x);
        entry.setOffsetY(y);
        entry.setOffsetZ(z);
        return this;
    }

    public EffectBuilder extra(double extra) {
        entry.setParticleExtra(extra);
        return this;
    }

    public EffectBuilder color(String color) {
        entry.setParticleColor(color);
        return this;
    }

    public EffectBuilder sound(String sound) {
        return sound(sound, 1.0f, 1.0f);
    }

    public EffectBuilder sound(String sound, float volume, float pitch) {
        entry.setType(EffectType.SOUND);
        entry.setSound(sound);
        entry.setSoundVolume(volume);
        entry.setSoundPitch(pitch);
        return this;
    }

    public EffectBuilder potion(String potion, int amplifier, int durationTicks) {
        entry.setType(EffectType.POTION);
        entry.setPotion(potion);
        entry.setPotionAmplifier(amplifier);
        entry.setPotionDurationTicks(durationTicks);
        return this;
    }

    public EffectBuilder firework(String fireworkType, List<String> colors) {
        entry.setType(EffectType.FIREWORK);
        if (fireworkType != null) entry.setFireworkType(fireworkType.toUpperCase());
        if (colors != null) entry.setFireworkColors(List.copyOf(colors));
        return this;
    }

    public EffectBuilder title(String title, String subtitle) {
        entry.setType(EffectType.TITLE);
        entry.setTitle(title);
        entry.setSubtitle(subtitle);
        return this;
    }

    public EffectBuilder times(int fadeIn, int stay, int fadeOut) {
        entry.setTitleFadeIn(fadeIn);
        entry.setTitleStay(stay);
        entry.setTitleFadeOut(fadeOut);
        return this;
    }

    public EffectBuilder actionbar(String text) {
        entry.setType(EffectType.ACTIONBAR);
        entry.setActionbar(text);
        return this;
    }

    public EffectBuilder message(String message) {
        entry.setType(EffectType.MESSAGE);
        entry.setMessage(message);
        return this;
    }

    public EffectBuilder messages(List<String> messages) {
        entry.setType(EffectType.MESSAGE);
        if (messages != null) entry.setMessages(List.copyOf(messages));
        return this;
    }

    public EffectBuilder centered(boolean centered) {
        entry.setCentered(centered);
        return this;
    }

    public EffectBuilder sequence(List<String> tokens) {
        entry.setType(EffectType.SEQUENCE);
        if (tokens != null) entry.setSequence(List.copyOf(tokens));
        return this;
    }

    // -------------------------------------------------------------- metadata

    public EffectBuilder name(String name) {
        entry.setName(name);
        return this;
    }

    public EffectBuilder icon(String icon) {
        entry.setIcon(icon);
        return this;
    }

    public EffectBuilder chance(double chance) {
        entry.setChance(chance);
        return this;
    }

    public EffectBuilder condition(String condition) {
        entry.setCondition(condition);
        return this;
    }

    public EffectBuilder permission(String permission) {
        entry.setPermission(permission);
        return this;
    }

    public EffectBuilder priority(int priority) {
        entry.setPriority(priority);
        return this;
    }

    /** Delay in ticks before the effect plays. */
    public EffectBuilder delay(long ticks) {
        entry.setDelayTicks(Math.max(0L, ticks));
        return this;
    }

    public EffectBuilder scope(EffectScope scope) {
        entry.setScope(scope);
        return this;
    }

    public EffectBuilder radius(double radius) {
        entry.setScope(EffectScope.RADIUS);
        entry.setRadius(radius);
        return this;
    }

    // ----------------------------------------------------------------- build

    public EffectEntry build() {
        if (!entry.isPlayable()) {
            throw new IllegalStateException("Effect payload is not set for type " + entry.getType());
        }
        return entry.copy();
    }

    public EffectResult play(Player player) {
        return play(player, PlaceholderContext.create());
    }

    public EffectResult play(Player player, PlaceholderContext placeholders) {
        return EffectManager.getInstance().playSingle(build(), EffectContext.of(player, placeholders));
    }

    public EffectResult playAt(Location location) {
        return EffectManager.getInstance().playSingle(build(), EffectContext.at(location));
    }
}

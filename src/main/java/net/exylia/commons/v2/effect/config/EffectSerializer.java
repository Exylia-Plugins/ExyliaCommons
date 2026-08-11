package net.exylia.commons.v2.effect.config;

import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.effect.model.EffectScope;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes {@link EffectEntry} back to the typed-list YAML shape understood by
 * {@link EffectConfigLoader}. Used by the effect editor UI to persist changes.
 *
 * <p>Only non-default values are written, so generated YAML stays readable.
 */
public final class EffectSerializer {

    private EffectSerializer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Writes the entries under {@code key} of {@code section}, replacing any previous value. */
    public static void write(ConfigurationSection section, String key, @Nullable List<EffectEntry> entries) {
        if (section == null || key == null) return;
        if (entries == null || entries.isEmpty()) {
            section.set(key, null);
            return;
        }
        section.set(key, serialize(entries));
    }

    /** @see #write(ConfigurationSection, String, List) */
    public static void write(ConfigurationSection section, @Nullable List<EffectEntry> entries) {
        write(section, EffectConfigLoader.DEFAULT_KEY, entries);
    }

    public static List<Map<String, Object>> serialize(@Nullable List<EffectEntry> entries) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (entries == null) return result;
        for (EffectEntry entry : entries) {
            if (entry != null) result.add(serialize(entry));
        }
        return result;
    }

    public static Map<String, Object> serialize(EffectEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", entry.getType().name());

        putIfPresent(map, "name", entry.getName());
        putIfPresent(map, "icon", entry.getIcon());

        if (entry.getChance() != 100.0) map.put("chance", entry.getChance());
        putIfPresent(map, "condition", entry.getCondition());
        putIfPresent(map, "permission", entry.getPermission());
        if (entry.getPriority() != 0) map.put("priority", entry.getPriority());
        if (entry.getDelayTicks() > 0) map.put("delay-ticks", entry.getDelayTicks());

        EffectScope scope = entry.getScope();
        if (scope != null && scope != EffectScope.PLAYER) {
            map.put("scope", scope.name());
            if (scope == EffectScope.RADIUS) map.put("radius", entry.getRadius());
        }

        switch (entry.getType()) {
            case PARTICLE -> serializeParticle(entry, map);
            case SOUND -> serializeSound(entry, map);
            case POTION -> serializePotion(entry, map);
            case FIREWORK -> serializeFirework(entry, map);
            case TITLE -> serializeTitle(entry, map);
            case ACTIONBAR -> putIfPresent(map, "actionbar", entry.getActionbar());
            case MESSAGE -> serializeMessage(entry, map);
            case SEQUENCE -> putIfNotEmpty(map, "sequence", entry.getSequence());
        }

        return map;
    }

    private static void serializeParticle(EffectEntry entry, Map<String, Object> map) {
        putIfPresent(map, "particle", entry.getParticle());
        if (entry.getParticleCount() != 1) map.put("count", entry.getParticleCount());
        if (entry.getOffsetX() != 0.0) map.put("offset-x", entry.getOffsetX());
        if (entry.getOffsetY() != 0.0) map.put("offset-y", entry.getOffsetY());
        if (entry.getOffsetZ() != 0.0) map.put("offset-z", entry.getOffsetZ());
        if (entry.getParticleExtra() != 0.0) map.put("extra", entry.getParticleExtra());
        putIfPresent(map, "color", entry.getParticleColor());
        if (entry.getDustSize() != 1.0f) map.put("dust-size", entry.getDustSize());
        putIfPresent(map, "block-material", entry.getParticleBlockMaterial());
    }

    private static void serializeSound(EffectEntry entry, Map<String, Object> map) {
        putIfPresent(map, "sound", entry.getSound());
        if (entry.getSoundVolume() != 1.0f) map.put("volume", entry.getSoundVolume());
        if (entry.getSoundPitch() != 1.0f) map.put("pitch", entry.getSoundPitch());
    }

    private static void serializePotion(EffectEntry entry, Map<String, Object> map) {
        putIfPresent(map, "potion", entry.getPotion());
        if (entry.getPotionAmplifier() != 0) map.put("amplifier", entry.getPotionAmplifier());
        if (entry.getPotionDurationTicks() != 200) map.put("duration-ticks", entry.getPotionDurationTicks());
        if (entry.isPotionAmbient()) map.put("ambient", true);
        if (!entry.isPotionParticles()) map.put("particles", false);
        if (!entry.isPotionIcon()) map.put("show-icon", false);
    }

    private static void serializeFirework(EffectEntry entry, Map<String, Object> map) {
        putIfPresent(map, "firework-type", entry.getFireworkType());
        putIfNotEmpty(map, "colors", entry.getFireworkColors());
        putIfNotEmpty(map, "fade-colors", entry.getFireworkFadeColors());
        if (entry.isFireworkFlicker()) map.put("flicker", true);
        if (entry.isFireworkTrail()) map.put("trail", true);
        if (entry.getFireworkPower() != 1) map.put("power", entry.getFireworkPower());
    }

    private static void serializeTitle(EffectEntry entry, Map<String, Object> map) {
        putIfPresent(map, "title", entry.getTitle());
        putIfPresent(map, "subtitle", entry.getSubtitle());
        if (entry.getTitleFadeIn() != 10) map.put("fade-in", entry.getTitleFadeIn());
        if (entry.getTitleStay() != 70) map.put("stay", entry.getTitleStay());
        if (entry.getTitleFadeOut() != 20) map.put("fade-out", entry.getTitleFadeOut());
    }

    private static void serializeMessage(EffectEntry entry, Map<String, Object> map) {
        List<String> messages = entry.getMessages();
        if (messages != null && !messages.isEmpty()) {
            map.put("messages", new ArrayList<>(messages));
        } else {
            putIfPresent(map, "message", entry.getMessage());
        }
        if (entry.isCentered()) map.put("centered", true);
    }

    private static void putIfPresent(Map<String, Object> map, String key, @Nullable String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }

    private static void putIfNotEmpty(Map<String, Object> map, String key, @Nullable List<String> values) {
        if (values != null && !values.isEmpty()) map.put(key, new ArrayList<>(values));
    }
}

package net.exylia.commons.v2.effect.config;

import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.effect.model.EffectScope;
import net.exylia.commons.v2.effect.model.EffectType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses {@link EffectEntry} lists from YAML. Mirrors
 * {@link net.exylia.commons.v2.reward.config.RewardConfigLoader}.
 *
 * <p>Three shapes are accepted under the configured key (default {@code effects}):
 *
 * <ol>
 *   <li><b>Grouped</b> — a section with {@code particles:}/{@code sounds:}/{@code titles:}/
 *       {@code actionbars:}/{@code messages:}/{@code potions:}/{@code fireworks:}/
 *       {@code sequence:} lists.</li>
 *   <li><b>Typed list</b> — a list of maps each carrying a {@code type} field.</li>
 *   <li><b>Inline strings</b> — {@code "sound: BLOCK_STONE_BREAK|1.0|1.2"}, or a bare
 *       {@code "[CIRCLE] FLAME;radius:1.2"} sequence token.</li>
 * </ol>
 */
public class EffectConfigLoader {

    public static final String DEFAULT_KEY = "effects";

    /** Loads from the {@code effects} key of the given section. */
    public List<EffectEntry> load(@Nullable ConfigurationSection section) {
        return loadFromKey(section, DEFAULT_KEY);
    }

    /** Loads from an arbitrary key of the given section. */
    public List<EffectEntry> loadFromKey(@Nullable ConfigurationSection section, String key) {
        List<EffectEntry> entries = new ArrayList<>();
        if (section == null || key == null) return entries;

        if (section.isList(key)) {
            entries.addAll(loadList(section.getList(key)));
        } else if (section.isConfigurationSection(key)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child != null) entries.addAll(loadGrouped(child));
        }

        return entries;
    }

    /**
     * Resolves effects with per-variant override semantics: if {@code variantKey} declares its own
     * effects they fully replace the global ones; otherwise the global list is used.
     *
     * <p>This is the "global or per-block" resolution used by mine/loot style plugins:
     *
     * <pre>{@code
     * effects:
     *   sounds:
     *     - sound: BLOCK_STONE_BREAK
     * blocks:
     *   DIAMOND_ORE:
     *     effects:
     *       sounds:
     *         - sound: ENTITY_PLAYER_LEVELUP
     * }</pre>
     *
     * @param root       section holding the global {@code effects} key
     * @param variants   name of the section holding per-variant overrides (e.g. {@code blocks})
     * @param variantKey the variant to resolve (e.g. {@code DIAMOND_ORE})
     */
    public List<EffectEntry> resolve(
            @Nullable ConfigurationSection root,
            String variants,
            @Nullable String variantKey
    ) {
        return resolve(root, variants, variantKey, DEFAULT_KEY);
    }

    /** @see #resolve(ConfigurationSection, String, String) */
    public List<EffectEntry> resolve(
            @Nullable ConfigurationSection root,
            String variants,
            @Nullable String variantKey,
            String effectsKey
    ) {
        if (root == null) return List.of();

        if (variantKey != null && variants != null) {
            ConfigurationSection variantsSection = root.getConfigurationSection(variants);
            if (variantsSection != null) {
                ConfigurationSection variant = findVariant(variantsSection, variantKey);
                if (variant != null && variant.contains(effectsKey)) {
                    return loadFromKey(variant, effectsKey);
                }
            }
        }

        return loadFromKey(root, effectsKey);
    }

    /** Case-insensitive variant lookup, so {@code diamond_ore} matches {@code DIAMOND_ORE}. */
    @Nullable
    private ConfigurationSection findVariant(ConfigurationSection variants, String key) {
        ConfigurationSection direct = variants.getConfigurationSection(key);
        if (direct != null) return direct;
        for (String candidate : variants.getKeys(false)) {
            if (candidate.equalsIgnoreCase(key)) {
                return variants.getConfigurationSection(candidate);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ list

    private List<EffectEntry> loadList(@Nullable List<?> raw) {
        List<EffectEntry> entries = new ArrayList<>();
        if (raw == null) return entries;

        for (Object element : raw) {
            EffectEntry entry = loadElement(element);
            if (entry != null) entries.add(entry);
        }
        return entries;
    }

    @Nullable
    private EffectEntry loadElement(@Nullable Object element) {
        if (element instanceof ConfigurationSection section) {
            return loadTyped(toMap(section));
        }
        if (element instanceof Map<?, ?> map) {
            return loadTyped(normalize(map));
        }
        if (element instanceof String raw) {
            return parseInline(raw);
        }
        return null;
    }

    // --------------------------------------------------------------- grouped

    private List<EffectEntry> loadGrouped(ConfigurationSection section) {
        List<EffectEntry> entries = new ArrayList<>();

        entries.addAll(loadGroup(section, "particles", EffectType.PARTICLE));
        entries.addAll(loadGroup(section, "sounds", EffectType.SOUND));
        entries.addAll(loadGroup(section, "potions", EffectType.POTION));
        entries.addAll(loadGroup(section, "fireworks", EffectType.FIREWORK));
        entries.addAll(loadGroup(section, "titles", EffectType.TITLE));
        entries.addAll(loadGroup(section, "actionbars", EffectType.ACTIONBAR));
        entries.addAll(loadGroup(section, "messages", EffectType.MESSAGE));

        // A `sequence:` group is a single entry holding a token list, not one entry per token.
        if (section.isList("sequence")) {
            List<String> tokens = section.getStringList("sequence");
            if (!tokens.isEmpty()) entries.add(EffectEntry.sequence(tokens));
        }

        return entries;
    }

    private List<EffectEntry> loadGroup(ConfigurationSection section, String key, EffectType type) {
        List<EffectEntry> entries = new ArrayList<>();
        if (!section.isList(key)) return entries;

        List<?> raw = section.getList(key);
        if (raw == null) return entries;

        for (Object element : raw) {
            if (element instanceof ConfigurationSection child) {
                entries.add(apply(new LinkedHashMap<>(toMap(child)), type));
            } else if (element instanceof Map<?, ?> map) {
                entries.add(apply(normalize(map), type));
            } else if (element instanceof String value) {
                entries.add(shorthand(type, value));
            }
        }
        return entries;
    }

    /** Builds an entry from the shorthand string form of a group, e.g. {@code FLAME|10}. */
    private EffectEntry shorthand(EffectType type, String value) {
        Map<String, Object> map = new LinkedHashMap<>();
        switch (type) {
            case PARTICLE -> {
                String[] parts = value.split("\\|");
                map.put("particle", parts[0].trim());
                if (parts.length >= 2) map.put("count", parts[1].trim());
                if (parts.length >= 3) map.put("offset-x", parts[2].trim());
                if (parts.length >= 4) map.put("offset-y", parts[3].trim());
                if (parts.length >= 5) map.put("offset-z", parts[4].trim());
                if (parts.length >= 6) map.put("extra", parts[5].trim());
                if (parts.length >= 7) map.put("color", parts[6].trim());
            }
            case SOUND -> {
                String[] parts = value.split("\\|");
                map.put("sound", parts[0].trim());
                if (parts.length >= 2) map.put("volume", parts[1].trim());
                if (parts.length >= 3) map.put("pitch", parts[2].trim());
            }
            case POTION -> {
                String[] parts = value.split("\\|");
                map.put("potion", parts[0].trim());
                if (parts.length >= 2) map.put("amplifier", parts[1].trim());
                if (parts.length >= 3) map.put("duration", parts[2].trim());
            }
            case TITLE -> {
                String[] parts = value.split("\\|", 2);
                map.put("title", parts[0].trim());
                if (parts.length >= 2) map.put("subtitle", parts[1].trim());
            }
            case ACTIONBAR -> map.put("actionbar", value);
            case MESSAGE -> map.put("message", value);
            case FIREWORK -> map.put("firework-type", value);
            case SEQUENCE -> map.put("sequence", List.of(value));
        }
        return apply(map, type);
    }

    // ----------------------------------------------------------------- typed

    private EffectEntry loadTyped(Map<String, Object> map) {
        EffectType type = EffectType.fromName(string(map, null, "type"));
        if (type == null) type = inferType(map);
        return apply(map, type);
    }

    /** Infers the type from whichever payload field is present. */
    private EffectType inferType(Map<String, Object> map) {
        if (map.containsKey("particle")) return EffectType.PARTICLE;
        if (map.containsKey("sound")) return EffectType.SOUND;
        if (map.containsKey("potion")) return EffectType.POTION;
        if (map.containsKey("sequence")) return EffectType.SEQUENCE;
        if (map.containsKey("title") || map.containsKey("subtitle")) return EffectType.TITLE;
        if (map.containsKey("actionbar")) return EffectType.ACTIONBAR;
        if (map.containsKey("firework-type") || map.containsKey("firework_type")) return EffectType.FIREWORK;
        return EffectType.MESSAGE;
    }

    // ---------------------------------------------------------------- inline

    /**
     * Parses the inline string form. A leading {@code [TOKEN]} is treated as a sequence step;
     * a {@code type:} prefix selects the type; anything else defaults to a message.
     */
    @Nullable
    private EffectEntry parseInline(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();

        if (value.startsWith("[")) {
            return EffectEntry.sequence(List.of(value));
        }

        int separator = value.indexOf(':');
        if (separator > 0) {
            EffectType type = EffectType.fromName(value.substring(0, separator));
            if (type != null) {
                return shorthand(type, value.substring(separator + 1).trim());
            }
        }

        return EffectEntry.message(value);
    }

    // ------------------------------------------------------------- field map

    /** Applies every recognised key of {@code map} onto a fresh entry of the given type. */
    private EffectEntry apply(Map<String, Object> map, EffectType type) {
        EffectEntry entry = new EffectEntry();
        entry.setType(type);

        entry.setName(string(map, entry.getName(), "name", "display-name", "display_name"));
        entry.setIcon(string(map, entry.getIcon(), "icon", "material"));

        entry.setChance(number(map, 100.0, "chance", "probability").doubleValue());
        entry.setCondition(string(map, null, "condition", "requires"));
        entry.setPermission(string(map, null, "permission", "perm"));
        entry.setPriority(number(map, 0, "priority", "order").intValue());
        entry.setDelayTicks(resolveDelayTicks(map));

        EffectScope scope = EffectScope.fromName(string(map, null, "scope", "target", "audience"));
        if (scope != null) entry.setScope(scope);
        entry.setRadius(number(map, entry.getRadius(), "radius", "range").doubleValue());

        switch (type) {
            case PARTICLE -> applyParticle(map, entry);
            case SOUND -> applySound(map, entry);
            case POTION -> applyPotion(map, entry);
            case FIREWORK -> applyFirework(map, entry);
            case TITLE -> applyTitle(map, entry);
            case ACTIONBAR -> entry.setActionbar(string(map, null, "actionbar", "action-bar", "action_bar", "text", "value"));
            case MESSAGE -> applyMessage(map, entry);
            case SEQUENCE -> entry.setSequence(stringList(map, "sequence", "steps", "tokens", "effects"));
        }

        return entry;
    }

    private void applyParticle(Map<String, Object> map, EffectEntry entry) {
        entry.setParticle(string(map, null, "particle", "value"));
        entry.setParticleCount(number(map, 1, "count", "amount", "particle-count", "particle_count").intValue());
        entry.setOffsetX(number(map, 0.0, "offset-x", "offset_x", "offsetX").doubleValue());
        entry.setOffsetY(number(map, 0.0, "offset-y", "offset_y", "offsetY").doubleValue());
        entry.setOffsetZ(number(map, 0.0, "offset-z", "offset_z", "offsetZ").doubleValue());
        entry.setParticleExtra(number(map, 0.0, "extra", "speed").doubleValue());
        entry.setParticleColor(string(map, null, "color", "particle-color", "particle_color"));
        entry.setDustSize(number(map, 1.0f, "dust-size", "dust_size", "size").floatValue());
        entry.setParticleBlockMaterial(string(map, null, "block-material", "block_material", "block"));
    }

    private void applySound(Map<String, Object> map, EffectEntry entry) {
        entry.setSound(string(map, null, "sound", "value"));
        entry.setSoundVolume(number(map, 1.0f, "volume", "vol").floatValue());
        entry.setSoundPitch(number(map, 1.0f, "pitch").floatValue());
    }

    private void applyPotion(Map<String, Object> map, EffectEntry entry) {
        entry.setPotion(string(map, null, "potion", "effect", "value"));
        entry.setPotionAmplifier(number(map, 0, "amplifier", "level", "power").intValue());
        entry.setPotionDurationTicks(resolvePotionDuration(map));
        entry.setPotionAmbient(bool(map, false, "ambient"));
        entry.setPotionParticles(bool(map, true, "particles", "show-particles", "show_particles"));
        entry.setPotionIcon(bool(map, true, "show-icon", "show_icon", "potion-icon", "potion_icon"));
    }

    private void applyFirework(Map<String, Object> map, EffectEntry entry) {
        String fireworkType = string(map, null, "firework-type", "firework_type", "shape", "value");
        if (fireworkType != null) entry.setFireworkType(fireworkType.toUpperCase());
        entry.setFireworkColors(stringList(map, "colors", "firework-colors", "firework_colors"));
        entry.setFireworkFadeColors(stringList(map, "fade-colors", "fade_colors", "fade"));
        entry.setFireworkFlicker(bool(map, false, "flicker", "twinkle"));
        entry.setFireworkTrail(bool(map, false, "trail"));
        entry.setFireworkPower(number(map, 1, "power").intValue());
    }

    private void applyTitle(Map<String, Object> map, EffectEntry entry) {
        entry.setTitle(string(map, null, "title", "value"));
        entry.setSubtitle(string(map, null, "subtitle", "sub-title", "sub_title"));
        entry.setTitleFadeIn(number(map, 10, "fade-in", "fade_in", "fadeIn").intValue());
        entry.setTitleStay(number(map, 70, "stay", "duration").intValue());
        entry.setTitleFadeOut(number(map, 20, "fade-out", "fade_out", "fadeOut").intValue());
    }

    private void applyMessage(Map<String, Object> map, EffectEntry entry) {
        List<String> lines = stringList(map, "messages", "lines");
        if (!lines.isEmpty()) {
            entry.setMessages(lines);
        } else {
            entry.setMessage(string(map, null, "message", "text", "value"));
        }
        entry.setCentered(bool(map, false, "centered", "center"));
    }

    /** Reads {@code delay} (seconds, decimal) or {@code delay-ticks} (whole ticks). */
    private long resolveDelayTicks(Map<String, Object> map) {
        Object ticks = first(map, "delay-ticks", "delay_ticks", "delayTicks");
        if (ticks != null) return Math.max(0L, toNumber(ticks, 0L).longValue());

        Object seconds = first(map, "delay", "delay-seconds", "delay_seconds");
        if (seconds != null) return Math.max(0L, Math.round(toNumber(seconds, 0.0).doubleValue() * 20.0));

        return 0L;
    }

    /** Reads {@code duration-ticks} or {@code duration} (seconds). */
    private int resolvePotionDuration(Map<String, Object> map) {
        Object ticks = first(map, "duration-ticks", "duration_ticks", "durationTicks");
        if (ticks != null) return Math.max(1, toNumber(ticks, 200).intValue());

        Object seconds = first(map, "duration", "duration-seconds", "duration_seconds");
        if (seconds != null) return Math.max(1, (int) Math.round(toNumber(seconds, 10.0).doubleValue() * 20.0));

        return 200;
    }

    // ---------------------------------------------------------------- access

    /** Keys are normalized to lowercase on load, so lookups normalize too. */
    @Nullable
    private Object first(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value == null) value = map.get(key.toLowerCase());
            if (value != null) return value;
        }
        return null;
    }

    @Nullable
    private String string(Map<String, Object> map, @Nullable String fallback, String... keys) {
        Object value = first(map, keys);
        if (value == null) return fallback;
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private Number number(Map<String, Object> map, Number fallback, String... keys) {
        Object value = first(map, keys);
        return value == null ? fallback : toNumber(value, fallback);
    }

    private Number toNumber(Object value, Number fallback) {
        if (value instanceof Number number) return number;
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean bool(Map<String, Object> map, boolean fallback, String... keys) {
        Object value = first(map, keys);
        if (value == null) return fallback;
        if (value instanceof Boolean flag) return flag;
        String text = String.valueOf(value).trim();
        if (text.equalsIgnoreCase("true")) return true;
        if (text.equalsIgnoreCase("false")) return false;
        return fallback;
    }

    private List<String> stringList(Map<String, Object> map, String... keys) {
        Object value = first(map, keys);
        if (value == null) return new ArrayList<>();
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object element : list) {
                if (element != null) result.add(String.valueOf(element));
            }
            return result;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? new ArrayList<>() : new ArrayList<>(List.of(text));
    }

    private Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            map.put(key.toLowerCase(), section.get(key));
        }
        return map;
    }

    private Map<String, Object> normalize(Map<?, ?> raw) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() == null) continue;
            map.put(String.valueOf(entry.getKey()).toLowerCase(), entry.getValue());
        }
        return map;
    }
}

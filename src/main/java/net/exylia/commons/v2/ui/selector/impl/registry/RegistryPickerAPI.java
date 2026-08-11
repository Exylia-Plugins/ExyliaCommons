package net.exylia.commons.v2.ui.selector.impl.registry;

import net.exylia.commons.v2.chat.api.ChatInputAPI;
import net.exylia.commons.v2.compat.ParticleCompat;
import net.exylia.commons.v2.compat.SoundCompat;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Browsable pickers for Minecraft's built-in registries: particles, sounds, potion effects,
 * firework shapes and materials.
 *
 * <p>Rendered as a native Minecraft dialog (a grid of buttons) with paging and a search box, so
 * an admin never has to remember an enum id. Falls back to a paged inventory on clients that
 * cannot use dialogs.
 *
 * <pre>{@code
 * RegistryPickerAPI.particle(player)
 *     .onPick(name -> entry.setParticle(name))
 *     .onCancel(() -> EffectEditMenu.open(player, entry))
 *     .open();
 * }</pre>
 *
 * <p>The picked value is always the raw registry name (e.g. {@code HAPPY_VILLAGER}), which is
 * exactly what {@link net.exylia.commons.v2.effect.model.EffectEntry} and the visual APIs expect.
 */
public final class RegistryPickerAPI {

    private RegistryPickerAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registry contents never change at runtime, and validating ~1539 sounds on every open would
     * be wasted work on the main thread, so each list is built once and reused. The picker copies
     * it before mutating, so {@link Picker#filter} cannot corrupt the cache.
     */
    private static final Map<String, List<Choice>> CACHE = new ConcurrentHashMap<>();

    private static List<Choice> cached(String key, Supplier<List<Choice>> supplier) {
        return CACHE.computeIfAbsent(key, ignored -> supplier.get());
    }

    /** Clears the cached registry snapshots. Only needed if a plugin registers new entries. */
    public static void invalidateCache() {
        CACHE.clear();
    }

    public static Picker particle(Player player) {
        return new Picker(player, "{primary}&lPARTICLE {muted}» Select a type",
                cached("particle", RegistryPickerAPI::collectParticles));
    }

    public static Picker sound(Player player) {
        return new Picker(player, "{primary}&lSOUND {muted}» Select a sound",
                cached("sound", RegistryPickerAPI::collectSounds));
    }

    public static Picker potionEffect(Player player) {
        return new Picker(player, "{primary}&lPOTION EFFECT {muted}» Select an effect",
                cached("potion", RegistryPickerAPI::collectPotionEffects));
    }

    public static Picker fireworkShape(Player player) {
        return new Picker(player, "{primary}&lFIREWORK {muted}» Select a shape",
                cached("firework", RegistryPickerAPI::collectFireworkShapes));
    }

    /** Every material. Use {@link #block(Player)} for placeable blocks only. */
    public static Picker material(Player player) {
        return new Picker(player, "{primary}&lMATERIAL {muted}» Select a material",
                cached("material", () -> collectMaterials(false)));
    }

    public static Picker block(Player player) {
        return new Picker(player, "{primary}&lBLOCK {muted}» Select a block",
                cached("block", () -> collectMaterials(true)));
    }

    /** A picker over an arbitrary list of values, reusing the same paged/searchable dialog. */
    public static Picker of(Player player, String title, List<String> values) {
        List<Choice> choices = new ArrayList<>(values.size());
        for (String value : values) {
            if (value != null) choices.add(new Choice(value, prettify(value)));
        }
        return new Picker(player, title, choices);
    }

    // ------------------------------------------------------------- collection

    private static List<Choice> collectParticles() {
        List<Choice> choices = new ArrayList<>();
        for (Particle particle : Particle.values()) {
            // Some enum constants exist without a usable registry entry on certain versions.
            if (ParticleCompat.fromName(particle.name()) == null) continue;
            choices.add(new Choice(particle.name(), prettify(particle.name())));
        }
        return sorted(choices);
    }

    private static List<Choice> collectSounds() {
        List<Choice> choices = new ArrayList<>();
        for (Sound sound : Sound.values()) {
            if (SoundCompat.fromName(sound.name()) == null) continue;
            choices.add(new Choice(sound.name(), prettify(sound.name())));
        }
        return sorted(choices);
    }

    private static List<Choice> collectPotionEffects() {
        List<Choice> choices = new ArrayList<>();
        for (PotionEffectType type : Registry.EFFECT) {
            if (type == null) continue;
            String name = type.getKey().getKey().toUpperCase(Locale.ROOT);
            choices.add(new Choice(name, prettify(name)));
        }
        return sorted(choices);
    }

    private static List<Choice> collectFireworkShapes() {
        List<Choice> choices = new ArrayList<>();
        for (FireworkEffect.Type type : FireworkEffect.Type.values()) {
            choices.add(new Choice(type.name(), prettify(type.name())));
        }
        return sorted(choices);
    }

    private static List<Choice> collectMaterials(boolean blocksOnly) {
        List<Choice> choices = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isLegacy()) continue;
            if (blocksOnly && !material.isBlock()) continue;
            choices.add(new Choice(material.name(), prettify(material.name())));
        }
        return sorted(choices);
    }

    private static List<Choice> sorted(List<Choice> choices) {
        choices.sort((a, b) -> a.label().compareToIgnoreCase(b.label()));
        return choices;
    }

    /** {@code HAPPY_VILLAGER} to {@code Happy Villager}, so the list reads like the vanilla UI. */
    static String prettify(String raw) {
        String[] words = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder(raw.length());
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
        }
        return out.length() == 0 ? raw : out.toString();
    }

    record Choice(String key, String label) {}

    // ---------------------------------------------------------------- builder

    public static final class Picker {

        /** Three columns matches the vanilla-style layout and keeps labels readable. */
        private static final int DEFAULT_COLUMNS = 3;
        private static final int DEFAULT_PAGE_SIZE = 45;

        private final Player player;
        private final List<Choice> choices;
        private String title;
        private int columns = DEFAULT_COLUMNS;
        private int pageSize = DEFAULT_PAGE_SIZE;
        private Consumer<String> onPick;
        private Runnable onCancel;

        private Picker(Player player, String title, List<Choice> choices) {
            this.player = player;
            this.title = title;
            // Defensive copy: filter() mutates this list and must not touch the shared cache.
            this.choices = new ArrayList<>(choices);
        }

        public Picker title(String title) {
            this.title = title;
            return this;
        }

        public Picker columns(int columns) {
            this.columns = Math.max(1, columns);
            return this;
        }

        public Picker pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /** Receives the raw registry name, e.g. {@code HAPPY_VILLAGER}. */
        public Picker onPick(Consumer<String> onPick) {
            this.onPick = onPick;
            return this;
        }

        public Picker onCancel(Runnable onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        /** Keeps only the values matching a predicate, e.g. to hide unsupported entries. */
        public Picker filter(java.util.function.Predicate<String> predicate) {
            choices.removeIf(choice -> !predicate.test(choice.key()));
            return this;
        }

        public void open() {
            ChatInputAPI.option(player, title)
                    .options(choices, Choice::key, Choice::label)
                    .columns(columns)
                    .pageSize(pageSize)
                    .searchable()
                    .onResponse(key -> {
                        if (onPick != null) onPick.accept(key);
                    })
                    .onCancel(() -> {
                        if (onCancel != null) onCancel.run();
                    })
                    .ask();
        }
    }
}

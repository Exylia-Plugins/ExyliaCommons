package net.exylia.commons.v2.effect.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.exylia.commons.v2.compat.ParticleCompat;
import net.exylia.commons.v2.compat.PotionEffectTypeCompat;
import net.exylia.commons.v2.compat.SoundCompat;
import net.exylia.commons.v2.visual.builder.ActionBarBuilder;
import net.exylia.commons.v2.visual.builder.EffectBuilder;
import net.exylia.commons.v2.visual.builder.FireworkBuilder;
import net.exylia.commons.v2.visual.builder.ParticleBuilder;
import net.exylia.commons.v2.visual.builder.SoundBuilder;
import net.exylia.commons.v2.visual.builder.TitleBuilder;
import net.exylia.commons.v2.visual.config.ActionBarConfig;
import net.exylia.commons.v2.visual.config.EffectConfig;
import net.exylia.commons.v2.visual.config.FireworkConfig;
import net.exylia.commons.v2.visual.config.ParticleConfig;
import net.exylia.commons.v2.visual.config.SoundConfig;
import net.exylia.commons.v2.visual.config.TitleConfig;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectEntry {
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Builder.Default
    private EffectType type = EffectType.MESSAGE;

    private String name;

    private String particle;
    @Builder.Default
    private int particleCount = 1;
    @Builder.Default
    private double offsetX = 0.0;
    @Builder.Default
    private double offsetY = 0.0;
    @Builder.Default
    private double offsetZ = 0.0;
    @Builder.Default
    private double particleExtra = 0.0;
    private String particleColor;
    @Builder.Default
    private float dustSize = 1.0f;
    private String particleBlockMaterial;
    @Builder.Default
    private ParticleConfig.ParticleScope particleScope = ParticleConfig.ParticleScope.PLAYER;

    private String sound;
    @Builder.Default
    private float soundVolume = 1.0f;
    @Builder.Default
    private float soundPitch = 1.0f;
    @Builder.Default
    private SoundConfig.SoundScope soundScope = SoundConfig.SoundScope.PLAYER;

    private String potion;
    @Builder.Default
    private int potionAmplifier = 0;
    @Builder.Default
    private int potionDurationTicks = 200;
    @Builder.Default
    private boolean potionAmbient = false;
    @Builder.Default
    private boolean potionParticles = true;
    @Builder.Default
    private boolean potionIcon = true;

    @Builder.Default
    private String fireworkType = FireworkEffect.Type.BALL.name();
    @Builder.Default
    private List<String> fireworkColors = new ArrayList<>();
    @Builder.Default
    private List<String> fireworkFadeColors = new ArrayList<>();
    @Builder.Default
    private boolean fireworkFlicker = false;
    @Builder.Default
    private boolean fireworkTrail = false;
    @Builder.Default
    private int fireworkPower = 1;

    private String title;
    private String subtitle;
    @Builder.Default
    private int titleFadeIn = 10;
    @Builder.Default
    private int titleStay = 70;
    @Builder.Default
    private int titleFadeOut = 20;

    private String actionbar;
    private String message;
    @Builder.Default
    private List<String> messages = new ArrayList<>();
    @Builder.Default
    private boolean centered = false;

    public static EffectEntry message(String message) {
        return builder().type(EffectType.MESSAGE).message(message).build();
    }

    public static EffectEntry actionbar(String text) {
        return builder().type(EffectType.ACTIONBAR).actionbar(text).build();
    }

    public static EffectEntry title(String title, String subtitle) {
        return builder().type(EffectType.TITLE).title(title).subtitle(subtitle).build();
    }

    public static EffectEntry particle(String particle) {
        return builder().type(EffectType.PARTICLE).particle(particle).build();
    }

    public static EffectEntry sound(String sound) {
        return builder().type(EffectType.SOUND).sound(sound).build();
    }

    public static EffectEntry potion(String potion) {
        return builder().type(EffectType.POTION).potion(potion).build();
    }

    public static EffectEntry firework() {
        return builder().type(EffectType.FIREWORK).build();
    }

    public EffectEntry copy() {
        EffectEntry copy = new EffectEntry();
        copy.id = UUID.randomUUID().toString();
        copy.type = type;
        copy.name = name;
        copy.particle = particle;
        copy.particleCount = particleCount;
        copy.offsetX = offsetX;
        copy.offsetY = offsetY;
        copy.offsetZ = offsetZ;
        copy.particleExtra = particleExtra;
        copy.particleColor = particleColor;
        copy.dustSize = dustSize;
        copy.particleBlockMaterial = particleBlockMaterial;
        copy.particleScope = particleScope;
        copy.sound = sound;
        copy.soundVolume = soundVolume;
        copy.soundPitch = soundPitch;
        copy.soundScope = soundScope;
        copy.potion = potion;
        copy.potionAmplifier = potionAmplifier;
        copy.potionDurationTicks = potionDurationTicks;
        copy.potionAmbient = potionAmbient;
        copy.potionParticles = potionParticles;
        copy.potionIcon = potionIcon;
        copy.fireworkType = fireworkType;
        copy.fireworkColors = copyList(fireworkColors);
        copy.fireworkFadeColors = copyList(fireworkFadeColors);
        copy.fireworkFlicker = fireworkFlicker;
        copy.fireworkTrail = fireworkTrail;
        copy.fireworkPower = fireworkPower;
        copy.title = title;
        copy.subtitle = subtitle;
        copy.titleFadeIn = titleFadeIn;
        copy.titleStay = titleStay;
        copy.titleFadeOut = titleFadeOut;
        copy.actionbar = actionbar;
        copy.message = message;
        copy.messages = copyList(messages);
        copy.centered = centered;
        return copy;
    }

    public String displayName() {
        if (name != null && !name.isBlank()) return name;
        return switch (type) {
            case PARTICLE -> particle != null ? particle : "Particle";
            case SOUND -> sound != null ? sound : "Sound";
            case POTION -> potion != null ? potion : "Potion effect";
            case FIREWORK -> "Firework";
            case TITLE -> title != null && !title.isBlank() ? title : "Title";
            case ACTIONBAR -> actionbar != null ? actionbar : "Actionbar";
            case MESSAGE -> message != null ? message : "Message";
        };
    }

    public String summary() {
        return switch (type) {
            case PARTICLE -> particle == null ? "Particle" : particle;
            case SOUND -> sound == null ? "Sound" : sound;
            case POTION -> potion == null ? "Potion" : potion + " " + (potionAmplifier + 1);
            case FIREWORK -> "Firework " + fireworkType;
            case TITLE -> title == null ? "Title" : title;
            case ACTIONBAR -> actionbar == null ? "Actionbar" : actionbar;
            case MESSAGE -> message == null ? "Message" : message;
        };
    }

    public ParticleConfig toParticleConfig(Location location) {
        ParticleBuilder builder = ParticleBuilder.create()
                .particle(require(ParticleCompat.fromName(particle), "particle", particle))
                .count(particleCount)
                .offset(offsetX, offsetY, offsetZ)
                .extra(particleExtra)
                .dustSize(dustSize)
                .scope(particleScope);
        if (particleColor != null && !particleColor.isBlank()) builder.color(parseColor(particleColor));
        if (particleBlockMaterial != null && !particleBlockMaterial.isBlank()) {
            builder.blockMaterial(require(Material.matchMaterial(particleBlockMaterial), "material", particleBlockMaterial));
        }
        if (particleScope == ParticleConfig.ParticleScope.LOCATION) builder.location(location);
        return builder.build();
    }

    public SoundConfig toSoundConfig(Location location) {
        SoundConfig.SoundScope scope = soundScope == null ? SoundConfig.SoundScope.PLAYER : soundScope;
        var builder = SoundConfig.builder()
                .sound(require(SoundCompat.fromName(sound), "sound", sound))
                .volume(soundVolume)
                .pitch(soundPitch)
                .scope(scope);
        if (scope == SoundConfig.SoundScope.LOCATION) builder.location(location);
        return builder.build();
    }

    public EffectConfig toPotionConfig() {
        return EffectBuilder.create()
                .effect(require(PotionEffectTypeCompat.resolve(potion), "potion", potion))
                .amplifier(potionAmplifier)
                .durationTicks(potionDurationTicks)
                .ambient(potionAmbient)
                .particles(potionParticles)
                .icon(potionIcon)
                .build();
    }

    public FireworkConfig toFireworkConfig(Location location) {
        FireworkBuilder builder = FireworkBuilder.create()
                .type(fireworkType == null ? FireworkEffect.Type.BALL : FireworkEffect.Type.valueOf(fireworkType.toUpperCase()))
                .colors(parseColors(fireworkColors))
                .fadeColors(parseColors(fireworkFadeColors))
                .flicker(fireworkFlicker)
                .trail(fireworkTrail)
                .power(fireworkPower)
                .location(location);
        return builder.build();
    }

    public TitleConfig toTitleConfig() {
        return TitleBuilder.create()
                .title(title)
                .subtitle(subtitle)
                .times(titleFadeIn, titleStay, titleFadeOut)
                .build();
    }

    public ActionBarConfig toActionBarConfig() {
        return ActionBarBuilder.create().text(actionbar).build();
    }

    public List<String> messageLines() {
        if (messages != null && !messages.isEmpty()) return List.copyOf(messages);
        return message == null ? List.of() : List.of(message);
    }

    private static Color parseColor(String value) {
        String raw = value.trim();
        if (raw.startsWith("#")) raw = raw.substring(1);
        if (raw.contains(",")) {
            String[] parts = raw.split(",");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid RGB color: " + value);
            return Color.fromRGB(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
        }
        if (raw.length() != 6) throw new IllegalArgumentException("Invalid hex color: " + value);
        return Color.fromRGB(Integer.parseInt(raw.substring(0, 2), 16), Integer.parseInt(raw.substring(2, 4), 16), Integer.parseInt(raw.substring(4, 6), 16));
    }

    private static List<Color> parseColors(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(value -> value != null && !value.isBlank()).map(EffectEntry::parseColor).toList();
    }

    private static <T> T require(T value, String field, String raw) {
        if (value == null) throw new IllegalArgumentException("Invalid " + field + ": " + raw);
        return value;
    }

    private static List<String> copyList(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}

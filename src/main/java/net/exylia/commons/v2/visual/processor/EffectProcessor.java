package net.exylia.commons.v2.visual.processor;

import lombok.Builder;
import lombok.Data;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.builder.EffectBuilder;
import net.exylia.commons.v2.visual.builder.FireworkBuilder;
import net.exylia.commons.v2.visual.builder.ParticleBuilder;
import net.exylia.commons.v2.visual.builder.SoundBuilder;
import net.exylia.commons.v2.visual.cache.CacheManager;
import net.exylia.commons.v2.visual.config.EffectConfig;
import net.exylia.commons.v2.visual.config.FireworkConfig;
import net.exylia.commons.v2.visual.config.ParticleConfig;
import net.exylia.commons.v2.visual.config.SoundConfig;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EffectProcessor {

    @Data
    @Builder
    public static class ParsedMessage {
        private final String cleanMessage;
        @Builder.Default
        private final List<SoundConfig> sounds = new ArrayList<>();
        @Builder.Default
        private final List<ParticleConfig> particles = new ArrayList<>();
        @Builder.Default
        private final List<FireworkConfig> fireworks = new ArrayList<>();
        @Builder.Default
        private final List<EffectConfig> effects = new ArrayList<>();
        @Builder.Default
        private final boolean centered = false;
    }

    public static ParsedMessage parse(String message, Player player, PlaceholderContext context) {
        if (message == null || !message.startsWith("[")) {
            return ParsedMessage.builder()
                    .cleanMessage(message != null ? message : "")
                    .sounds(Collections.emptyList())
                    .particles(Collections.emptyList())
                    .fireworks(Collections.emptyList())
                    .effects(Collections.emptyList())
                    .centered(false)
                    .build();
        }

        int effectsEnd = message.indexOf(']');
        if (effectsEnd == -1) {
            return ParsedMessage.builder()
                    .cleanMessage(message)
                    .sounds(Collections.emptyList())
                    .particles(Collections.emptyList())
                    .fireworks(Collections.emptyList())
                    .effects(Collections.emptyList())
                    .centered(false)
                    .build();
        }

        String effectsSection = message.substring(1, effectsEnd);
        String cleanMessage = message.substring(effectsEnd + 1);

        String processedEffects = CacheManager.getInstance().processPlaceholders(effectsSection, player, context);

        return parseEffectsSection(processedEffects, cleanMessage);
    }

    private static ParsedMessage parseEffectsSection(String effectsSection, String cleanMessage) {
        List<SoundConfig> sounds = new ArrayList<>();
        List<ParticleConfig> particles = new ArrayList<>();
        List<FireworkConfig> fireworks = new ArrayList<>();
        List<EffectConfig> effects = new ArrayList<>();
        boolean centered = false;

        if (effectsSection == null || effectsSection.trim().isEmpty()) {
            return ParsedMessage.builder()
                    .cleanMessage(cleanMessage)
                    .sounds(sounds)
                    .particles(particles)
                    .fireworks(fireworks)
                    .effects(effects)
                    .centered(centered)
                    .build();
        }

        String[] effectTypes = effectsSection.split(";");

        for (String effectType : effectTypes) {
            String trimmed = effectType.trim();

            if (trimmed.equalsIgnoreCase("center") || trimmed.equalsIgnoreCase("centered")) {
                centered = true;
                continue;
            }

            String[] parts = trimmed.split(":", 2);
            if (parts.length != 2) continue;

            String type = parts[0].toLowerCase().trim();
            String config = parts[1].trim();

            switch (type) {
                case "sounds", "sound" -> parseSounds(config, sounds);
                case "particles", "particle" -> parseParticles(config, particles);
                case "fireworks", "firework" -> parseFireworks(config, fireworks);
                case "effects", "effect" -> parseEffects(config, effects);
            }
        }

        return ParsedMessage.builder()
                .cleanMessage(cleanMessage)
                .sounds(sounds)
                .particles(particles)
                .fireworks(fireworks)
                .effects(effects)
                .centered(centered)
                .build();
    }

    private static void parseSounds(String soundsConfig, List<SoundConfig> sounds) {
        if (soundsConfig == null || soundsConfig.trim().isEmpty()) return;

        String[] soundStrings = soundsConfig.split(",");
        for (String soundString : soundStrings) {
            try {
                sounds.add(SoundBuilder.fromString(soundString.trim()));
            } catch (Exception e) {
            }
        }
    }

    private static void parseParticles(String particlesConfig, List<ParticleConfig> particles) {
        if (particlesConfig == null || particlesConfig.trim().isEmpty()) return;

        String[] particleStrings = particlesConfig.split(",");
        for (String particleString : particleStrings) {
            try {
                particles.add(ParticleBuilder.fromString(particleString.trim()));
            } catch (Exception e) {
            }
        }
    }

    private static void parseFireworks(String fireworksConfig, List<FireworkConfig> fireworks) {
        if (fireworksConfig == null || fireworksConfig.trim().isEmpty()) return;

        String[] fireworkStrings = fireworksConfig.split(",");
        for (String fireworkString : fireworkStrings) {
            try {
                fireworks.add(FireworkBuilder.fromString(fireworkString.trim()));
            } catch (Exception e) {
            }
        }
    }

    private static void parseEffects(String effectsConfig, List<EffectConfig> effects) {
        if (effectsConfig == null || effectsConfig.trim().isEmpty()) return;

        String[] effectStrings = effectsConfig.split(",");
        for (String effectString : effectStrings) {
            try {
                effects.add(EffectBuilder.fromString(effectString.trim()));
            } catch (Exception e) {
            }
        }
    }
}

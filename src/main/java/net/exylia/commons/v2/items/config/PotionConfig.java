package net.exylia.commons.v2.items.config;

import lombok.Getter;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.*;

@Getter
public class PotionConfig {

    private String basePotionType;
    private boolean potionUpgraded = false;
    private boolean potionExtended = false;
    private Color potionColor;
    private final List<PotionEffectData> customEffects = new ArrayList<>();

    public static class PotionEffectData {
        public final String type;
        public final String amplifier;
        public final String duration;
        public final boolean ambient;
        public final boolean particles;
        public final boolean icon;

        public PotionEffectData(String type, String amplifier, String duration) {
            this(type, amplifier, duration, false, true, true);
        }

        public PotionEffectData(String type, String amplifier, String duration,
                                boolean ambient, boolean particles, boolean icon) {
            this.type = type;
            this.amplifier = amplifier;
            this.duration = duration;
            this.ambient = ambient;
            this.particles = particles;
            this.icon = icon;
        }
    }

    public static PotionConfig fromConfig(ConfigurationSection config) {
        if (config == null) return null;

        PotionConfig potionConfig = new PotionConfig();

        if (config.contains("base_type")) {
            potionConfig.setBasePotionType(config.getString("base_type"));
        }

        if (config.contains("upgraded")) {
            potionConfig.setPotionUpgraded(config.getBoolean("upgraded", false));
        }

        if (config.contains("extended")) {
            potionConfig.setPotionExtended(config.getBoolean("extended", false));
        }

        if (config.contains("color")) {
            potionConfig.setPotionColor(config.getString("color"));
        }

        if (config.contains("custom_effects")) {
            List<?> effectsList = config.getList("custom_effects");
            if (effectsList != null) {
                for (Object effectObj : effectsList) {
                    if (effectObj instanceof Map<?, ?> effectMap) {
                        String type = String.valueOf(effectMap.get("type"));
                        Object amplifier = effectMap.get("amplifier");
                        Object duration = effectMap.get("duration");
                        Object ambientObj = effectMap.get("ambient");
                        Object particlesObj = effectMap.get("particles");
                        Object iconObj = effectMap.get("icon");

                        String amplifierStr = amplifier != null ? String.valueOf(amplifier) : "0";
                        String durationStr = duration != null ? String.valueOf(duration) : "600";
                        boolean ambient = ambientObj != null && Boolean.parseBoolean(String.valueOf(ambientObj));
                        boolean particles = particlesObj == null || Boolean.parseBoolean(String.valueOf(particlesObj));
                        boolean icon = iconObj == null || Boolean.parseBoolean(String.valueOf(iconObj));

                        potionConfig.addCustomEffect(type, amplifierStr, durationStr, ambient, particles, icon);
                    }
                }
            }
        }

        return potionConfig;
    }

    public PotionConfig setBasePotionType(String basePotionType) {
        this.basePotionType = basePotionType;
        return this;
    }

    public PotionConfig setPotionUpgraded(boolean upgraded) {
        this.potionUpgraded = upgraded;
        return this;
    }

    public PotionConfig setPotionExtended(boolean extended) {
        this.potionExtended = extended;
        return this;
    }

    public PotionConfig setPotionColor(String colorString) {
        this.potionColor = parseColor(colorString);
        return this;
    }

    public PotionConfig setPotionColor(Color color) {
        this.potionColor = color;
        return this;
    }

    public PotionConfig addCustomEffect(String type, String amplifier, String duration) {
        return addCustomEffect(type, amplifier, duration, false, true, true);
    }

    public PotionConfig addCustomEffect(String type, String amplifier, String duration,
                                        boolean ambient, boolean particles, boolean icon) {
        customEffects.add(new PotionEffectData(type, amplifier, duration, ambient, particles, icon));
        return this;
    }

    public PotionConfig addCustomEffect(PotionEffectType type, int amplifier, int duration) {
        return addCustomEffect(type.getName(), String.valueOf(amplifier), String.valueOf(duration));
    }

    public PotionConfig clearCustomEffects() {
        customEffects.clear();
        return this;
    }

    public PotionType createPotionType() {
        if (basePotionType == null) {
            return PotionType.WATER;
        }

        PotionType potionType = getPotionTypeByName(basePotionType);
        return potionType != null ? potionType : PotionType.WATER;
    }

    public List<PotionEffect> createCustomEffects(org.bukkit.entity.Player player,
                                                  PlaceholderContext context) {
        List<PotionEffect> effects = new ArrayList<>();

        for (PotionEffectData effectData : customEffects) {
            String effectType = effectData.type;
            String amplifierStr = effectData.amplifier;
            String durationStr = effectData.duration;

            if (player != null && context != null) {
                effectType = Placeholders.process(effectType, player, context);
                amplifierStr = Placeholders.process(amplifierStr, player, context);
                durationStr = Placeholders.process(durationStr, player, context);
            }

            try {
                PotionEffectType type = getPotionEffectByName(effectType);
                int amplifier = Integer.parseInt(amplifierStr);
                int duration = Integer.parseInt(durationStr);

                if (type != null) {
                    PotionEffect effect = new PotionEffect(type, duration, amplifier,
                            effectData.ambient, effectData.particles, effectData.icon);
                    effects.add(effect);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return effects;
    }

    public Color getProcessedColor(org.bukkit.entity.Player player,
                                   PlaceholderContext context) {
        if (potionColor == null) return null;

        if (player != null && context != null) {
            String colorString = Placeholders.process(potionColor.toString(), player, context);
            Color processedColor = parseColor(colorString);
            return processedColor != null ? processedColor : potionColor;
        }

        return potionColor;
    }

    private PotionType getPotionTypeByName(String name) {
        try {
            return PotionType.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return switch (name.toLowerCase()) {
                case "speed", "swiftness" -> PotionType.SPEED;
                case "slowness", "slow" -> PotionType.STRONG_SLOWNESS;
                case "strength" -> PotionType.STRONG_STRENGTH;
                case "instant_health", "healing", "heal" -> PotionType.STRONG_HEALING;
                case "instant_damage", "harming", "harm" -> PotionType.STRONG_HARMING;
                case "jump_boost", "jump" -> PotionType.STRONG_LEAPING;
                case "regeneration", "regen" -> PotionType.REGEN;
                case "fire_resistance", "fire_resist" -> PotionType.FIRE_RESISTANCE;
                case "water_breathing" -> PotionType.WATER_BREATHING;
                case "invisibility", "invis" -> PotionType.INVISIBILITY;
                case "night_vision" -> PotionType.NIGHT_VISION;
                case "weakness", "weak" -> PotionType.WEAKNESS;
                case "poison" -> PotionType.POISON;
                case "luck" -> PotionType.LUCK;
                case "turtle_master" -> PotionType.TURTLE_MASTER;
                case "slow_falling" -> PotionType.SLOW_FALLING;
                default -> null;
            };
        }
    }

    private PotionEffectType getPotionEffectByName(String name) {
        try {
            return PotionEffectType.getByName(name.toUpperCase());
        } catch (Exception e) {
            for (PotionEffectType type : PotionEffectType.values()) {
                if (type != null && (type.getName().equalsIgnoreCase(name) ||
                        type.toString().equalsIgnoreCase(name))) {
                    return type;
                }
            }
            return null;
        }
    }

    private Color parseColor(String colorString) {
        if (colorString == null) return null;

        try {
            if (colorString.startsWith("#")) {
                int rgb = Integer.parseInt(colorString.substring(1), 16);
                return Color.fromRGB(rgb);
            }

            return switch (colorString.toLowerCase()) {
                case "red" -> Color.RED;
                case "blue" -> Color.BLUE;
                case "green" -> Color.GREEN;
                case "yellow" -> Color.YELLOW;
                case "purple" -> Color.PURPLE;
                case "orange" -> Color.ORANGE;
                case "white" -> Color.WHITE;
                case "black" -> Color.BLACK;
                case "aqua", "cyan" -> Color.AQUA;
                case "fuchsia", "magenta" -> Color.FUCHSIA;
                case "gray", "grey" -> Color.GRAY;
                case "lime" -> Color.LIME;
                case "maroon" -> Color.MAROON;
                case "navy" -> Color.NAVY;
                case "olive" -> Color.OLIVE;
                case "silver" -> Color.SILVER;
                case "teal" -> Color.TEAL;
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    public List<PotionEffectData> getCustomEffects() { return new ArrayList<>(customEffects); }

    public boolean hasConfiguration() {
        return basePotionType != null || potionColor != null || !customEffects.isEmpty();
    }
}

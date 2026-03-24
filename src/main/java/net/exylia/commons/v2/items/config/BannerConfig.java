package net.exylia.commons.v2.items.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.compat.ShieldMetaCompat;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Getter
public class BannerConfig {

    private static final Gson GSON = new Gson();

    private String baseColor;
    private final List<BannerPatternData> patterns = new ArrayList<>();

    public static class BannerPatternData {
        public final String pattern;
        public final String color;

        public BannerPatternData(String pattern, String color) {
            this.pattern = pattern;
            this.color = color;
        }
    }

    public static BannerConfig fromConfig(ConfigurationSection config) {
        if (config == null) return null;

        BannerConfig bannerConfig = new BannerConfig();

        if (config.contains("base_color")) {
            bannerConfig.setBaseColor(config.getString("base_color"));
        }

        if (config.contains("patterns")) {
            List<?> patternsList = config.getList("patterns");
            if (patternsList != null) {
                for (Object obj : patternsList) {
                    if (obj instanceof Map<?, ?> map) {
                        String pattern = map.get("pattern") != null ? String.valueOf(map.get("pattern")) : null;
                        String color = map.get("color") != null ? String.valueOf(map.get("color")) : null;
                        if (pattern != null && color != null) {
                            bannerConfig.addPattern(pattern, color);
                        }
                    }
                }
            }
        }

        return bannerConfig.hasConfiguration() ? bannerConfig : null;
    }

    public static BannerConfig fromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return null;

        try {
            String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            BannerConfig bannerConfig = new BannerConfig();

            if (root.has("base")) {
                bannerConfig.setBaseColor(root.get("base").getAsString());
            }

            if (root.has("p") && root.get("p").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("p")) {
                    JsonObject patternObj = element.getAsJsonObject();
                    String pattern = patternObj.has("p") ? patternObj.get("p").getAsString() : null;
                    String color = patternObj.has("c") ? patternObj.get("c").getAsString() : null;
                    if (pattern != null && color != null) {
                        bannerConfig.addPattern(pattern, color);
                    }
                }
            }

            return bannerConfig.hasConfiguration() ? bannerConfig : null;
        } catch (Exception e) {
            DebugAPI.logLibWarn("Failed to decode banner design from base64: " + e.getMessage());
            return null;
        }
    }

    public String toBase64() {
        try {
            JsonObject root = new JsonObject();
            if (baseColor != null) {
                root.addProperty("base", baseColor);
            }
            JsonArray patternsArray = new JsonArray();
            for (BannerPatternData data : patterns) {
                JsonObject obj = new JsonObject();
                obj.addProperty("p", data.pattern);
                obj.addProperty("c", data.color);
                patternsArray.add(obj);
            }
            root.add("p", patternsArray);
            String json = GSON.toJson(root);
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            DebugAPI.logLibWarn("Failed to encode banner design to base64: " + e.getMessage());
            return null;
        }
    }

    public BannerConfig setBaseColor(String baseColor) {
        this.baseColor = baseColor;
        return this;
    }

    public BannerConfig addPattern(String pattern, String color) {
        patterns.add(new BannerPatternData(pattern, color));
        return this;
    }

    public void applyToBanner(BannerMeta meta, Player player, PlaceholderContext context) {
        for (Pattern pattern : resolvePatterns(player, context)) {
            meta.addPattern(pattern);
        }
        DebugAPI.logLibDebug(DebugCategory.ITEMS, "Applied " + patterns.size() + " patterns to banner");
    }

    public void applyToShieldMeta(ItemMeta meta, Player player, PlaceholderContext context) {
        DyeColor dyeColor = baseColor != null
                ? parseDyeColor(resolvePlaceholder(baseColor, player, context))
                : null;
        List<Pattern> resolved = resolvePatterns(player, context);
        ShieldMetaCompat.apply(meta, dyeColor, resolved);
        DebugAPI.logLibDebug(DebugCategory.ITEMS, "Applied " + resolved.size() + " patterns to shield via ShieldMetaCompat");
    }

    public void applyToShield(BlockStateMeta meta, Player player, PlaceholderContext context) {
        org.bukkit.block.BlockState bs = meta.getBlockState();
        DebugAPI.logPluginDebug("[BannerConfig.applyToShield] blockStateClass=" + (bs != null ? bs.getClass().getSimpleName() : "null") + " isBanner=" + (bs instanceof Banner));
        if (!(bs instanceof Banner bannerState)) return;

        if (baseColor != null) {
            DyeColor dyeColor = parseDyeColor(resolvePlaceholder(baseColor, player, context));
            if (dyeColor != null) {
                bannerState.setBaseColor(dyeColor);
            }
        }

        for (Pattern pattern : resolvePatterns(player, context)) {
            bannerState.addPattern(pattern);
        }

        bannerState.update();
        meta.setBlockState(bannerState);
        DebugAPI.logLibDebug(DebugCategory.ITEMS, "Applied " + patterns.size() + " patterns to shield via BlockStateMeta");
    }

    private List<Pattern> resolvePatterns(Player player, PlaceholderContext context) {
        List<Pattern> result = new ArrayList<>();
        for (BannerPatternData data : patterns) {
            PatternType patternType = parsePatternType(resolvePlaceholder(data.pattern, player, context));
            DyeColor dyeColor = parseDyeColor(resolvePlaceholder(data.color, player, context));
            if (patternType != null && dyeColor != null) {
                result.add(new Pattern(dyeColor, patternType));
            } else {
                DebugAPI.logLibDebug(DebugCategory.ITEMS,
                        "Skipping invalid banner pattern: " + data.pattern + " / " + data.color);
            }
        }
        return result;
    }

    private String resolvePlaceholder(String value, Player player, PlaceholderContext context) {
        if (value == null) return null;
        if (player != null && context != null) {
            return Placeholders.process(value, player, context);
        }
        return value;
    }

    private PatternType parsePatternType(String name) {
        if (name == null) return null;
        try {
            return Registry.BANNER_PATTERN.get(NamespacedKey.minecraft(name.toLowerCase()));
        } catch (Exception ignored) {}
        DebugAPI.logLibDebug(DebugCategory.ITEMS, "Unknown banner pattern type: " + name);
        return null;
    }

    private DyeColor parseDyeColor(String name) {
        if (name == null) return null;
        try {
            return DyeColor.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            DebugAPI.logLibDebug(DebugCategory.ITEMS, "Unknown dye color: " + name);
            return null;
        }
    }

    public boolean hasConfiguration() {
        return !patterns.isEmpty() || baseColor != null;
    }
}

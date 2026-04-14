package net.exylia.commons.v2.items.snapshot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.items.config.ArmorTrimConfig;
import net.exylia.commons.v2.items.config.BannerConfig;
import net.exylia.commons.v2.items.config.LeatherArmorConfig;
import net.exylia.commons.v2.items.config.PotionConfig;
import net.exylia.commons.v2.items.model.ItemData;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionType;

public final class ItemSnapshot {

    private static final String PREFIX = "item:";
    private static final Gson GSON = new Gson();

    private final String serialized;

    private ItemSnapshot(String serialized) {
        this.serialized = serialized;
    }

    public static ItemSnapshot from(String value) {
        if (value == null || value.isEmpty()) return new ItemSnapshot("STONE");
        return new ItemSnapshot(value);
    }

    public static ItemSnapshot from(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return new ItemSnapshot("AIR");
        }

        ItemMeta meta = itemStack.getItemMeta();
        String material = itemStack.getType().name();

        if (meta instanceof SkullMeta skullMeta) {
            return extractFromSkull(skullMeta);
        }

        JsonObject json = new JsonObject();
        json.addProperty("m", material);
        boolean needsJson = false;

        if (meta instanceof PotionMeta potionMeta) {
            JsonObject potionJson = extractPotionJson(potionMeta);
            if (potionJson != null) {
                json.add("potion", potionJson);
                needsJson = true;
            }
        }

        if (meta instanceof LeatherArmorMeta leatherMeta) {
            String colorHex = extractLeatherColor(leatherMeta);
            if (colorHex != null) {
                json.addProperty("leather", colorHex);
                needsJson = true;
            }
        }

        if (meta instanceof BannerMeta bannerMeta && !bannerMeta.getPatterns().isEmpty()) {
            BannerConfig bannerConfig = extractBannerFromMeta(bannerMeta, material);
            String b64 = bannerConfig.toBase64();
            if (b64 != null) {
                json.addProperty("banner", b64);
                needsJson = true;
            }
        }

        if (itemStack.getType() == Material.SHIELD && meta instanceof BlockStateMeta blockMeta) {
            BannerConfig shieldBanner = extractBannerFromShield(blockMeta);
            if (shieldBanner != null) {
                String b64 = shieldBanner.toBase64();
                if (b64 != null) {
                    json.addProperty("banner", b64);
                    needsJson = true;
                }
            }
        }

        if (!needsJson) return new ItemSnapshot(material);
        return new ItemSnapshot(PREFIX + GSON.toJson(json));
    }

    public ItemData toItemData() {
        if (serialized.startsWith(PREFIX)) {
            return parseItemJson(serialized.substring(PREFIX.length()));
        }
        return ItemData.builder().rawMaterial(serialized).build();
    }

    public String serialize() {
        return serialized;
    }

    @Override
    public String toString() {
        return serialized;
    }

    private static ItemSnapshot extractFromSkull(SkullMeta skullMeta) {
        try {
            org.bukkit.profile.PlayerProfile profile = skullMeta.getOwnerProfile();
            if (profile != null) {
                java.net.URL skinUrl = profile.getTextures().getSkin();
                if (skinUrl != null) {
                    return new ItemSnapshot("urlhead:" + skinUrl);
                }
            }
        } catch (Exception ignored) {}
        try {
            org.bukkit.OfflinePlayer owner = skullMeta.getOwningPlayer();
            if (owner != null && owner.getName() != null) {
                return new ItemSnapshot("playerhead:" + owner.getName());
            }
        } catch (Exception ignored) {}
        return new ItemSnapshot("PLAYER_HEAD");
    }

    private static JsonObject extractPotionJson(PotionMeta potionMeta) {
        JsonObject potionJson = new JsonObject();
        try {
            PotionType baseType = potionMeta.getBasePotionType();
            if (baseType != null) potionJson.addProperty("base", baseType.name());
        } catch (Exception ignored) {}
        try {
            Color color = potionMeta.getColor();
            if (color != null) potionJson.addProperty("color", colorToHex(color));
        } catch (Exception ignored) {}
        return potionJson.size() > 0 ? potionJson : null;
    }

    private static String extractLeatherColor(LeatherArmorMeta leatherMeta) {
        try {
            Color color = leatherMeta.getColor();
            if (color != null) return colorToHex(color);
        } catch (Exception ignored) {}
        return null;
    }

    private static BannerConfig extractBannerFromMeta(BannerMeta bannerMeta, String material) {
        BannerConfig config = new BannerConfig();
        String baseColor = material.replace("_WALL_BANNER", "").replace("_BANNER", "").toLowerCase();
        config.setBaseColor(baseColor);
        for (Pattern pattern : bannerMeta.getPatterns()) {
            config.addPattern(
                pattern.getPattern().getKey().getKey(),
                pattern.getColor().name().toLowerCase()
            );
        }
        return config;
    }

    private static BannerConfig extractBannerFromShield(BlockStateMeta blockMeta) {
        try {
            org.bukkit.block.BlockState state = blockMeta.getBlockState();
            if (!(state instanceof Banner bannerState)) return null;
            BannerConfig config = new BannerConfig();
            if (bannerState.getBaseColor() != null) {
                config.setBaseColor(bannerState.getBaseColor().name().toLowerCase());
            }
            for (Pattern pattern : bannerState.getPatterns()) {
                config.addPattern(
                    pattern.getPattern().getKey().getKey(),
                    pattern.getColor().name().toLowerCase()
                );
            }
            return config.hasConfiguration() ? config : null;
        } catch (Exception ignored) {}
        return null;
    }

    private static ItemData parseItemJson(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            ItemData.ItemDataBuilder builder = ItemData.builder();

            builder.rawMaterial(obj.has("m") ? obj.get("m").getAsString() : "STONE");

            if (obj.has("potion")) {
                JsonObject p = obj.getAsJsonObject("potion");
                PotionConfig potionConfig = new PotionConfig();
                if (p.has("base")) potionConfig.setBasePotionType(p.get("base").getAsString());
                if (p.has("color")) potionConfig.setPotionColor(p.get("color").getAsString());
                if (p.has("upgraded")) potionConfig.setPotionUpgraded(p.get("upgraded").getAsBoolean());
                if (p.has("extended")) potionConfig.setPotionExtended(p.get("extended").getAsBoolean());
                builder.potionConfig(potionConfig);
            }

            if (obj.has("leather")) {
                builder.leatherArmorConfig(new LeatherArmorConfig().setColor(obj.get("leather").getAsString()));
            }

            if (obj.has("trim")) {
                JsonObject t = obj.getAsJsonObject("trim");
                ArmorTrimConfig trimConfig = new ArmorTrimConfig();
                if (t.has("material")) trimConfig.setMaterial(t.get("material").getAsString());
                if (t.has("pattern")) trimConfig.setPattern(t.get("pattern").getAsString());
                builder.armorTrimConfig(trimConfig);
            }

            if (obj.has("banner")) {
                BannerConfig bannerConfig = BannerConfig.fromBase64(obj.get("banner").getAsString());
                if (bannerConfig != null) builder.bannerConfig(bannerConfig);
            }

            if (obj.has("glowing")) {
                builder.glowing(obj.get("glowing").getAsBoolean());
            }

            return builder.build();
        } catch (Exception e) {
            DebugAPI.logLibWarn("ItemSnapshot: Failed to parse item JSON: " + e.getMessage());
            return ItemData.builder().build();
        }
    }

    private static String colorToHex(Color color) {
        return String.format("#%06X", color.asRGB());
    }
}

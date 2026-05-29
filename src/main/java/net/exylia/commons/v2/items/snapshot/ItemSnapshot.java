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
import net.exylia.commons.v2.skull.api.SkullAPI;
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

import java.util.Base64;

public final class ItemSnapshot {

    private static final String PREFIX = "item:";
    private static final String BYTES_PREFIX = "bytes:";
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
        try {
            byte[] bytes = itemStack.serializeAsBytes();
            return new ItemSnapshot(BYTES_PREFIX + Base64.getEncoder().encodeToString(bytes));
        } catch (Exception e) {
            DebugAPI.logLibWarn("ItemSnapshot: Failed to serialize item, falling back to material: " + e.getMessage());
            return new ItemSnapshot(itemStack.getType().name());
        }
    }

    public ItemData toItemData() {
        if (serialized.startsWith(BYTES_PREFIX)) {
            try {
                byte[] bytes = Base64.getDecoder().decode(serialized.substring(BYTES_PREFIX.length()));
                ItemStack item = ItemStack.deserializeBytes(bytes);
                return ItemData.builder().rawMaterial(item.getType().name()).build();
            } catch (Exception e) {
                return ItemData.builder().rawMaterial("PAPER").build();
            }
        }
        if (serialized.startsWith(PREFIX)) {
            return parseItemJson(serialized.substring(PREFIX.length()));
        }
        return ItemData.builder().rawMaterial(serialized).build();
    }

    public ItemStack toItemStack() {
        if (serialized.startsWith(BYTES_PREFIX)) {
            try {
                byte[] bytes = Base64.getDecoder().decode(serialized.substring(BYTES_PREFIX.length()));
                return ItemStack.deserializeBytes(bytes);
            } catch (Exception e) {
                DebugAPI.logLibWarn("ItemSnapshot: Failed to deserialize bytes: " + e.getMessage());
                return new ItemStack(Material.PAPER);
            }
        }
        if (serialized.startsWith("urlhead:")) {
            return SkullAPI.isInitialized()
                    ? SkullAPI.fromTextureURL(serialized.substring(8))
                    : new ItemStack(Material.PLAYER_HEAD);
        }
        if (serialized.startsWith("basehead:")) {
            return SkullAPI.isInitialized()
                    ? SkullAPI.fromTexture(serialized.substring(9))
                    : new ItemStack(Material.PLAYER_HEAD);
        }
        if (serialized.startsWith("playerhead:") || serialized.startsWith("urlhead:") || serialized.startsWith("basehead:")) {
            return new ItemStack(Material.PLAYER_HEAD);
        }
        ItemData data = toItemData();
        String rawMat = data.getRawMaterial();
        Material mat = rawMat != null ? Material.matchMaterial(rawMat) : null;
        return new ItemStack(mat != null ? mat : Material.PAPER);
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

package net.exylia.commons.v2.skull.renderer;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.UUID;

public class SkullFactory {

    @Getter
    private static final boolean IS_PAPER = checkPaperSupport();

    public ItemStack createSkull(String base64Texture) {
        if (base64Texture == null || base64Texture.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        return IS_PAPER ? createPaperSkull(base64Texture) : createCraftBukkitSkull(base64Texture);
    }

    private ItemStack createPaperSkull(String base64Texture) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta == null) {
            return skull;
        }

        try {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", base64Texture));
            meta.setPlayerProfile(profile);
            skull.setItemMeta(meta);
        } catch (Exception e) {
            return skull;
        }

        return skull;
    }

    private ItemStack createCraftBukkitSkull(String base64Texture) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta == null) {
            return skull;
        }

        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", base64Texture));
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
            skull.setItemMeta(meta);
        } catch (Exception e) {
            return skull;
        }

        return skull;
    }

    private static boolean checkPaperSupport() {
        try {
            Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

package net.exylia.commons.v2.compat;

import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.List;

public final class ShieldMetaCompat {

    private static final Class<?> SHIELD_META_CLASS;
    private static final Method SET_BASE_COLOR;
    private static final Method SET_PATTERNS;

    static {
        Class<?> clazz = null;
        Method setBaseColor = null;
        Method setPatterns = null;
        try {
            clazz = Class.forName("org.bukkit.inventory.meta.ShieldMeta");
            setBaseColor = clazz.getMethod("setBaseColor", DyeColor.class);
            setPatterns = clazz.getMethod("setPatterns", List.class);
            DebugAPI.logPluginDebug("[ShieldMetaCompat] ShieldMeta found via reflection");
        } catch (Throwable e) {
            DebugAPI.logPluginDebug("[ShieldMetaCompat] ShieldMeta NOT found - " + e.getMessage());
        }
        SHIELD_META_CLASS = clazz;
        SET_BASE_COLOR = setBaseColor;
        SET_PATTERNS = setPatterns;
    }

    private ShieldMetaCompat() {}

    public static boolean isShieldMeta(ItemMeta meta) {
        return SHIELD_META_CLASS != null && SHIELD_META_CLASS.isInstance(meta);
    }

    public static void apply(ItemMeta meta, DyeColor baseColor, List<Pattern> patterns) {
        if (SHIELD_META_CLASS == null || !SHIELD_META_CLASS.isInstance(meta)) return;
        try {
            if (SET_BASE_COLOR != null) {
                SET_BASE_COLOR.invoke(meta, baseColor);
            }
            if (SET_PATTERNS != null) {
                SET_PATTERNS.invoke(meta, patterns);
            }
        } catch (Throwable e) {
            DebugAPI.logLibWarn("ShieldMetaCompat: Failed to apply shield design - " + e.getMessage());
        }
    }
}

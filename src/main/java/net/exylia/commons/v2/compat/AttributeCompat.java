package net.exylia.commons.v2.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;

import java.util.EnumMap;
import java.util.Map;

public final class AttributeCompat {

    public enum Type {
        MAX_HEALTH("GENERIC_MAX_HEALTH", "MAX_HEALTH"),
        FOLLOW_RANGE("GENERIC_FOLLOW_RANGE", "FOLLOW_RANGE"),
        KNOCKBACK_RESISTANCE("GENERIC_KNOCKBACK_RESISTANCE", "KNOCKBACK_RESISTANCE"),
        MOVEMENT_SPEED("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED"),
        FLYING_SPEED("GENERIC_FLYING_SPEED", "FLYING_SPEED"),
        ATTACK_DAMAGE("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE"),
        ATTACK_KNOCKBACK("GENERIC_ATTACK_KNOCKBACK", "ATTACK_KNOCKBACK"),
        ATTACK_SPEED("GENERIC_ATTACK_SPEED", "ATTACK_SPEED"),
        ARMOR("GENERIC_ARMOR", "ARMOR"),
        ARMOR_TOUGHNESS("GENERIC_ARMOR_TOUGHNESS", "ARMOR_TOUGHNESS"),
        LUCK("GENERIC_LUCK", "LUCK"),
        MAX_ABSORPTION("GENERIC_MAX_ABSORPTION", "MAX_ABSORPTION"),
        JUMP_STRENGTH("HORSE_JUMP_STRENGTH", "JUMP_STRENGTH"),
        SPAWN_REINFORCEMENTS("ZOMBIE_SPAWN_REINFORCEMENTS", "SPAWN_REINFORCEMENTS"),
        BLOCK_INTERACTION_RANGE("PLAYER_BLOCK_INTERACTION_RANGE", "BLOCK_INTERACTION_RANGE"),
        ENTITY_INTERACTION_RANGE("PLAYER_ENTITY_INTERACTION_RANGE", "ENTITY_INTERACTION_RANGE"),
        BLOCK_BREAK_SPEED("PLAYER_BLOCK_BREAK_SPEED", "BLOCK_BREAK_SPEED"),
        MINING_EFFICIENCY("PLAYER_MINING_EFFICIENCY", "MINING_EFFICIENCY"),
        SNEAKING_SPEED("PLAYER_SNEAKING_SPEED", "SNEAKING_SPEED"),
        SUBMERGED_MINING_SPEED("PLAYER_SUBMERGED_MINING_SPEED", "SUBMERGED_MINING_SPEED"),
        SWEEPING_DAMAGE_RATIO("PLAYER_SWEEPING_DAMAGE_RATIO", "SWEEPING_DAMAGE_RATIO"),
        SCALE("GENERIC_SCALE", "SCALE"),
        STEP_HEIGHT("GENERIC_STEP_HEIGHT", "STEP_HEIGHT"),
        GRAVITY("GENERIC_GRAVITY", "GRAVITY"),
        SAFE_FALL_DISTANCE("GENERIC_SAFE_FALL_DISTANCE", "SAFE_FALL_DISTANCE"),
        FALL_DAMAGE_MULTIPLIER("GENERIC_FALL_DAMAGE_MULTIPLIER", "FALL_DAMAGE_MULTIPLIER"),
        BURNING_TIME("GENERIC_BURNING_TIME", "BURNING_TIME"),
        EXPLOSION_KNOCKBACK_RESISTANCE("GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE", "EXPLOSION_KNOCKBACK_RESISTANCE"),
        MOVEMENT_EFFICIENCY("GENERIC_MOVEMENT_EFFICIENCY", "MOVEMENT_EFFICIENCY"),
        OXYGEN_BONUS("GENERIC_OXYGEN_BONUS", "OXYGEN_BONUS"),
        WATER_MOVEMENT_EFFICIENCY("GENERIC_WATER_MOVEMENT_EFFICIENCY", "WATER_MOVEMENT_EFFICIENCY"),
        TEMPT_RANGE("GENERIC_TEMPT_RANGE", "TEMPT_RANGE");

        private final String[] names;

        Type(String... names) {
            this.names = names;
        }
    }

    private static final Map<Type, Attribute> RESOLVED = new EnumMap<>(Type.class);

    static {
        for (Type type : Type.values()) {
            RESOLVED.put(type, resolve(type.names));
        }
    }

    private AttributeCompat() {}

    public static Attribute get(Type type) {
        return RESOLVED.get(type);
    }

    public static Attribute getMaxHealth() { return get(Type.MAX_HEALTH); }
    public static Attribute getFollowRange() { return get(Type.FOLLOW_RANGE); }
    public static Attribute getKnockbackResistance() { return get(Type.KNOCKBACK_RESISTANCE); }
    public static Attribute getMovementSpeed() { return get(Type.MOVEMENT_SPEED); }
    public static Attribute getFlyingSpeed() { return get(Type.FLYING_SPEED); }
    public static Attribute getAttackDamage() { return get(Type.ATTACK_DAMAGE); }
    public static Attribute getAttackKnockback() { return get(Type.ATTACK_KNOCKBACK); }
    public static Attribute getAttackSpeed() { return get(Type.ATTACK_SPEED); }
    public static Attribute getArmor() { return get(Type.ARMOR); }
    public static Attribute getArmorToughness() { return get(Type.ARMOR_TOUGHNESS); }
    public static Attribute getLuck() { return get(Type.LUCK); }
    public static Attribute getMaxAbsorption() { return get(Type.MAX_ABSORPTION); }
    public static Attribute getJumpStrength() { return get(Type.JUMP_STRENGTH); }
    public static Attribute getSpawnReinforcements() { return get(Type.SPAWN_REINFORCEMENTS); }
    public static Attribute getBlockInteractionRange() { return get(Type.BLOCK_INTERACTION_RANGE); }
    public static Attribute getEntityInteractionRange() { return get(Type.ENTITY_INTERACTION_RANGE); }
    public static Attribute getBlockBreakSpeed() { return get(Type.BLOCK_BREAK_SPEED); }
    public static Attribute getMiningEfficiency() { return get(Type.MINING_EFFICIENCY); }
    public static Attribute getSneakingSpeed() { return get(Type.SNEAKING_SPEED); }
    public static Attribute getSubmergedMiningSpeed() { return get(Type.SUBMERGED_MINING_SPEED); }
    public static Attribute getSweepingDamageRatio() { return get(Type.SWEEPING_DAMAGE_RATIO); }
    public static Attribute getScale() { return get(Type.SCALE); }
    public static Attribute getStepHeight() { return get(Type.STEP_HEIGHT); }
    public static Attribute getGravity() { return get(Type.GRAVITY); }
    public static Attribute getSafeFallDistance() { return get(Type.SAFE_FALL_DISTANCE); }
    public static Attribute getFallDamageMultiplier() { return get(Type.FALL_DAMAGE_MULTIPLIER); }
    public static Attribute getBurningTime() { return get(Type.BURNING_TIME); }
    public static Attribute getExplosionKnockbackResistance() { return get(Type.EXPLOSION_KNOCKBACK_RESISTANCE); }
    public static Attribute getMovementEfficiency() { return get(Type.MOVEMENT_EFFICIENCY); }
    public static Attribute getOxygenBonus() { return get(Type.OXYGEN_BONUS); }
    public static Attribute getWaterMovementEfficiency() { return get(Type.WATER_MOVEMENT_EFFICIENCY); }
    public static Attribute getTemptRange() { return get(Type.TEMPT_RANGE); }

    private static Attribute resolve(String... names) {
        for (String name : names) {
            try {
                Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(name.toLowerCase()));
                if (attr != null) return attr;
            } catch (Throwable ignored) {}
            try {
                Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(name.toLowerCase().replace('_', '.')));
                if (attr != null) return attr;
            } catch (Throwable ignored) {}
            try {
                return (Attribute) Attribute.class.getField(name).get(null);
            } catch (Throwable ignored) {}
        }
        return null;
    }
}

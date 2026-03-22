package net.exylia.commons.v2.compat;

import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;

public final class EntityTypeCompat {

    public enum Type {
        TNT("TNT", "PRIMED_TNT"),
        ITEM("ITEM", "DROPPED_ITEM"),
        LEASH_KNOT("LEASH_KNOT", "LEASH_HITCH"),
        FISHING_BOBBER("FISHING_BOBBER", "FISHING_HOOK"),
        LIGHTNING_BOLT("LIGHTNING_BOLT", "LIGHTNING"),
        POTION("POTION", "SPLASH_POTION"),
        SNOW_GOLEM("SNOW_GOLEM", "SNOWMAN"),
        ZOMBIFIED_PIGLIN("ZOMBIFIED_PIGLIN", "PIG_ZOMBIE"),
        MOOSHROOM("MOOSHROOM", "MUSHROOM_COW"),
        EXPERIENCE_ORB("EXPERIENCE_ORB"),
        FALLING_BLOCK("FALLING_BLOCK"),
        AREA_EFFECT_CLOUD("AREA_EFFECT_CLOUD"),
        END_CRYSTAL("END_CRYSTAL"),
        ARROW("ARROW"),
        SPECTRAL_ARROW("SPECTRAL_ARROW"),
        TRIDENT("TRIDENT"),
        SNOWBALL("SNOWBALL"),
        EGG("EGG"),
        ENDER_PEARL("ENDER_PEARL"),
        FIREBALL("FIREBALL"),
        SMALL_FIREBALL("SMALL_FIREBALL"),
        DRAGON_FIREBALL("DRAGON_FIREBALL"),
        WITHER_SKULL("WITHER_SKULL"),
        SHULKER_BULLET("SHULKER_BULLET"),
        LLAMA_SPIT("LLAMA_SPIT"),
        WIND_CHARGE("WIND_CHARGE"),
        EVOKER_FANGS("EVOKER_FANGS"),
        MINECART("MINECART"),
        BOAT("BOAT"),
        FIREWORK_ROCKET("FIREWORK_ROCKET", "FIREWORK"),
        ARMOR_STAND("ARMOR_STAND"),
        ITEM_FRAME("ITEM_FRAME"),
        GLOW_ITEM_FRAME("GLOW_ITEM_FRAME"),
        PAINTING("PAINTING"),
        MARKER("MARKER"),
        INTERACTION("INTERACTION"),
        BLOCK_DISPLAY("BLOCK_DISPLAY"),
        ITEM_DISPLAY("ITEM_DISPLAY"),
        TEXT_DISPLAY("TEXT_DISPLAY");

        private final String[] names;

        Type(String... names) {
            this.names = names;
        }
    }

    private static final Map<Type, EntityType> RESOLVED = new EnumMap<>(Type.class);

    static {
        for (Type type : Type.values()) {
            RESOLVED.put(type, resolve(type.names));
        }
    }

    private EntityTypeCompat() {}

    public static EntityType get(Type type) {
        return RESOLVED.get(type);
    }

    public static EntityType getTnt()              { return get(Type.TNT); }
    public static EntityType getItem()             { return get(Type.ITEM); }
    public static EntityType getLeashKnot()        { return get(Type.LEASH_KNOT); }
    public static EntityType getFishingBobber()    { return get(Type.FISHING_BOBBER); }
    public static EntityType getLightningBolt()    { return get(Type.LIGHTNING_BOLT); }
    public static EntityType getPotion()           { return get(Type.POTION); }
    public static EntityType getSnowGolem()        { return get(Type.SNOW_GOLEM); }
    public static EntityType getZombifiedPiglin()  { return get(Type.ZOMBIFIED_PIGLIN); }
    public static EntityType getMooshroom()        { return get(Type.MOOSHROOM); }
    public static EntityType getExperienceOrb()    { return get(Type.EXPERIENCE_ORB); }
    public static EntityType getFallingBlock()     { return get(Type.FALLING_BLOCK); }
    public static EntityType getAreaEffectCloud()  { return get(Type.AREA_EFFECT_CLOUD); }
    public static EntityType getEndCrystal()       { return get(Type.END_CRYSTAL); }
    public static EntityType getArrow()            { return get(Type.ARROW); }
    public static EntityType getSpectralArrow()    { return get(Type.SPECTRAL_ARROW); }
    public static EntityType getTrident()          { return get(Type.TRIDENT); }
    public static EntityType getSnowball()         { return get(Type.SNOWBALL); }
    public static EntityType getEgg()              { return get(Type.EGG); }
    public static EntityType getEnderPearl()       { return get(Type.ENDER_PEARL); }
    public static EntityType getFireball()         { return get(Type.FIREBALL); }
    public static EntityType getSmallFireball()    { return get(Type.SMALL_FIREBALL); }
    public static EntityType getDragonFireball()   { return get(Type.DRAGON_FIREBALL); }
    public static EntityType getWitherSkull()      { return get(Type.WITHER_SKULL); }
    public static EntityType getShulkerBullet()    { return get(Type.SHULKER_BULLET); }
    public static EntityType getLlamaSpit()        { return get(Type.LLAMA_SPIT); }
    public static EntityType getWindCharge()       { return get(Type.WIND_CHARGE); }
    public static EntityType getEvokerFangs()      { return get(Type.EVOKER_FANGS); }
    public static EntityType getMinecart()         { return get(Type.MINECART); }
    public static EntityType getBoat()             { return get(Type.BOAT); }
    public static EntityType getFireworkRocket()   { return get(Type.FIREWORK_ROCKET); }
    public static EntityType getArmorStand()       { return get(Type.ARMOR_STAND); }
    public static EntityType getItemFrame()        { return get(Type.ITEM_FRAME); }
    public static EntityType getGlowItemFrame()    { return get(Type.GLOW_ITEM_FRAME); }
    public static EntityType getPainting()         { return get(Type.PAINTING); }
    public static EntityType getMarker()           { return get(Type.MARKER); }
    public static EntityType getInteraction()      { return get(Type.INTERACTION); }
    public static EntityType getBlockDisplay()     { return get(Type.BLOCK_DISPLAY); }
    public static EntityType getItemDisplay()      { return get(Type.ITEM_DISPLAY); }
    public static EntityType getTextDisplay()      { return get(Type.TEXT_DISPLAY); }

    private static EntityType resolve(String... names) {
        for (String name : names) {
            try {
                return EntityType.valueOf(name);
            } catch (IllegalArgumentException ignored) {}
            try {
                return (EntityType) EntityType.class.getField(name).get(null);
            } catch (Exception ignored) {}
        }
        return null;
    }
}

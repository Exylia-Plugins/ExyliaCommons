package net.exylia.commons.v2.snapshot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SnapshotData {

    private static final Attribute MAX_HEALTH_ATTR = resolveAttribute("GENERIC_MAX_HEALTH", "MAX_HEALTH");

    private static Attribute resolveAttribute(String... names) {
        for (String name : names) {
            try {
                return (Attribute) Attribute.class.getField(name).get(null);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }
        throw new IllegalStateException("Cannot resolve Attribute from names: " + Arrays.toString(names));
    }

    private GameMode gameMode;

    private ItemStack[] armor;
    private ItemStack[] inventory;
    private ItemStack offHand;

    private double health;
    private double maxHealth;

    private int foodLevel;
    private float saturation;

    private int level;
    private float exp;

    private List<PotionEffectData> potionEffects;

    private boolean allowFlight;
    private boolean flying;
    private float flySpeed;

    public static SnapshotData fromPlayer(Player player) {
        SnapshotData data = new SnapshotData();

        data.setGameMode(player.getGameMode());

        data.setArmor(Arrays.copyOf(player.getInventory().getArmorContents(), 4));
        data.setInventory(Arrays.copyOf(player.getInventory().getContents(), 36));
        data.setOffHand(player.getInventory().getItemInOffHand());

        data.setHealth(player.getHealth());
        data.setMaxHealth(player.getAttribute(MAX_HEALTH_ATTR).getValue());

        data.setFoodLevel(player.getFoodLevel());
        data.setSaturation(player.getSaturation());

        data.setLevel(player.getLevel());
        data.setExp(player.getExp());

        data.setPotionEffects(
                player.getActivePotionEffects().stream()
                        .map(PotionEffectData::fromPotionEffect)
                        .collect(Collectors.toList())
        );

        data.setAllowFlight(player.getAllowFlight());
        data.setFlying(player.isFlying());
        data.setFlySpeed(player.getFlySpeed());

        return data;
    }

    public void applyToPlayer(Player player) {
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType())
        );

        player.setGameMode(gameMode);

        if (inventory != null) {
            ItemStack[] contents = new ItemStack[36];
            System.arraycopy(inventory, 0, contents, 0, Math.min(inventory.length, 36));
            player.getInventory().setContents(contents);
        }
        if (armor != null) {
            player.getInventory().setArmorContents(armor);
        }
        if (offHand != null) {
            player.getInventory().setItemInOffHand(offHand);
        }

        player.getAttribute(MAX_HEALTH_ATTR).setBaseValue(maxHealth);
        player.setHealth(Math.min(health, maxHealth));

        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);

        player.setLevel(level);
        player.setExp(exp);

        if (potionEffects != null) {
            potionEffects.forEach(effectData ->
                    effectData.applyToPlayer(player)
            );
        }

        player.setAllowFlight(allowFlight);
        player.setFlying(flying && allowFlight);
        player.setFlySpeed(flySpeed);

        player.updateInventory();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PotionEffectData {
        private String type;
        private int duration;
        private int amplifier;
        private boolean ambient;
        private boolean particles;
        private boolean icon;

        public static PotionEffectData fromPotionEffect(PotionEffect effect) {
            return new PotionEffectData(
                    effect.getType().getName(),
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.hasParticles(),
                    effect.hasIcon()
            );
        }

        public void applyToPlayer(Player player) {
            PotionEffectType effectType = PotionEffectType.getByName(type);
            if (effectType != null) {
                PotionEffect effect = new PotionEffect(
                        effectType,
                        duration,
                        amplifier,
                        ambient,
                        particles,
                        icon
                );
                player.addPotionEffect(effect);
            }
        }
    }
}

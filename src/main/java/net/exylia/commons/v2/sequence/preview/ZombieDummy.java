package net.exylia.commons.v2.sequence.preview;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.Plugin;

class ZombieDummy implements PreviewDummy {

    private final LivingEntity entity;

    ZombieDummy(Location loc, Player viewer, Plugin plugin) {
        entity = loc.getWorld().spawn(loc, Zombie.class, z -> {
            z.setAI(false);
            z.setSilent(true);
            z.setCanPickupItems(false);
            z.setRemoveWhenFarAway(false);
            z.setShouldBurnInDay(false);
            z.setCustomNameVisible(false);
            EntityEquipment eq = z.getEquipment();
            if (eq != null) {
                eq.setHelmetDropChance(0f);
                eq.setChestplateDropChance(0f);
                eq.setLeggingsDropChance(0f);
                eq.setBootsDropChance(0f);
                eq.setItemInMainHandDropChance(0f);
                eq.setItemInOffHandDropChance(0f);
            }
        });
        EffectPreview.PREVIEW_DUMMIES.add(entity.getUniqueId());
        if (plugin != null) {
            for (Player p : entity.getServer().getOnlinePlayers()) {
                if (!p.equals(viewer)) p.hideEntity(plugin, entity);
            }
        }
    }

    @Override
    public void die() {
        entity.damage(entity.getMaxHealth() * 2);
    }

    @Override
    public void remove() {
        EffectPreview.PREVIEW_DUMMIES.remove(entity.getUniqueId());
        entity.remove();
    }
}

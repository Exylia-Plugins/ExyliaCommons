package net.exylia.commons.v2.cooldown.core;

import net.exylia.commons.v2.cooldown.model.CooldownTrigger;
import net.exylia.commons.v2.cooldown.model.ItemCooldownDefinition;
import org.bukkit.Material;
import org.bukkit.entity.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemCooldownRegistry {

    static final Map<Material, Class<? extends Projectile>> MATERIAL_TO_PROJECTILE;

    static {
        Map<Material, Class<? extends Projectile>> map = new EnumMap<>(Material.class);
        map.put(Material.ENDER_PEARL, EnderPearl.class);
        map.put(Material.SNOWBALL, Snowball.class);
        map.put(Material.EGG, Egg.class);
        map.put(Material.TRIDENT, Trident.class);
        MATERIAL_TO_PROJECTILE = Collections.unmodifiableMap(map);
    }

    private final Map<String, ItemCooldownDefinition> byId = new ConcurrentHashMap<>();
    private final Map<Material, ItemCooldownDefinition> byMaterial = new ConcurrentHashMap<>();
    private final Map<Class<? extends Projectile>, ItemCooldownDefinition> byProjectile = new ConcurrentHashMap<>();

    public void register(ItemCooldownDefinition definition) {
        byId.put(definition.getId(), definition);
        byMaterial.put(definition.getMaterial(), definition);

        if (definition.getTrigger() == CooldownTrigger.LAUNCH) {
            Class<? extends Projectile> projClass = MATERIAL_TO_PROJECTILE.get(definition.getMaterial());
            if (projClass != null) {
                byProjectile.put(projClass, definition);
            }
        }
    }

    public void unregister(String id) {
        ItemCooldownDefinition def = byId.remove(id);
        if (def == null) return;
        byMaterial.remove(def.getMaterial());
        if (def.getTrigger() == CooldownTrigger.LAUNCH) {
            Class<? extends Projectile> projClass = MATERIAL_TO_PROJECTILE.get(def.getMaterial());
            if (projClass != null) byProjectile.remove(projClass);
        }
    }

    public ItemCooldownDefinition getById(String id) {
        return byId.get(id);
    }

    public ItemCooldownDefinition getByMaterial(Material material) {
        return byMaterial.get(material);
    }

    public ItemCooldownDefinition getByProjectile(Class<? extends Projectile> projectileClass) {
        ItemCooldownDefinition def = byProjectile.get(projectileClass);
        if (def != null) return def;
        for (Map.Entry<Class<? extends Projectile>, ItemCooldownDefinition> entry : byProjectile.entrySet()) {
            if (entry.getKey().isAssignableFrom(projectileClass)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Collection<ItemCooldownDefinition> getAll() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public void clear() {
        byId.clear();
        byMaterial.clear();
        byProjectile.clear();
    }
}

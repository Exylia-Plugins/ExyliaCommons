package net.exylia.commons.v2.cooldown.core;

import net.exylia.commons.v2.cooldown.model.CooldownTrigger;
import net.exylia.commons.v2.cooldown.model.ItemCooldownDefinition;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemCooldownRegistry {

    private final Map<String, ItemCooldownDefinition> byId = new ConcurrentHashMap<>();
    private final Map<Material, ItemCooldownDefinition> byMaterial = new ConcurrentHashMap<>();
    private final Map<EntityType, ItemCooldownDefinition> byEntityType = new ConcurrentHashMap<>();

    public void register(ItemCooldownDefinition definition) {
        byId.put(definition.getId(), definition);
        byMaterial.put(definition.getMaterial(), definition);

        if (definition.getTrigger() == CooldownTrigger.LAUNCH) {
            try {
                EntityType entityType = EntityType.valueOf(definition.getMaterial().name());
                byEntityType.put(entityType, definition);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void unregister(String id) {
        ItemCooldownDefinition def = byId.remove(id);
        if (def == null) return;
        byMaterial.remove(def.getMaterial());
        if (def.getTrigger() == CooldownTrigger.LAUNCH) {
            try {
                EntityType entityType = EntityType.valueOf(def.getMaterial().name());
                byEntityType.remove(entityType);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public ItemCooldownDefinition getById(String id) {
        return byId.get(id);
    }

    public ItemCooldownDefinition getByMaterial(Material material) {
        return byMaterial.get(material);
    }

    public ItemCooldownDefinition getByEntityType(EntityType entityType) {
        return byEntityType.get(entityType);
    }

    public Collection<ItemCooldownDefinition> getAll() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public void clear() {
        byId.clear();
        byMaterial.clear();
        byEntityType.clear();
    }
}

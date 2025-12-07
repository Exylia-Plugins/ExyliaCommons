package net.exylia.commons.v2.database.serialization.builtin;

import net.exylia.commons.selection.model.Selection;
import net.exylia.commons.v2.database.serialization.SerializationRegistry;
import net.exylia.commons.v2.region.model.RegionV2;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class SerializerFactory {

    public static void registerBuiltinSerializers() {
        SerializationRegistry registry = SerializationRegistry.getInstance();

        registry.registerSerializer(Location.class, LocationSerializer.INSTANCE);
        registry.registerDeserializer(Location.class, new LocationDeserializer());

        registry.registerSerializer(ItemStack.class, ItemStackSerializer.INSTANCE);
        registry.registerDeserializer(ItemStack.class, new ItemStackDeserializer());

        registry.registerSerializer(Component.class, ComponentSerializer.INSTANCE);
        registry.registerDeserializer(Component.class, new ComponentDeserializer());

        registry.registerSerializer(Selection.class, SelectionSerializer.INSTANCE);
        registry.registerDeserializer(Selection.class, new SelectionDeserializer());

        registry.registerSerializer(RegionV2.class, RegionV2Serializer.INSTANCE);
        registry.registerDeserializer(RegionV2.class, new RegionV2Deserializer());
    }
}

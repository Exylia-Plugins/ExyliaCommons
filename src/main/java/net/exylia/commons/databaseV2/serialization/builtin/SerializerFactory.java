package net.exylia.commons.databaseV2.serialization.builtin;

import net.exylia.commons.databaseV2.serialization.SerializationRegistry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import net.kyori.adventure.text.Component;

public class SerializerFactory {

    public static void registerBuiltinSerializers() {
        SerializationRegistry registry = SerializationRegistry.getInstance();

        registry.registerSerializer(Location.class, LocationSerializer.INSTANCE);
        registry.registerDeserializer(Location.class, new LocationDeserializer());

        registry.registerSerializer(ItemStack.class, ItemStackSerializer.INSTANCE);
        registry.registerDeserializer(ItemStack.class, new ItemStackDeserializer());

        registry.registerSerializer(Component.class, ComponentSerializer.INSTANCE);
        registry.registerDeserializer(Component.class, new ComponentDeserializer());
    }
}

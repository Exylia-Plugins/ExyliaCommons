package net.exylia.commons.v2.database.serialization.builtin;

import net.exylia.commons.v2.database.serialization.Deserializer;
import net.exylia.commons.v2.database.serialization.Serializer;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;

public class ItemStackSerializer implements Serializer<ItemStack> {

    public static final ItemStackSerializer INSTANCE = new ItemStackSerializer();

    @Override
    public String serialize(ItemStack value) {
        if (value == null || value.getType().isAir()) {
            return null;
        }

        try {
            return Base64.getEncoder().encodeToString(value.serializeAsBytes());
        } catch (Exception e) {
            return null;
        }
    }
}

class ItemStackDeserializer implements Deserializer<ItemStack> {

    @Override
    public ItemStack deserialize(String value, Class<ItemStack> type) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(value));
        } catch (Exception e) {
            return null;
        }
    }
}

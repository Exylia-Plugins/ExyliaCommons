package net.exylia.commons.v2.database.serialization.builtin;

import net.exylia.commons.selection.model.Selection;
import net.exylia.commons.v2.database.serialization.SerializationRegistry;
import net.exylia.commons.v2.hologram.model.HologramData;
import net.exylia.commons.v2.region.model.Region;
import net.exylia.commons.v2.scoreboard.model.ScoreboardData;
import net.exylia.commons.v2.snapshot.model.SnapshotData;
import net.exylia.commons.v2.visual.model.ActionBarData;
import net.exylia.commons.v2.visual.model.BossBarData;
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

        registry.registerSerializer(Region.class, RegionSerializer.INSTANCE);
        registry.registerDeserializer(Region.class, new RegionDeserializer());

        registry.registerSerializer(BossBarData.class, BossBarDataSerializer.INSTANCE);
        registry.registerDeserializer(BossBarData.class, new BossBarDataDeserializer());

        registry.registerSerializer(ActionBarData.class, ActionBarDataSerializer.INSTANCE);
        registry.registerDeserializer(ActionBarData.class, new ActionBarDataDeserializer());

        registry.registerSerializer(ScoreboardData.class, ScoreboardDataSerializer.INSTANCE);
        registry.registerDeserializer(ScoreboardData.class, new ScoreboardDataDeserializer());

        registry.registerSerializer(HologramData.class, HologramDataSerializer.INSTANCE);
        registry.registerDeserializer(HologramData.class, new HologramDataDeserializer());

        registry.registerSerializer(SnapshotData.class, SnapshotDataSerializer.INSTANCE);
        registry.registerDeserializer(SnapshotData.class, new SnapshotDataDeserializer());
    }
}

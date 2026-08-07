package net.exylia.commons.v2.hologram.protocol;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import net.exylia.commons.v2.hologram.model.HologramLine;
import net.exylia.commons.v2.hologram.model.HologramProperties;
import net.exylia.commons.v2.hologram.model.HologramType;
import net.exylia.commons.v2.hologram.renderer.LineRenderer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Renders a single hologram line entirely through PacketEvents: a
 * {@code SpawnEntity} + {@code EntityMetadata} packet pair on show, and a
 * {@code DestroyEntities} packet on hide. No real Bukkit entity is ever
 * created, so nothing can be left orphaned in a chunk after a crash or a
 * rough server restart — a known issue with real Display entities, which
 * some server implementations persist to the chunk NBT even when
 * {@code setPersistent(false)} is set.
 * <p>
 * Supports the three Display subtypes (TextDisplay, ItemDisplay,
 * BlockDisplay) using the shared Display metadata layout (indices 0 and
 * 8-22, stable since 1.19.4/protocol 762 through 1.21.x) plus each
 * subtype's own trailing fields.
 */
public class PacketLineRenderer implements LineRenderer {

    private final int entityId;
    private final UUID uuid = UUID.randomUUID();
    private final HologramLine line;

    private volatile Location location;
    private volatile HologramProperties properties;
    private volatile Component text;
    private volatile float currentAngle = 0f;

    private final CopyOnWriteArraySet<UUID> viewers = new CopyOnWriteArraySet<>();
    /** Per-viewer text override, used only in per-player hologram mode. */
    private final java.util.Map<UUID, Component> perViewerText = new java.util.concurrent.ConcurrentHashMap<>();

    public PacketLineRenderer(Location location, HologramLine line, HologramProperties properties) {
        this.entityId = SpigotReflectionUtil.generateEntityId();
        this.location = location.clone();
        this.line = line;
        this.properties = properties;
        this.text = line.getComponent() != null ? line.getComponent() : Component.text(
                line.getText() != null ? line.getText() : "");
    }

    private EntityType entityType() {
        return switch (line.getType()) {
            case TEXT -> EntityTypes.TEXT_DISPLAY;
            case ITEM -> EntityTypes.ITEM_DISPLAY;
            case BLOCK -> EntityTypes.BLOCK_DISPLAY;
        };
    }

    @Override
    public void spawnFor(Player player) {
        if (!player.isOnline()) return;
        User user = getUser(player);
        if (user == null) return;

        Location loc = location;
        Vector3d position = new Vector3d(loc.getX(), loc.getY(), loc.getZ());
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId, Optional.of(uuid), entityType(), position, 0f, 0f, 0f, 0, Optional.empty());
        user.sendPacket(spawnPacket);
        Component viewerText = perViewerText.get(player.getUniqueId());
        user.sendPacket(buildMetadataPacket(viewerText != null ? viewerText : text));

        viewers.add(player.getUniqueId());
    }

    @Override
    public void despawnFor(Player player) {
        perViewerText.remove(player.getUniqueId());
        if (!viewers.remove(player.getUniqueId())) return;
        if (!player.isOnline()) return;
        User user = getUser(player);
        if (user == null) return;
        user.sendPacket(new WrapperPlayServerDestroyEntities(entityId));
    }

    @Override
    public void despawnForAll() {
        for (UUID viewerId : new ArrayList<>(viewers)) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player != null) {
                despawnFor(player);
            } else {
                viewers.remove(viewerId);
            }
        }
    }

    @Override
    public boolean isViewing(Player player) {
        return viewers.contains(player.getUniqueId());
    }

    @Override
    public void updateText(Component text) {
        if (line.getType() != HologramType.TEXT) return;
        this.text = text;
        broadcastMetadata();
    }

    @Override
    public void updateTextFor(Player player, Component text) {
        if (line.getType() != HologramType.TEXT) return;
        perViewerText.put(player.getUniqueId(), text);
        if (!player.isOnline()) return;
        User user = getUser(player);
        if (user == null) return;
        user.sendPacketSilently(buildMetadataPacket(text));
    }

    @Override
    public void updateProperties(HologramProperties properties) {
        this.properties = properties;
        broadcastMetadata();
    }

    @Override
    public void teleport(Location location) {
        this.location = location.clone();
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                entityId,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                0f, 0f, false);
        for (UUID viewerId : viewers) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player == null || !player.isOnline()) continue;
            User user = getUser(player);
            if (user != null) user.sendPacketSilently(packet);
        }
    }

    @Override
    public void rotateTo(float angleRadians) {
        this.currentAngle = angleRadians;
        broadcastMetadata();
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public int getEntityId() {
        return entityId;
    }

    private void broadcastMetadata() {
        for (UUID viewerId : viewers) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player == null || !player.isOnline()) continue;
            User user = getUser(player);
            if (user == null) continue;
            Component viewerText = perViewerText.get(viewerId);
            user.sendPacketSilently(buildMetadataPacket(viewerText != null ? viewerText : text));
        }
    }

    private WrapperPlayServerEntityMetadata buildMetadataPacket(Component displayText) {
        List<EntityData<?>> data = new ArrayList<>();
        HologramProperties props = properties;

        byte sharedFlags = 0;
        if (props.isGlowing()) sharedFlags |= 0x40;
        data.add(new EntityData<>(0, EntityDataTypes.BYTE, sharedFlags));

        // Display base (indices 8-22)
        data.add(new EntityData<>(8, EntityDataTypes.INT, 0));
        data.add(new EntityData<>(9, EntityDataTypes.INT, 2));
        data.add(new EntityData<>(11, EntityDataTypes.VECTOR3F, new Vector3f(0f, 0f, 0f)));
        data.add(new EntityData<>(12, EntityDataTypes.VECTOR3F,
                new Vector3f(props.getScaleX(), props.getScaleY(), props.getScaleZ())));

        float halfAngle = currentAngle / 2f;
        Quaternion4f leftRotation = new Quaternion4f(
                0f, (float) Math.sin(halfAngle), 0f, (float) Math.cos(halfAngle));
        data.add(new EntityData<>(13, EntityDataTypes.QUATERNION, leftRotation));

        data.add(new EntityData<>(15, EntityDataTypes.BYTE, billboardByte(props.getBillboard())));

        if (props.getBrightness() >= 0) {
            int sky = (props.getBrightness() >> 4) & 0xF;
            int block = props.getBrightness() & 0xF;
            data.add(new EntityData<>(16, EntityDataTypes.INT, (block << 4) | (sky << 20)));
        }

        if (props.getGlowColorOverride() != null) {
            data.add(new EntityData<>(22, EntityDataTypes.INT, props.getGlowColorOverride().asRGB()));
        }

        switch (line.getType()) {
            case TEXT -> appendTextData(data, props, displayText);
            case ITEM -> appendItemData(data);
            case BLOCK -> appendBlockData(data);
        }

        return new WrapperPlayServerEntityMetadata(entityId, data);
    }

    private void appendTextData(List<EntityData<?>> data, HologramProperties props, Component displayText) {
        data.add(new EntityData<>(23, EntityDataTypes.ADV_COMPONENT, displayText != null ? displayText : Component.empty()));
        data.add(new EntityData<>(24, EntityDataTypes.INT, props.getLineWidth()));

        // Mirrors BukkitLineRenderer/vanilla semantics: only compute a custom
        // background value when the caller explicitly asked for one (via
        // backgroundColor(...) or a non-default backgroundAlpha(...));
        // otherwise send the real vanilla default (translucent black,
        // 0x40000000) instead of synthesizing an opaque-black background
        // from the builder's "unset" defaults (backgroundColor=null,
        // backgroundAlpha=255 — HologramProperties' @Builder.Default).
        int backgroundColor;
        if (props.isDefaultBackground()) {
            backgroundColor = 0x40000000;
        } else if (props.getBackgroundColor() != null) {
            backgroundColor = (props.getBackgroundAlpha() << 24) | (props.getBackgroundColor().asRGB() & 0xFFFFFF);
        } else if (props.getBackgroundAlpha() == 255) {
            backgroundColor = 0x40000000;
        } else {
            backgroundColor = props.getBackgroundAlpha() << 24;
        }
        data.add(new EntityData<>(25, EntityDataTypes.INT, backgroundColor));
        data.add(new EntityData<>(26, EntityDataTypes.BYTE, props.getTextOpacity()));

        byte textFlags = 0;
        if (props.isShadow()) textFlags |= 0x01;
        if (props.isSeeThrough()) textFlags |= 0x02;
        if (props.isDefaultBackground()) textFlags |= 0x04;
        textFlags |= alignmentBits(props.getAlignment());
        data.add(new EntityData<>(27, EntityDataTypes.BYTE, textFlags));
    }

    private void appendItemData(List<EntityData<?>> data) {
        org.bukkit.inventory.ItemStack bukkitItem = line.getItem();
        ItemStack peItem = bukkitItem != null
                ? SpigotConversionUtil.fromBukkitItemStack(bukkitItem)
                : ItemStack.EMPTY;
        data.add(new EntityData<>(23, EntityDataTypes.ITEMSTACK, peItem));

        ItemDisplay.ItemDisplayTransform transform = line.getItemTransform() != null
                ? line.getItemTransform() : ItemDisplay.ItemDisplayTransform.FIXED;
        data.add(new EntityData<>(24, EntityDataTypes.BYTE, (byte) transform.ordinal()));
    }

    private void appendBlockData(List<EntityData<?>> data) {
        org.bukkit.block.data.BlockData blockData = line.getBlock();
        int globalId;
        if (blockData != null) {
            WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(blockData);
            globalId = state.getGlobalId();
        } else {
            globalId = 0;
        }
        data.add(new EntityData<>(23, EntityDataTypes.BLOCK_STATE, globalId));
    }

    private byte billboardByte(org.bukkit.entity.Display.Billboard billboard) {
        return switch (billboard) {
            case FIXED -> 0;
            case VERTICAL -> 1;
            case HORIZONTAL -> 2;
            case CENTER -> 3;
        };
    }

    private byte alignmentBits(org.bukkit.entity.TextDisplay.TextAlignment alignment) {
        return switch (alignment) {
            case CENTER -> 0x00;
            case LEFT -> 0x08;
            case RIGHT -> 0x10;
        };
    }

    private User getUser(Player player) {
        try {
            return PacketEvents.getAPI().getPlayerManager().getUser(player);
        } catch (Exception e) {
            return null;
        }
    }
}

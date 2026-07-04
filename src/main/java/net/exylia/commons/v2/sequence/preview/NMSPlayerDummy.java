package net.exylia.commons.v2.sequence.preview;

import com.mojang.authlib.GameProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

class NMSPlayerDummy implements PreviewDummy {

    private final Method send;
    private final Object conn;
    private final Object deathPacket;
    private final Object removeEntitiesPacket;
    private final Object removeInfoPacket;

    NMSPlayerDummy(Location loc, Player viewer) throws ReflectiveOperationException {
        Class<?> craftPlayerCls = craftClass("entity.CraftPlayer");
        Object viewerNMS = craftPlayerCls.getMethod("getHandle").invoke(viewer);
        Field connField = findField(viewerNMS.getClass(), "connection");
        connField.setAccessible(true);
        conn = connField.get(viewerNMS);

        Class<?> packetCls = Class.forName("net.minecraft.network.protocol.Packet");
        send = findMethod(conn.getClass(), "send", packetCls);
        send.setAccessible(true);

        UUID fakeUuid = UUID.randomUUID();
        GameProfile profile = new GameProfile(fakeUuid, "Preview");

        Class<?> msCls = Class.forName("net.minecraft.server.MinecraftServer");
        Class<?> levelCls = Class.forName("net.minecraft.server.level.ServerLevel");
        Class<?> spCls = Class.forName("net.minecraft.server.level.ServerPlayer");

        Object ms = craftClass("CraftServer").getMethod("getServer").invoke(Bukkit.getServer());
        Object level = craftClass("CraftWorld").getMethod("getHandle").invoke(loc.getWorld());

        Object sp;
        try {
            Class<?> ciCls = Class.forName("net.minecraft.server.level.ClientInformation");
            Object ci = ciCls.getMethod("createDefault").invoke(null);
            sp = spCls.getConstructor(msCls, levelCls, GameProfile.class, ciCls)
                    .newInstance(ms, level, profile, ci);
        } catch (Exception e) {
            sp = spCls.getConstructor(msCls, levelCls, GameProfile.class)
                    .newInstance(ms, level, profile);
        }

        float faceYaw = viewer.getLocation().getYaw() + 180f;
        if (faceYaw > 180f) faceYaw -= 360f;

        findMethod(spCls, "setPos", double.class, double.class, double.class)
                .invoke(sp, loc.getX(), loc.getY(), loc.getZ());
        try { findMethod(spCls, "setYRot", float.class).invoke(sp, faceYaw); } catch (Exception ignored) {}
        try { findMethod(spCls, "setXRot", float.class).invoke(sp, 0f); } catch (Exception ignored) {}
        try { findMethod(spCls, "setYHeadRot", float.class).invoke(sp, faceYaw); } catch (Exception ignored) {}

        int eid = (int) findMethod(spCls, "getId").invoke(sp);

        Class<?> infoPktCls = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
        Object infoPacket = buildInfoPacket(sp, infoPktCls);
        Object spawnPacket = buildSpawnPacket(sp);

        Class<?> entityCls = Class.forName("net.minecraft.world.entity.Entity");
        Class<?> evtCls = Class.forName("net.minecraft.network.protocol.game.ClientboundEntityEventPacket");
        deathPacket = evtCls.getConstructor(entityCls, byte.class).newInstance(sp, (byte) 3);

        Class<?> removeEntCls = Class.forName("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");
        removeEntitiesPacket = removeEntCls.getConstructor(int[].class).newInstance(new int[]{eid});

        Class<?> removeInfoCls = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket");
        removeInfoPacket = removeInfoCls.getConstructor(List.class).newInstance(List.of(fakeUuid));

        send.invoke(conn, infoPacket);
        send.invoke(conn, spawnPacket);

        Object metaPacket = buildEntityDataPacket(sp, spCls, eid);
        if (metaPacket != null) {
            send.invoke(conn, metaPacket);
        }
    }

    @Override
    public void die() {
        try { send.invoke(conn, deathPacket); } catch (Exception ignored) {}
    }

    @Override
    public void remove() {
        try { send.invoke(conn, removeEntitiesPacket); } catch (Exception ignored) {}
        try { send.invoke(conn, removeInfoPacket); } catch (Exception ignored) {}
    }

    private static Object buildEntityDataPacket(Object sp, Class<?> spCls, int eid) {
        try {
            Class<?> entityCls = Class.forName("net.minecraft.world.entity.Entity");
            Field entityDataField = findField(entityCls, "entityData");
            entityDataField.setAccessible(true);
            Object synchedData = entityDataField.get(sp);

            Object values = null;
            try {
                Method packDirty = findMethod(synchedData.getClass(), "packDirty");
                values = packDirty.invoke(synchedData);
            } catch (Exception ignored) {}
            if (values == null) {
                try {
                    Method nonDefault = findMethod(synchedData.getClass(), "getNonDefaultValues");
                    values = nonDefault.invoke(synchedData);
                } catch (Exception ignored) {}
            }

            if (values == null) return null;

            Class<?> metaCls = Class.forName("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
            Constructor<?> metaCtor = metaCls.getConstructor(int.class, List.class);
            return metaCtor.newInstance(eid, values);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ExyliaCommons] NMSPlayerDummy entity data failed: " + e);
            return null;
        }
    }

    private static Object buildInfoPacket(Object sp, Class<?> infoPktCls) throws ReflectiveOperationException {
        try {
            Method factory = infoPktCls.getDeclaredMethod("createPlayerInitializing", java.util.Collection.class);
            factory.setAccessible(true);
            return factory.invoke(null, List.of(sp));
        } catch (NoSuchMethodException ignored) {}

        Class<?> actionCls = Class.forName(infoPktCls.getName() + "$Action");
        Object addPlayer = null;
        for (Object c : actionCls.getEnumConstants()) {
            if (((Enum<?>) c).name().equals("ADD_PLAYER")) { addPlayer = c; break; }
        }
        if (addPlayer == null) throw new NoSuchFieldException("ADD_PLAYER");
        @SuppressWarnings({"unchecked", "rawtypes"})
        EnumSet<?> set = EnumSet.of((Enum) addPlayer);
        Constructor<?> ctor = infoPktCls.getConstructor(EnumSet.class, Iterable.class);
        return ctor.newInstance(set, List.of(sp));
    }

    private static Object buildSpawnPacket(Object sp) throws ReflectiveOperationException {
        Class<?> entityCls = Class.forName("net.minecraft.world.entity.Entity");
        try {
            Class<?> cls = Class.forName("net.minecraft.network.protocol.game.ClientboundAddPlayerPacket");
            return cls.getConstructor(entityCls).newInstance(sp);
        } catch (ClassNotFoundException ignored) {}
        Class<?> cls = Class.forName("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
        try {
            return cls.getConstructor(entityCls).newInstance(sp);
        } catch (NoSuchMethodException ignored) {}
        return cls.getConstructor(entityCls, int.class).newInstance(sp, 0);
    }

    private static Class<?> craftClass(String name) throws ClassNotFoundException {
        try {
            return Class.forName("org.bukkit.craftbukkit." + name);
        } catch (ClassNotFoundException e) {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            String ver = pkg.replace("org.bukkit.craftbukkit.", "");
            return Class.forName("org.bukkit.craftbukkit." + ver + "." + name);
        }
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) throws NoSuchMethodException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredMethod(name, params); } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(name);
    }
}

package net.exylia.commons.v2.sequence.preview;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.BitSet;

class NMSChunkHandler implements ChunkHandler {

    private final Method getHandle;
    private final Field connectionField;
    private final Method sendMethod;
    private final Constructor<?> forgetPacketCtor;
    private final Constructor<?> chunkPosCtor;

    NMSChunkHandler() throws ReflectiveOperationException {
        Class<?> craftPlayerCls = findCraftClass("entity.CraftPlayer");
        getHandle = craftPlayerCls.getMethod("getHandle");

        Class<?> serverPlayerCls = getHandle.getReturnType();
        connectionField = findField(serverPlayerCls, "connection");
        connectionField.setAccessible(true);

        Class<?> forgetPacketCls = Class.forName(
                "net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket");
        Constructor<?> cpCtor = null;
        Constructor<?> ctor;
        try {
            Class<?> chunkPosCls = Class.forName("net.minecraft.world.level.ChunkPos");
            cpCtor = chunkPosCls.getConstructor(int.class, int.class);
            ctor = forgetPacketCls.getConstructor(chunkPosCls);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            ctor = forgetPacketCls.getConstructor(int.class, int.class);
        }
        this.forgetPacketCtor = ctor;
        this.chunkPosCtor = cpCtor;

        Class<?> packetCls = Class.forName("net.minecraft.network.protocol.Packet");
        sendMethod = findMethod(connectionField.getType(), "send", packetCls);
        sendMethod.setAccessible(true);
    }

    @Override
    public void clear(Player player, int radius) {
        try {
            int cx = player.getLocation().getBlockX() >> 4;
            int cz = player.getLocation().getBlockZ() >> 4;
            Object handle = getHandle.invoke(player);
            Object conn = connectionField.get(handle);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Object packet = chunkPosCtor != null
                            ? forgetPacketCtor.newInstance(chunkPosCtor.newInstance(cx + dx, cz + dz))
                            : forgetPacketCtor.newInstance(cx + dx, cz + dz);
                    sendMethod.invoke(conn, packet);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ExyliaCommons] NMSChunkHandler clear failed: " + e);
        }
    }

    @Override
    public void restore(Player player, int radius) {
        ChunkResendSupport.restore(player, radius);
    }

    private static Class<?> findCraftClass(String name) throws ClassNotFoundException {
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
        throw new NoSuchFieldException(name + " in " + cls.getName());
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) throws NoSuchMethodException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredMethod(name, params); } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(name + " in " + cls.getName());
    }
}

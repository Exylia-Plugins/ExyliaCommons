package net.exylia.commons.v2.sequence.preview;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.logging.Level;

/**
 * Shared NMS-based real chunk resend logic. A fake chunk "unload" packet only hides
 * chunks on the client; the server never forgets it already sent them, so a plain
 * re-teleport to the same location does not make it resend anything. The only reliable
 * fix is to rebuild and resend the actual chunk packet, which is what this class does.
 * Used by both {@link NMSChunkHandler} and {@link PacketEventsChunkHandler}, since the
 * latter has no equivalent of its own for reconstructing real chunk packet data.
 */
final class ChunkResendSupport {

    private static final Method GET_HANDLE;
    private static final java.lang.reflect.Field CONNECTION_FIELD;
    private static final Method SEND_METHOD;
    private static final Method WORLD_GET_HANDLE;
    private static final Method LIGHT_ENGINE_METHOD;
    private static final Method GET_CHUNK_METHOD;
    private static final Constructor<?> CHUNK_WITH_LIGHT_CTOR;

    static {
        Method getHandle = null;
        java.lang.reflect.Field connectionField = null;
        Method sendMethod = null;
        Method worldGetHandle = null;
        Method lightEngineMethod = null;
        Method getChunkMethod = null;
        Constructor<?> chunkWithLightCtor = null;
        try {
            Class<?> craftPlayerCls = findCraftClass("entity.CraftPlayer");
            getHandle = craftPlayerCls.getMethod("getHandle");

            Class<?> serverPlayerCls = getHandle.getReturnType();
            connectionField = findField(serverPlayerCls, "connection");
            connectionField.setAccessible(true);

            Class<?> packetCls = Class.forName("net.minecraft.network.protocol.Packet");
            sendMethod = findMethod(connectionField.getType(), "send", packetCls);
            sendMethod.setAccessible(true);

            // CraftWorld#getHandle() is a stable CraftBukkit wrapper, unlike the
            // obfuscated ServerPlayer accessor for its level (renamed across versions:
            // seen as both "serverLevel()" and other names depending on the mapping run).
            Class<?> craftWorldCls = findCraftClass("CraftWorld");
            worldGetHandle = craftWorldCls.getMethod("getHandle");
            Class<?> serverLevelCls = worldGetHandle.getReturnType();

            lightEngineMethod = findMethod(serverLevelCls, "getLightEngine");
            lightEngineMethod.setAccessible(true);

            getChunkMethod = findMethod(serverLevelCls, "getChunk", int.class, int.class);
            getChunkMethod.setAccessible(true);

            Class<?> levelChunkCls = Class.forName("net.minecraft.world.level.chunk.LevelChunk");
            Class<?> cwlCls = Class.forName(
                    "net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket");

            for (Constructor<?> c : cwlCls.getDeclaredConstructors()) {
                Class<?>[] pts = c.getParameterTypes();
                if (pts.length == 4
                        && pts[0].isAssignableFrom(levelChunkCls)
                        && pts[2] == BitSet.class
                        && pts[3] == BitSet.class) {
                    c.setAccessible(true);
                    chunkWithLightCtor = c;
                    break;
                }
            }
            if (chunkWithLightCtor == null) {
                throw new NoSuchMethodException(cwlCls.getName() + "(LevelChunk, LightEngine, BitSet, BitSet)");
            }
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.WARNING, "[ExyliaCommons] ChunkResendSupport setup failed, " +
                    "chunks will not reappear correctly after preview: " + t, t);
            getHandle = null;
            connectionField = null;
            sendMethod = null;
            worldGetHandle = null;
            lightEngineMethod = null;
            getChunkMethod = null;
            chunkWithLightCtor = null;
        }
        GET_HANDLE = getHandle;
        CONNECTION_FIELD = connectionField;
        SEND_METHOD = sendMethod;
        WORLD_GET_HANDLE = worldGetHandle;
        LIGHT_ENGINE_METHOD = lightEngineMethod;
        GET_CHUNK_METHOD = getChunkMethod;
        CHUNK_WITH_LIGHT_CTOR = chunkWithLightCtor;
    }

    private ChunkResendSupport() {}

    static boolean isAvailable() {
        return WORLD_GET_HANDLE != null && CHUNK_WITH_LIGHT_CTOR != null;
    }

    static void restore(Player player, int radius) {
        if (!isAvailable()) {
            player.teleport(player.getLocation());
            return;
        }
        try {
            int cx = player.getLocation().getBlockX() >> 4;
            int cz = player.getLocation().getBlockZ() >> 4;
            Object handle = GET_HANDLE.invoke(player);
            Object conn = CONNECTION_FIELD.get(handle);
            Object level = WORLD_GET_HANDLE.invoke(player.getWorld());
            Object lightEngine = LIGHT_ENGINE_METHOD.invoke(level);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Object chunk = GET_CHUNK_METHOD.invoke(level, cx + dx, cz + dz);
                    if (chunk == null) continue;
                    Object packet = CHUNK_WITH_LIGHT_CTOR.newInstance(chunk, lightEngine, null, null);
                    SEND_METHOD.invoke(conn, packet);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[ExyliaCommons] ChunkResendSupport restore failed: " + e, e);
            player.teleport(player.getLocation());
        }
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

    private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name + " in " + cls.getName());
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) throws NoSuchMethodException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredMethod(name, params); } catch (NoSuchMethodException ignored) {}
            Method fromInterface = findMethodInInterfaces(c, name, params);
            if (fromInterface != null) return fromInterface;
        }
        throw new NoSuchMethodException(name + " in " + cls.getName());
    }

    private static Method findMethodInInterfaces(Class<?> cls, String name, Class<?>... params) {
        for (Class<?> iface : cls.getInterfaces()) {
            try {
                return iface.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException ignored) {}
            Method nested = findMethodInInterfaces(iface, name, params);
            if (nested != null) return nested;
        }
        return null;
    }
}

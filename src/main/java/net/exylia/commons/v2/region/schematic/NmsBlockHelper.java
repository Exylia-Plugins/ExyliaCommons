package net.exylia.commons.v2.region.schematic;

import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

final class NmsBlockHelper {

    // Block.UPDATE_CLIENTS(2) | Block.UPDATE_KNOWN_SHAPE(16)
    // Skips neighbor shape notifications during bulk paste while keeping client updates and light
    static final int PASTE_FLAGS = 2 | 16;

    private static final MethodHandle CRAFT_WORLD_GET_HANDLE;
    private static final MethodHandle CRAFT_BLOCK_DATA_GET_STATE;
    private static final MethodHandle MUTABLE_BLOCK_POS_CTOR;
    private static final MethodHandle MUTABLE_BLOCK_POS_SET;
    private static final MethodHandle SERVER_LEVEL_SET_BLOCK;
    static final boolean AVAILABLE;

    static {
        MethodHandle craftWorldGetHandle = null;
        MethodHandle craftBlockDataGetState = null;
        MethodHandle mutableBlockPosCtor = null;
        MethodHandle mutableBlockPosSet = null;
        MethodHandle serverLevelSetBlock = null;
        boolean ok = false;

        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            Class<?> craftWorldClass;
            try {
                craftWorldClass = Class.forName("org.bukkit.craftbukkit.CraftWorld");
            } catch (ClassNotFoundException ex) {
                String pkg = Bukkit.getServer().getClass().getPackage().getName();
                String ver = pkg.substring(pkg.lastIndexOf('.') + 1);
                craftWorldClass = Class.forName("org.bukkit.craftbukkit." + ver + ".CraftWorld");
            }

            Method handleMethod = craftWorldClass.getMethod("getHandle");
            Class<?> serverLevelClass = handleMethod.getReturnType();
            craftWorldGetHandle = lookup.unreflect(handleMethod)
                .asType(MethodType.methodType(Object.class, Object.class));

            String craftBlockDataName = craftWorldClass.getName()
                .replace(".CraftWorld", ".block.data.CraftBlockData");
            Class<?> craftBlockDataClass = Class.forName(craftBlockDataName);
            craftBlockDataGetState = lookup.unreflect(craftBlockDataClass.getMethod("getState"))
                .asType(MethodType.methodType(Object.class, Object.class));

            Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
            Class<?> mutableBlockPosClass = Class.forName("net.minecraft.core.BlockPos$MutableBlockPos");
            Class<?> blockStateClass = Class.forName("net.minecraft.world.level.block.state.BlockState");

            Constructor<?> ctor = mutableBlockPosClass.getConstructor();
            mutableBlockPosCtor = lookup.unreflectConstructor(ctor)
                .asType(MethodType.methodType(Object.class));

            mutableBlockPosSet = lookup.unreflect(
                mutableBlockPosClass.getMethod("set", int.class, int.class, int.class)
            ).asType(MethodType.methodType(void.class, Object.class, int.class, int.class, int.class));

            MethodHandle rawSetBlock = lookup.unreflect(
                serverLevelClass.getMethod("setBlock", blockPosClass, blockStateClass, int.class)
            ).asType(MethodType.methodType(void.class, Object.class, Object.class, Object.class, int.class));
            serverLevelSetBlock = MethodHandles.insertArguments(rawSetBlock, 3, PASTE_FLAGS);

            ok = true;
        } catch (Exception e) {
            DebugAPI.logLibDebug("[SchematicEngine] NMS optimized block set unavailable: " + e.getMessage() + ", using Bukkit fallback");
        }

        CRAFT_WORLD_GET_HANDLE = craftWorldGetHandle;
        CRAFT_BLOCK_DATA_GET_STATE = craftBlockDataGetState;
        MUTABLE_BLOCK_POS_CTOR = mutableBlockPosCtor;
        MUTABLE_BLOCK_POS_SET = mutableBlockPosSet;
        SERVER_LEVEL_SET_BLOCK = serverLevelSetBlock;
        AVAILABLE = ok;
    }

    private NmsBlockHelper() {}

    static Object getServerLevel(World world) throws Throwable {
        return CRAFT_WORLD_GET_HANDLE.invokeExact((Object) world);
    }

    static Object[] toNmsStates(BlockData[] palette) throws Throwable {
        Object[] states = new Object[palette.length];
        for (int i = 0; i < palette.length; i++) {
            states[i] = CRAFT_BLOCK_DATA_GET_STATE.invokeExact((Object) palette[i]);
        }
        return states;
    }

    static Object createMutableBlockPos() throws Throwable {
        return MUTABLE_BLOCK_POS_CTOR.invokeExact();
    }

    static void setBlock(Object serverLevel, Object mutablePos, int x, int y, int z, Object nmsState) throws Throwable {
        MUTABLE_BLOCK_POS_SET.invokeExact(mutablePos, x, y, z);
        SERVER_LEVEL_SET_BLOCK.invokeExact(serverLevel, mutablePos, nmsState);
    }
}

package net.exylia.commons.v2.items.processor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.exylia.commons.v2.compat.SoundCompat;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.items.utils.AttributeManager;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConsumableProcessor {

    private static Boolean available = null;
    private static Class<?> dataComponentTypesClass = null;
    private static Class<?> foodClass = null;
    private static Class<?> consumableClass = null;
    private static Class<?> itemAnimationClass = null;
    private static Method foodFactoryMethod = null;
    private static Method consumableFactoryMethod = null;
    private static Method setDataMethod = null;
    private static boolean setDataPassBuilder = true;
    private static Object eatAnimation = null;
    private static Object foodType = null;
    private static Object consumableType = null;

    public static void apply(ItemStack itemStack, boolean forceConsumable, float consumableTime,
                             int nutrition, float saturation, String soundKey) {
        if (itemStack == null || !forceConsumable) return;
        if (!isAvailable()) {
            DebugAPI.logLibWarn(DebugCategory.ITEMS, "ConsumableProcessor not available — DataComponent API missing");
            return;
        }
        try {
            applyFood(itemStack, nutrition, saturation);
            applyConsumable(itemStack, consumableTime, resolveKey(soundKey));
            AttributeManager.applyBlockInteractionRange(itemStack, -1.0);
        } catch (Exception e) {
            DebugAPI.logLibError(DebugCategory.ITEMS, "ConsumableProcessor.apply failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                DebugAPI.logLibError(DebugCategory.ITEMS, "  Caused by: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
        }
    }

    private static Key resolveKey(String soundString) {
        if (soundString == null || soundString.isBlank()) return null;
        String name = soundString.split("\\|")[0].trim();
        String keyStr = SoundCompat.keyStringOf(name);
        if (keyStr == null) {
            DebugAPI.logLibWarn(DebugCategory.ITEMS, "ConsumableProcessor: sound '" + name + "' not resolved — using default");
            return null;
        }
        return Key.key(keyStr);
    }

    private static void applyFood(ItemStack itemStack, int nutrition, float saturation) throws Exception {
        Object builder = foodFactoryMethod.invoke(null);
        builder = invoke(builder, "nutrition", new Class[]{int.class}, nutrition);
        builder = invoke(builder, "saturation", new Class[]{float.class}, saturation);
        builder = invoke(builder, "canAlwaysEat", new Class[]{boolean.class}, true);
        Object value = setDataPassBuilder ? builder : invoke(builder, "build", new Class[0]);
        setDataMethod.invoke(itemStack, foodType, value);
    }

    private static void applyConsumable(ItemStack itemStack, float time, Key sound) throws Exception {
        Object builder = consumableFactoryMethod.invoke(null);
        builder = invoke(builder, "consumeSeconds", new Class[]{float.class}, time);
        builder = invoke(builder, "animation", new Class[]{itemAnimationClass}, eatAnimation);
        if (sound != null) {
            builder = invoke(builder, "sound", new Class[]{Key.class}, sound);
        }
        Object value = setDataPassBuilder ? builder : invoke(builder, "build", new Class[0]);
        setDataMethod.invoke(itemStack, consumableType, value);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = target.getClass().getMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Method findSetDataMethod() {
        for (Method m : ItemStack.class.getMethods()) {
            if (!m.getName().equals("setData") || m.getParameterCount() != 2) continue;
            m.setAccessible(true);
            // Older Paper: second param is Object (expects built value via Handleable)
            // Newer Paper: second param is DataComponentBuilder (pass builder directly)
            setDataPassBuilder = !m.getParameterTypes()[1].equals(Object.class);
            return m;
        }
        DebugAPI.logLibWarn(DebugCategory.ITEMS, "ConsumableProcessor: setData not found on ItemStack");
        return null;
    }

    @SuppressWarnings("unchecked")
    private static boolean isAvailable() {
        if (available != null) return available;
        try {
            dataComponentTypesClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");

            foodClass = resolveComponentClass("FOOD");
            if (foodClass == null) throw new IllegalStateException("Could not resolve Food class from FOOD field");

            consumableClass = resolveComponentClass("CONSUMABLE");
            if (consumableClass == null) throw new IllegalStateException("Could not resolve Consumable class from CONSUMABLE field");

            foodFactoryMethod = findStaticBuilderFactory(foodClass);
            if (foodFactoryMethod == null) throw new IllegalStateException("No builder factory found on " + foodClass.getName());

            consumableFactoryMethod = findStaticBuilderFactory(consumableClass);
            if (consumableFactoryMethod == null) throw new IllegalStateException("No builder factory found on " + consumableClass.getName());

            itemAnimationClass = resolveAnimationClass(consumableFactoryMethod);
            if (itemAnimationClass == null) throw new IllegalStateException("Could not find ItemAnimation type for Consumable.animation()");

            eatAnimation = resolveEatConstant(itemAnimationClass);
            if (eatAnimation == null) throw new IllegalStateException("Could not find EAT constant in " + itemAnimationClass.getName());

            foodType = dataComponentTypesClass.getField("FOOD").get(null);
            consumableType = dataComponentTypesClass.getField("CONSUMABLE").get(null);

            setDataMethod = findSetDataMethod();
            if (setDataMethod == null) throw new IllegalStateException("Could not find ItemStack.setData(type, value) method");

            available = true;
        } catch (Exception e) {
            DebugAPI.logLibError(DebugCategory.ITEMS, "ConsumableProcessor init failed: " + e.getMessage());
            available = false;
        }
        return available;
    }

    private static Class<?> resolveComponentClass(String fieldName) {
        try {
            Field field = dataComponentTypesClass.getField(fieldName);
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType pt) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length > 0 && args[0] instanceof Class<?> cls) return cls;
            }
        } catch (Exception e) {
            DebugAPI.logLibWarn(DebugCategory.ITEMS, "ConsumableProcessor: error resolving " + fieldName + ": " + e.getMessage());
        }
        return null;
    }

    private static Method findStaticBuilderFactory(Class<?> cls) {
        for (String name : new String[]{"food", "consumable", "builder", "create", "of"}) {
            try {
                Method m = cls.getMethod(name);
                if (Modifier.isStatic(m.getModifiers())) return m;
            } catch (NoSuchMethodException ignored) {}
        }
        for (Method m : cls.getMethods()) {
            if (Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 0) {
                try {
                    m.getReturnType().getMethod("build");
                    return m;
                } catch (NoSuchMethodException ignored) {}
            }
        }
        return null;
    }

    private static Class<?> resolveAnimationClass(Method factory) {
        try {
            Object builder = factory.invoke(null);
            for (Method m : builder.getClass().getMethods()) {
                if (m.getName().equals("animation") && m.getParameterCount() == 1) {
                    return m.getParameterTypes()[0];
                }
            }
        } catch (Exception e) {
            DebugAPI.logLibWarn(DebugCategory.ITEMS, "ConsumableProcessor: could not resolve animation type: " + e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Object resolveEatConstant(Class<?> animationClass) {
        if (animationClass.isEnum()) {
            try {
                return Enum.valueOf((Class<Enum>) animationClass, "EAT");
            } catch (IllegalArgumentException ignored) {}
        }
        for (String name : new String[]{"EAT", "eat", "Eat"}) {
            try {
                return animationClass.getField(name).get(null);
            } catch (Exception ignored) {}
        }
        DebugAPI.logLibWarn(DebugCategory.ITEMS, "ConsumableProcessor: EAT not found in " + animationClass.getSimpleName());
        return null;
    }
}

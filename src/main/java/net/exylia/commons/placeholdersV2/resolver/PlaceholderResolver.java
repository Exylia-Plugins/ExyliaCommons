package net.exylia.commons.placeholdersV2.resolver;

import net.exylia.commons.placeholdersV2.annotation.Placeholder;
import net.exylia.commons.placeholdersV2.context.PlaceholderContext;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class PlaceholderResolver {
    private final String name;
    private final Method method;
    private final Object instance;
    private final Placeholder annotation;

    public PlaceholderResolver(String name, Method method, Object instance, Placeholder annotation) {
        this.name = name;
        this.method = method;
        this.instance = instance;
        this.annotation = annotation;
        this.method.setAccessible(true);
    }

    public String getName() {
        return name;
    }

    public Method getMethod() {
        return method;
    }

    public Object getInstance() {
        return instance;
    }

    public Placeholder getAnnotation() {
        return annotation;
    }

    public Object resolve(Player player, PlaceholderContext context) {
        try {
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] args = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];
                if (paramType == Player.class) {
                    args[i] = player;
                } else if (paramType == PlaceholderContext.class) {
                    args[i] = context;
                } else if (context != null) {
                    args[i] = context.find(paramType);
                }
            }

            return method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException("Error resolving placeholder: " + name, e);
        }
    }

    public boolean isAsync() {
        return annotation.async();
    }

    public boolean isCacheable() {
        return annotation.cacheable();
    }

    public long getCacheTtlMs() {
        return annotation.cacheTtlMs();
    }

    @Override
    public String toString() {
        return "PlaceholderResolver{" +
                "name='" + name + '\'' +
                ", method=" + method.getName() +
                ", instance=" + instance.getClass().getSimpleName() +
                '}';
    }
}

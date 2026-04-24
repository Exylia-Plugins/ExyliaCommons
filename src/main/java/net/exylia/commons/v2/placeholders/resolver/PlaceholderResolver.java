package net.exylia.commons.v2.placeholders.resolver;

import net.exylia.commons.v2.placeholders.annotation.Placeholder;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class PlaceholderResolver {
    private final String name;
    private final Method method;
    private final Object instance;
    private final Placeholder annotation;
    private final boolean hasArgument;
    private final String basePattern;

    public PlaceholderResolver(String name, Method method, Object instance, Placeholder annotation) {
        this.name = name;
        this.method = method;
        this.instance = instance;
        this.annotation = annotation;
        this.hasArgument = annotation.hasArgument() || name.endsWith("_*");
        this.basePattern = hasArgument ? name.replace("_*", "_") : name;
        this.method.setAccessible(true);
    }

    public boolean hasArgument() {
        return hasArgument;
    }

    public String getBasePattern() {
        return basePattern;
    }

    public boolean matches(String placeholderName) {
        if (!hasArgument) {
            return name.equalsIgnoreCase(placeholderName);
        }
        return placeholderName.toLowerCase().startsWith(basePattern.toLowerCase());
    }

    public String extractArgument(String placeholderName) {
        if (!hasArgument) {
            return null;
        }
        if (placeholderName.toLowerCase().startsWith(basePattern.toLowerCase())) {
            return placeholderName.substring(basePattern.length());
        }
        return null;
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
        return resolve(player, context, null);
    }

    public Object resolve(Player player, PlaceholderContext context, String argument) {
        try {
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] args = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];
                if (paramType == Player.class) {
                    args[i] = player;
                } else if (paramType == PlaceholderContext.class) {
                    args[i] = context;
                } else if (paramType == String.class && hasArgument && argument != null) {
                    args[i] = argument;
                } else if (context != null) {
                    args[i] = context.find(paramType);
                }
            }

            return method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException("Error resolving placeholder: " + name, e);
        }
    }

    public Object resolveRelational(Player requester, Player target, PlaceholderContext context) {
        try {
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] args = new Object[paramTypes.length];
            int playerIndex = 0;

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];
                if (paramType == Player.class) {
                    args[i] = (playerIndex == 0) ? requester : target;
                    playerIndex++;
                } else if (paramType == PlaceholderContext.class) {
                    args[i] = context;
                } else if (context != null) {
                    args[i] = context.find(paramType);
                }
            }

            return method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException("Error resolving relational placeholder: " + name, e);
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

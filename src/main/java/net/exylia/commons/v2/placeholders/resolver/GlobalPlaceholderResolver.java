package net.exylia.commons.v2.placeholders.resolver;

@FunctionalInterface
public interface GlobalPlaceholderResolver {
    Object resolve();

    static GlobalPlaceholderResolver of(GlobalPlaceholderResolver resolver) {
        return resolver;
    }
}

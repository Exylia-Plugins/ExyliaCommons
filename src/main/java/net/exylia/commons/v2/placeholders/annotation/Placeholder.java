package net.exylia.commons.v2.placeholders.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Placeholder {
    String name();
    String description() default "";
    PlaceholderScope scope() default PlaceholderScope.GLOBAL;
    boolean cacheable() default true;
    long cacheTtlMs() default 1000;
    boolean async() default false;
    boolean hasArgument() default false;
}

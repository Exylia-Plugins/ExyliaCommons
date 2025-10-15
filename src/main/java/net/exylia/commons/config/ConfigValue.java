package net.exylia.commons.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ConfigValue {
     
    String value();

    String defaultValue() default "";

    boolean required() default false;

    boolean autoReload() default true;
}

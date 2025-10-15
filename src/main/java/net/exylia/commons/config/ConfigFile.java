package net.exylia.commons.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigFile {
     
    String value();

    boolean required() default true;

    String[] dependencies() default {};
}

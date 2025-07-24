package net.exylia.commons.database.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String name() default "";
    boolean primaryKey() default false;
    boolean autoIncrement() default false;
    boolean nullable() default true;
    boolean unique() default false;
    int length() default 255;
    String defaultValue() default "";

    boolean autoSerialize() default false;
    SerializationType serializationType() default SerializationType.AUTO;
    boolean initializeEmpty() default true; // Por defecto true para colecciones
}
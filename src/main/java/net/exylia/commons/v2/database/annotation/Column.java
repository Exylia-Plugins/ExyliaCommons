package net.exylia.commons.v2.database.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
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

    boolean initializeEmpty() default true;
}

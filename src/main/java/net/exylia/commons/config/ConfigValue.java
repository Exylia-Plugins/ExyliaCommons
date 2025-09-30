package net.exylia.commons.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ConfigValue {
    /**
     * Ruta en el archivo de configuración
     */
    String value();

    /**
     * Valor por defecto si no se encuentra la ruta
     */
    String defaultValue() default "";

    /**
     * Si el valor es requerido
     */
    boolean required() default false;

    /**
     * Si se debe recargar automáticamente en hot-reload
     */
    boolean autoReload() default true;
}
package net.exylia.commons.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigFile {
    /**
     * Nombre del archivo de configuración (sin extensión .yml)
     */
    String value();

    /**
     * Si el archivo es requerido (se creará automáticamente si no existe)
     */
    boolean required() default true;

    /**
     * Archivos de los que depende este archivo (se cargarán primero)
     */
    String[] dependencies() default {};
}

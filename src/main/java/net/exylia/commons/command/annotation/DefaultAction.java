package net.exylia.commons.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para definir la acción por defecto cuando no se proporcionan argumentos
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultAction {

    /**
     * Tipo de acción por defecto
     */
    ActionType value() default ActionType.SHOW_HELP;

    /**
     * Subcomando específico a ejecutar (solo para EXECUTE_SUBCOMMAND)
     */
    String subcommand() default "";

    enum ActionType {
        /**
         * Mostrar ayuda (comportamiento por defecto)
         */
        SHOW_HELP,

        /**
         * Ejecutar un subcomando específico
         */
        EXECUTE_SUBCOMMAND
    }
}
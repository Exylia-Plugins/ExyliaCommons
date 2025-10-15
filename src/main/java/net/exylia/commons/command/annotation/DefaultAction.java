package net.exylia.commons.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultAction {

    ActionType value() default ActionType.SHOW_HELP;

    String subcommand() default "";

    enum ActionType {
         
        SHOW_HELP,

        EXECUTE_SUBCOMMAND
    }
}

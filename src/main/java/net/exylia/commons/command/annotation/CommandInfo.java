package net.exylia.commons.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandInfo {

    String usage();

    String permission() default "";

    String description() default "";

    boolean playerOnly() default false;

    String[] aliases() default {};

    int order() default 100;
}

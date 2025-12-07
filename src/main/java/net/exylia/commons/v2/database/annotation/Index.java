package net.exylia.commons.v2.database.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Indexes.class)
public @interface Index {
    String name();

    String[] fields();
}

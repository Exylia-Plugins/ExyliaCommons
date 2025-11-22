package net.exylia.commons.databaseV2.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Indexes.class)
public @interface Index {
    String name();

    String[] fields();
}

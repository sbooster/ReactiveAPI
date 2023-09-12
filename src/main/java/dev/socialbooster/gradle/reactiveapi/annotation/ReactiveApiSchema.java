package dev.socialbooster.gradle.reactiveapi.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface ReactiveApiSchema {
    String description() default "";
}


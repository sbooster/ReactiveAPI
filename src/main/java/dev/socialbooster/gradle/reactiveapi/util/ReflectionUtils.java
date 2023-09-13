package dev.socialbooster.gradle.reactiveapi.util;

import dev.socialbooster.gradle.reactiveapi.annotation.ReactiveApiSchema;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils {
    @Setter
    private static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    public static boolean isDependsEnabled() {
        try {
            getControllerAnnotation();
            getMessageMappingAnnotation();
            getRequestBodyAnnotation();
            getMonoClass();
            getFluxClass();
            getReactiveApiSchemaAnnotation();
        } catch (Exception exception) {
            return false;
        }
        return true;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static Class<? extends Annotation> getControllerAnnotation() {
        return (Class<Annotation>) classLoader.loadClass("org.springframework.stereotype.Controller");
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static Class<? extends Annotation> getRequestBodyAnnotation() {
        return (Class<Annotation>) classLoader.loadClass("org.springframework.web.bind.annotation.RequestBody");
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static Class<? extends Annotation> getMessageMappingAnnotation() {
        return (Class<Annotation>) classLoader.loadClass("org.springframework.messaging.handler.annotation.MessageMapping");
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static Class<? extends Annotation> getReactiveApiSchemaAnnotation() {
        return (Class<Annotation>) classLoader.loadClass("dev.socialbooster.gradle.reactiveapi.annotation.ReactiveApiSchema");
    }

    public static Object getAnnotationValue(Annotation annotation) {
        Class<? extends Annotation> annotationClass = annotation.getClass();
        try {
            Method valueMethod = annotationClass.getDeclaredMethod("value");
            return valueMethod.invoke(annotation);
        } catch (Exception exception) {
            return null;
        }
    }

    @SneakyThrows
    public static String getMethodDescription(Method method) {
        String description = null;
        if (method.isAnnotationPresent(ReactiveApiSchema.class)) {
            Annotation annotation = method.getAnnotation(ReactiveApiSchema.class);
            Method descriptionMethod = annotation.getClass().getDeclaredMethod("description");
            description = (String) descriptionMethod.invoke(annotation);
        }
        return description;
    }

    @SneakyThrows
    public static Class<?> getMonoClass() {
        return classLoader.loadClass("reactor.core.publisher.Mono");
    }

    @SneakyThrows
    public static Class<?> getFluxClass() {
        return classLoader.loadClass("reactor.core.publisher.Flux");
    }

    @SneakyThrows
    public static String getClassDescription(Class<?> clazz) {
        String description = null;
        if (clazz.isAnnotationPresent(ReactiveApiSchema.class)) {
            Annotation annotation = clazz.getAnnotation(ReactiveApiSchema.class);
            Method descriptionMethod = annotation.getClass().getDeclaredMethod("description");
            description = (String) descriptionMethod.invoke(annotation);
        }
        return description;
    }

    @SneakyThrows
    public static String getFieldDescription(Field field) {
        String description = null;
        if (field.isAnnotationPresent(ReactiveApiSchema.class)) {
            Annotation annotation = field.getAnnotation(ReactiveApiSchema.class);
            Method descriptionMethod = annotation.getClass().getDeclaredMethod("description");
            description = (String) descriptionMethod.invoke(annotation);
        }
        return description;
    }
}

package dev.socialbooster.gradle.reactiveapi.util;

import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class ReflectionUtils {
    @Setter
    private static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private static Class<Annotation> controller;
    private static Class<Annotation> requestBody;
    private static Class<Annotation> messageMapping;
    private static Class<?> monoClass;
    private static Class<?> fluxClass;

    public static boolean isDependsEnabled() {
        try {
            getControllerAnnotation();
            getMessageMappingAnnotation();
            getRequestBodyAnnotation();
            getMonoClass();
            getFluxClass();
        } catch (Exception exception) {
            return false;
        }
        return true;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static Class<? extends Annotation> getControllerAnnotation() {
        if (controller == null) {
            controller = (Class<Annotation>) classLoader.loadClass("org.springframework.stereotype.Controller");
        }
        return controller;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static Class<? extends Annotation> getRequestBodyAnnotation() {
        if (requestBody == null) {
            requestBody = (Class<Annotation>) classLoader.loadClass("org.springframework.web.bind.annotation.RequestBody");
        }
        return requestBody;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static Class<? extends Annotation> getMessageMappingAnnotation() {
        if (messageMapping == null) {
            messageMapping = (Class<Annotation>) classLoader.loadClass("org.springframework.messaging.handler.annotation.MessageMapping");
        }
        return messageMapping;
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
    public static Class<?> getMonoClass() {
        if (monoClass == null) {
            monoClass = classLoader.loadClass("reactor.core.publisher.Mono");
        }
        return monoClass;
    }

    @SneakyThrows
    public static Class<?> getFluxClass() {
        if (fluxClass == null) {
            fluxClass = classLoader.loadClass("reactor.core.publisher.Flux");
        }
        return fluxClass;
    }
}

package dev.socialbooster.gradle.reactiveapi.task;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import dev.socialbooster.gradle.reactiveapi.exception.PlainOutputFoundException;
import dev.socialbooster.gradle.reactiveapi.exception.TaskNotFoundException;
import dev.socialbooster.gradle.reactiveapi.model.MessageType;
import dev.socialbooster.gradle.reactiveapi.model.MessagesDescription;
import dev.socialbooster.gradle.reactiveapi.model.ModelDescription;
import dev.socialbooster.gradle.reactiveapi.model.RouteDescription;
import dev.socialbooster.gradle.reactiveapi.util.ReflectionUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

@Getter
@Log
public class GenerateReactiveAPI extends DefaultTask {
    @Optional
    @InputFiles
    private Iterable<File> compileClasspath;

    @Input
    @Setter
    private Boolean prettyPrint = false;

    @Input
    @Setter
    private String outputFile = Paths.get(getProject().getBuildDir().getAbsolutePath(),
            "libs", "ReactiveAPI.json").toString();

    @TaskAction
    public void execute() throws IOException, TaskNotFoundException, PlainOutputFoundException {
        this.validateOutputs();
        this.loadDepends();
        ClassLoader classLoader = this.getClassLoader();
        ReflectionUtils.setClassLoader(classLoader);
        if (ReflectionUtils.isDependsEnabled()) {
            Set<Class<?>> controllers = this.getControllers(classLoader);
            MessagesDescription messagesDescription = this.getMessageDescription(controllers);
            this.save(messagesDescription);
        } else {
            log.warning("""
                    The task GenerateReactiveAPI was applied, but the plugin could not find the dependencies.
                    Make sure your project contains:
                                RSocket,
                                Reactor,
                                SpringWeb,
                                SpringMessaging,
                                SpringContext,
                    then try again.
                    """);
        }
    }

    public void save(MessagesDescription messagesDescription) {
        File file = new File(outputFile);
        File parentFile = file.getParentFile();
        if ((parentFile.exists() &&
                (parentFile.isDirectory()
                        || (parentFile.delete()
                        && parentFile.mkdirs()))
        ) || parentFile.mkdirs()) {
            try (FileWriter fileWriter = new FileWriter(outputFile)) {
                java.util.Optional.of(new GsonBuilder())
                        .map(gsonBuilder -> prettyPrint ? gsonBuilder.setPrettyPrinting() : gsonBuilder)
                        .map(GsonBuilder::create)
                        .ifPresent(gson -> gson.toJson(messagesDescription, fileWriter));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public MessagesDescription getMessageDescription(Set<Class<?>> controllers) {
        MessagesDescription messagesDescription = new MessagesDescription();
        Class<? extends Annotation> messageMappingAnnotation = ReflectionUtils.getMessageMappingAnnotation();
        controllers.forEach(controller -> Arrays.stream(controller.getDeclaredMethods())
                .filter(method -> {
                    method.setAccessible(true);
                    return method.isAnnotationPresent(messageMappingAnnotation);
                }).forEach(method -> {
                    Annotation annotation = method.getAnnotation(messageMappingAnnotation);
                    Object annotationValue = ReflectionUtils.getAnnotationValue(annotation);
                    if (!(annotationValue instanceof String[] routes)) return;
                    if (routes.length < 1) return;
                    String route = routes[0];
                    Class<?> responseType = this.getUnwrappedPublisherReturnType(method);
                    String responseTypeName = responseType.getName();
                    MessageType type = this.getMessageType(method, responseType);
                    Class<?> requestType = this.getRequestBodyType(method);
                    String requestTypeName = requestType.getName();
                    try {
                        this.registerRoute(messagesDescription,
                                new RouteDescription(
                                        route,
                                        type,
                                        responseTypeName,
                                        requestTypeName
                                )
                        );
                        this.declareAndRegisterModel(messagesDescription, responseType);
                        this.declareAndRegisterModel(messagesDescription, requestType);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }));
        return messagesDescription;
    }

    public void registerRoute(MessagesDescription messagesDescription, RouteDescription routeDescription) {
        if (messagesDescription.containsRoute(routeDescription.getRoute())) return;
        messagesDescription.declareRoute(routeDescription);
    }

    public void declareAndRegisterModel(MessagesDescription messagesDescription, Class<?> type) {
        String typeName = type.getName();
        if (typeName.startsWith("java") || !typeName.contains(".")) return;
        if (messagesDescription.containsModel(typeName)) return;
        ModelDescription modelDescription = new ModelDescription(typeName);
        messagesDescription.declareModel(modelDescription);
        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                if (Modifier.isStatic(field.getModifiers())) continue;
                String fieldName = field.getName();
                Class<?> fieldType = Primitives.unwrap(field.getType());
                String fieldTypeName = fieldType.getName();
                if (fieldType.isEnum()) {
                    try {
                        Method values = fieldType.getMethod("values");
                        values.setAccessible(true);
                        Object enumArray = values.invoke(null);
                        Set<String> enumValues = new HashSet<>();
                        for (Object enumValue : (Object[]) enumArray) {
                            enumValues.add(enumValue.toString());
                        }
                        fieldTypeName = String.join(" | ", enumValues);
                    } catch (Exception ignored) {
                    }
                }
                if (!fieldTypeName.startsWith("java.lang")) {
                    declareAndRegisterModel(messagesDescription, fieldType);
                } else {
                    fieldTypeName = fieldTypeName.substring("java.lang.".length()).toLowerCase();
                }
                modelDescription.declareField(fieldName, fieldTypeName);
            } catch (Exception ignored) {
            }
        }
    }

    public Class<?> getRequestBodyType(Method method) {
        for (AnnotatedType annotatedParameterType : method.getAnnotatedParameterTypes()) {
            if (annotatedParameterType.isAnnotationPresent(ReflectionUtils.getRequestBodyAnnotation())) {
                return Primitives.unwrap(TypeToken.of(annotatedParameterType.getType()).getRawType());
            }
        }
        return void.class;
    }

    @SuppressWarnings("unused")
    public MessageType getMessageType(Method method) {
        return this.getMessageType(method, this.getUnwrappedPublisherReturnType(method));
    }

    public MessageType getMessageType(Method method, Class<?> unwrappedPublisherReturnType) {
        Type genericReturnType = method.getGenericReturnType();
        boolean returnsVoid = Primitives.unwrap(unwrappedPublisherReturnType).equals(void.class);
        if (genericReturnType instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (Objects.equals(rawType, ReflectionUtils.getFluxClass())) {
                return MessageType.REQUEST_STREAM;
            }
            if (!returnsVoid && Objects.equals(rawType, ReflectionUtils.getMonoClass())) {
                return MessageType.REQUEST_RESPONSE;
            }
        }
        return returnsVoid ? MessageType.FIRE_AND_FORGET : MessageType.REQUEST_RESPONSE;
    }

    public Class<?> getUnwrappedPublisherReturnType(Method method) {
        Type genericReturnType = method.getGenericReturnType();
        Class<?> returnType = method.getReturnType();
        if (Objects.equals(returnType, ReflectionUtils.getMonoClass()) || Objects.equals(returnType, ReflectionUtils.getFluxClass())) {
            if (genericReturnType instanceof ParameterizedType parameterizedType) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments.length > 0) {
                    return Primitives.unwrap(TypeToken.of(actualTypeArguments[0]).getRawType());
                }
            }
        }
        return Primitives.unwrap(returnType);
    }

    public Set<Class<?>> getControllers(ClassLoader classLoader) throws IOException {
        Project project = getProject();
        return ClassPath.from(classLoader)
                .getAllClasses()
                .stream()
                .filter(classInfo -> classInfo.getPackageName().startsWith(project.getGroup().toString()))
                .map(ClassPath.ClassInfo::load)
                .filter(type -> type.isAnnotationPresent(ReflectionUtils.getControllerAnnotation()))
                .collect(Collectors.toSet());
    }

    public void validateOutputs() {
        Preconditions.checkArgument(outputFile != null,
                "Current output(%s) is undefined", outputFile);
        Preconditions.checkArgument(!outputFile.isBlank(),
                "Current output(%s) is blank", outputFile);
        Preconditions.checkArgument(!outputFile.isEmpty(),
                "Current output(%s) is empty", outputFile);
        String absolutePath = getProject().getRootDir().getAbsolutePath();
        Preconditions.checkArgument(outputFile.startsWith(absolutePath),
                "Current output(%s) is out of project dir(%s)", outputFile, absolutePath);
    }

    @Internal
    public ClassLoader getClassLoader() throws TaskNotFoundException, PlainOutputFoundException {
        Set<File> files = Sets.newHashSet(compileClasspath);
        files.add(getProject().getTasksByName("jar", false)
                .stream()
                .findFirst()
                .orElseThrow(() -> new TaskNotFoundException("Task named as jar is not found"))
                .getOutputs()
                .getFiles()
                .getFiles()
                .stream()
                .filter(this::isJarFile)
                .findFirst()
                .orElseThrow(() -> new PlainOutputFoundException("Plain output file  (without deps) not found")));
        return URLClassLoader.newInstance(files.stream().map(file -> {
                    try {
                        return file.toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }).toArray(URL[]::new),
                Thread.currentThread().getContextClassLoader()
        );
    }

    public void loadDepends() {
        this.compileClasspath = getProject().getConfigurations().getByName("compileClasspath").getFiles();
    }

    public boolean isJarFile(File file) {
        try {
            new JarInputStream(new FileInputStream(file));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

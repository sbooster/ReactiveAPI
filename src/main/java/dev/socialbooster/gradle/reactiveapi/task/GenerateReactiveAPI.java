package dev.socialbooster.gradle.reactiveapi.task;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import dev.socialbooster.gradle.reactiveapi.annotations.Schema;
import dev.socialbooster.gradle.reactiveapi.exception.PlainOutputFoundException;
import dev.socialbooster.gradle.reactiveapi.exception.TaskNotFoundException;
import dev.socialbooster.gradle.reactiveapi.model.*;
import dev.socialbooster.gradle.reactiveapi.util.ReflectionUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.Optional;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

@Getter
@Log
public class GenerateReactiveAPI extends DefaultTask {
    private static final String JAVA_LANG = "java.lang.";

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
        try(URLClassLoader classLoader = this.getClassLoader()) {
            ReflectionUtils.setClassLoader(classLoader);
            if (ReflectionUtils.isDependsEnabled()) {
                Set<Class<?>> controllers = this.getControllers(classLoader);

                controllers.stream().map(Class::getAnnotations)
                        .forEach(annotations -> {
                            for (Annotation annotation : annotations) {
                                Class<? extends Annotation> annotationType = annotation.annotationType();
                                System.out.println(annotationType);
                                if (annotationType == Schema.class) {
                                    System.out.println(((Schema) annotation).description());
                                }
                            }
                        });

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

                    Class<?> responseType = method.getReturnType();
                    String responseGenericsSuffix = "";
                    if (method.getGenericReturnType() instanceof ParameterizedType parameterizedType) {
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        if (actualTypeArguments.length > 0) {
                            Class<?> unwrappedPublisherReturnType = this.getUnwrappedPublisherReturnType(actualTypeArguments, responseType);
                            if (Objects.equals(unwrappedPublisherReturnType, responseType)) {
                                responseGenericsSuffix = this.getGenericsSuffix(actualTypeArguments);
                            } else {
                                responseType = unwrappedPublisherReturnType;
                                Type[] responseGenericTypes = this.getActualTypeArgumentsForType(actualTypeArguments[0]);
                                if (responseGenericTypes.length > 0) {
                                    responseGenericsSuffix = this.getGenericsSuffix(responseGenericTypes);
                                }
                            }
                        }
                    }
                    String responseTypeName = responseType.getName() + responseGenericsSuffix;
                    Parameter requestBodyParameter = this.getRequestBodyParameter(method);
                    Class<?> requestType = requestBodyParameter != null ?
                            Primitives.unwrap(requestBodyParameter.getType()) : void.class;
                    String requestGenericsSuffix = "";
                    if (requestBodyParameter != null && requestBodyParameter.getParameterizedType() instanceof ParameterizedType parameterizedType) {
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        if (actualTypeArguments.length > 0) {
                            requestGenericsSuffix = this.getGenericsSuffix(actualTypeArguments);
                        }
                    }
                    String requestTypeName = requestType.getName() + requestGenericsSuffix;

                    MessageType type = this.getMessageType(method, responseType);

                    String description = ReflectionUtils.getMethodDescription(method);
                    String[] tags = ReflectionUtils.getMethodTags(method);

                    try {
                        this.registerRoute(messagesDescription,
                                new RouteDescription(
                                        route,
                                        type,
                                        responseTypeName,
                                        requestTypeName,
                                        description,
                                        tags
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
        String classDescription = ReflectionUtils.getClassDescription(type);
        String[] classTags = ReflectionUtils.getClassTags(type);
        ModelDescription modelDescription = new ModelDescription(classDescription, typeName, classTags);
        messagesDescription.declareModel(modelDescription);
        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                if (Modifier.isStatic(field.getModifiers())) continue;
                String fieldName = field.getName();
                Class<?> fieldType = Primitives.unwrap(field.getType());
                AtomicReference<String> fieldTypeName = new AtomicReference<>(fieldType.getTypeName());

                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType parameterizedType) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length > 0) {
                        fieldTypeName.set(fieldTypeName + this.getGenericsSuffix(actualTypeArguments));
                    }
                }
                if (fieldType.isEnum()) {
                    try {
                        Method values = fieldType.getMethod("values");
                        Method valueOf = fieldType.getMethod("valueOf", String.class);
                        Arrays.stream(fieldType.getMethods())
                                .filter(method -> method.getName().equals("compareTo"))
                                .findFirst()
                                .ifPresent(compareTo -> {
                                    try {
                                        Object enumArray = values.invoke(null);
                                        Set<String> enumValues = new HashSet<>();
                                        for (Object enumValue : (Object[]) enumArray) {
                                            enumValues.add(enumValue.toString());
                                        }
                                        fieldTypeName.set(enumValues.stream()
                                                .sorted((o1, o2) -> {
                                                    try {
                                                        Object e1 = valueOf.invoke(null, o1);
                                                        Object e2 = valueOf.invoke(null, o2);
                                                        return (int) compareTo.invoke(e1, e2);
                                                    } catch (IllegalAccessException | InvocationTargetException e) {
                                                        return 0;
                                                    }
                                                })
                                                .collect(Collectors.joining(" | ")));
                                    } catch (IllegalAccessException | InvocationTargetException ignored) {
                                    }
                                });
                    } catch (Exception ignored) {
                    }
                }
                if (!fieldTypeName.get().startsWith(JAVA_LANG)) {
                    declareAndRegisterModel(messagesDescription, fieldType);
                } else {
                    fieldTypeName.set(fieldTypeName.get().substring(JAVA_LANG.length()).toLowerCase());
                }
                String description = ReflectionUtils.getFieldDescription(field);
                modelDescription.declareField(fieldName, new DescriptionValuePair(description, fieldTypeName.get()) );
            } catch (Exception ignored) {
            }
        }
    }

    public Parameter getRequestBodyParameter(Method method) {
        Class<? extends Annotation> requestBodyAnnotation = ReflectionUtils.getRequestBodyAnnotation();
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(requestBodyAnnotation)) {
                return parameter;
            }
        }
        return null;
    }

    public String getGenericsSuffix(Type[] types) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Type type : types) {
            String typeName = type.getTypeName();
            if (typeName.startsWith(JAVA_LANG)) {
                typeName = typeName.substring(JAVA_LANG.length()).toLowerCase();
            }
            joiner.add(typeName);
        }
        return '<' + joiner.toString() + '>';
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

    public Class<?> getUnwrappedPublisherReturnType(Type[] actualTypeArguments, Class<?> returnType) {
        if (Objects.equals(returnType, ReflectionUtils.getMonoClass()) || Objects.equals(returnType, ReflectionUtils.getFluxClass()))
            return Primitives.unwrap(TypeToken.of(actualTypeArguments[0]).getRawType());
        else
            return Primitives.unwrap(returnType);
    }

    public Type[] getActualTypeArgumentsForType(Type type) {
        if (type instanceof ParameterizedType parameterizedType)
            return parameterizedType.getActualTypeArguments();
        else
            return new Type[0];
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
    public URLClassLoader getClassLoader() throws TaskNotFoundException, PlainOutputFoundException {
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

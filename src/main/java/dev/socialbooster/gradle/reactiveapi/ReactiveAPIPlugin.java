package dev.socialbooster.gradle.reactiveapi;

import dev.socialbooster.gradle.reactiveapi.task.GenerateReactiveAPI;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

public class ReactiveAPIPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("generateReactiveAPI", GenerateReactiveAPI.class, task -> task.dependsOn("jar"));
        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> project.afterEvaluate(this::addDependency));
    }

    private void addDependency(Project project) {
        project.getDependencies().add(
                JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
                "dev.socialbooster.gradle:reactiveapi-annotations:1.3.0-SNAPSHOT"
        );
    }
}

package dev.socialbooster.gradle.reactiveapi;

import dev.socialbooster.gradle.reactiveapi.task.GenerateReactiveAPI;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ReactiveAPIPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("generateReactiveAPI", GenerateReactiveAPI.class, task -> task.dependsOn("jar"));
    }
}

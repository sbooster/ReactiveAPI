package dev.socialbooster.gradle.reactiveapi.model;

import java.util.HashMap;
import java.util.Map;

public class MessagesDescription {

    private final Map<String, RouteDescription> routes = new HashMap<>();
    private final Map<String, ModelDescription> models = new HashMap<>();

    public void declareRoute(RouteDescription routeDescription) {
        routes.put(routeDescription.getRoute(), routeDescription);
    }

    public boolean containsRoute(String route) {
        return routes.containsKey(route);
    }

    public void declareModel(ModelDescription modelDescription) {
        models.put(modelDescription.getType(), modelDescription);
    }

    public boolean containsModel(String modelTypeName) {
        return models.containsKey(modelTypeName);
    }
}

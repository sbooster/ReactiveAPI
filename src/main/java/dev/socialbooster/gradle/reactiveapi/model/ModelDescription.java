package dev.socialbooster.gradle.reactiveapi.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class ModelDescription {
    private final String type;
    private final Map<String, String> fields = new HashMap<>();

    public void declareField(String fieldName, String fieldType) {
        fields.put(fieldName, fieldType);
    }
}

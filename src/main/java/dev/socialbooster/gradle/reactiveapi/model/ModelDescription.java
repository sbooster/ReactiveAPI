package dev.socialbooster.gradle.reactiveapi.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class ModelDescription {
    @Nullable
    private final String description;
    private final String type;
    private final String[] tags;
    private final Map<String, DescriptionValuePair> fields = new HashMap<>();

    public void declareField(String fieldName, DescriptionValuePair descriptionValuePair) {
        fields.put(fieldName, descriptionValuePair);
    }
}

package dev.socialbooster.gradle.reactiveapi.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;

@Getter
@RequiredArgsConstructor
public final class RouteDescription {
    private final String route;
    private final MessageType type;
    private final String responseType;
    private final String requestType;
    @Nullable
    private final String description;
    private final String[] tags;
}

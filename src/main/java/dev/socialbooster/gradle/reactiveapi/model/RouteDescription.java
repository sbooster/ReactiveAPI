package dev.socialbooster.gradle.reactiveapi.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class RouteDescription {
    private final String route;
    private final MessageType type;
    private final String responseType;
    private final String requestType;
}

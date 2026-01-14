package org.pragmatica.aether.http.handler;
/**
 * Route metadata for KV-Store registration.
 * <p>
 * Maps HTTP method + path prefix to artifact + slice method.
 * Used by HttpRouteRegistry to route incoming requests.
 *
 * @param httpMethod    HTTP method (GET, POST, PUT, DELETE, etc.)
 * @param pathPrefix    path prefix for TreeMap matching (e.g., "/users/", "/api/orders/")
 * @param artifactCoord full artifact coordinate (e.g., "org.example:user-service:1.0.0")
 * @param sliceMethod   slice method name to invoke
 */
public record HttpRouteDefinition(String httpMethod,
                                  String pathPrefix,
                                  String artifactCoord,
                                  String sliceMethod) {
    /**
     * Create route definition.
     */
    public static HttpRouteDefinition httpRouteDefinition(String httpMethod,
                                                          String pathPrefix,
                                                          String artifactCoord,
                                                          String sliceMethod) {
        return new HttpRouteDefinition(httpMethod, normalizePrefix(pathPrefix), artifactCoord, sliceMethod);
    }

    private static String normalizePrefix(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        var normalized = path.strip();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }
}

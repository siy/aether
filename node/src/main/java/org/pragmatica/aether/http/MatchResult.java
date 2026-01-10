package org.pragmatica.aether.http;

import org.pragmatica.aether.slice.routing.Route;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import java.util.Map;

import static org.pragmatica.lang.Verify.Is;
import static org.pragmatica.lang.Verify.ensure;

/**
 * Result of matching a request to a route.
 */
public record MatchResult(Route route,
                          Map<String, String> pathVariables) {
    private static final Cause NULL_ROUTE = Causes.cause("Route cannot be null");
    private static final Cause NULL_PATH_VARIABLES = Causes.cause("Path variables map cannot be null");

    public static Result<MatchResult> matchResult(Route route, Map<String, String> pathVariables) {
        return Result.all(ensure(route, Is::notNull, NULL_ROUTE),
                          ensure(pathVariables, Is::notNull, NULL_PATH_VARIABLES))
                     .map((r, pv) -> new MatchResult(r,
                                                     Map.copyOf(pv)));
    }
}

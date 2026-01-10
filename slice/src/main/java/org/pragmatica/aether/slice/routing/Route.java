package org.pragmatica.aether.slice.routing;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import java.util.List;

import static org.pragmatica.lang.Verify.Is;
import static org.pragmatica.lang.Verify.ensure;

public record Route(String pattern, RouteTarget target, List<Binding> bindings) {
    private static final Cause EMPTY_PATTERN = Causes.cause("Route pattern cannot be empty");
    private static final Cause NULL_TARGET = Causes.cause("Route target cannot be null");

    public static Result<Route> route(String pattern, RouteTarget target, List<Binding> bindings) {
        return Result.all(ensure(pattern, Is::notBlank, EMPTY_PATTERN),
                          ensure(target, Is::notNull, NULL_TARGET))
                     .map((p, t) -> new Route(p,
                                              t,
                                              List.copyOf(bindings)));
    }
}

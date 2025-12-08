package org.pragmatica.aether.slice.blueprint;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import java.util.List;
import java.util.regex.Pattern;

public record Route(String pattern, RouteTarget target, List<Binding> bindings) {
    public static Result<Route> route(String pattern, RouteTarget target, List<Binding> bindings) {
        return Result.success(new Route(pattern, target, bindings));
    }
}

package org.pragmatica.http;

import org.pragmatica.lang.Option;

import java.util.List;
import java.util.Map;

/**
 * HTTP query parameters.
 */
public interface QueryParams {
    /**
     * Get the first value for a parameter.
     */
    Option<String> get(String name);

    /**
     * Get all values for a parameter.
     */
    List<String> getAll(String name);

    /**
     * Get all parameters as a map.
     */
    Map<String, List<String>> asMap();

    /**
     * Check if parameter exists.
     */
    default boolean has(String name) {
        return get(name)
               .isPresent();
    }

    /**
     * Create query parameters from a map.
     */
    static QueryParams queryParams(Map<String, List<String>> raw) {
        record queryParams(Map<String, List<String>> params) implements QueryParams {
            @Override
            public Option<String> get(String name) {
                var values = params.get(name);
                return values == null || values.isEmpty()
                       ? Option.empty()
                       : Option.some(values.getFirst());
            }

            @Override
            public List<String> getAll(String name) {
                var values = params.get(name);
                return values == null
                       ? List.of()
                       : values;
            }

            @Override
            public Map<String, List<String>> asMap() {
                return params;
            }
        }
        return new queryParams(Map.copyOf(raw));
    }

    /**
     * Empty query parameters.
     */
    static QueryParams empty() {
        return queryParams(Map.of());
    }
}

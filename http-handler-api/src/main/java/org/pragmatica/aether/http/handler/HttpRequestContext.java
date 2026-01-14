package org.pragmatica.aether.http.handler;

import java.util.List;
import java.util.Map;

/**
 * Raw HTTP request data passed through SliceInvoker.
 * <p>
 * Contains all information needed to reconstruct HTTP request at destination node
 * where parameter extraction occurs.
 *
 * @param path        request path (e.g., "/users/123")
 * @param method      HTTP method (e.g., "GET", "POST")
 * @param queryParams query parameters as multi-value map
 * @param headers     HTTP headers as multi-value map
 * @param body        request body bytes (empty for GET)
 * @param requestId   unique request identifier for tracing
 */
public record HttpRequestContext(String path,
                                 String method,
                                 Map<String, List<String>> queryParams,
                                 Map<String, List<String>> headers,
                                 byte[] body,
                                 String requestId) {
    /**
     * Create context with empty body.
     */
    public static HttpRequestContext httpRequestContext(String path,
                                                        String method,
                                                        Map<String, List<String>> queryParams,
                                                        Map<String, List<String>> headers,
                                                        String requestId) {
        return new HttpRequestContext(path, method, queryParams, headers, new byte[0], requestId);
    }

    /**
     * Create context with body.
     */
    public static HttpRequestContext httpRequestContext(String path,
                                                        String method,
                                                        Map<String, List<String>> queryParams,
                                                        Map<String, List<String>> headers,
                                                        byte[] body,
                                                        String requestId) {
        return new HttpRequestContext(path, method, queryParams, headers, body, requestId);
    }
}

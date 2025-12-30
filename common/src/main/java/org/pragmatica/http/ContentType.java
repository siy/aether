package org.pragmatica.http;
/**
 * HTTP Content-Type abstraction.
 * <p>
 * Use {@link CommonContentType} for standard content types, or create custom ones via factory method.
 */
public interface ContentType {
    String headerText();

    ContentCategory category();

    static ContentType contentType(String headerText, ContentCategory category) {
        record contentType(String headerText, ContentCategory category) implements ContentType {}
        return new contentType(headerText, category);
    }
}

package org.pragmatica.http;
/**
 * Common HTTP content types.
 */
public enum CommonContentType implements ContentType {
    TEXT_PLAIN("text/plain; charset=UTF-8", ContentCategory.TEXT),
    TEXT_HTML("text/html; charset=UTF-8", ContentCategory.TEXT),
    TEXT_CSS("text/css; charset=UTF-8", ContentCategory.TEXT),
    TEXT_JAVASCRIPT("text/javascript; charset=UTF-8", ContentCategory.TEXT),
    APPLICATION_JSON("application/json; charset=UTF-8", ContentCategory.JSON),
    APPLICATION_XML("application/xml; charset=UTF-8", ContentCategory.XML),
    APPLICATION_OCTET_STREAM("application/octet-stream", ContentCategory.BINARY),
    IMAGE_PNG("image/png", ContentCategory.BINARY),
    IMAGE_JPEG("image/jpeg", ContentCategory.BINARY),
    IMAGE_SVG("image/svg+xml", ContentCategory.XML);
    private final String headerText;
    private final ContentCategory category;
    CommonContentType(String headerText, ContentCategory category) {
        this.headerText = headerText;
        this.category = category;
    }
    @Override
    public String headerText() {
        return headerText;
    }
    @Override
    public ContentCategory category() {
        return category;
    }
}

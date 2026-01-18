package org.pragmatica.aether.slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a record component as the cache key.
 * <p>
 * Used with {@link WithAspects} containing {@link AspectKind#CACHE} to specify
 * which field of the request record should be used as the cache key.
 * <p>
 * If no @Key annotation is present on the request type, the entire
 * request object is used as the cache key.
 * <p>
 * Only one @Key annotation is allowed per record.
 *
 * <pre>{@code
 * record GetUserRequest(@Key UserId userId, boolean includeDetails) {}
 * }</pre>
 *
 * Generated key extractor: {@code GetUserRequest::userId}
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.SOURCE)
public @interface Key {}

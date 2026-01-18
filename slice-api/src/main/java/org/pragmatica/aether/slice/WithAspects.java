package org.pragmatica.aether.slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares aspects to apply to slice methods.
 * <p>
 * Can be placed on:
 * <ul>
 *   <li>Type level - applies to all methods in the slice</li>
 *   <li>Method level - applies to specific method</li>
 * </ul>
 * <p>
 * Aspect order in the array defines composition order.
 * {@code @WithAspects({LOG, CACHE})} means {@code LOG(CACHE(method))}.
 *
 * <pre>{@code
 * @WithAspects({AspectKind.CACHE, AspectKind.METRICS})
 * public interface UserService extends Slice {
 *     Promise<User> getUser(GetUserRequest request);
 * }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface WithAspects {
    /**
     * Aspects to apply, in composition order.
     *
     * @return Array of aspect kinds
     */
    AspectKind[] value();
}

package org.pragmatica.aether.infra;

import org.pragmatica.lang.Option;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Version-tagged instance stored in InfraStore.
 * <p>
 * Contains compatibility logic for finding compatible instances
 * from a list of available versions.
 *
 * @param version  Semantic version string (e.g., "1.2.3" or "1.2.3-SNAPSHOT")
 * @param instance The infrastructure service instance
 * @param <T>      Instance type
 */
public record VersionedInstance<T>(String version, T instance) {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-(.+))?$");

    /**
     * Check if this instance is compatible with requested version.
     * <p>
     * Compatibility rules (semver-style):
     * <ul>
     *   <li>Major version must match</li>
     *   <li>Minor version of this instance must be >= requested</li>
     *   <li>Qualifiers (SNAPSHOT, etc.) are ignored for compatibility</li>
     * </ul>
     *
     * @param requested Version string to check against
     * @return true if this instance is compatible
     */
    public boolean isCompatibleWith(String requested) {
        return parseVersion(version)
                           .flatMap(thisV -> parseVersion(requested)
                                                         .map(reqV -> isCompatible(thisV, reqV)))
                           .or(() -> stripQualifier(version)
                                                   .equals(stripQualifier(requested)));
    }

    private static boolean isCompatible(ParsedVersion thisV, ParsedVersion reqV) {
        // Major must match
        if (thisV.major() != reqV.major()) {
            return false;
        }
        // This minor must be >= requested minor
        if (thisV.minor() < reqV.minor()) {
            return false;
        }
        // If minor matches, this patch must be >= requested patch
        return thisV.minor() != reqV.minor() || thisV.patch() >= reqV.patch();
    }

    /**
     * Check if this instance has exact version match.
     *
     * @param requested Version string to check against
     * @return true if versions match exactly (ignoring qualifier)
     */
    public boolean isExactMatch(String requested) {
        return stripQualifier(version)
                             .equals(stripQualifier(requested));
    }

    /**
     * Find first compatible instance from list.
     * <p>
     * Prefers exact matches over compatible matches.
     *
     * @param instances List of available instances
     * @param version   Requested version
     * @param <T>       Instance type
     * @return Compatible instance if found
     */
    public static <T> Option<T> findCompatible(List<VersionedInstance<T>> instances,
                                               String version) {
        // First try exact match
        for (var vi : instances) {
            if (vi.isExactMatch(version)) {
                return Option.some(vi.instance());
            }
        }
        // Then try compatible
        for (var vi : instances) {
            if (vi.isCompatibleWith(version)) {
                return Option.some(vi.instance());
            }
        }
        return Option.none();
    }

    /**
     * Find instance with exact version match.
     *
     * @param instances List of available instances
     * @param version   Requested version
     * @param <T>       Instance type
     * @return Exact match if found
     */
    public static <T> Option<T> findExact(List<VersionedInstance<T>> instances,
                                          String version) {
        for (var vi : instances) {
            if (vi.isExactMatch(version)) {
                return Option.some(vi.instance());
            }
        }
        return Option.none();
    }

    private record ParsedVersion(int major, int minor, int patch) {}

    private static Option<ParsedVersion> parseVersion(String version) {
        var stripped = stripQualifier(version);
        var matcher = VERSION_PATTERN.matcher(stripped + "-x");
        // Add dummy qualifier for regex
        if (!matcher.matches()) {
            // Try without qualifier pattern
            var parts = stripped.split("\\.");
            if (parts.length >= 3) {
                try{
                    return Option.some(new ParsedVersion(Integer.parseInt(parts[0]),
                                                         Integer.parseInt(parts[1]),
                                                         Integer.parseInt(parts[2])));
                } catch (NumberFormatException e) {
                    return Option.none();
                }
            }
            return Option.none();
        }
        try{
            return Option.some(new ParsedVersion(Integer.parseInt(matcher.group(1)),
                                                 Integer.parseInt(matcher.group(2)),
                                                 Integer.parseInt(matcher.group(3))));
        } catch (NumberFormatException e) {
            return Option.none();
        }
    }

    private static String stripQualifier(String version) {
        var dashIndex = version.indexOf('-');
        return dashIndex > 0
               ? version.substring(0, dashIndex)
               : version;
    }
}

package org.pragmatica.aether.artifact;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Verify.Is;

import java.util.regex.Pattern;

import static org.pragmatica.lang.Verify.ensure;

public record Version(int major, int minor, int patch, String qualifier) {
    public static Result<Version> version(int major, int minor, int patch, Option<String> qualifier) {
        var innerQualifier = qualifier.map(text -> "-" + text).or("");

        return Result.all(ensure(major, Is::greaterThanOrEqualTo, 0),
                          ensure(minor, Is::greaterThanOrEqualTo, 0),
                          ensure(patch, Is::greaterThanOrEqualTo, 0),
                          ensure(innerQualifier, Is::matches, QUALIFIER_PATTERN))
                     .map(Version::new);
    }

    public String raw() {
        return major + "." + minor + "." + patch;
    }

    public String withQualifier() {
        return raw() + qualifier;
    }

    private static final Pattern QUALIFIER_PATTERN = Pattern.compile("^[\\-a-zA-Z0-9-_.]*$");
}

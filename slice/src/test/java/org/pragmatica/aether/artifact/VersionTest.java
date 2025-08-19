package org.pragmatica.aether.artifact;

import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Option;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class VersionTest {

    @Test
    void version_parsing_with_valid_major_minor_patch() {
        Version.version("1.2.3")
            .onSuccess(version -> {
                assertThat(version.major()).isEqualTo(1);
                assertThat(version.minor()).isEqualTo(2);
                assertThat(version.patch()).isEqualTo(3);
                assertThat(version.qualifier()).isEmpty();
            })
            .onFailure(cause -> fail("Should have succeeded: " + cause.message()));
    }

    @Test
    void version_parsing_with_qualifier() {
        Version.version("1.0.0-SNAPSHOT")
            .onSuccess(version -> {
                assertThat(version.major()).isEqualTo(1);
                assertThat(version.minor()).isEqualTo(0);
                assertThat(version.patch()).isEqualTo(0);
                assertThat(version.qualifier()).isEqualTo("-SNAPSHOT");
            })
            .onFailure(cause -> fail("Should have succeeded: " + cause.message()));
    }

    @Test
    void version_parsing_with_complex_qualifier() {
        Version.version("2.1.5-beta1")
            .onSuccess(version -> {
                assertThat(version.major()).isEqualTo(2);
                assertThat(version.minor()).isEqualTo(1);
                assertThat(version.patch()).isEqualTo(5);
                assertThat(version.qualifier()).isEqualTo("-beta1");
            })
            .onFailure(cause -> fail("Should have succeeded: " + cause.message()));
    }

    @Test
    void version_parsing_rejects_invalid_format() {
        Version.version("1.2")
            .onSuccessRun(() -> fail("Should have failed"))
            .onFailure(cause -> assertThat(cause.message()).contains("Invalid version format"));

        Version.version("1.2.3.4")
            .onSuccessRun(() -> fail("Should have failed"))
            .onFailure(cause -> assertThat(cause.message()).contains("Invalid version format"));

        Version.version("")
            .onSuccessRun(() -> fail("Should have failed"))
            .onFailure(cause -> assertThat(cause.message()).contains("Invalid version format"));
    }

    @Test
    void version_parsing_rejects_non_numeric_components() {
        var result1 = Version.version("a.2.3");
        assertThat(result1.isFailure()).isTrue();

        var result2 = Version.version("1.b.3");
        assertThat(result2.isFailure()).isTrue();

        var result3 = Version.version("1.2.c");
        assertThat(result3.isFailure()).isTrue();
    }

    @Test
    void version_parsing_rejects_negative_numbers() {
        var result1 = Version.version("-1.2.3");
        assertThat(result1.isFailure()).isTrue();

        var result2 = Version.version("1.-2.3");
        assertThat(result2.isFailure()).isTrue();

        var result3 = Version.version("1.2.-3");
        assertThat(result3.isFailure()).isTrue();
    }

    @Test
    void version_bare_version_excludes_qualifier() {
        var version = Version.version(2, 3, 4, Option.option("rc1")).unwrap();
        assertThat(version.bareVersion()).isEqualTo("2.3.4");
    }

    @Test
    void version_with_qualifier_includes_qualifier() {
        var version = Version.version(1, 0, 0, Option.option("SNAPSHOT")).unwrap();
        assertThat(version.withQualifier()).isEqualTo("1.0.0-SNAPSHOT");
    }

    @Test
    void version_with_no_qualifier_shows_bare_version() {
        var version = Version.version(1, 2, 3, Option.none()).unwrap();
        assertThat(version.withQualifier()).isEqualTo("1.2.3");
    }

    @Test
    void version_parsing_handles_edge_cases() {
        // Version with dash but no qualifier
        Version.version("1.2.3-")
            .onSuccess(version -> assertThat(version.qualifier()).isEqualTo("-"))
            .onFailure(cause -> fail("Should have succeeded: " + cause.message()));

        // Version with zero components
        Version.version("0.0.0")
            .onSuccess(version -> {
                assertThat(version.major()).isEqualTo(0);
                assertThat(version.minor()).isEqualTo(0);
                assertThat(version.patch()).isEqualTo(0);
            })
            .onFailure(cause -> fail("Should have succeeded: " + cause.message()));
    }
}
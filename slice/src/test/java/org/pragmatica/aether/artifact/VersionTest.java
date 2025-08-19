package org.pragmatica.aether.artifact;

import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Option;

import static org.assertj.core.api.Assertions.assertThat;

class VersionTest {

    @Test
    void version_parsing_with_valid_major_minor_patch() {
        var result = Version.version("1.2.3");
        
        assertThat(result.isSuccess()).isTrue();
        var version = result.unwrap();
        assertThat(version.major()).isEqualTo(1);
        assertThat(version.minor()).isEqualTo(2);
        assertThat(version.patch()).isEqualTo(3);
        assertThat(version.qualifier()).isEmpty();
    }

    @Test
    void version_parsing_with_qualifier() {
        var result = Version.version("1.0.0-SNAPSHOT");
        
        assertThat(result.isSuccess()).isTrue();
        var version = result.unwrap();
        assertThat(version.major()).isEqualTo(1);
        assertThat(version.minor()).isEqualTo(0);
        assertThat(version.patch()).isEqualTo(0);
        assertThat(version.qualifier()).isEqualTo("-SNAPSHOT");
    }

    @Test
    void version_parsing_with_complex_qualifier() {
        var result = Version.version("2.1.5-beta1");
        
        assertThat(result.isSuccess()).isTrue();
        var version = result.unwrap();
        assertThat(version.major()).isEqualTo(2);
        assertThat(version.minor()).isEqualTo(1);
        assertThat(version.patch()).isEqualTo(5);
        assertThat(version.qualifier()).isEqualTo("-beta1");
    }

    @Test
    void version_parsing_rejects_invalid_format() {
        Version.version("1.2")
            .onSuccessRun(() -> org.junit.jupiter.api.Assertions.fail("Should have failed"))
            .onFailure(cause -> assertThat(cause.message()).contains("Invalid version format"));

        Version.version("1.2.3.4")
            .onSuccessRun(() -> org.junit.jupiter.api.Assertions.fail("Should have failed"))
            .onFailure(cause -> assertThat(cause.message()).contains("Invalid version format"));

        Version.version("")
            .onSuccessRun(() -> org.junit.jupiter.api.Assertions.fail("Should have failed"))
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
        var result1 = Version.version("1.2.3-");
        assertThat(result1.isSuccess()).isTrue();
        assertThat(result1.unwrap().qualifier()).isEqualTo("-");

        // Version with zero components
        var result2 = Version.version("0.0.0");
        assertThat(result2.isSuccess()).isTrue();
        var version = result2.unwrap();
        assertThat(version.major()).isEqualTo(0);
        assertThat(version.minor()).isEqualTo(0);
        assertThat(version.patch()).isEqualTo(0);
    }
}
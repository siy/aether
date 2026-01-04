package org.pragmatica.aether.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentTest {

    @Test
    void local_hasCorrectDefaults() {
        var env = Environment.LOCAL;

        assertThat(env.defaultNodes()).isEqualTo(3);
        assertThat(env.defaultHeap()).isEqualTo("256m");
        assertThat(env.defaultTls()).isFalse();
    }

    @Test
    void docker_hasCorrectDefaults() {
        var env = Environment.DOCKER;

        assertThat(env.defaultNodes()).isEqualTo(5);
        assertThat(env.defaultHeap()).isEqualTo("512m");
        assertThat(env.defaultTls()).isFalse();
    }

    @Test
    void kubernetes_hasCorrectDefaults() {
        var env = Environment.KUBERNETES;

        assertThat(env.defaultNodes()).isEqualTo(5);
        assertThat(env.defaultHeap()).isEqualTo("1g");
        assertThat(env.defaultTls()).isTrue();
    }

    @Test
    void fromString_parsesLocalVariants() {
        assertThat(Environment.fromString("local").unwrap()).isEqualTo(Environment.LOCAL);
        assertThat(Environment.fromString("LOCAL").unwrap()).isEqualTo(Environment.LOCAL);
        assertThat(Environment.fromString("Local").unwrap()).isEqualTo(Environment.LOCAL);
    }

    @Test
    void fromString_parsesDockerVariants() {
        assertThat(Environment.fromString("docker").unwrap()).isEqualTo(Environment.DOCKER);
        assertThat(Environment.fromString("DOCKER").unwrap()).isEqualTo(Environment.DOCKER);
        assertThat(Environment.fromString("Docker").unwrap()).isEqualTo(Environment.DOCKER);
    }

    @Test
    void fromString_parsesKubernetesVariants() {
        assertThat(Environment.fromString("kubernetes").unwrap()).isEqualTo(Environment.KUBERNETES);
        assertThat(Environment.fromString("KUBERNETES").unwrap()).isEqualTo(Environment.KUBERNETES);
        assertThat(Environment.fromString("k8s").unwrap()).isEqualTo(Environment.KUBERNETES);
        assertThat(Environment.fromString("K8S").unwrap()).isEqualTo(Environment.KUBERNETES);
    }

    @Test
    void fromString_defaultsToDockerForBlank() {
        assertThat(Environment.fromString("").unwrap()).isEqualTo(Environment.DOCKER);
        assertThat(Environment.fromString(null).unwrap()).isEqualTo(Environment.DOCKER);
        assertThat(Environment.fromString("  ").unwrap()).isEqualTo(Environment.DOCKER);
    }

    @Test
    void fromString_failsForUnknown() {
        Environment.fromString("unknown")
            .onSuccessRun(org.junit.jupiter.api.Assertions::fail)
            .onFailure(cause -> assertThat(cause.message()).contains("Unknown environment"));
    }

    @Test
    void displayName_returnsLowercaseName() {
        assertThat(Environment.LOCAL.displayName()).isEqualTo("local");
        assertThat(Environment.DOCKER.displayName()).isEqualTo("docker");
        assertThat(Environment.KUBERNETES.displayName()).isEqualTo("kubernetes");
    }
}

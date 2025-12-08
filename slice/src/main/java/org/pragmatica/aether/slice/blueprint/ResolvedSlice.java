package org.pragmatica.aether.slice.blueprint;

import org.pragmatica.aether.artifact.Artifact;

public record ResolvedSlice(Artifact artifact, int instances, boolean isDependency) {
    public static ResolvedSlice resolvedSlice(Artifact artifact, int instances, boolean isDependency) {
        return new ResolvedSlice(artifact, instances, isDependency);
    }
}
